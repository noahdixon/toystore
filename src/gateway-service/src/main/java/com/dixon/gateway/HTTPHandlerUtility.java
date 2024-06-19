package com.dixon.gateway;

import com.dixon.common.Error;
import com.dixon.common.ErrorWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Contains static methods used across several HttpHandler classes
 */
public class HTTPHandlerUtility {
    /**
     * Sends error object back to user in the event of a disallowed http method
     * @param exchange The http exchange object
     * @throws IOException
     */
    protected static void handleUnknownMethodResponse(HttpExchange exchange, ObjectMapper objectMapper) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        // 405 error for method not allowed
        exchange.sendResponseHeaders(405, 0);

        // Build error message
        String errorMessage = exchange.getRequestMethod() + " method not allowed for URL " + exchange.getRequestURI().toString() + ".";

        // Send error object response
        Error error = Error.builder().code(405).message(errorMessage).build();
        outputStream.write(objectMapper.writeValueAsString(ErrorWrapper.builder().error(error).build()).getBytes());
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Extracts product name from the request URI
     * @param exchange The http exchange object
     * @return String representation of product name
     */
    protected static String getProductName(HttpExchange exchange) {
        String requestURI = exchange.getRequestURI().toString();
        String[] uriParts = requestURI.split("/");
        return uriParts[2];
    }
}
