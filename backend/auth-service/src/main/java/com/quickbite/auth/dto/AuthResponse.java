package com.quickbite.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
}
