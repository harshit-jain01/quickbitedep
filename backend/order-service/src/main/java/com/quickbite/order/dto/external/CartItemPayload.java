package com.quickbite.order.dto.external;

public record CartItemPayload(
        Long id,
        Long menuItemId,
        String itemName,
        String imageUrl,
        double unitPrice,
        int quantity,
        double lineTotal
) {
}
