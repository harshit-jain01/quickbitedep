package com.quickbite.payment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.payment.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PaymentTransactionRepositoryTest {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void findTopByOrderIdOrderByIdDesc_shouldReturnLatestTransaction() {
        PaymentTransaction first = transaction("OD-R1", "PENDING", "pay_1");
        PaymentTransaction second = transaction("OD-R1", "SUCCESS", "pay_2");
        paymentTransactionRepository.save(first);
        paymentTransactionRepository.save(second);

        var result = paymentTransactionRepository.findTopByOrderIdOrderByIdDesc("OD-R1");

        assertTrue(result.isPresent());
        assertEquals("SUCCESS", result.get().getPaymentStatus());
        assertEquals("pay_2", result.get().getRazorpayPaymentId());
    }

    private PaymentTransaction transaction(String orderId, String status, String paymentId) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId(orderId);
        tx.setUserId("user@mail.com");
        tx.setTotalAmount(100.0);
        tx.setPaymentMode("UPI");
        tx.setPaymentStatus(status);
        tx.setRazorpayOrderId("order_x");
        tx.setRazorpayPaymentId(paymentId);
        tx.setCurrency("INR");
        return tx;
    }
}
