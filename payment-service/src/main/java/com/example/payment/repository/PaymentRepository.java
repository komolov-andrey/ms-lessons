package com.example.payment.repository;

import com.example.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(String status);
    Optional<Payment> findByTransactionId(String transactionId);
}
