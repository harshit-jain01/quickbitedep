package com.quickbite.cart.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

class CustomUserDetailsServiceTest {

    private static final String SECRET = "UVVpY2tCaXRlU3VwZXJTZWNyZXRLZXlGb3JKV1RTaWduaW5nMTIzNDU2Nzg5MA==";

    private final CustomUserDetailsService service = new CustomUserDetailsService();

    @Test
    void loadUserByUsername_shouldReturnDefaultRoleUser() {
        var user = service.loadUserByUsername("user@mail.com");
        assertEquals("user@mail.com", user.getUsername());
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsernameWithRole_shouldExtractRoleFromToken() {
        String token = Jwts.builder()
                .subject("user@mail.com")
                .claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        var user = service.loadUserByUsernameWithRole("user@mail.com", token);

        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameWithRole_shouldFallbackToRoleUserWhenTokenInvalid() {
        var user = service.loadUserByUsernameWithRole("user@mail.com", "invalid");
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
