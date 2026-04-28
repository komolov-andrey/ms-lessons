package com.example.payment.messaging;

import com.example.payment.config.RabbitMQConfig;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestListener {

    private final PaymentService paymentService;
    private final PaymentResponsePublisher responsePublisher;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE)
    public void handlePaymentRequest(PaymentRequest request,
                                     @Header(value = "Idempotency-Key", required = false) String idempotencyKey) {
        log.info("Received payment request from RabbitMQ - OrderId: {}, IdempotencyKey: {}",
                request.getOrderId(), idempotencyKey);
        try {
            PaymentResponse response = paymentService.processPayment(request);
            responsePublisher.sendPaymentResponse(response);
        } catch (Exception e) {
            log.error("Error processing payment for order {}: {}", request.getOrderId(), e.getMessage(), e);
            PaymentResponse errorResponse = PaymentResponse.builder()
                    .orderId(request.getOrderId())
                    .status("FAILED")
                    .errorMessage("Payment processing error: " + e.getMessage())
                    .build();
            responsePublisher.sendPaymentResponse(errorResponse);
        }
    }
}
