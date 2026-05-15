package com.quickbite.auth.dto;

import com.quickbite.auth.model.UserRole;

public record ValidateTokenResponse(
        boolean valid,
        String email,
        UserRole role
) {
}
