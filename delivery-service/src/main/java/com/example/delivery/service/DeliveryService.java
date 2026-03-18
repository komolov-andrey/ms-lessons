package com.example.delivery.service;

import com.example.delivery.domain.Delivery;
import com.example.delivery.domain.DeliveryStatus;
import com.example.delivery.domain.DeliveryPerson;
import com.example.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    
    private final DeliveryRepository deliveryRepository;
    
    @Transactional
    public Delivery createDelivery(Delivery delivery) {
        delivery.setDeliveryNumber(generateDeliveryNumber());
        delivery.setCreationDate(LocalDateTime.now());
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setTrackingNumber(generateTrackingNumber());
        delivery.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(3));
        
        log.info("Creating delivery for order: {}", delivery.getOrderId());
        return deliveryRepository.save(delivery);
    }
    
    @Transactional(readOnly = true)
    public Delivery getDelivery(UUID id) {
        return deliveryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }
    
    @Transactional(readOnly = true)
    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Delivery> getDeliveriesByOrder(String orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }
    
    @Transactional
    public Delivery updateDeliveryStatus(UUID id, DeliveryStatus status) {
        Delivery delivery = getDelivery(id);
        delivery.setStatus(status);
        
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryDate(LocalDateTime.now());
        }
        
        log.info("Updating delivery {} status to: {}", delivery.getDeliveryNumber(), status);
        return deliveryRepository.save(delivery);
    }
    
    @Transactional
    public Delivery assignDeliveryPerson(UUID id, String name, String phone, String vehicle) {
        Delivery delivery = getDelivery(id);
        
        DeliveryPerson person = new DeliveryPerson(name, phone, vehicle);
        delivery.setDeliveryPerson(person);
        delivery.setStatus(DeliveryStatus.PROCESSING);
        
        log.info("Assigning delivery person to: {}", delivery.getDeliveryNumber());
        return deliveryRepository.save(delivery);
    }
    
    @Transactional
    public void deleteDelivery(UUID id) {
        Delivery delivery = getDelivery(id);
        deliveryRepository.delete(delivery);
        log.info("Deleted delivery: {}", delivery.getDeliveryNumber());
    }
    
    @Transactional(readOnly = true)
    public Delivery trackDelivery(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new RuntimeException("Delivery not found with tracking number: " + trackingNumber));
    }
    
    private String generateDeliveryNumber() {
        return "DEL-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String generateTrackingNumber() {
        return "TRK" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
