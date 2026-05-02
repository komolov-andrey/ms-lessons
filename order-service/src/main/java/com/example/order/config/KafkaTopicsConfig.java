package com.example.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    public static final String DELIVERY_REQUEST_TOPIC = "delivery.request.topic";
    public static final String DELIVERY_RESPONSE_TOPIC = "delivery.response.topic";

    private static final int TOPIC_PARTITIONS = 1;
    private static final int TOPIC_REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic deliveryRequestTopic() {
        return TopicBuilder.name(DELIVERY_REQUEST_TOPIC)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic deliveryResponseTopic() {
        return TopicBuilder.name(DELIVERY_RESPONSE_TOPIC)
                .partitions(TOPIC_PARTITIONS)
                .replicas(TOPIC_REPLICATION_FACTOR)
                .build();
    }
}
