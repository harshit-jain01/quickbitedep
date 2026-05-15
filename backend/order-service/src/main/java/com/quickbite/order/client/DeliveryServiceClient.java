package com.quickbite.order.client;

import com.quickbite.order.dto.external.DeliveryAssignmentRequest;
import com.quickbite.order.dto.external.DeliveryAssignmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {

    @PostMapping("/api/v1/agents/assign")
    DeliveryAssignmentResponse assign(
            @RequestHeader("X-Authenticated-Role") String role,
            @RequestBody DeliveryAssignmentRequest request
    );
}

