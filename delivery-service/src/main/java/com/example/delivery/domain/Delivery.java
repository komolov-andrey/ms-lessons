package com.example.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String deliveryNumber;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private LocalDateTime creationDate;
    
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    @Embedded
    private DeliveryAddress deliveryAddress;
    
    @Embedded
    private DeliveryPerson deliveryPerson;
    
    private String trackingNumber;
    private String notes;
}
