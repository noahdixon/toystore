package com.dixon.order;

import com.dixon.OrderServiceGrpc;
import com.dixon.common.Address;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Data;

import static com.dixon.order.OrderNodeMode.FOLLOWER;
import static com.dixon.order.OrderNodeStatus.ACTIVE;

/**
 * Holds meta data about an order service node
 */
@Data
public class OrderNode {
    /**
     * Order service node address
     */
    private Address address;

    /**
     * Node id
     */
    private int id;

    /**
     * Node status
     */
    private Enum<OrderNodeStatus> currentStatus = ACTIVE;

    /**
     * Node mode
     */
    private Enum<OrderNodeMode> mode = FOLLOWER;

//    /**
//     * Channel to the node
//     */
//    private ManagedChannel channel;

    /**
     * Grpc stub to the node
     */
    private OrderServiceGrpc.OrderServiceFutureStub futureStub;

    /**
     * Instantiates a new OrderNode instance
     * @param address Node address
     * @param id Node id
     */
    public OrderNode(int id, Address address) {
        this.address = address;
        this.id = id;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(address.toString()).usePlaintext().build();
        this.futureStub = OrderServiceGrpc.newFutureStub(channel);
    }
}
