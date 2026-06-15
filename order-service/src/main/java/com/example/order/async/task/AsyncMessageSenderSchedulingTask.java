package com.example.order.async.task;

import com.example.order.async.domain.AsyncMessage;
import com.example.order.async.service.AsyncMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author a.komolov
 * @date 2026-06-15
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AsyncMessageSenderSchedulingTask {

    private final AsyncMessageService asyncMessageService;
    private final AsyncMessageProcessor asyncMessageProcessor;

    @Scheduled(fixedRate = 10_000)
    public void sendOutboxMessages() {
        log.info("Sending outbox messages");
        List<AsyncMessage> unsentMessages = asyncMessageService.getUnsentMessages(50);
        for (AsyncMessage unsentMessage : unsentMessages) {
            log.info("process message {}", unsentMessage);
            asyncMessageProcessor.sendMessage(unsentMessage);
        }
    }

}
