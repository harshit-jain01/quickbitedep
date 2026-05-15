package com.quickbite.order.client;

import com.quickbite.order.dto.external.CartPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cart-service")
public interface CartServiceClient {

    @GetMapping("/api/v1/cart")
    CartPayload getCart(@RequestHeader("X-Authenticated-User") String userEmail);

    @DeleteMapping("/api/v1/cart")
    void clearCart(@RequestHeader("X-Authenticated-User") String userEmail);
}

