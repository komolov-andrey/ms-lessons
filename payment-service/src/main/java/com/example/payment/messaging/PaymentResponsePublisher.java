package com.example.payment.messaging;

import com.example.payment.config.RabbitMQConfig;
import com.example.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResponsePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void sendPaymentResponse(PaymentResponse response) {
        log.info("Publishing payment response to RabbitMQ - OrderId: {}, Status: {}",
                response.getOrderId(), response.getStatus());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_RESPONSE_ROUTING_KEY,
                response
        );
        log.info("Payment response published successfully for order: {}", response.getOrderId());
    }
}
