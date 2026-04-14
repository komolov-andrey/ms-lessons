package com.example.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Entity
@Table(name = "idempotency_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(nullable = false, unique = true)
    private String ikey;
    
    @Enumerated(EnumType.STRING)
    private KeyStatus status;
    
    @Lob
    private String response;

    private int statusCode;

    public IdempotencyKey(String ikey, KeyStatus status) {
        this.ikey = ikey;
        this.status = status;
    }
}
