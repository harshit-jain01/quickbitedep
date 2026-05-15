package com.quickbite.delivery.dto;

public record AgentEarningsResponse(
        String agentId,
        int completedDeliveries,
        double estimatedEarnings
) {
}

