package com.quickbite.order.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record DeliveryStatusUpdateRequest(@NotBlank String deliveryStatus) {
}

