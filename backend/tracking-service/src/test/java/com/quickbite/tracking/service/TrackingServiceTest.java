package com.quickbite.tracking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.quickbite.tracking.client.OrderServiceClient;
import com.quickbite.tracking.dto.external.OrderStatusPayload;
import com.quickbite.tracking.service.impl.TrackingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @Test
    void getOrderTracking_shouldReturnResponse_whenOrderFound() {
        OrderStatusPayload payload = new OrderStatusPayload("OD9001", "PICKED_UP", "Ravi", 25, "20-30 min");

        when(orderServiceClient.getOrder("user@mail.com", "OD9001")).thenReturn(payload);

        var response = trackingService.getOrderTracking("user@mail.com", "OD9001");

        assertEquals("OD9001", response.orderReference());
        assertEquals("PICKED_UP", response.status());
        assertEquals("Delivery agent picked up your order", response.message());
    }

    @Test
    void getOrderTracking_shouldThrow_whenOrderNotFound() {
        when(orderServiceClient.getOrder("user@mail.com", "OD9002")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> trackingService.getOrderTracking("user@mail.com", "OD9002")
        );

        assertEquals("Order status not found", ex.getMessage());
    }
}
