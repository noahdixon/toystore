package com.dixon.order;

import com.dixon.*;
import com.dixon.common.Address;
import com.dixon.common.OrderServiceNodesReader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Server that handles buy requests from a frontend server
 */
public class OrderServiceServer {
    /**
     * gRPC Server instance
     */
    private Server server;

    /**
     * The ID of this order service instance
     */
    private static int id;

    /**
     * OrderLogDb to store and retrieve the order logs
     */
    private static OrderLogDb orderLogDb;

    /**
     * gRPC stub to make rpc calls the Catalog service
     */
    private final CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;

    /**
     * Generator to create incrementing order numbers
     */
    private final OrderNumberGenerator orderNumberGenerator;

    /**
     * Boolean to indicate whether test mode is activated to show response before sending
     */
    private static boolean testMode;

    /**
     * Replica manager to track other order service instances
     */
    private static ReplicaManager replicaManager;

    /**
     * Instantiates a new order service server instance
     * @param catalogHost The catalog service host
     * @param catalogPort The catalog service port
     * @param orderLogFilePath The file path to the order log database file
     * @param dockerMode Indicates whether the service is being run using Docker or not
     */
    private OrderServiceServer(String catalogHost, int catalogPort, String orderLogFilePath, boolean dockerMode) {
        // Create a communication channel to catalog server and get stub
        String target = catalogHost + ":" + catalogPort;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        catalogStub = CatalogServiceGrpc.newBlockingStub(channel);
        orderLogDb = new OrderLogDb(orderLogFilePath);

        // Read the last order number from the log book
        int lastOrderNumber = orderLogDb.getMaxOrderNumber();

        // Create order number generator
        orderNumberGenerator = new OrderNumberGenerator(lastOrderNumber+1);

        // Read order nodes addresses from the conf file
        HashMap<Integer, Address> addressHashMap = OrderServiceNodesReader.readOrderNodes(dockerMode);

        // Map order node addresses to OrderNode objects
        HashMap<Integer, OrderNode> orderNodeHashMap = convertAddressToNodeMap(addressHashMap);

        // Create replica manager
        replicaManager = new ReplicaManager(id, orderNodeHashMap);

        // Sync database with peer nodes
        new Thread(() -> {
            syncFromOtherPeers(lastOrderNumber);
        }).start();
    }

    /**
     * Converts a map of order service node Addresses to order service OrderNodes
     * @param addressHashMap A mapping of order service node ids to Address objects
     * @return A mapping of order service node ids to OrderNode objects
     */
    private HashMap<Integer, OrderNode> convertAddressToNodeMap(HashMap<Integer, Address> addressHashMap) {

        HashMap<Integer, OrderNode> orderNodeHashMap = new HashMap<>(addressHashMap.size());

        // Iterate over map and add new OrderNode objects as values in new map
        for (Map.Entry<Integer, Address> entry : addressHashMap.entrySet()) {
            orderNodeHashMap.put(entry.getKey(), new OrderNode(entry.getKey(), entry.getValue()));
        }

        return orderNodeHashMap;
    }

    /**
     * Queries other active order service nodes for orders higher than the current max order number,
     * and adds all received orders to database
     * @param current_max_order_number
     */
    private void syncFromOtherPeers(int current_max_order_number) {
        // On the active hosts, run this sync. If none of them are active, then
        // no need to sync and return.
        List<OrderNode> activeOrderNodePeers = replicaManager.getActivePeerNodes();
        if (activeOrderNodePeers.isEmpty()) return;

        // Get orders from all other nodes after current max order number
        List<NodeAndListenableFuture> ordersDataFutures = activeOrderNodePeers.stream()
                .map(peerNode -> new NodeAndListenableFuture(peerNode,
                                peerNode.getFutureStub()
                                        .fetchAllOrdersFrom(FetchAllOrdersFromRequest.newBuilder()
                                        .setRequesterId(replicaManager.getSelfId())
                                        .setAfterOrderNumber(current_max_order_number)
                                        .build()), null))
                .collect(Collectors.toList());

        // Extract distinct order entries from collected responses
        List<OrderRecord> allOrdersResponses = ordersDataFutures.stream()
                .map(this::getFetchAllOrdersResponseOrEmpty)
                .flatMap(response -> response.getOrdersList().stream())
                .distinct() // Remove duplicate orders
                .collect(Collectors.toList());
        if(allOrdersResponses.isEmpty()) return;

        // Insert records into db and update max order number
        orderLogDb.insertOrderRecords(allOrdersResponses);
        orderNumberGenerator.updateMaxOrderNumber(orderLogDb.getMaxOrderNumber());
    }

