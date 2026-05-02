package com.example.order.messaging;

import com.example.order.config.KafkaTopicsConfig;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.DeliveryResponse;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryResponseListener {

    private static final String DELIVERY_STATUS_CREATED = "CREATED";

    private final OrderRepository orderRepository;

    @KafkaListener(topics = KafkaTopicsConfig.DELIVERY_RESPONSE_TOPIC, groupId = "order-service-delivery-group")
    @Transactional
    public void handleDeliveryResponse(DeliveryResponse response) {
        log.info("Received delivery response - OrderId: {}, DeliveryId: {}, Status: {}",
                response.getOrderId(), response.getDeliveryId(), response.getStatus());

        UUID orderId = UUID.fromString(response.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for delivery response: {}", response.getOrderId());
                    return new RuntimeException("Order not found: " + response.getOrderId());
                });

        if (DELIVERY_STATUS_CREATED.equals(response.getStatus())) {
            order.setDeliveryId(response.getDeliveryId());
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            log.info("Delivery created for order {}. DeliveryId: {}", order.getOrderNumber(), response.getDeliveryId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.error("Delivery creation failed for order {}: {}", order.getOrderNumber(), response.getErrorMessage());
    }
}
