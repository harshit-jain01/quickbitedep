package com.quickbite.delivery.dto;

public record DeliveryAssignmentResponse(
        String orderReference,
        String agentId,
        String agentName,
        String assignmentStatus,
        int etaMinutes
) {
}
