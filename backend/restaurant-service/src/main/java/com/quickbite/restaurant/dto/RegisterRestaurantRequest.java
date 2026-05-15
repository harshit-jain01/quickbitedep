package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRestaurantRequest(
        @NotBlank String restaurantName,
        @NotBlank String cuisineType,
        @NotBlank String address
) {
}

