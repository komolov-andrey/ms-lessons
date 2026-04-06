package com.example.payment.repository;

import com.example.payment.domain.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyKey> findByKey(String key);
}
