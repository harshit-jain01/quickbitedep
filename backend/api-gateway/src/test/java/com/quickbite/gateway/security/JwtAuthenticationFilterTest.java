package com.quickbite.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Test
    void filter_shouldBypassPublicPath() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build()
        );

        CapturingWebFilterChain chain = new CapturingWebFilterChain();
        filter.filter(exchange, chain).block();

        assertEquals(1, chain.exchanges.size());
        assertEquals("/api/v1/auth/login", chain.exchanges.get(0).getRequest().getPath().value());
    }

    @Test
    void filter_shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header(HttpHeaders.ORIGIN, "http://localhost:5173").build()
        );

        filter.filter(exchange, e -> Mono.empty()).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("http://localhost:5173", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void filter_shouldReturnUnauthorizedWhenTokenInvalid() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer bad").build()
        );

        filter.filter(exchange, e -> Mono.empty()).block();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void filter_shouldPropagateIdentityHeadersWhenTokenValid() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        when(jwtService.isTokenValid("ok")).thenReturn(true);
        when(jwtService.extractSubject("ok")).thenReturn("user@mail.com");
        when(jwtService.extractRole("ok")).thenReturn("USER");
        when(jwtService.extractPhone("ok")).thenReturn("9999999999");

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ok")
                        .build()
        );

        CapturingWebFilterChain chain = new CapturingWebFilterChain();
        filter.filter(exchange, chain).block();

        assertEquals(1, chain.exchanges.size());
        var request = chain.exchanges.get(0).getRequest();
        assertEquals("user@mail.com", request.getHeaders().getFirst("X-Authenticated-User"));
        assertEquals("USER", request.getHeaders().getFirst("X-Authenticated-Role"));
        assertEquals("9999999999", request.getHeaders().getFirst("X-Authenticated-Phone"));
    }

    @Test
    void filter_shouldBypassOptionsRequest() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/orders").build()
        );

        CapturingWebFilterChain chain = new CapturingWebFilterChain();
        filter.filter(exchange, chain).block();

        assertTrue(chain.exchanges.size() == 1);
    }

    private static class CapturingWebFilterChain implements WebFilterChain {
        private final List<ServerWebExchange> exchanges = new ArrayList<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            exchanges.add(exchange);
            return Mono.empty();
        }
    }
}
