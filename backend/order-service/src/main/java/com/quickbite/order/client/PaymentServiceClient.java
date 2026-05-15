package com.quickbite.order.client;

import com.quickbite.order.dto.external.PaymentStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @GetMapping("/api/v1/payments/orders/{orderReference}")
    PaymentStatusResponse getPaymentStatus(@PathVariable("orderReference") String orderReference);
}

