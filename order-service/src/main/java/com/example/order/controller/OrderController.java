package com.example.order.controller;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.PaymentCardDto;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @PostMapping("/{orderId}/process-payment")
    public ResponseEntity<Order> processPayment(
            @PathVariable UUID orderId,
            @RequestBody PaymentCardDto cardDetails) {
        Order processedOrder = orderService.processOrderWithPayment(orderId, cardDetails);
        return ResponseEntity.ok(processedOrder);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable Long customerId) {
        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable UUID id, @RequestParam String status) {
        Order updatedOrder = orderService.updateOrderStatus(id, OrderStatus.valueOf(status));
        return ResponseEntity.ok(updatedOrder);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/payment")
    public ResponseEntity<Order> updatePayment(@PathVariable UUID id, @RequestParam String paymentId) {
        Order updatedOrder = orderService.updatePaymentInfo(id, paymentId);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PutMapping("/{id}/delivery")
    public ResponseEntity<Order> updateDelivery(@PathVariable UUID id, @RequestParam String deliveryId) {
        Order updatedOrder = orderService.updateDeliveryInfo(id, deliveryId);
        return ResponseEntity.ok(updatedOrder);
    }
}
