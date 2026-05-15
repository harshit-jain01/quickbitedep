package com.quickbite.gateway.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorsHeaderSanitizingFilter implements GlobalFilter, Ordered {

    private static final String ALLOW_ORIGIN = HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
    private static final String ALLOW_CREDENTIALS = HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            sanitizeAllowOrigin(headers);
            sanitizeAllowCredentials(headers);
        }));
    }

    @Override
    public int getOrder() {
        // Run late so it can normalize headers added by both downstream services and gateway filters.
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void sanitizeAllowOrigin(HttpHeaders headers) {
        List<String> values = headers.get(ALLOW_ORIGIN);
        if (values == null || values.isEmpty()) {
            return;
        }

        Set<String> uniqueOrigins = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueOrigins.isEmpty()) {
            headers.remove(ALLOW_ORIGIN);
            return;
        }

        headers.set(ALLOW_ORIGIN, uniqueOrigins.iterator().next());
    }

    private void sanitizeAllowCredentials(HttpHeaders headers) {
        List<String> values = headers.get(ALLOW_CREDENTIALS);
        if (values == null || values.isEmpty()) {
            return;
        }
        headers.set(ALLOW_CREDENTIALS, "true");
    }
}

