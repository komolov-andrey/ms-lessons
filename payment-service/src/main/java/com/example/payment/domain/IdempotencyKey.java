package com.example.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "idempotency_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {
    
    @Id
    @Column(nullable = false, unique = true)
    private String key;
    
    @Enumerated(EnumType.STRING)
    private KeyStatus status;
    
    @Lob
    private String response;

    private int statusCode;

    public IdempotencyKey(String key, KeyStatus status) {
        this.key = key;
        this.status = status;
    }
}
