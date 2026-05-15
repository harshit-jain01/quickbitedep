package com.quickbite.order.dto;

public record OrderItemResponse(
        String itemName,
        String imageUrl,
        double unitPrice,
        int quantity,
        double lineTotal
) {
}
