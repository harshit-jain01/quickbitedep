package com.quickbite.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {

    private static final String SECRET = "UVVpY2tCaXRlU3VwZXJTZWNyZXRLZXlGb3JKV1RTaWduaW5nMTIzNDU2Nzg5MA==";
    private final JwtService jwtService = new JwtService(SECRET, 60_000L);

    @Test
    void generateToken_shouldProduceValidTokenWithExpectedClaims() {
        UserAccount user = buildUser("jwt@mail.com", UserRole.ADMIN, AuthProvider.LOCAL);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("jwt@mail.com", jwtService.extractSubject(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValidWithUserDetails_shouldFailForDifferentUser() {
        String token = jwtService.generateToken(buildUser("one@mail.com", UserRole.CUSTOMER, AuthProvider.LOCAL));
        var mismatched = User.withUsername("two@mail.com").password("x").authorities("ROLE_CUSTOMER").build();

        boolean valid = jwtService.isTokenValid(token, mismatched);

        assertFalse(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtService.isTokenValid("not-a-jwt"));
    }

    @Test
    void getExpirationSeconds_shouldConvertMillisToSeconds() {
        assertEquals(60L, jwtService.getExpirationSeconds());
    }

    private UserAccount buildUser(String email, UserRole role, AuthProvider provider) {
        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Jwt User");
        user.setPasswordHash("ENC");
        user.setPhone("9999999999");
        user.setRole(role);
        user.setProvider(provider);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
