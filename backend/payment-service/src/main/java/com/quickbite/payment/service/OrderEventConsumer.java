package com.quickbite.payment.service;

import com.quickbite.payment.dto.event.OrderEvent;
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
                    "payment-service consumed event={} orderReference={} paymentMode={} amount={}",
                    event.eventType(),
                    event.orderReference(),
                    event.paymentMode(),
                    event.totalAmount()
            );
        } catch (Exception ex) {
            // Consumer errors must not bubble up into service APIs.
            logger.error("payment-service failed to process order event", ex);
        }
    }
}

