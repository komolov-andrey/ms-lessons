package com.example.delivery.messaging;

import com.example.delivery.domain.Delivery;
import com.example.delivery.domain.DeliveryAddress;
import com.example.delivery.dto.DeliveryRequest;
import com.example.delivery.dto.DeliveryResponse;
import com.example.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryRequestListener {

    private static final String DELIVERY_CREATED_STATUS = "CREATED";
    private static final String DELIVERY_FAILED_STATUS = "FAILED";
    private static final String DEFAULT_CITY = "UNKNOWN_CITY";
    private static final String DEFAULT_STREET = "UNKNOWN_STREET";
    private static final String DEFAULT_ZIP = "000000";
    private static final String DEFAULT_COUNTRY = "UNKNOWN_COUNTRY";
    private static final String DELIVERY_REQUEST_TOPIC = "delivery.request.topic";
    private static final String DELIVERY_RESPONSE_TOPIC = "delivery.response.topic";

    private final DeliveryService deliveryService;
    private final KafkaTemplate<String, DeliveryResponse> kafkaTemplate;

    @KafkaListener(topics = DELIVERY_REQUEST_TOPIC, groupId = "delivery-service-group")
    public void handleDeliveryRequest(DeliveryRequest request) {
        log.info("Received delivery request for order: {}", request.getOrderNumber());

        try {
            Delivery createdDelivery = deliveryService.createDelivery(Delivery.builder()
                    .orderId(request.getOrderId())
                    .deliveryAddress(new DeliveryAddress(DEFAULT_STREET, DEFAULT_CITY, DEFAULT_ZIP, DEFAULT_COUNTRY, null))
                    .build());

            DeliveryResponse response = DeliveryResponse.builder()
                    .orderId(request.getOrderId())
                    .deliveryId(createdDelivery.getId().toString())
                    .status(DELIVERY_CREATED_STATUS)
                    .build();

            kafkaTemplate.send(DELIVERY_RESPONSE_TOPIC, request.getOrderId(), response);
            log.info("Delivery created and response sent for order: {}", request.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to create delivery for order {}: {}", request.getOrderNumber(), e.getMessage(), e);

            DeliveryResponse failedResponse = DeliveryResponse.builder()
                    .orderId(request.getOrderId())
                    .status(DELIVERY_FAILED_STATUS)
                    .errorMessage(e.getMessage())
                    .build();

            kafkaTemplate.send(DELIVERY_RESPONSE_TOPIC, request.getOrderId(), failedResponse);
        }
    }
}
