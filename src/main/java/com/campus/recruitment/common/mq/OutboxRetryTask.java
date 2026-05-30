package com.campus.recruitment.common.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRetryTask {

    private final MqMessageLogMapper mqMessageLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_SEND_RETRIES = 5;

    @Scheduled(fixedDelay = 30_000)
    public void retryFailedOutbox() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        List<MqMessageLog> failedRecords = mqMessageLogMapper.selectList(
                new LambdaQueryWrapper<MqMessageLog>()
                        .in(MqMessageLog::getSendStatus, "SEND_FAILED", "SENDING", "RETRYING")
                        .lt(MqMessageLog::getUpdateTime, threshold)
                        .lt(MqMessageLog::getSendRetryCount, MAX_SEND_RETRIES)
                        .orderByAsc(MqMessageLog::getUpdateTime, MqMessageLog::getId)
                        .last("LIMIT 20"));

        for (MqMessageLog record : failedRecords) {
            if (mqMessageLogMapper.claimRetry(record.getId(), threshold, MAX_SEND_RETRIES) != 1) {
                continue;
            }

            if (record.getSendExchange() == null || record.getSendRoutingKey() == null || record.getMessageBody() == null) {
                log.warn("Skip outbox retry because exchange/routingKey/body is missing: messageId={}", record.getMessageId());
                record.setSendStatus("SEND_FAILED");
                record.setSendRetryCount(nextSendRetryCount(record));
                record.setErrorMessage("Missing exchange/routingKey/body for outbox retry");
                record.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(record);
                continue;
            }

            try {
                Object body = objectMapper.readValue(record.getMessageBody(), Object.class);
                rabbitTemplate.convertAndSend(record.getSendExchange(), record.getSendRoutingKey(), body);
                record.setSendStatus("SENT");
                record.setSendRetryCount(nextSendRetryCount(record));
                record.setErrorMessage(null);
                record.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(record);
                log.info("Outbox retry sent: messageId={}, sendRetryCount={}",
                        record.getMessageId(), record.getSendRetryCount());
            } catch (Exception e) {
                record.setSendStatus("SEND_FAILED");
                record.setSendRetryCount(nextSendRetryCount(record));
                record.setErrorMessage(e.getMessage());
                record.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(record);
                log.warn("Outbox retry failed: messageId={}, sendRetryCount={}, error={}",
                        record.getMessageId(), record.getSendRetryCount(), e.getMessage());
            }
        }
    }

    private int nextSendRetryCount(MqMessageLog record) {
        return (record.getSendRetryCount() == null ? 0 : record.getSendRetryCount()) + 1;
    }
}
