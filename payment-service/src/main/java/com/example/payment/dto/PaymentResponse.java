package com.example.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private String paymentNumber;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private Currency currency;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String errorMessage;
}