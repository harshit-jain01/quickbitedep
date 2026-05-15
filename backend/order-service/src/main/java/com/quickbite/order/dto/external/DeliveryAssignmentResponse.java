package com.quickbite.order.dto.external;

public record DeliveryAssignmentResponse(
        String orderReference,
        String agentId,
        String agentName,
        String assignmentStatus,
        int etaMinutes
) {
}
