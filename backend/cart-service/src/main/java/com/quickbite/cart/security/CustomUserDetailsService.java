package com.quickbite.cart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final String jwtSecret = System.getenv("JWT_SECRET") != null ?
            System.getenv("JWT_SECRET") :
            "UVVpY2tCaXRlU3VwZXJTZWNyZXRLZXlGb3JKV1RTaWduaW5nMTIzNDU2Nzg5MA==";

    private static final String PLACEHOLDER_PASSWORD = "N/A";

    private SecretKey getSigningKey() {
        byte[] decodedKey = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // For now, we accept any username from the JWT token
        // In a production system, you would query a database here
        return User.builder()
                .username(username)
                .password(PLACEHOLDER_PASSWORD)  // Not used for JWT auth, but must be non-empty
                .authorities("ROLE_USER")
                .build();
    }

    public UserDetails loadUserByUsernameWithRole(String username, String token) throws UsernameNotFoundException {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String role = claims.get("role", String.class);
            Collection<? extends GrantedAuthority> authorities = role != null
                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

            return User.builder()
                    .username(username)
                    .password(PLACEHOLDER_PASSWORD)
                    .authorities(authorities)
                    .build();
        } catch (Exception e) {
            // Fallback to default user if token parsing fails
            logger.warn("Token role extraction failed, falling back to default role for username={}", username);
            return loadUserByUsername(username);
        }
    }
}

