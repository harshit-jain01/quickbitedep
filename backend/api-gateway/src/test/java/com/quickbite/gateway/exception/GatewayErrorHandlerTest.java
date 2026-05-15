package com.quickbite.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

class GatewayErrorHandlerTest {

    @Test
    void handle_shouldWriteStructuredErrorForResponseStatusException() {
        GatewayErrorHandler handler = new GatewayErrorHandler(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request data")).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"status\":400"));
        assertTrue(body.contains("\"path\":\"/api/v1/orders\""));
    }

    @Test
    void handle_shouldFallbackWhenSerializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsBytes(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("serialize"));
        GatewayErrorHandler handler = new GatewayErrorHandler(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart").build());

        handler.handle(exchange, new RuntimeException("boom")).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("Gateway error"));
    }
}
