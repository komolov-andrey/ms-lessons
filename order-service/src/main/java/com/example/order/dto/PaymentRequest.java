package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String orderId;
    private String orderNumber;
    private BigDecimal amount;
    private Currency currency;
    private String paymentMethod;
    private PaymentCardDto cardDetails;
}