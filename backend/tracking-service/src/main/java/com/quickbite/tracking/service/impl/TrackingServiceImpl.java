package com.quickbite.tracking.service.impl;

import com.quickbite.tracking.client.OrderServiceClient;
import com.quickbite.tracking.dto.TrackingResponse;
import com.quickbite.tracking.dto.external.OrderStatusPayload;
import com.quickbite.tracking.service.TrackingService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final OrderServiceClient orderServiceClient;

    public TrackingServiceImpl(OrderServiceClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    @Override
    public TrackingResponse getOrderTracking(String userEmail, String orderReference) {
        logger.debug("Fetching tracking data from order-service for orderReference={}", orderReference);
        OrderStatusPayload order = orderServiceClient.getOrder(userEmail, orderReference);

        if (order == null) {
            logger.warn("Tracking data not found for orderReference={}", orderReference);
            throw new IllegalArgumentException("Order status not found");
        }

        logger.info("Tracking data prepared for orderReference={} status={}", orderReference, order.deliveryStatus());

        return new TrackingResponse(
                order.orderReference(),
                order.deliveryStatus(),
                order.deliveryAgent(),
                order.estimatedDeliveryWindow(),
                buildMessage(order.deliveryStatus()),
                LocalDateTime.now()
        );
    }

    private String buildMessage(String status) {
        return switch (status) {
            case "PLACED" -> "Order placed successfully";
            case "CONFIRMED" -> "Restaurant confirmed your order";
            case "PREPARING" -> "Your food is being prepared";
            case "PICKED_UP" -> "Delivery agent picked up your order";
            case "DELIVERED" -> "Order delivered";
            default -> "Status updated";
        };
    }
}

