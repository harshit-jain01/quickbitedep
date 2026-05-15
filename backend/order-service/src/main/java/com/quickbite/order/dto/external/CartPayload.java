package com.quickbite.order.dto.external;

import java.util.List;

public record CartPayload(
        String userEmail,
        Long restaurantId,
        String restaurantName,
        List<CartItemPayload> items,
        int itemCount,
        double subtotal,
        double deliveryFee,
        double taxes,
        double total
) {
}
