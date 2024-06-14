package com.dixon.client;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Order to be sent to server
 */
@lombok.Data
@Builder
@Jacksonized
public class SentOrder {
    /**
     * The product name
     */
    private String name;

    /**
     * The order quantity
     */
    private int quantity;

    /**
     * The order number
     */
    private int number;
}
