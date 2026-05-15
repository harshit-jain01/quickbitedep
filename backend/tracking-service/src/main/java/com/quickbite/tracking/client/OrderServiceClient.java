package com.quickbite.tracking.client;

import com.quickbite.tracking.dto.external.OrderStatusPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{orderReference}")
    OrderStatusPayload getOrder(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @PathVariable("orderReference") String orderReference
    );
}

