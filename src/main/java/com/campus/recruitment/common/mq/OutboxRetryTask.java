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
                        .in(MqMessageLog::getSendStatus, "SEND_FAILED", "SENDING")
                        .lt(MqMessageLog::getUpdateTime, threshold)
                        .lt(MqMessageLog::getRetryCount, MAX_SEND_RETRIES)
                        .last("LIMIT 20"));

        for (MqMessageLog record : failedRecords) {
            if (record.getSendExchange() == null || record.getSendRoutingKey() == null || record.getMessageBody() == null) {
                log.warn("Outbox重试跳过: messageId={}, 缺少exchange/routingKey/body", record.getMessageId());
                continue;
            }

            try {
                Object body = objectMapper.readValue(record.getMessageBody(), Object.class);
                rabbitTemplate.convertAndSend(record.getSendExchange(), record.getSendRoutingKey(), body);
                record.setSendStatus("SENT");
                record.setRetryCount(record.getRetryCount() + 1);
                record.setErrorMessage(null);
                record.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(record);
                log.info("Outbox重试发送成功: messageId={}, retryCount={}", record.getMessageId(), record.getRetryCount());
            } catch (Exception e) {
                record.setSendStatus("SEND_FAILED");
                record.setRetryCount(record.getRetryCount() + 1);
                record.setErrorMessage(e.getMessage());
                record.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(record);
                log.warn("Outbox重试发送失败: messageId={}, retryCount={}, error={}",
                        record.getMessageId(), record.getRetryCount(), e.getMessage());
            }
        }
    }
}
