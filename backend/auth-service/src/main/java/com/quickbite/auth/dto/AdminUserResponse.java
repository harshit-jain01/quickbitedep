package com.quickbite.auth.dto;

import com.quickbite.auth.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}

