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
        logRecord.setSendStatus("SENDING");
        logRecord.setConsumeStatus("INIT");
        logRecord.setRetryCount(0);
        logRecord.setCreateTime(LocalDateTime.now());
        logRecord.setUpdateTime(LocalDateTime.now());
        mqMessageLogMapper.insert(logRecord);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("No active transaction synchronization, sending MQ directly: messageId={}", messageId);
            sendAndUpdateLog(logRecord, exchange, routingKey, message, messageId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendAndUpdateLog(logRecord, exchange, routingKey, message, messageId);
            }
        });
    }

    private void sendAndUpdateLog(MqMessageLog logRecord, String exchange, String routingKey,
                                  Object message, String messageId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            logRecord.setSendStatus("SENT");
            logRecord.setUpdateTime(LocalDateTime.now());
            log.info("Outbox MQ发送成功: messageId={}", messageId);
        } catch (Exception e) {
            logRecord.setSendStatus("SEND_FAILED");
            logRecord.setErrorMessage(e.getMessage());
            logRecord.setUpdateTime(LocalDateTime.now());
            log.error("Outbox MQ发送失败: messageId={}, error={}", messageId, e.getMessage(), e);
        }
        mqMessageLogMapper.updateById(logRecord);
    }
}
