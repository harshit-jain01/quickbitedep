package com.quickbite.cart.dto;

import java.util.List;

public record CartResponse(
        String userEmail,
        Long restaurantId,
        String restaurantName,
        List<CartItemResponse> items,
        int itemCount,
        double subtotal,
        double deliveryFee,
        double taxes,
        double total
) {
}
