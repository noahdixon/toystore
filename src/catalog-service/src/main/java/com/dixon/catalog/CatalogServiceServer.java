package com.dixon.catalog;

import com.dixon.*;
import com.dixon.common.Address;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Server that handles query requests from a gateway server,
 * and buy requests from an order service server
 */
public class CatalogServiceServer {
    /**
     * gRPC Server instance
     */
    private Server server;

    /**
     * ProductCatalog variable that holds the information of all the products
     */
    private static ProductCatalog productCatalog;

    /**
     * Boolean to indicate whether test mode is activated to show response before sending
     */
    private static boolean testMode;

    /**
     * Starts the server
     * @param port Port that the server will listen on
     * @param maxThreads Maximum number of threads in the dynamic thread pool
     * @throws IOException
     */
    private void start(int port, int maxThreads) throws IOException {

        // Build server with dynamic pool
        server = ServerBuilder.forPort(port)
                .addService(new CatalogServiceImpl(testMode))
                .executor(new ThreadPoolExecutor(0, maxThreads,
                        60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()))
                .build()
                .start();
        System.out.println("Catalog service started, listening on " + port);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down catalog service server since JVM is shutting down");

                // When the application is shutting down, write the final inventory into the file
                try {
                    productCatalog.persistToFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // stop the server
                try {
                    CatalogServiceServer.this.stop();
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
        options.addOption("f", "filePath", true, "file path to the inventory.csv file");
        options.addOption("ut", "updateTime", true, "frequency in seconds of db writes to disk");
        options.addOption("rt", "restockTime", true, "frequency in seconds of restocks");
        options.addOption("te", "test", false, "testing mode activated");
        options.addOption("fs", "gatewayServer", true, "gateway server address");
        options.addOption("fp", "gatewayPort", true, "gateway server port");
        options.addOption("ec", "enableCache", false, "enables sending cache invalidation");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Parse command line arguments
        int port = Integer.parseInt(cmd.getOptionValue("port", "1765"));
        int maxThreads = Integer.parseInt(cmd.getOptionValue("maxThreads", "50"));
        int updateTime = Integer.parseInt(cmd.getOptionValue("updateTime", "600"));
        int restockTime = Integer.parseInt(cmd.getOptionValue("restockTime", "10"));
        InetAddress gatewayServerName = InetAddress.getByName(cmd.getOptionValue("gatewayServer", "localhost"));
        int gatewayPort = Integer.parseInt(cmd.getOptionValue("gatewayPort", "1764"));
        testMode = cmd.hasOption("te");
        boolean isCacheEnabled = cmd.hasOption("ec");

        // Define gateway address
        Address gatewayAddress = Address.builder().host(gatewayServerName.getHostName()).port(gatewayPort).build();

        // Check if this is being run in docker by reading the DOCKER_RUN env variable
        boolean dockerRun = false;
        String dockerEnv = System.getenv("DOCKER_RUN");
        if(dockerEnv != null) {
            dockerRun = Boolean.parseBoolean(dockerEnv);
        }

        // Decide the file to read, based on location of the program run
        String catalogFilePath;
        if(!dockerRun) {
            catalogFilePath = cmd.getOptionValue("filePath", "catalog-service/src/main/resources/inventory.csv");
        } else {
            catalogFilePath = cmd.getOptionValue("filePath", "/data/inventory.csv");
        }

        // Initialize the product catalog
        productCatalog = new ProductCatalog(catalogFilePath, updateTime, restockTime, gatewayAddress, isCacheEnabled);

        // Create and start server
        final CatalogServiceServer server = new CatalogServiceServer();
        server.start(port, maxThreads);
        server.blockUntilShutdown();
    }

    /**
     * Implementation of CatalogService gRPC Service
     */
    static class CatalogServiceImpl extends CatalogServiceGrpc.CatalogServiceImplBase {

        /**
         * Boolean to indicate whether test mode is activated to show response before sending
         */
        private final boolean testMode;

        /**
         * Initializes the catalog service
         * @param testMode The testing mode for printing reply objects
         */
        public CatalogServiceImpl(boolean testMode) {
            super();
            this.testMode = testMode;
        }

        /**
         * Query's the catalog for item cost and stock
         * @param req Client request
         * @param responseObserver Response observer
         */
        @Override
        public void query(CatalogQueryRequest req, StreamObserver<CatalogQueryResponse> responseObserver) {
            // Query catalog
            CatalogQueryResponse reply = productCatalog.query(req);

            // Optionally print response object
            if (testMode) {
                System.out.println("Query response object:");
                System.out.println(reply);
            }

            // Send response
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        /**
         * Changes an item stock from the catalog
         * @param req Client request
         * @param responseObserver Response observer
         */
        @Override
        public void changeStock(CatalogChangeRequest req, StreamObserver<CatalogChangeResponse> responseObserver) {
            // Attempt change from catalog
            CatalogChangeResponse reply = productCatalog.changeItem(req);

            // Optionally print response object
            if (testMode) {
                System.out.println("Buy response object:");
                System.out.println(reply);
            }

            // Send response
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
