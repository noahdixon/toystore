package com.dixon.frontend;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Data to be sent to client upon successful query
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
     * Gives the string representation of a Data object
     * @return string representation of a Data object
     */
    @Override
    public String toString() {
        return name + "(" + quantity + " in stock @ $" + price + ")";
    }
}
