package com.dixon.order;

/**
 * Generator for getting next order number
 */
public class OrderNumberGenerator {

    /**
     * Stores next order number
     */
    private int orderNumber;

    /**
     * Initializes an OrderNumberGenerator with the initialNumber as the first order number
     * @param initialNumber
     */
    public OrderNumberGenerator(int initialNumber) {
        orderNumber = initialNumber;
    }

    /**
     * Returns the next order number, then increments the order numbers
     * @return The next order number
     */
    public synchronized int getOrderNumber() {
        return orderNumber++;
    }

    /**
     * Update the max order number
     */
    public synchronized void updateMaxOrderNumber(int maxOrderNumber) {
        this.orderNumber = maxOrderNumber + 1;
    }
}
