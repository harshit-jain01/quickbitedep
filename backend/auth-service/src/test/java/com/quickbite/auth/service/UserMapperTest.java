package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toProfile_shouldMapAllFields() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        UserAccount user = new UserAccount();
        user.setUserId(userId);
        user.setFullName("Mapped User");
        user.setEmail("mapped@mail.com");
        user.setPhone("9999999999");
        user.setRole(UserRole.ADMIN);
        user.setProvider(AuthProvider.GOOGLE);
        user.setActive(true);
        user.setCreatedAt(createdAt);
        user.setProfilePicUrl("https://image");

        var profile = mapper.toProfile(user);

        assertEquals(userId, profile.userId());
        assertEquals("mapped@mail.com", profile.email());
        assertEquals(UserRole.ADMIN, profile.role());
        assertEquals(AuthProvider.GOOGLE, profile.provider());
        assertEquals("https://image", profile.profilePicUrl());
        assertTrue(profile.isActive());
    }
}
