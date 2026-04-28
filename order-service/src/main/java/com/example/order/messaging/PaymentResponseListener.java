package com.example.order.messaging;

import com.example.order.config.RabbitMQConfig;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.PaymentResponse;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResponseListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESPONSE_QUEUE)
    @Transactional
    public void handlePaymentResponse(PaymentResponse response) {
        log.info("Received payment response - PaymentId: {}, OrderId: {}, Status: {}",
                response.getPaymentId(), response.getOrderId(), response.getStatus());

        UUID orderId = UUID.fromString(response.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for payment response: {}", response.getOrderId());
                    return new RuntimeException("Order not found: " + response.getOrderId());
                });

        switch (response.getStatus()) {
            case "COMPLETED" -> {
                order.setPaymentId(response.getPaymentId().toString());
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Order {} paid successfully. PaymentId: {}, TransactionId: {}",
                        order.getOrderNumber(), response.getPaymentId(), response.getTransactionId());
            }
            case "FAILED" -> {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.error("Payment failed for order {}: {}",
                        order.getOrderNumber(), response.getErrorMessage());
            }
            case "PENDING" -> log.info("Payment still pending for order: {}", order.getOrderNumber());
            default -> {
                log.warn("Unknown payment status '{}' for order: {}", response.getStatus(), order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
            }
        }
    }
}
