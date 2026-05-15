package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryAssignmentRequest(
        @NotBlank String orderReference,
        @NotBlank String restaurantName,
        @NotBlank String deliveryAddress
) {
}
