package com.example.payment.service.impl;

import com.example.payment.domain.IdempotencyKey;
import com.example.payment.domain.KeyStatus;
import com.example.payment.repository.IdempotencyRepository;
import com.example.payment.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author a.komolov
 * @date 2026-04-06
 */
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    @Override
    public void createPendingKey(String key) {
        idempotencyRepository.save(new IdempotencyKey(key, KeyStatus.PENDING));
    }

    @Override
    @Transactional
    public Optional<IdempotencyKey> getByIkey(String key) {
        return idempotencyRepository.findByIkey(key);
    }

    @Override
    @Transactional
    public void markKeyAsCompleted(String key, String responseData, int statusCode) {
        idempotencyRepository.findByIkey(key).ifPresentOrElse(idempotency -> {
            idempotency.setStatus(KeyStatus.COMPLETED);
            idempotency.setResponse(responseData);
            idempotency.setStatusCode(statusCode);
            idempotencyRepository.save(idempotency);
        }, () -> {
            throw new IllegalArgumentException("Key " + key + " not found");
        });
    }
}
