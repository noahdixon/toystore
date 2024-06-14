package com.dixon.common;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Error object to be returned from server
 */
@lombok.Data
@Builder
@Jacksonized
public class Error {
    /**
     * Error code
     */
    private int code;

    /**
     * Error message
     */
    private String message;
}
