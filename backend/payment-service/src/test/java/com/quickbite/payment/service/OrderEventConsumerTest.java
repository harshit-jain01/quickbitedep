package com.quickbite.payment.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.quickbite.payment.dto.event.OrderEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderEventConsumerTest {

    private final OrderEventConsumer consumer = new OrderEventConsumer();

    @Test
    void consume_shouldHandleValidEventWithoutThrowing() {
        OrderEvent event = new OrderEvent(
                "evt-1", "ORDER_CREATED", "OD-1", "user@mail.com",
                "Restaurant", "Address", "UPI", "PENDING", 230.0, Instant.now()
        );

        assertDoesNotThrow(() -> consumer.consume(event));
    }
}
