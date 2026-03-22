package com.example.payment.service;

import com.example.payment.domain.*;
import com.example.payment.dto.PaymentCardDto;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Проверка, существует ли уже платеж для этого заказа
        List<Payment> existingPayments = paymentRepository.findByOrderId(request.getOrderId());
        if (!existingPayments.isEmpty()) {
            log.warn("Payment already exists for order: {}", request.getOrderId());
            return mapToResponse(existingPayments.get(0));
        }

        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .orderId(request.getOrderId())
                .paymentDate(LocalDateTime.now())
                .status(PaymentStatus.PENDING)
                .amount(new Money(request.getAmount(), request.getCurrency()))
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .cardDetails(mapToPaymentCard(request.getCardDetails()))
                .build();

        log.info("Processing payment for order: {}", request.getOrderId());

        Payment savedPayment = paymentRepository.save(payment);

        try {
            // Симуляция обработки платежа через платежный шлюз
            Thread.sleep(1000);

            // Симуляция успешного/неуспешного платежа (80% успеха)
            if (Math.random() < 0.8) {
                savedPayment.setStatus(PaymentStatus.COMPLETED);
                savedPayment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
                log.info("Payment completed for order: {}", request.getOrderId());
            } else {
                savedPayment.setStatus(PaymentStatus.FAILED);
                savedPayment.setErrorMessage("Insufficient funds or card declined");
                log.warn("Payment failed for order: {}", request.getOrderId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage("Payment processing timeout");
            log.error("Payment timeout for order: {}", request.getOrderId());
        }

        savedPayment = paymentRepository.save(savedPayment);
        return mapToResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentResponse(UUID id) {
        Payment payment = getPayment(id);
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new RuntimeException("Payment not found for order: " + orderId);
        }
        return mapToResponse(payments.get(0));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPaymentResponses() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse refundPayment(UUID id) {
        Payment payment = getPayment(id);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Refunding payment: {}", payment.getPaymentNumber());

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Transactional
    public void deletePayment(UUID id) {
        Payment payment = getPayment(id);
        paymentRepository.delete(payment);
        log.info("Deleted payment: {}", payment.getPaymentNumber());
    }

    private Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    private PaymentCard mapToPaymentCard(PaymentCardDto dto) {
        if (dto == null) return null;
        return new PaymentCard(
                dto.getCardNumber(),
                dto.getCardHolderName(),
                dto.getExpiryDate(),
                dto.getCvv()
        );
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .orderId(payment.getOrderId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount().getAmount())
                .currency(payment.getAmount().getCurrency())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaymentDate())
                .errorMessage(payment.getErrorMessage())
                .build();
    }

    private String generatePaymentNumber() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
