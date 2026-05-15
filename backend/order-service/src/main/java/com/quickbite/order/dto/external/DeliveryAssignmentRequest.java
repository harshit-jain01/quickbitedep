package com.quickbite.order.dto.external;

public record DeliveryAssignmentRequest(
        String orderReference,
        String restaurantName,
        String deliveryAddress
) {
}
