package com.dixon.gateway;

import java.util.HashMap;
import java.util.Map;

/**
 * A Custom implementation of an LRU cache, containing product stock and price data for recently queried items
 */
public class LRUCache {
    /**
     * Hashmap to access product data with String product name key
     */
    private Map<String, LRUNode> hashMap;

    /**
     * LRU queue to maintain the least recently used order
     */
    private LRUQueue queue;

    /**
     * Maximum size of the cache
     */
    private int size;

    /**
     * Instantiates a new LRUCache using the specified size
     * @param size
     */
    public LRUCache(int size) {
        hashMap = new HashMap<>(size);
        queue = new LRUQueue();
        this.size = size;
    }

    /**
     * Retrieves the data for the specified product if it exists in the cache
     * @param item The desired product
     * @return The data object containing the product stock and price information if it exists in the cache,
     * else returns null
     */
    public synchronized Data get(String item) {
        // Get data
        LRUNode node = hashMap.get(item);
        if (node == null) {
            return null;
        }

        // Move accessed node to front of queue
        queue.moveFront(node);
        return node.getData();
    }

    /**
     * Adds new data to the cache
     * @param data The data to be added to the cache
     */
    public synchronized void put(Data data) {
        LRUNode node;
        String item = data.getName();

        // If item is already in cache, just update quantity and move it to front
        node = hashMap.get(item);
        if (node != null) {
            node.setData(data);
            queue.moveFront(node);
            return;
        }

        // Item is not in cache, create and add new node
        node = new LRUNode(data);
        hashMap.put(item, node);
        queue.enqueue(node);

        // Evict least recently used data if cache is full
        if (hashMap.size() > size) {
            hashMap.remove(queue.dequeue().getData().getName());
        }
    }

    /**
     * Invalidates an item, removing it from the cache
     * @param item The item to be invalidated
     */
    public synchronized void invalidate(String item) {
        LRUNode node = hashMap.remove(item);
        if (node == null) {
            return;
        }
        queue.evict(node);
    }

    /**
     * Gives the string representation of an LRUCache
     * @return String representation of an LRUCache
     */
    @Override
    public String toString() {
        return "HASHMAP SIZE: " + hashMap.size() + "\n" + queue.toString();
    }
}