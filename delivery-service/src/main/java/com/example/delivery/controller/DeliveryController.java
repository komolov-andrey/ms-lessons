package com.example.delivery.controller;

import com.example.delivery.domain.Delivery;
import com.example.delivery.domain.DeliveryStatus;
import com.example.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    
    private final DeliveryService deliveryService;
    
    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@RequestBody Delivery delivery) {
        Delivery createdDelivery = deliveryService.createDelivery(delivery);
        return new ResponseEntity<>(createdDelivery, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Delivery> getDelivery(@PathVariable UUID id) {
        Delivery delivery = deliveryService.getDelivery(id);
        return ResponseEntity.ok(delivery);
    }
    
    @GetMapping
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        List<Delivery> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Delivery>> getDeliveriesByOrder(@PathVariable String orderId) {
        List<Delivery> deliveries = deliveryService.getDeliveriesByOrder(orderId);
        return ResponseEntity.ok(deliveries);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Delivery> updateDeliveryStatus(@PathVariable UUID id, @RequestParam String status) {
        Delivery updatedDelivery = deliveryService.updateDeliveryStatus(id, DeliveryStatus.valueOf(status));
        return ResponseEntity.ok(updatedDelivery);
    }
    
    @PostMapping("/{id}/assign")
    public ResponseEntity<Delivery> assignDeliveryPerson(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String vehicle) {
        Delivery updatedDelivery = deliveryService.assignDeliveryPerson(id, name, phone, vehicle);
        return ResponseEntity.ok(updatedDelivery);
    }
    
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<Delivery> trackDelivery(@PathVariable String trackingNumber) {
        Delivery delivery = deliveryService.trackDelivery(trackingNumber);
        return ResponseEntity.ok(delivery);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable UUID id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }
}
