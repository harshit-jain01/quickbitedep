package com.quickbite.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.PaymentStatusResponse;
import com.quickbite.payment.dto.RazorpayOrderResponse;
import com.quickbite.payment.dto.RazorpayVerifyResponse;
import com.quickbite.payment.exception.GlobalExceptionHandler;
import com.quickbite.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void charge_shouldReturn200_whenRequestIsValid() throws Exception {
        when(paymentService.charge(any())).thenReturn(new PaymentResponse("TXN1", "OD2001", "COD", 250.0, "SUCCESS"));

        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"OD2001",
                                  "customerEmail":"customer@mail.com",
                                  "paymentMethod":"COD",
                                  "amount":250.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).charge(any());
    }

    @Test
    void charge_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"",
                                  "customerEmail":"customer@mail.com",
                                  "paymentMethod":"COD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void verifyRazorpayPayment_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(paymentService.verifyRazorpayPayment(any())).thenReturn(new RazorpayVerifyResponse(
                "OD3001", "UPI", "SUCCESS", "order_1", "pay_1", true, "Payment verified successfully"
        ));

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"OD3001",
                                  "customerEmail":"customer@mail.com",
                                  "paymentMode":"UPI",
                                  "amount":300.0,
                                  "currency":"INR",
                                  "razorpayOrderId":"order_1",
                                  "razorpayPaymentId":"pay_1",
                                  "razorpaySignature":"sig"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        verify(paymentService).verifyRazorpayPayment(any());
    }

    @Test
    void createRazorpayOrder_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(paymentService.createRazorpayOrder(any())).thenReturn(new RazorpayOrderResponse(
                "rzp_key", "OD3002", "order_1", 30000L, "INR", "UPI", "created"
        ));

        mockMvc.perform(post("/api/v1/payments/razorpay/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"OD3002",
                                  "customerEmail":"customer@mail.com",
                                  "paymentMode":"UPI",
                                  "amount":300.0,
                                  "currency":"INR",
                                  "notes":"n"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD3002"));

        verify(paymentService).createRazorpayOrder(any());
    }

    @Test
    void createRazorpayOrder_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"",
                                  "customerEmail":"customer@mail.com",
                                  "paymentMode":"UPI",
                                  "amount":0,
                                  "currency":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getPaymentStatus_shouldReturn200_whenRecordExists() throws Exception {
        when(paymentService.getPaymentStatus("OD4001")).thenReturn(new PaymentStatusResponse(
                "OD4001", "CARD", "SUCCESS", "order_x", "pay_x", "INR", 999.0
        ));

        mockMvc.perform(get("/api/v1/payments/orders/OD4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    void getPaymentStatus_shouldReturn400_whenServiceThrowsIllegalArgument() throws Exception {
        when(paymentService.getPaymentStatus("MISSING")).thenThrow(new IllegalArgumentException("Payment record not found"));

        mockMvc.perform(get("/api/v1/payments/orders/MISSING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment record not found"));
    }
}

