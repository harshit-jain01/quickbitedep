package com.quickbite.order.service;

import com.quickbite.order.dto.event.OrderEvent;
import com.quickbite.order.model.Order;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;

    public OrderEventProducer(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${app.kafka.order-events-topic:order-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishOrderCreated(Order order) {
        try {
            OrderEvent event = new OrderEvent(
                    UUID.randomUUID().toString(),
                    "ORDER_CREATED",
                    order.orderReference(),
                    order.userEmail(),
                    order.restaurantName(),
                    order.deliveryAddress(),
                    order.paymentMode(),
                    order.paymentStatus(),
                    order.total(),
                    order.createdAt() == null ? Instant.now() : order.createdAt()
            );

            kafkaTemplate.send(topic, order.orderReference(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Kafka publish failed for orderReference={}", order.orderReference(), ex);
                        } else if (result != null && result.getRecordMetadata() != null) {
                            logger.info(
                                    "Kafka event published for orderReference={} topic={} partition={} offset={}",
                                    order.orderReference(),
                                    topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            );
                        } else {
                            logger.info("Kafka event publish completed for orderReference={}", order.orderReference());
                        }
                    });
        } catch (Exception ex) {
            // Keep checkout flow independent of Kafka availability.
            logger.error("Skipping Kafka publish due to error for orderReference={}", order.orderReference(), ex);
        }
    }
}

