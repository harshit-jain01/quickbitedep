package com.quickbite.delivery.client;

import com.quickbite.delivery.dto.internal.DeliveryAssignmentUpdateRequest;
import com.quickbite.delivery.dto.internal.DeliveryStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderInternalClient {

    @PutMapping("/api/v1/orders/internal/{orderReference}/agent")
    void updateAgent(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable("orderReference") String orderReference,
            @RequestBody DeliveryAssignmentUpdateRequest request
    );

    @PutMapping("/api/v1/orders/internal/{orderReference}/status")
    void updateStatus(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable("orderReference") String orderReference,
            @RequestBody DeliveryStatusUpdateRequest request
    );
}

