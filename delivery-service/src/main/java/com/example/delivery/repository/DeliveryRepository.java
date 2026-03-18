package com.example.delivery.repository;

import com.example.delivery.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);
    List<Delivery> findByOrderId(String orderId);
    List<Delivery> findByStatus(String status);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
}
