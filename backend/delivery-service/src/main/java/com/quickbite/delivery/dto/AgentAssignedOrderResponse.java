package com.quickbite.delivery.dto;

import java.time.Instant;

public record AgentAssignedOrderResponse(
        String orderReference,
        String restaurantName,
        String deliveryAddress,
        String assignmentStatus,
        Instant assignedAt,
        Instant acceptedAt,
        Instant pickedUpAt,
        Instant deliveredAt
) {
}

