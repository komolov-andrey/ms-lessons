package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCardDto {
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
}