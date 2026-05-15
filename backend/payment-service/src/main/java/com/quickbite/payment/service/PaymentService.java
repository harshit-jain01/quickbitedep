package com.quickbite.payment.service;

import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.PaymentStatusResponse;
import com.quickbite.payment.dto.RazorpayOrderRequest;
import com.quickbite.payment.dto.RazorpayOrderResponse;
import com.quickbite.payment.dto.RazorpayVerifyRequest;
import com.quickbite.payment.dto.RazorpayVerifyResponse;
public interface PaymentService {

    PaymentResponse charge(PaymentRequest request);

    RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request);

    RazorpayVerifyResponse verifyRazorpayPayment(RazorpayVerifyRequest request);

    PaymentStatusResponse getPaymentStatus(String orderReference);
}
