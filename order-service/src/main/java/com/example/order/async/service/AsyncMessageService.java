package com.example.order.async.service;

import com.example.order.async.domain.AsyncMessage;
import com.example.order.async.repository.AsyncMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author a.komolov
 * @date 2026-06-14
 */
@Service
@RequiredArgsConstructor
public class AsyncMessageService {

    private final AsyncMessageRepository asyncMessageRepository;

    @Transactional
    public void saveMessage(AsyncMessage asyncMessage) {
        asyncMessageRepository.save(asyncMessage);
    }

    public List<AsyncMessage> getUnsentMessages(int batchSize) {
        return asyncMessageRepository.findUnsentOutboxMessages(Pageable.ofSize(batchSize));
    }

    @Transactional
    public void markMessageAsSent(AsyncMessage asyncMessage) {
        asyncMessage.setStatus(AsyncMessage.Status.SENT);
        asyncMessageRepository.save(asyncMessage);
    }

}
