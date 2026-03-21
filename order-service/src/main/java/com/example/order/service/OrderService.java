package com.example.order.service;

import com.example.order.client.PaymentServiceClient;
import com.example.order.domain.*;
import com.example.order.dto.PaymentCardDto;
import com.example.order.dto.PaymentRequest;
import com.example.order.dto.PaymentResponse;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    
    @Transactional
    public Order createOrder(Order order) {
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        
        Money total = order.getItems().stream()
            .map(item -> item.getPrice())
            .reduce(new Money(BigDecimal.ZERO, Currency.getInstance("USD")), 
                (acc, money) -> acc.add(money));
        order.setTotalAmount(total);
        
        log.info("Creating new order: {}", order.getOrderNumber());
        return orderRepository.save(order);
    }

    @Transactional
    public Order processOrderWithPayment(UUID orderId, PaymentCardDto cardDetails) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Order is not in CREATED state");
        }

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        // Создаем запрос на оплату
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .amount(order.getTotalAmount().getAmount())
                .currency(order.getTotalAmount().getCurrency())
                .paymentMethod("CREDIT_CARD")
                .cardDetails(cardDetails)
                .build();

        try {
            // Отправляем запрос в payment-service
            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);

            if ("COMPLETED".equals(paymentResponse.getStatus())) {
                order.setPaymentId(paymentResponse.getPaymentId().toString());
                order.setStatus(OrderStatus.PAID);
                log.info("Payment successful for order: {}", order.getOrderNumber());
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                log.error("Payment failed for order: {} - {}", order.getOrderNumber(),
                        paymentResponse.getErrorMessage());
            }

            return orderRepository.save(order);

        } catch (Exception e) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.error("Error processing payment for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
    
    @Transactional
    public Order updateOrderStatus(UUID id, OrderStatus status) {
        Order order = getOrder(id);
        order.setStatus(status);
        log.info("Updating order {} status to: {}", order.getOrderNumber(), status);
        return orderRepository.save(order);
    }
    
    @Transactional
    public void deleteOrder(UUID id) {
        Order order = getOrder(id);
        orderRepository.delete(order);
        log.info("Deleted order: {}", order.getOrderNumber());
    }
    
    @Transactional
    public Order updatePaymentInfo(UUID id, String paymentId) {
        Order order = getOrder(id);
        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order updateDeliveryInfo(UUID id, String deliveryId) {
        Order order = getOrder(id);
        order.setDeliveryId(deliveryId);
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }
    
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
