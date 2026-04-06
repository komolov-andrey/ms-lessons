package com.example.payment.service;

import com.example.payment.domain.IdempotencyKey;

import java.util.Optional;

/**
 * @author a.komolov
 * @date 2026-04-06
 */
public interface IdempotencyService {

    void createPendingKey(String key);

    Optional<IdempotencyKey> getByKey(String key);

    void markKeyAsCompleted(String key, String responseData, int statusCode);
}
