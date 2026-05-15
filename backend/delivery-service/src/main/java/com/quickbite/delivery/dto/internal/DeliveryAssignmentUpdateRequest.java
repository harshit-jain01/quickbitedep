package com.quickbite.delivery.dto.internal;

public record DeliveryAssignmentUpdateRequest(
        String agentId,
        String agentName,
        int etaMinutes
) {
}

