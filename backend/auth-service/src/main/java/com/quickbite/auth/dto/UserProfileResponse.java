package com.quickbite.auth.dto;

import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        UserRole role,
        AuthProvider provider,
        boolean isActive,
        Instant createdAt,
        String profilePicUrl
) {
}
