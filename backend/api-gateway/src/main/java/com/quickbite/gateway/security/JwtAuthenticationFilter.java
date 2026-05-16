package com.quickbite.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();
        if (isPublicPath(method, path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        if (!jwtService.isTokenValid(token)) {
            return unauthorized(exchange, "Invalid or expired JWT token");
        }

        String email = jwtService.extractSubject(token);
        String role = jwtService.extractRole(token);
        String phone = jwtService.extractPhone(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-Authenticated-User", email)
                .header("X-Authenticated-Role", role);
        if (phone != null && !phone.isBlank()) {
            requestBuilder.header("X-Authenticated-Phone", phone);
        }
        ServerHttpRequest request = requestBuilder.build();

        return chain.filter(exchange.mutate().request(request).build())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private boolean isPublicPath(HttpMethod method, String path) {
        if (HttpMethod.POST.equals(method) && ("/api/v1/agents".equals(path) || "/api/v1/agents/".equals(path))) {
            return true;
        }

        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null && !origin.isBlank()) {
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            exchange.getResponse().getHeaders().add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        }
        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s","path":"%s"}
                """.formatted(Instant.now(), message, exchange.getRequest().getPath().value()).trim();
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
