package com.quickbite.payment.controller;

import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.PaymentStatusResponse;
import com.quickbite.payment.dto.RazorpayOrderRequest;
import com.quickbite.payment.dto.RazorpayOrderResponse;
import com.quickbite.payment.dto.RazorpayVerifyRequest;
import com.quickbite.payment.dto.RazorpayVerifyResponse;
import com.quickbite.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public PaymentResponse charge(@Valid @RequestBody PaymentRequest request) {
        logger.info("Payment charge request received for orderReference={} paymentMethod={}", request.orderReference(), request.paymentMethod());
        return paymentService.charge(request);
    }

    @PostMapping("/razorpay/orders")
    public RazorpayOrderResponse createRazorpayOrder(@Valid @RequestBody RazorpayOrderRequest request) {
        logger.info("Razorpay order creation request received for orderReference={}", request.orderReference());
        return paymentService.createRazorpayOrder(request);
    }

    @PostMapping("/razorpay/verify")
    public RazorpayVerifyResponse verifyRazorpayPayment(@Valid @RequestBody RazorpayVerifyRequest request) {
        logger.info("Razorpay payment verification request received for orderReference={}", request.orderReference());
        return paymentService.verifyRazorpayPayment(request);
    }

    @GetMapping("/orders/{orderReference}")
    public PaymentStatusResponse getPaymentStatus(@PathVariable String orderReference) {
        logger.debug("Payment status requested for orderReference={}", orderReference);
        return paymentService.getPaymentStatus(orderReference);
    }
}
