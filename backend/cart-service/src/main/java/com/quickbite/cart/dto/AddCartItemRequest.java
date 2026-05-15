package com.quickbite.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long restaurantId,
        @NotBlank String restaurantName,
        @NotNull @Positive Long menuItemId,
        @NotBlank String itemName,
        @NotBlank String imageUrl,
        @NotNull @Positive Double unitPrice,
        @Min(1) int quantity,
        boolean replaceCart
) {
}
