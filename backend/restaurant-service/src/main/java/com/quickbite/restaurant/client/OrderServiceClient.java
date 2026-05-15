package com.quickbite.restaurant.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders")
    List<Map<String, Object>> getOrders(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @RequestHeader("X-Authenticated-Role") String role
    );

    @PutMapping("/api/v1/orders/internal/{orderReference}/status")
    Map<String, Object> updateOrderStatus(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable("orderReference") String orderReference,
            @RequestBody Map<String, String> body
    );
}

