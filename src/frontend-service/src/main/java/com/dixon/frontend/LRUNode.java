package com.dixon.frontend;

import lombok.Getter;
import lombok.Setter;

/**
 * A node in an LRUCache containing product stock and price data
 */
public class LRUNode {
    /**
     * Points to the next node in the LRU queue
     */
    @Getter @Setter
    private LRUNode next;

    /**
     * Points to the previous node in the LRU queue
     */
    @Getter @Setter
    private LRUNode last;

    /**
     * Data object containing product stock and price data
     */
    @Getter @Setter
    private Data data;

    /**
     * Instantiates a new LRUNode
     * @param data the data encapsulated by this node
     */
    public LRUNode(Data data){
        this.next = null;
        this.last = null;
        this.data = data;
    }

    /**
     * Gives the string representation of an LRUNode
     * @return String representation of an LRUNode
     */
    @Override
    public String toString() {
        String last;
        String next;

        if(this.last == null) {
            last = "null";
        } else {
            last = this.last.getData().getName();
        }

        if(this.next == null) {
            next = "null";
        } else {
            next = this.next.getData().getName();
        }
        return "| " + last + " <- " + data.toString() + " -> " + next + " |";
    }
}
