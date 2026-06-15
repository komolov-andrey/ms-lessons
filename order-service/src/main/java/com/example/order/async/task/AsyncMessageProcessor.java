package com.example.order.async.task;

import com.example.order.async.domain.AsyncMessage;
import com.example.order.async.service.AsyncMessageService;
import com.example.order.config.KafkaTopicsConfig;
import com.example.order.dto.DeliveryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author a.komolov
 * @date 2026-06-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncMessageProcessor {

    private final AsyncMessageService asyncMessageService;
    private final KafkaTemplate<String, DeliveryRequest> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void sendMessage(AsyncMessage asyncMessage) {
        try {
            DeliveryRequest deliveryRequest = objectMapper.readValue(asyncMessage.getVal(), DeliveryRequest.class);
            log.info("Publishing delivery request to Kafka for order: {}", deliveryRequest.getOrderNumber());
            kafkaTemplate.send(KafkaTopicsConfig.DELIVERY_REQUEST_TOPIC, asyncMessage.getId().getId(), deliveryRequest)
                    .exceptionally(e -> {
                        throw new RuntimeException("Error publishing delivery message " + asyncMessage, e);
                    })
                    .get();
            log.info("Delivery request published successfully for order: {}", deliveryRequest.getOrderNumber());
            asyncMessageService.markMessageAsSent(asyncMessage);
        } catch (Exception e) {
            log.error("Error while processing async message", e);
        }
    }

}
