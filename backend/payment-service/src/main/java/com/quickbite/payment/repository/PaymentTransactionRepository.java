package com.quickbite.payment.repository;

import com.quickbite.payment.model.PaymentTransaction;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Logger logger = LoggerFactory.getLogger(PaymentTransactionRepository.class);

    Optional<PaymentTransaction> findTopByOrderIdOrderByIdDesc(String orderId);
}

