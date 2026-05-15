package com.quickbite.gateway.security;

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

class JwtServiceTest {

    private static final String SECRET = "UVVpY2tCaXRlU3VwZXJTZWNyZXRLZXlGb3JKV1RTaWduaW5nMTIzNDU2Nzg5MA==";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = buildToken("user@mail.com", "USER", "9999999999", Instant.now().plusSeconds(300));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtService.isTokenValid("not-a-jwt"));
    }

    @Test
    void extractClaims_shouldReturnSubjectRoleAndPhone() {
        String token = buildToken("owner@mail.com", "ADMIN", "8888888888", Instant.now().plusSeconds(300));

        assertEquals("owner@mail.com", jwtService.extractSubject(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals("8888888888", jwtService.extractPhone(token));
    }

    @Test
    void extractPhone_shouldReturnNullWhenClaimMissing() {
        String token = Jwts.builder()
                .subject("user@mail.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        assertNull(jwtService.extractPhone(token));
    }

    private String buildToken(String subject, String role, String phone, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("phone", phone)
                .expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }
}
