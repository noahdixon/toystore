package com.dixon.common;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Order to be sent to server
 */
@lombok.Data
@Builder
@Jacksonized
public class Order {
    /**
     * The product name
     */
    private String name;

    /**
     * The desired order quantity
     */
    private int quantity;
}
