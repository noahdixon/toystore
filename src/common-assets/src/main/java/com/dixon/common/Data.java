package com.dixon.common;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Data object to be returned from server
 */
@lombok.Data
@Builder
@Jacksonized
public class Data {
    /**
     * Name of product
     */
    private String name;

    /**
     * Price of product
     */
    private double price;

    /**
     * Quantity of product
     */
    private int quantity;

    /**
     * Order number of order returned during buy request
     */
    private int orderNumber;

    /**
     * Order number returned during get order request
     */
    private int number;
}
