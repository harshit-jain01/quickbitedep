package com.quickbite.auth.client;

import com.quickbite.auth.dto.OrderCountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderAdminClient {

    @GetMapping("/api/v1/admin/orders/count")
    OrderCountResponse getTotalOrders(@RequestHeader("X-Authenticated-Role") String role);
}

