package com.dixon.client;

/**
 * Custom printer class to toggle printing depending on latency testing mode
 */
public class CustomPrinter {
    /**
     * Holds the latency mode (on or off)
     */
    private final boolean latencyMode;

    /**
     * Instantiates a new CustomPrinter with the given latency Mode (on or off)
     * @param latencyMode The desired latency mode
     */
    CustomPrinter(boolean latencyMode) {
        this.latencyMode = latencyMode;
    }

    /**
     * Prints to console if latency mode is off, else does nothing.
     * @param message The value to print
     * @param <T> The type to print
     */
    public <T> void println(T message) {
        if (!latencyMode) System.out.println(message);
    }
}
