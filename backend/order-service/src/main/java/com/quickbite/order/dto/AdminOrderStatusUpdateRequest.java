package com.quickbite.order.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderStatusUpdateRequest(@NotBlank String status) {
}

