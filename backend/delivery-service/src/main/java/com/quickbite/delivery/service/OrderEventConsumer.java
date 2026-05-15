package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.order-events-topic:order-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(OrderEvent event) {
        try {
            logger.info(
                    "delivery-service consumed event={} orderReference={} restaurant={}",
                    event.eventType(),
                    event.orderReference(),
                    event.restaurantName()
            );
        } catch (Exception ex) {
            // Consumer errors must not affect existing synchronous delivery flow.
            logger.error("delivery-service failed to process order event", ex);
        }
    }
}

