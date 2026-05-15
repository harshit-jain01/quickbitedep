package com.quickbite.order.model;

public record OrderItem(
        String itemName,
        String imageUrl,
        double unitPrice,
        int quantity,
        double lineTotal
) {
}
