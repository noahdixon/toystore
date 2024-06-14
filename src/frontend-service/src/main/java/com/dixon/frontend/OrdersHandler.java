package com.dixon.frontend;

import com.dixon.*;
import com.dixon.common.Address;
import com.dixon.common.Error;
import com.dixon.common.ErrorWrapper;
import com.dixon.common.Order;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles order requests from a client
 */
public class OrdersHandler implements HttpHandler {
    /**
     * String representation of GET method
     */
    private static final String GET_METHOD = "GET";

    /**
     * String representation of POST method
     */
    private static final String POST_METHOD = "POST";

    /**
     * Object mapper used for marshaling and unmarshalling
     */
    private final ObjectMapper objectMapper;

    /**
     * gRPC stub to make rpc calls the order service
     */
    private OrderServiceGrpc.OrderServiceBlockingStub orderStub;

    /**
     * Boolean to indicate whether test mode is activated to show response before sending
     */
    private static boolean testMode;

    /**
     * Stores the id's of the order service instances
     */
    private static int[] orderIds;

    /**
     * Stores the total id's of the order service instances
     */
    private static int totalIds;

    /**
     * Maps the id's of the order service instances to their addresses
     */
    private static HashMap<Integer, Address> orderAddresses;

    /**
     * Determines the amount of time before an order service health check will timeout
     */
    private static long timeoutSeconds;

    /**
     * Stores the id of the current leader
     */
    private static int currentOrderServiceId = -1;

    /**
     * Handles order requests from a client
     */
    public OrdersHandler(long timeoutSeconds, HashMap<Integer, Address> orderAddresses, boolean testMode) {
        super();
        // Create object mapper
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        this.objectMapper = new ObjectMapper(jsonFactory);

        this.timeoutSeconds = timeoutSeconds;
        this.orderAddresses = orderAddresses;

        List<Integer> idList = new ArrayList<>(orderAddresses.keySet());
        // Sort the list in descending order
        Collections.sort(idList, Collections.reverseOrder());
        int[] orderIds = new int[idList.size()];

        // Convert List<Integer> to int[]
        for (int i = 0; i < idList.size(); i++) {
            orderIds[i] = idList.get(i);
        }

        this.orderIds = orderIds;
        this.totalIds = orderIds.length;

        // Set test mode
        this.testMode = testMode;

        // Elect first leader
        electLeader(-1);
    }

    /**
     * Handles order requests
     * @param exchange The http exchange object
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        switch (exchange.getRequestMethod()) {
            case GET_METHOD:
                handleGetOrderRequest(exchange);
                break;
            case POST_METHOD:
                handleBuyOrderRequest(exchange);
                break;
            default:
                HTTPHandlerUtility.handleUnknownMethodResponse(exchange, objectMapper);
                break;
        }
    }

    /**
     * Handles get order requests by sending an OrderQueryRequest to order service leader
     * and responding to client. If order number is invalid returns error. If leader
     * is unresponsive, triggers leader election and sends request to new leader.
     * @param exchange The http exchange object
     * @throws IOException
     */
    private void handleGetOrderRequest(HttpExchange exchange) throws IOException {

        // Query orderService with the order number
        int orderNumber = -1;
        try {
            orderNumber = getOrderNumber(exchange);
        } catch (NumberFormatException e) {
            // reply with unrecognized order number format
            handleQueryOrderResponse(OrderQueryResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setErrorMessage("Invalid order number format")
                                        .build(), exchange);
            return;
        }
        OrderQueryRequest request = OrderQueryRequest.newBuilder().setOrderNumber(orderNumber).build();

        // Loop on request until leader order service responds
        // If it doesn't respond, elect new leader
        while (true) {
            int leaderId = currentOrderServiceId;
            try {
                OrderQueryResponse response = orderStub.queryOrderNumber(request);
                // Send response data or error back to client
                handleQueryOrderResponse(response, exchange);
                break;
            } catch (StatusRuntimeException e) {
                electLeader(leaderId);
            }
        }

    }

    /**
     * Extracts order number from the request URI
     * @param exchange The http exchange object
     * @return int representation of order number
     */
    private int getOrderNumber(HttpExchange exchange) {
        String requestURI = exchange.getRequestURI().toString();
        String[] uriParts = requestURI.split("/");
        int orderNumber = 0;
        orderNumber = Integer.parseInt(uriParts[2]);
        return orderNumber;
    }

    /**
     * Handles buy requests by sending an OrderBuyRequest to leader and responding to client.
     *  If leader is unresponsive, triggers leader election and sends request to new leader.
     * @param exchange The http exchange object
     * @throws IOException
     */
    private void handleBuyOrderRequest(HttpExchange exchange) throws IOException {
        // Order product
        Order order = objectMapper.readValue(exchange.getRequestBody(), Order.class);
        OrderBuyRequest request = OrderBuyRequest.newBuilder()
                .setName(order.getName())
                .setQuantity(order.getQuantity())
                .build();

        // Loop on request until leader order service responds
        // If it doesn't respond, elect new leader
        while (true) {
            int leaderId = currentOrderServiceId;
            try {
                OrderBuyResponse response = orderStub.buy(request);
                // Send response data or error back to client
                handleBuyOrderResponse(response, exchange);
                break;
            } catch (StatusRuntimeException e) {
                electLeader(leaderId);
            }
        }


    }

