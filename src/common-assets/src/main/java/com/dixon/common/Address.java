package com.dixon.common;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * A server address
 */
@lombok.Data
@Builder
@Jacksonized
public class Address {
    /**
     * The server hostname
     */
    @Getter
    private String host;

    /**
     * The server port
     */
    @Getter
    private int port;

    /**
     * Gives the string representation of an Address as host:port
     * @return string representation of an Address as host:port
     */
    @Override
    public String toString(){
        return host + ":" + port;
    }
}
