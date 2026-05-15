package com.quickbite.cart.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "UVVpY2tCaXRlU3VwZXJTZWNyZXRLZXlGb3JKV1RTaWduaW5nMTIzNDU2Nzg5MA==";

    private final JwtService jwtService = new JwtService();

    JwtServiceTest() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
    }

    @Test
    void extractMethods_shouldReturnClaimsForValidToken() {
        String token = buildToken("user@mail.com", "3", "USER", Instant.now().plusSeconds(300));

        assertEquals("user@mail.com", jwtService.extractSubject(token));
        assertEquals("3", jwtService.extractUserId(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    void extractMethods_shouldReturnNullForInvalidToken() {
        assertNull(jwtService.extractSubject("bad-token"));
        assertNull(jwtService.extractUserId("bad-token"));
        assertNull(jwtService.extractRole("bad-token"));
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidTokenAndMatchingUser() {
        String token = buildToken("user@mail.com", "5", "USER", Instant.now().plusSeconds(300));
        var user = User.withUsername("user@mail.com").password("x").authorities("ROLE_USER").build();

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalseForMismatchedUserOrExpiredToken() {
        String validToken = buildToken("user@mail.com", "5", "USER", Instant.now().plusSeconds(300));
        String expiredToken = buildToken("user@mail.com", "5", "USER", Instant.now().minusSeconds(10));

        var otherUser = User.withUsername("other@mail.com").password("x").authorities("ROLE_USER").build();
        assertFalse(jwtService.isTokenValid(validToken, otherUser));
        assertFalse(jwtService.isTokenValid(expiredToken, otherUser));
    }

    private String buildToken(String subject, String userId, String role, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("role", role)
                .expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }
}
