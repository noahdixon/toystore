package com.dixon.order;

import com.dixon.AcceptOrdersResponse;
import com.dixon.FetchAllOrdersResponse;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates an OrderNode object and a ListenableFuture object from a Grpc call made on it's stub
 */
@Getter
@Setter
public class NodeAndListenableFuture {
    /**
     * An order node
     */
    private OrderNode peerNode;

    /**
     * A ListenableFuture containing the response to a fetchAllOrdersFrom call
     */
    private ListenableFuture<FetchAllOrdersResponse> fetchAllOrdersFuture;

    /**
     * A ListenableFuture containing the response to a acceptOrdersFromLeader call
     */
    private ListenableFuture<AcceptOrdersResponse> acceptOrdersFuture;

    /**
     * instantiates a new NodeAndListenableFuture instance
     * @param peerNode OrderNode
     * @param fetchAllOrdersFuture ListenableFuture containing the response to a fetchAllOrdersFrom call, set to null if acceptOrdersFromLeader call made
     * @param acceptOrdersFuture ListenableFuture containing the response to a acceptOrdersFromLeader call, set to null if fetchAllOrdersFrom call made
     */
    public NodeAndListenableFuture(OrderNode peerNode,
                                   ListenableFuture<FetchAllOrdersResponse> fetchAllOrdersFuture,
                                   ListenableFuture<AcceptOrdersResponse> acceptOrdersFuture) {
        this.peerNode = peerNode;
        this.fetchAllOrdersFuture = fetchAllOrdersFuture;
        this.acceptOrdersFuture =  acceptOrdersFuture;
    }
}