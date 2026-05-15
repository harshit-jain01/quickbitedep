package com.quickbite.delivery.model;

import java.time.Instant;

public record AgentOrderAssignment(
        String orderReference,
        String agentId,
        String restaurantName,
        String deliveryAddress,
        String assignmentStatus,
        Instant assignedAt,
        Instant acceptedAt,
        Instant pickedUpAt,
        Instant deliveredAt
) {

    public AgentOrderAssignment withAgentAndStatus(String nextAgentId, String status, Instant accepted, Instant pickedUp, Instant delivered) {
        return new AgentOrderAssignment(
                orderReference,
                nextAgentId,
                restaurantName,
                deliveryAddress,
                status,
                assignedAt,
                accepted,
                pickedUp,
                delivered
        );
    }

    public AgentOrderAssignment withStatus(String status, Instant accepted, Instant pickedUp, Instant delivered) {
        return withAgentAndStatus(agentId, status, accepted, pickedUp, delivered);
    }
}

