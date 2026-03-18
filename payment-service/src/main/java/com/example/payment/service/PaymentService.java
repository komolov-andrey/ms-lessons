package com.example.payment.service;

import com.example.payment.domain.*;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public Payment processPayment(Payment payment) {
        payment.setPaymentNumber(generatePaymentNumber());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING);
        
        log.info("Processing payment for order: {}", payment.getOrderId());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        try {
            Thread.sleep(1000);
            
            if (Math.random() < 0.8) {
                savedPayment.setStatus(PaymentStatus.COMPLETED);
                savedPayment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
                log.info("Payment completed for order: {}", payment.getOrderId());
            } else {
                savedPayment.setStatus(PaymentStatus.FAILED);
                savedPayment.setErrorMessage("Insufficient funds");
                log.warn("Payment failed for order: {}", payment.getOrderId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage("Payment processing timeout");
        }
        
        return paymentRepository.save(savedPayment);
    }
    
    @Transactional(readOnly = true)
    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
    
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    @Transactional
    public Payment refundPayment(UUID id) {
        Payment payment = getPayment(id);
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Refunding payment: {}", payment.getPaymentNumber());
        
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public void deletePayment(UUID id) {
        Payment payment = getPayment(id);
        paymentRepository.delete(payment);
        log.info("Deleted payment: {}", payment.getPaymentNumber());
    }
    
    private String generatePaymentNumber() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
