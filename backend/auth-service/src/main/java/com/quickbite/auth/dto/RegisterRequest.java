package com.quickbite.auth.dto;

import com.quickbite.auth.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Pattern(regexp = "^[A-Za-z ]{3,50}$", message = "Name must be 3-50 characters and contain only letters")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please enter a valid email address")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$", message = "Password must include uppercase, lowercase, number, and special character")
        String password,
        @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter a valid 10-digit phone number")
        String phone,
        UserRole role,
        String profilePicUrl
) {
}
