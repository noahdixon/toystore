package com.dixon.frontend;

/**
 * A custom implementation of an LRU queue
 * Used inside an LRUCache to maintain the least recently used order
 */
public class LRUQueue {
    /**
     * Points to the first node in the queue
     */
    private LRUNode head;

    /**
     * Points to the last node in the queue
     */
    private LRUNode tail;

    /**
     * Instantiates a new LRUQueue
     */
    public LRUQueue() {
        this.head = null;
        this.tail = null;
    }

    /**
     * Moves a node to the front of the queue
     * @param node The node to be moved to the front
     */
    public void moveFront(LRUNode node) {
        // If node is head, already at front
        if (node == head) {
            return;
        }

        // Evict node from middle or end of queue
        evictNonHead(node);

        // Move node pointers
        node.setNext(head);
        node.setLast(null);

        // Update head
        head.setLast(node);
        head = node;
    }

    /**
     * Adds a new node to the front of the queue
     * @param node The new node
     */
    public void enqueue(LRUNode node) {
        node.setNext(head);
        if (head != null) {
            head.setLast(node);
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
    }

    /**
     * Removes a node from the end of the queue
     * @return The node to be moved to the front
     */
    public LRUNode dequeue() {
        LRUNode evictedNode = tail;
        tail = tail.getLast();
        tail.setNext(null);
        return evictedNode;
    }

    /**
     * Evicts a node from the queue
     * @param node The node to be evicted
     */
    public void evict(LRUNode node) {
        // If node is head, evict and reassign head
        if (node == head) {
            head = node.getNext();
            // Check if node was only node in queue, if so head and tail should be null
            if (head == null) {
                tail = null;
            } else {
                head.setLast(null);
            }
            return;
        }

        // Evict non-head node
        evictNonHead(node);
    }

    /**
     * Evicts non-head node from the middle or end of the queue
     * @param node The node to be evicted
     */
    private void evictNonHead(LRUNode node) {
        // Move neighbor pointers
        LRUNode last = node.getLast();
        LRUNode next = node.getNext();
        last.setNext(next);
        if (node == tail) {
            tail = last;
            tail.setNext(null);
        } else {
            next.setLast(last);
        }
    }

    /**
     * Gives the string representation of an LRUQueue
     * @return String representation of an LRUQueue
     */
    @Override
    public String toString() {
        String head;
        if (this.head == null) {
            head = "null";
        } else {
            head = this.head.toString();
        }

        String tail;
        if (this.tail == null) {
            tail = "null";
        } else {
            tail = this.tail.toString();
        }

        String s = "HEAD: " + head + "\n" + "FULL QUEUE: ";
        LRUNode node = this.head;
        while (node != null) {
            s += node.toString();
            node = node.getNext();
        }
        s += "\n";
        s += "TAIL: " + tail;
        return s;
    }
}
