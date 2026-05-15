package com.quickbite.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CorsHeaderSanitizingFilterTest {

    private final CorsHeaderSanitizingFilter filter = new CorsHeaderSanitizingFilter();

    @Test
    void filter_shouldKeepSingleAllowOriginAndForceCredentialsTrue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test").build());
        exchange.getResponse().getHeaders().put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, List.of("http://a.com", "http://a.com, http://b.com"));
        exchange.getResponse().getHeaders().put(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, List.of("false", "true"));

        GatewayFilterChain chain = e -> Mono.empty();
        filter.filter(exchange, chain).block();

        assertEquals("http://a.com", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void filter_shouldRemoveAllowOriginWhenOnlyBlankValuesPresent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test").build());
        exchange.getResponse().getHeaders().put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, List.of(" ", ","));

        filter.filter(exchange, e -> Mono.empty()).block();

        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
