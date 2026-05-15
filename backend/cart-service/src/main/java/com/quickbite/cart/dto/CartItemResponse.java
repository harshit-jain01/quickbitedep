package com.quickbite.cart.dto;

public record CartItemResponse(
        Long id,
        Long menuItemId,
        String itemName,
        String imageUrl,
        double unitPrice,
        int quantity,
        double lineTotal
) {
}
