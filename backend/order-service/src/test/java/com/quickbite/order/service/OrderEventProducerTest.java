package com.quickbite.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.order.dto.event.OrderEvent;
import com.quickbite.order.model.Order;
import com.quickbite.order.model.OrderItem;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Test
    void publishOrderCreated_shouldSendEventWithOrderReferenceAsKey() {
        OrderEventProducer producer = new OrderEventProducer(kafkaTemplate, "order-events");
        Order order = order("OD-E1");
        SendResult<String, OrderEvent> sendResult = org.mockito.Mockito.mock(SendResult.class);
        when(kafkaTemplate.send(eq("order-events"), eq("OD-E1"), any(OrderEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publishOrderCreated(order);

        verify(kafkaTemplate).send(eq("order-events"), eq("OD-E1"), any(OrderEvent.class));
    }

    @Test
    void publishOrderCreated_shouldNotThrow_whenKafkaFutureCompletesExceptionally() {
        OrderEventProducer producer = new OrderEventProducer(kafkaTemplate, "order-events");
        CompletableFuture<SendResult<String, OrderEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(eq("order-events"), eq("OD-E2"), any(OrderEvent.class))).thenReturn(failed);

        producer.publishOrderCreated(order("OD-E2"));

        verify(kafkaTemplate).send(eq("order-events"), eq("OD-E2"), any(OrderEvent.class));
    }

    @Test
    void publishOrderCreated_shouldNotThrow_whenSendInvocationThrows() {
        OrderEventProducer producer = new OrderEventProducer(kafkaTemplate, "order-events");
        doThrow(new RuntimeException("producer failure"))
                .when(kafkaTemplate).send(eq("order-events"), eq("OD-E3"), any(OrderEvent.class));

        producer.publishOrderCreated(order("OD-E3"));

        verify(kafkaTemplate).send(eq("order-events"), eq("OD-E3"), any(OrderEvent.class));
    }

    private Order order(String reference) {
        return new Order(
                reference,
                "user@mail.com",
                "Restaurant",
                "Address",
                "COD",
                "PENDING",
                null,
                null,
                "PLACED",
                null,
                null,
                30,
                100.0,
                null,
                Instant.now(),
                List.of(new OrderItem("Item", "img", 100.0, 1, 100.0))
        );
    }
}