    /**
     * Retrieves a FetchAllOrdersResponse from the ListenableFuture embedded in a NodeAndListenableFuture
     * @param future a NodeAndListenableFuture object containing a ListenableFuture
     * @return the ListenableFuture from the NodeAndListenableFuture
     */
    private FetchAllOrdersResponse getFetchAllOrdersResponseOrEmpty(NodeAndListenableFuture future) {
        try {
            FetchAllOrdersResponse response = future.getFetchAllOrdersFuture().get();
            System.out.println("Received records " +
                    response.getOrdersList().size() + " from " +
                    future.getPeerNode().getId());
            return response;
        } catch (Exception e) {
            // Catching the exception
            System.out.println("Exception while contacting peer " + future.getPeerNode().getId());
            replicaManager.changeNodeStatus(future.getPeerNode().getId(), OrderNodeStatus.OFFLINE);
            // Return an empty response
            return FetchAllOrdersResponse.newBuilder().build();
        }
    }

    /**
     * Starts the server
     * @param port Port that the server will listen on
     * @param maxThreads Maximum number of threads in the dynamic thread pool
     * @throws IOException
     */
    private void start(int port, int maxThreads) throws IOException {

        // Build server with dynamic pool
        server = ServerBuilder.forPort(port)
                .addService(new OrderServiceImpl(catalogStub, orderNumberGenerator, testMode))
                .executor(new ThreadPoolExecutor(0, maxThreads,
                        60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()))
                .build()
                .start();
        System.out.println("Order service started, listening on " + port);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down order service server since JVM is shutting down");

                // stop the server
                try {
                    OrderServiceServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /**
     * Stops the server
     * @throws InterruptedException
     */
    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Awaits termination on main thread since the grpc library uses daemon threads
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        // Get command line arguments
        Options options = new Options();
        options.addOption("p", "port", true, "port");
        options.addOption("m", "maxThreads", true, "maximum number of threads in dynamic pool");
        options.addOption("ch", "catalogHost", true, "catalog service server address");
        options.addOption("cp", "catalogPort", true, "catalog service server port");
        options.addOption("f", "filePath", true, "file path to read the product catalog information");
        options.addOption("t", "updateTime", true, "time in seconds to update the db file on disk");
        options.addOption("te", "test", false, "testing mode activated");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Parse command line arguments
        int port = Integer.parseInt(cmd.getOptionValue("port", "1766"));
        int maxThreads = Integer.parseInt(cmd.getOptionValue("maxThreads", "50"));
        String catalogHost = cmd.getOptionValue("catalogHost", "0.0.0.0");
        int catalogPort = Integer.parseInt(cmd.getOptionValue("catalogPort", "1765"));
        int updateTime = Integer.parseInt(cmd.getOptionValue("updateTime", "600"));
        testMode = cmd.hasOption("te");

        // Check if this is being run in docker by reading the DOCKER_RUN env variable
        boolean dockerRun = false;
        String dockerEnv = System.getenv("DOCKER_RUN");
        if(dockerEnv != null) {
            dockerRun = Boolean.parseBoolean(dockerEnv);
        }

        // Read the self ID
        if (System.getenv("SELF_ID") != null) {
            id = Integer.parseInt(System.getenv("SELF_ID"));
        } else id = 1;


        // Decide the file to write orderlog, based on location of the program run
        String orderLogFilePath;
        if(!dockerRun) {
            orderLogFilePath = cmd.getOptionValue("filePath", "src/order-service/src/main/resources/orderlog_"+ id + ".db");
        } else {
            orderLogFilePath = cmd.getOptionValue("filePath", "/data/orderlog_"+ id + ".db");
        }

        // Read the dependent Catalog service environment variables
        String catalogHostFromEnv = System.getenv("CATALOG_HOST");
        if(catalogHostFromEnv!= null) {
            catalogHost = catalogHostFromEnv;
        }

        // Create and start server
        final OrderServiceServer server = new OrderServiceServer(catalogHost, catalogPort, orderLogFilePath, dockerRun);
        server.start(port, maxThreads);
        server.blockUntilShutdown();
    }

    /**
     * Implementation of OrderService gRPC Service
     */
    static class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

        /**
         * gRPC stub to make rpc calls the Catalog service
         */
        private final CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;

        /**
         * Generator to generate order numbers
         */
        private final OrderNumberGenerator orderNumberGenerator;

        /**
         * Boolean to indicate whether test mode is activated to show response before sending
         */
        private final boolean testMode;

        /**
         * Initializes the order service with a stub to the catalog and an order number generator
         * @param catalogStub The catalog stub
         * @param orderNumberGenerator The order number generator
         */

        /**
         * Initializes the order service with a stub to the catalog and an order number generator
         * @param catalogStub The catalog stub
         * @param orderNumberGenerator The order number generator
         * @param testMode The testing mode for printing reply objects
         */
        private OrderServiceImpl (CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub,
                                  OrderNumberGenerator orderNumberGenerator,
                                  boolean testMode) {
            super();
            this.catalogStub = catalogStub;
            this.orderNumberGenerator = orderNumberGenerator;
            this.testMode = testMode;
        }

        /**
         * Purchases an item from the catalog, records the order number
         * and propagates new order entries to other active order service nodes
         * @param req Client request
         * @param responseObserver Response observer
         */
        @Override
        public void buy(OrderBuyRequest req, StreamObserver<OrderBuyResponse> responseObserver) {
            CatalogChangeRequest catalogReq = CatalogChangeRequest.newBuilder()
                                                            .setIsIncrement(false)
                                                            .setName(req.getName())
                                                            .setQuantity(req.getQuantity())
                                                            .build();

            // Make gRPC
            CatalogChangeResponse catalogReply = catalogStub.changeStock(catalogReq);

            OrderBuyResponse reply = null;
            // Check buy success
            if(catalogReply.getSuccess()) {
                // Generate an order number for this transaction
                int generatedOrderNumber = orderNumberGenerator.getOrderNumber();

                // Create reply
                reply = OrderBuyResponse.newBuilder()
                                        .setSuccess(catalogReply.getSuccess())
                                        .setOrderNumber(generatedOrderNumber)
                                        .build();

                // Create an order log object and then add the order record to the db
                OrderRecord orderRecord = OrderRecord.newBuilder()
                        .setName(req.getName())
                        .setOrderNumber(generatedOrderNumber)
                        .setQuantity(req.getQuantity())
                        .build();
                orderLogDb.insertOrderRecord(orderRecord);

                // Optionally print response object
                if (testMode) {
                    System.out.println("Buy response object:");
                    System.out.println(reply);
                }

                responseObserver.onNext(reply);
                responseObserver.onCompleted();

                // Send order to followers
                if (testMode) {
                    System.out.println("Sending order number: " + generatedOrderNumber + " to replicas");
                }
                List<OrderNode> activePeerNodes = replicaManager.getActivePeerNodes();
                if (activePeerNodes.isEmpty()) return;
                List<NodeAndListenableFuture> ordersDataFutures = activePeerNodes.stream()
                        .map(peerNode ->
                                new NodeAndListenableFuture(peerNode, null,
                                        peerNode.getFutureStub()
                                                .acceptOrdersFromLeader(
                                                    AcceptOrdersRequest.newBuilder()
                                                        .setOrder(orderRecord)
                                                        .setRequesterId(replicaManager.getSelfId())
                                                        .build())))
                        // check that nodes accepted new orders
                        .collect(Collectors.toList());
                ordersDataFutures.forEach(this::completeFuturesForFollowers);
            } else {
                // Create failed order reply
                reply = OrderBuyResponse.newBuilder()
                                        .setSuccess(catalogReply.getSuccess())
                                        .setErrorMessage(catalogReply.getErrorMessage())
                                        .build();

                // Optionally print response object
                if (testMode) {
                    System.out.println("Buy response object:");
                    System.out.println(reply);
                }

                // Send reply
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }

        /**
         * Gets the AcceptOrdersResponse from a peer order service node,
         * or recognizes the node is offline and changes the nodes' status in the replica manager
         * @param future
         * @return
         */
        private AcceptOrdersResponse completeFuturesForFollowers(NodeAndListenableFuture future) {
            try {
                return future.getAcceptOrdersFuture().get();
            } catch (Exception e) {
                // Catching the exception
                System.out.println("Exception while contacting peer " + future.getPeerNode().getId());
                replicaManager.changeNodeStatus(future.getPeerNode().getId(), OrderNodeStatus.OFFLINE);

                // Return an empty response
                return AcceptOrdersResponse.newBuilder().build();
            }
        }

        /**
         * Queries an already placed order with order number
         * @param req Client request consisting of the Order Number
         * @param responseObserver Response observer
         */
        @Override
        public void queryOrderNumber(OrderQueryRequest req, StreamObserver<OrderQueryResponse> responseObserver) {

            OrderQueryResponse reply;
            OrderRecord orderRecord = orderLogDb.getOrderByNumber(req.getOrderNumber());
            if (orderRecord != null) {
                reply = OrderQueryResponse.newBuilder()
                            .setSuccess(true)
                            .setOrder(orderRecord)
                            .build();
            } else {
                reply = OrderQueryResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorMessage("No records found with the given order number")
                        .build();
            }

            // Optionally print response object
            if (testMode) {
                System.out.println("Query order number response object:");
                System.out.println(reply);
            }

            // Send reply
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        /**
         * Checks if an order service server node is online
         * @param req The request object asking if the node is online
         * @param responseObserver Response observer
         */
        @Override
        public void checkHealth(HealthRequest req, StreamObserver<HealthResponse> responseObserver) {
            HealthResponse reply = HealthResponse.newBuilder()
                    .setMessage("I am online!")
                    .build();

            // Optionally print response object
            if (testMode) {
                System.out.println("Health check response object:");
                System.out.println(reply);
            }

            // Send reply
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        /**
         * Fetches all orders after a given order number
         * @param req Client request consisting of the Order Number
         * @param responseObserver Response observer
         */
        @Override
        public void fetchAllOrdersFrom(FetchAllOrdersFromRequest req,
                                       StreamObserver<FetchAllOrdersResponse> responseObserver) {

            System.out.println("Received all orders request from " + req.getRequesterId());

            // Make the node active if a fetch request is received from a node
            replicaManager.changeNodeStatus(req.getRequesterId(), OrderNodeStatus.ACTIVE);

            // Retrieve all records after requested order number
            List<OrderRecord> orderRecords =
                    orderLogDb.getOrdersAfterOrderNumber(req.getAfterOrderNumber());

            // Build response
            FetchAllOrdersResponse reply = FetchAllOrdersResponse.newBuilder()
                    .addAllOrders(orderRecords)
                    .setAfterOrderNumber(req.getAfterOrderNumber())
                    .build();

            System.out.println("Sent out " + orderRecords.size() + " Records");
            // Optionally print response object
            if (testMode) {
                System.out.println("Order record objects contained in fetch all orders from response object:");
                for (OrderRecord orderRecord : orderRecords) {
                    System.out.println(orderRecord);
                }
            }

            // Send reply
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        /**
         * Assigns a new order service leader node
         * @param req Request object containing the new leader node id
         * @param responseObserver Response observer
         */
        @Override
        public void assignLeader(LeaderAssignment req, StreamObserver<LeaderAssignmentResponse> responseObserver) {
            // get leader ID and assign that node leader in the replica manager
            int leaderId = req.getId();
            replicaManager.assignLeader(leaderId);

            // Build response
            LeaderAssignmentResponse reply;
            if(replicaManager.getSelfId() == leaderId) {
                reply = LeaderAssignmentResponse.newBuilder()
                        .setMessage("Received the leader id: " + leaderId + ", I am the leader!")
                        .build();
            } else {
                reply = LeaderAssignmentResponse.newBuilder()
                        .setMessage("Received the leader id: " + leaderId)
                        .build();
            }

            // Optionally print response object
            if (testMode) {
                System.out.println("Assign leader response object:");
                System.out.println(reply);
            }

            // Send reply
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            if(replicaManager.getSelfId() == leaderId) {
                orderNumberGenerator.updateMaxOrderNumber(orderLogDb.getMaxOrderNumber());
            }
        }

        /**
         * Accept the orders from the leader node and insert into the database
         * @param req Request consisting of order record
         * @param responseObserver Response observer
         */
        @Override
        public void acceptOrdersFromLeader(AcceptOrdersRequest req,
                                       StreamObserver<AcceptOrdersResponse> responseObserver) {
            // insert the record from the leader
            int insertedRows = orderLogDb.insertOrderRecord(req.getOrder());
            AcceptOrdersResponse reply = AcceptOrdersResponse.newBuilder()
                    .setSuccess(insertedRows == 1)
                    .build();

            // Optionally print response object
            if (testMode) {
                System.out.println("Accept orders from leader response object:");
                System.out.println(reply);
            }

            // Send reply
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

    }
}
