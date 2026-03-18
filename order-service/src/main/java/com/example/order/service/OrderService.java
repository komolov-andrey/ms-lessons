package com.example.order.service;

import com.example.order.domain.*;
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
