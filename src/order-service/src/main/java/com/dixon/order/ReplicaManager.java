package com.dixon.order;

import com.dixon.HealthRequest;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.dixon.order.OrderNodeMode.LEADER;
import static com.dixon.order.OrderNodeStatus.ACTIVE;

/**
 * Stores meta data about all order service node replicas
 */
@Data
public class ReplicaManager {
    /**
     * Current leader node id
     */
    private int currentLeader = -1;

    /**
     * ID of this order service node
     */
    private int selfId;

    /**
     * Maps node ID's to OrderNode instances
     */
    private HashMap<Integer, OrderNode> orderNodesHashMap;

    /**
     * HealthRequest object to check node activity
     */
    private static HealthRequest req = HealthRequest.newBuilder().setMessage("Are you online?").build();

    /**
     * Instantiates a new ReplicaManager
     * @param selfId This order nodes' id
     * @param orderNodesHashMap A mapping of order service node ids to OrderNode objects
     */
    public ReplicaManager(int selfId, HashMap<Integer, OrderNode> orderNodesHashMap) {
        this.selfId = selfId;
        this.orderNodesHashMap = orderNodesHashMap;
    }

    /**
     * Assigns a new node to be the leader
     * @param leaderId The new leader id
     */
    public void assignLeader(int leaderId) {
        currentLeader = leaderId;
        OrderNode leaderNode = orderNodesHashMap.get(leaderId);
        if(leaderNode != null) {
            leaderNode.setMode(LEADER);
        }
    }

    /**
     * Adds an order service node to the replica manager
     * @param node the new OrderNode
     */
    public void addNodeToMap(OrderNode node) {
        orderNodesHashMap.put(node.getId(), node);
    }

    /**
     * Changes a node's status
     * @param nodeId The node ID
     * @param targetStatus The new node status
     */
    public void changeNodeStatus(int nodeId, OrderNodeStatus targetStatus) {
        OrderNode targetNode = orderNodesHashMap.get(nodeId);
        if(targetNode != null) {
            targetNode.setCurrentStatus(targetStatus);
        } else {
            System.out.println(nodeId + " is not present in the Hashmap");
        }
    }

    /**
     * Gets all active peer order service nodes
     * @return a List of the active OrderNodes.
     */
    public List<OrderNode> getActivePeerNodes() {
        return orderNodesHashMap.values()
                                .stream()
                                .filter(orderNode -> orderNode.getCurrentStatus() == ACTIVE
                                                        && orderNode.getId() != selfId)
                                .collect(Collectors.toList());
    }
}
