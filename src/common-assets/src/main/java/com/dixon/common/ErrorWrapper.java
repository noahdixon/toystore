package com.dixon.common;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Wrapper for error object for serialization
 */
@lombok.Data
@Builder
@Jacksonized
public class ErrorWrapper {
    /**
     * Wrapped error
     */
    private Error error;
}
