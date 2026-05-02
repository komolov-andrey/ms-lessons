package com.example.order.messaging;

import com.example.order.config.RabbitMQConfig;
import com.example.order.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void sendPaymentRequest(PaymentRequest paymentRequest, String idempotencyKey) {
        log.info("Publishing payment request to RabbitMQ for order: {}", paymentRequest.getOrderNumber());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    RabbitMQConfig.PAYMENT_REQUEST_ROUTING_KEY,
                    paymentRequest,
                    message -> {
                        message.getMessageProperties().setHeader("Idempotency-Key", idempotencyKey);
                        return message;
                    }
            );
            log.info("Payment request published successfully for order: {}", paymentRequest.getOrderNumber());
        } catch (AmqpException e) {
            log.error("Failed to publish payment request for order {}: {}", paymentRequest.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to send payment request via RabbitMQ: " + e.getMessage(), e);
        }
    }
}
