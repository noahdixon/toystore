package com.dixon.frontend;

import com.dixon.CatalogQueryRequest;
import com.dixon.CatalogQueryResponse;
import com.dixon.CatalogServiceGrpc;
import com.dixon.common.Error;
import com.dixon.common.ErrorWrapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles query requests from a client
 */
public class ProductsHandler implements HttpHandler {
    /**
     * String representation of GET method
     */
    protected static final String GET_METHOD = "GET";

    /**
     * Object mapper used for marshaling and unmarshalling
     */
    protected final ObjectMapper objectMapper;

    /**
     * gRPC stub to make rpc calls the catalog service
     */
    protected final CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;

    /**
     * Boolean to indicate whether test mode is activated to show response before sending
     */
    protected static boolean testMode;

    /**
     * Instantiates a ProductsHandler instance, obtaining a gRPC stub to the catalog service at host:port
     * @param host The host address of the catalog service
     * @param port The port address of the catalog service
     * @param testMode Indicates whether the handler should show testing mode output
     */
    public ProductsHandler(String host, int port, boolean testMode) {
        super();
        // Create object mapper
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        this.objectMapper = new ObjectMapper(jsonFactory);

        // Create a communication channel to the server and get stub
        String target = host + ":" + port;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        catalogStub = CatalogServiceGrpc.newBlockingStub(channel);

        // Set test mode
        this.testMode = testMode;
    }

    /**
     * Handles query requests
     * @param exchange The http exchange object
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check that http method is GET
        if(!exchange.getRequestMethod().equals(GET_METHOD)) {
            HTTPHandlerUtility.handleUnknownMethodResponse(exchange, objectMapper);
            return;
        }

        // Get product name
        String productName = HTTPHandlerUtility.getProductName(exchange);

        // Query catalog service
        CatalogQueryRequest request = CatalogQueryRequest.newBuilder().setName(productName).build();
        CatalogQueryResponse response = catalogStub.query(request);

        // Send error data back to client if error occurred
        if (!response.getSuccess()) {
            Error error = Error.builder().code(404).message(response.getErrorMessage()).build();
            handleErrorResponse(error, exchange);
            return;
        }

        // Get data from response
        Data data = Data.builder().name(response.getName()).price(response.getPrice()).quantity(response.getQuantity()).build();

        // Send response data back to client
        handleDataResponse(data, exchange);
    }

    /**
     * Sends data object back to client
     * @param data The data object containing product name, price, and quantity
     * @param exchange The http exchange object
     * @throws IOException
     */
    protected void handleDataResponse(Data data, HttpExchange exchange) throws IOException {
        // Build data object response
        String reply = objectMapper.writeValueAsString(DataWrapper.builder().data(data).build());
        exchange.sendResponseHeaders(200, 0);

        // Send response
        sendResponse(exchange, reply);
    }

    /**
     * Sends error object back to client
     * @param error The error object
     * @param exchange The http exchange object
     * @throws IOException
     */
    protected void handleErrorResponse(Error error, HttpExchange exchange) throws IOException {
        // Build error object response
        String reply = objectMapper.writeValueAsString(ErrorWrapper.builder().error(error).build());
        exchange.sendResponseHeaders(404, 0);

        // Send response
        sendResponse(exchange, reply);
    }

    /**
     * Sends response back to client
     * @param exchange The http exchange object
     * @param reply The response object as a string
     * @throws IOException
     */
    protected void sendResponse(HttpExchange exchange, String reply) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        // Optionally print response object
        if(testMode) {
            System.out.println("Query response object: ");
            System.out.println(reply);
            System.out.println();
        }

        // Send response
        outputStream.write(reply.getBytes());
        outputStream.flush();
        outputStream.close();
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
}
