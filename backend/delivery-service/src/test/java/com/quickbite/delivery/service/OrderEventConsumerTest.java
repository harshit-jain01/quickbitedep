package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.event.OrderEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderEventConsumerTest {

    private final OrderEventConsumer consumer = new OrderEventConsumer();

    @Test
    void consume_shouldHandleValidEventWithoutThrowing() {
        OrderEvent event = new OrderEvent(
                "EVT-1",
                "ORDER_CREATED",
                "ORD-1",
                "user@mail.com",
                "R1",
                "A1",
                "COD",
                "PENDING",
                250.0,
                Instant.now()
        );

        consumer.consume(event);
    }

    @Test
    void consume_shouldHandleNullEventWithoutThrowing() {
        consumer.consume(null);
    }
}
