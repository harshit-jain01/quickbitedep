package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;

public record DeliveryAgentAvailabilityRequest(@NotNull Boolean online) {
}

