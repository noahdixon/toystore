package com.dixon.gateway;

import com.dixon.CatalogQueryRequest;
import com.dixon.CatalogQueryResponse;
import com.dixon.common.Error;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles query requests from a client and uses an LRUCache to improve query latency
 */
public class CachingProductsHandler extends ProductsHandler {

    /**
     * LRUCache to hold recent query data
     */
    private final LRUCache cache;

    /**
     * Instantiates a CachingProductsHandler instance, obtaining a gRPC stub to the catalog service at host:port
     * @param host The host address of the catalog service
     * @param port The port address of the catalog service
     * @param cache The gateway server LRUCache
     * @param testMode Indicates whether the handler should show testing mode output
     */
    public CachingProductsHandler(String host, int port, LRUCache cache, boolean testMode) {
        super(host, port, testMode);
        this.cache = cache;
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
            HTTPHandlerUtility.handleUnknownMethodResponse(exchange, super.objectMapper);
            return;
        }

        // Get product name
        String productName = HTTPHandlerUtility.getProductName(exchange);

        // Try cache
        Data data =cache.get(productName);

        // If cache miss, query catalog service
        if(data == null) {
            CatalogQueryRequest request = CatalogQueryRequest.newBuilder().setName(productName).build();
            CatalogQueryResponse response = catalogStub.query(request);

            // Send error data back to client if error occurred
            if (!response.getSuccess()) {
                Error error = Error.builder().code(404).message(response.getErrorMessage()).build();
                handleErrorResponse(error, exchange);
                return;
            }

            // Get data from response and add to cache
            data = Data.builder().name(response.getName()).price(response.getPrice()).quantity(response.getQuantity()).build();
            cache.put(data);
        }

        // Send response data back to client
        handleDataResponse(data, exchange);
    }

    /**
     * Sends response back to client
     * @param exchange The http exchange object
     * @param reply The response object as a string
     * @throws IOException
     */
    @Override
    protected void sendResponse(HttpExchange exchange, String reply) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        // Optionally print response object
        if(testMode) {
            System.out.println("Query response object: ");
            System.out.println(reply);
            System.out.println("Cache: ");
            System.out.println(cache);
            System.out.println();
        }

        // Send response
        outputStream.write(reply.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
