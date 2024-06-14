package com.dixon.frontend;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Handles cache invalidation requests from catalog service
 */
public class InvalidateHandler implements HttpHandler {
    /**
     * String representation of GET method
     */
    protected static final String DELETE_METHOD = "DELETE";

    /**
     * Object mapper used for marshaling and unmarshalling
     */
    private final ObjectMapper objectMapper;

    /**
     * The frontend service LRU cache
     */
    private final LRUCache cache;

    /**
     * Instantiates an InvalidateHandler
     * @param cache the frontend service LRU cache
     */
    public InvalidateHandler(LRUCache cache) {
        super();
        // Create object mapper
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        this.objectMapper = new ObjectMapper(jsonFactory);

        // Initialize cache
        this.cache = cache;
    }

    /**
     * Handles cache invalidation requests
     * @param exchange The http exchange object
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check that http method is DELETE
        if(!exchange.getRequestMethod().equals(DELETE_METHOD)) {
            HTTPHandlerUtility.handleUnknownMethodResponse(exchange, objectMapper);
            return;
        }

        String productName = HTTPHandlerUtility.getProductName(exchange);
        cache.invalidate(productName);
    }
}
