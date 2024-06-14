package com.dixon.common;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Wrapper for serialization of data or error objects returned from server
 */
@lombok.Data
@Builder
@Jacksonized
public class ResponseWrapper {
    /**
     * Data object
     */
    private Data data;
    /**
     * Error object
     */
    private Error error;
}
