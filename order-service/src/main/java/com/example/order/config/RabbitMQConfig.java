package com.example.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_REQUEST_QUEUE = "payment.request.queue";
    public static final String PAYMENT_RESPONSE_QUEUE = "payment.response.queue";
    public static final String PAYMENT_REQUEST_ROUTING_KEY = "payment.request";
    public static final String PAYMENT_RESPONSE_ROUTING_KEY = "payment.response";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue paymentResponseQueue() {
        return QueueBuilder.durable(PAYMENT_RESPONSE_QUEUE).build();
    }

    @Bean
    public Binding paymentRequestBinding(Queue paymentRequestQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentRequestQueue).to(paymentExchange).with(PAYMENT_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding paymentResponseBinding(Queue paymentResponseQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentResponseQueue).to(paymentExchange).with(PAYMENT_RESPONSE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
