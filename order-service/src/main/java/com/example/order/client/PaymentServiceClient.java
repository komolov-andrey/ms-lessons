package com.example.order.client;

import com.example.order.dto.PaymentRequest;
import com.example.order.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/{id}")
    PaymentResponse getPayment(@PathVariable("id") UUID id);

    @GetMapping("/api/payments/order/{orderId}")
    PaymentResponse getPaymentByOrderId(@PathVariable("orderId") String orderId);
}
