package com.quickbite.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.dto.RazorpayOrderRequest;
import com.quickbite.payment.dto.RazorpayVerifyRequest;
import com.quickbite.payment.model.PaymentTransaction;
import com.quickbite.payment.repository.PaymentTransactionRepository;
import com.quickbite.payment.service.impl.PaymentServiceImpl;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    private PaymentService paymentService;

    private static final String SECRET = "secret_key";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentTransactionRepository, "rzp_key", SECRET);
    }

    @Test
    void charge_shouldPersistSuccessTransaction() {
        PaymentRequest request = new PaymentRequest("OD1001", "customer@mail.com", "COD", 450.0);

        var response = paymentService.charge(request);

        assertEquals("OD1001", response.orderReference());
        assertEquals("SUCCESS", response.status());

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals("OD1001", captor.getValue().getOrderId());
        assertEquals("SUCCESS", captor.getValue().getPaymentStatus());
        assertEquals("INR", captor.getValue().getCurrency());
    }

    @Test
    void createRazorpayOrder_shouldThrow_whenPaymentModeUnsupported() {
        RazorpayOrderRequest request = new RazorpayOrderRequest("OD1002", "customer@mail.com", "COD", 300.0, "INR", "note");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.createRazorpayOrder(request));

        assertEquals("paymentMode must be UPI or CARD", ex.getMessage());
    }

    @Test
    void createRazorpayOrder_shouldThrow_whenCurrencyIsNotInr() {
        RazorpayOrderRequest request = new RazorpayOrderRequest("OD1003", "customer@mail.com", "UPI", 300.0, "USD", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.createRazorpayOrder(request));

        assertEquals("Only INR currency is supported", ex.getMessage());
    }

    @Test
    void createRazorpayOrder_shouldThrow_whenAmountIsNotPositive() {
        RazorpayOrderRequest request = new RazorpayOrderRequest("OD1004", "customer@mail.com", "CARD", 0.0, "INR", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.createRazorpayOrder(request));

        assertEquals("amount must be greater than zero", ex.getMessage());
    }

    @Test
    void verifyRazorpayPayment_shouldSaveFailedStatusAndThrow_whenSignatureInvalid() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest(
                "OD1005", "customer@mail.com", "UPI", 310.0, "INR", "order_1", "pay_1", "bad_signature"
        );
        when(paymentTransactionRepository.findTopByOrderIdOrderByIdDesc("OD1005")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.verifyRazorpayPayment(request));

        assertEquals("Invalid Razorpay signature", ex.getMessage());
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getPaymentStatus());
    }

    @Test
    void verifyRazorpayPayment_shouldSaveSuccess_whenSignatureValid() throws Exception {
        String orderId = "OD1006";
        String razorpayOrderId = "order_abc";
        String razorpayPaymentId = "pay_abc";
        String signature = sign(razorpayOrderId + "|" + razorpayPaymentId, SECRET);
        PaymentTransaction existing = new PaymentTransaction();
        existing.setOrderId(orderId);
        when(paymentTransactionRepository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.of(existing));

        var response = paymentService.verifyRazorpayPayment(new RazorpayVerifyRequest(
                orderId, "customer@mail.com", "upi_collect", 520.0, "inr",
                razorpayOrderId, razorpayPaymentId, signature
        ));

        assertEquals("SUCCESS", response.paymentStatus());
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getPaymentStatus());
        assertEquals("UPI", captor.getValue().getPaymentMode());
        assertEquals("INR", captor.getValue().getCurrency());
    }

    @Test
    void getPaymentStatus_shouldReturnLatestPaymentRecord() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId("OD1007");
        transaction.setPaymentMode("CARD");
        transaction.setPaymentStatus("SUCCESS");
        transaction.setRazorpayOrderId("order_x");
        transaction.setRazorpayPaymentId("pay_x");
        transaction.setCurrency("INR");
        transaction.setTotalAmount(999.0);
        when(paymentTransactionRepository.findTopByOrderIdOrderByIdDesc("OD1007")).thenReturn(Optional.of(transaction));

        var response = paymentService.getPaymentStatus("OD1007");

        assertEquals("OD1007", response.orderReference());
        assertEquals("SUCCESS", response.paymentStatus());
        assertEquals("order_x", response.razorpayOrderId());
    }

    @Test
    void getPaymentStatus_shouldReturnZeroAmount_whenStoredAmountIsNull() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId("OD1008");
        transaction.setPaymentMode("CARD");
        transaction.setPaymentStatus("PENDING");
        transaction.setCurrency("INR");
        transaction.setTotalAmount(null);
        when(paymentTransactionRepository.findTopByOrderIdOrderByIdDesc("OD1008")).thenReturn(Optional.of(transaction));

        var response = paymentService.getPaymentStatus("OD1008");

        assertEquals(0.0, response.amount());
    }

    @Test
    void getPaymentStatus_shouldThrow_whenPaymentNotFound() {
        when(paymentTransactionRepository.findTopByOrderIdOrderByIdDesc("MISSING")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.getPaymentStatus("MISSING"));

        assertEquals("Payment record not found", ex.getMessage());
    }

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
