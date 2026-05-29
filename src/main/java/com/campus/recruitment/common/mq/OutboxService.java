package com.campus.recruitment.common.mq;

import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxService {

    private final RabbitTemplate rabbitTemplate;
    private final MqMessageLogMapper mqMessageLogMapper;

    public void sendAfterCommit(String exchange, String routingKey, Object message,
                                String messageId, String messageType, Long businessId) {
        MqMessageLog logRecord = new MqMessageLog();
        logRecord.setMessageId(messageId);
        logRecord.setMessageType(messageType);
        logRecord.setBusinessId(businessId);
        logRecord.setConsumeStatus("INIT");
        logRecord.setRetryCount(0);
        logRecord.setCreateTime(LocalDateTime.now());
        logRecord.setUpdateTime(LocalDateTime.now());
        mqMessageLogMapper.insert(logRecord);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rabbitTemplate.convertAndSend(exchange, routingKey, message);
                    logRecord.setConsumeStatus("SUCCESS");
                    logRecord.setConsumeTime(LocalDateTime.now());
                    log.info("Outbox MQ发送成功: messageId={}", messageId);
                } catch (Exception e) {
                    logRecord.setConsumeStatus("FAIL");
                    logRecord.setErrorMessage(e.getMessage());
                    log.error("Outbox MQ发送失败: messageId={}, error={}", messageId, e.getMessage(), e);
                }
                mqMessageLogMapper.updateById(logRecord);
            }
        });
    }
}
