package com.quickbite.order.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record DeliveryAssignmentUpdateRequest(
        @NotBlank String agentId,
        @NotBlank String agentName,
        @PositiveOrZero int etaMinutes
) {
}