    /**
     * Sends data object back to client if order is successful,
     * else sends error object
     * @param response The response object from the order service
     * @param exchange The http exchange object
     * @throws IOException
     */
    private void handleBuyOrderResponse(OrderBuyResponse response, HttpExchange exchange) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        // Build reply object based on success
        String reply;
        if (response.getSuccess()) {
            // Build data object response
            Data data = Data.builder().orderNumber(response.getOrderNumber()).build();
            reply = objectMapper.writeValueAsString(DataWrapper.builder().data(data).build());
            exchange.sendResponseHeaders(200, 0);
        } else {
            // Build error object response
            Error error = Error.builder().code(404).message(response.getErrorMessage()).build();
            reply = objectMapper.writeValueAsString(ErrorWrapper.builder().error(error).build());
            exchange.sendResponseHeaders(404, 0);
        }

        // Optionally print response object
        if(testMode) {
            System.out.println("Order response object: ");
            System.out.println(reply);
            System.out.println();
        }

        // Send response
        outputStream.write(reply.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Sends data object back to client if order query is successful,
     * else sends error object
     * @param response The response object from the order service
     * @param exchange The http exchange object
     * @throws IOException
     */
    private void handleQueryOrderResponse(OrderQueryResponse response, HttpExchange exchange) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        // Build reply object based on success
        String reply;
        if (response.getSuccess()) {
            // Get the order record from the response
            OrderRecord orderRecord = response.getOrder();

            // Build data object response
            OrderData data = OrderData.builder()
                                    .number(orderRecord.getOrderNumber())
                                    .name(orderRecord.getName())
                                    .quantity(orderRecord.getQuantity())
                                    .build();
            reply = objectMapper.writeValueAsString(OrderDataWrapper.builder().data(data).build());
            exchange.sendResponseHeaders(200, 0);
        } else {
            // Build error object response
            Error error = Error.builder().code(404).message(response.getErrorMessage()).build();
            reply = objectMapper.writeValueAsString(ErrorWrapper.builder().error(error).build());
            exchange.sendResponseHeaders(404, 0);
        }

        // Optionally print response object
        if(testMode) {
            System.out.println("Query Order response object: ");
            System.out.println(reply);
            System.out.println();
        }

        // Send response
        outputStream.write(reply.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Elects a new leader order service node
     * @param offlineLeaderId The previous leader that is unresponsive,
     *                       -1 indicates that a leader has not been elected yet
     */
    private synchronized void electLeader(int offlineLeaderId) {
        // handle elections triggered while an election was already in place
        if (currentOrderServiceId != offlineLeaderId) {
            return;
        }
        System.out.println("Electing a new leader with current leader id " + offlineLeaderId);

        HealthRequest req = HealthRequest.newBuilder().setMessage("Are you online?").build();

        int idIndex = 0;
        Address leaderAddress = null;
        OrderServiceGrpc.OrderServiceBlockingStub stub = null;

        while (idIndex < totalIds) {
            leaderAddress = orderAddresses.get(orderIds[idIndex]);
            String target = leaderAddress.toString();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            stub = OrderServiceGrpc.newBlockingStub(channel);

            try {
                HealthResponse response = stub.withWaitForReady().withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS).checkHealth(req);
                break;
            } catch (StatusRuntimeException e) {}
            idIndex++;
        }

        // Check if no services responded
        if (idIndex == totalIds) {
            System.out.println("No active order service found.");
            System.out.println("Exiting gracefully.");
            System.exit(0);
            return;
        }

        orderStub = stub;
        currentOrderServiceId = orderIds[idIndex];

        System.out.println("Elected a new leader id - " + orderIds[idIndex]);

        // Create leader assignment
        LeaderAssignment assignment = LeaderAssignment.newBuilder()
                .setId(orderIds[idIndex])
                .setHost(leaderAddress.getHost())
                .setPort(leaderAddress.getPort())
                .build();

        // Notify online nodes of assignment
        while (idIndex < totalIds) {
            try {
                LeaderAssignmentResponse response = stub.assignLeader(assignment);
            } catch (StatusRuntimeException e) {}
            idIndex++;
            if (idIndex < totalIds) {
                leaderAddress = orderAddresses.get(orderIds[idIndex]);
                String target = leaderAddress.toString();
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                stub = OrderServiceGrpc.newBlockingStub(channel);
            }
        }
    }

    /**
     * Data to be sent to client upon successful order
     */
    @lombok.Data
    @Builder
    @Jacksonized
    private static class Data {
        /**
         * Order number of order
         */
        private int orderNumber;
    }

    /**
     * Wrapper for data object for serialization
     */
    @lombok.Data
    @Builder
    @Jacksonized
    private static class DataWrapper {
        /**
         * Wrapped data
         */
        private Data data;
    }


    /**
     * Data to be sent to client upon successful order
     */
    @lombok.Data
    @Builder
    @Jacksonized
    private static class OrderData {
        /**
         * Order number of order
         */
        private int number;

        /**
         * Product name of order
         */
        private String name;

        /**
         * Quantity of items in order
         */
        private int quantity;
    }

    /**
     * Wrapper for data object for serialization
     */
    @lombok.Data
    @Builder
    @Jacksonized
    private static class OrderDataWrapper {
        /**
         * Wrapped data
         */
        private OrderData data;
    }
}
