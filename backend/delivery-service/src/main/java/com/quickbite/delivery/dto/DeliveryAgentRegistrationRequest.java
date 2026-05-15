package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeliveryAgentRegistrationRequest(
        @NotBlank(message = "Name is required")
        @Pattern(regexp = "^[A-Za-z ]{3,50}$", message = "Name must be 3-50 characters and contain only letters")
        String name,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit phone number")
        String phone,
        @NotBlank(message = "Vehicle type is required")
        String vehicleType,
        @NotBlank(message = "Vehicle number is required")
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$", message = "Enter valid vehicle number (e.g., MP04AB1234)")
        String vehicleNumber
) {
}
