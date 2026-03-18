package com.example.delivery.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String instructions;
}
