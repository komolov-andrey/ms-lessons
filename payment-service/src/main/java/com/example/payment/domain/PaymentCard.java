package com.example.payment.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCard {
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
}
