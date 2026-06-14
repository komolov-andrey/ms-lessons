package com.example.order.messaging;

import com.example.order.config.KafkaTopicsConfig;
import com.example.order.dto.DeliveryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryMessagePublisher {

    private final KafkaTemplate<String, DeliveryRequest> kafkaTemplate;

    public void sendDeliveryRequest(DeliveryRequest deliveryRequest) {
        log.info("Publishing delivery request to Kafka for order: {}", deliveryRequest.getOrderNumber());
        kafkaTemplate.send(KafkaTopicsConfig.DELIVERY_REQUEST_TOPIC, deliveryRequest.getOrderId(), deliveryRequest);
        log.info("Delivery request published successfully for order: {}", deliveryRequest.getOrderNumber());
    }
}
