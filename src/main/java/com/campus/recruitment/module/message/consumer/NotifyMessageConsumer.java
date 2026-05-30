package com.campus.recruitment.module.message.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.campus.recruitment.module.message.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyMessageConsumer {

    private final MessageService messageService;
    private final MqMessageLogMapper mqMessageLogMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConstants.NOTIFY_QUEUE)
    public void handleMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            Map<String, Object> notifyMessage = parseMessage(message);
            String messageId = (String) notifyMessage.get("messageId");

            if (messageId == null) {
                log.error("Notify message is missing messageId, send to DLQ");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                    new LambdaQueryWrapper<MqMessageLog>()
                            .eq(MqMessageLog::getMessageId, messageId));

            if (existingLog != null && "CONSUMED".equals(existingLog.getConsumeStatus())) {
                log.info("Notify message already consumed, skip: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Long receiverId = toLong(notifyMessage.get("receiverId"));
            Long senderId = toLong(notifyMessage.get("senderId"));
            String messageType = (String) notifyMessage.get("messageType");
            String title = (String) notifyMessage.get("title");
            String content = (String) notifyMessage.get("content");
            String businessType = (String) notifyMessage.get("businessType");
            Long businessId = toLong(notifyMessage.get("businessId"));

            messageService.saveMessage(messageId, receiverId, senderId, messageType,
                    title, content, businessType, businessId);

            if (existingLog != null) {
                existingLog.setConsumeStatus("CONSUMED");
                existingLog.setConsumeTime(LocalDateTime.now());
                existingLog.setConsumeRetryCount(0);
                existingLog.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(existingLog);
            } else {
                MqMessageLog logRecord = new MqMessageLog();
                logRecord.setMessageId(messageId);
                logRecord.setMessageType(messageType);
                logRecord.setBusinessId(businessId);
                logRecord.setConsumeStatus("CONSUMED");
                logRecord.setSendRetryCount(0);
                logRecord.setConsumeRetryCount(0);
                logRecord.setConsumeTime(LocalDateTime.now());
                logRecord.setCreateTime(LocalDateTime.now());
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.insert(logRecord);
            }

            channel.basicAck(deliveryTag, false);
            log.info("Notify message consumed: messageId={}, receiverId={}", messageId, receiverId);

        } catch (JsonProcessingException jsonEx) {
            log.error("Notify message is invalid JSON, send to DLQ: {}", jsonEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (NumberFormatException formatEx) {
            log.error("Notify message has invalid numeric field, send to DLQ: {}", formatEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("Notify message consume failed: {}", e.getMessage(), e);
            handleConsumeError(message, channel, deliveryTag, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Message message) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received notify message: {}", body);
        return objectMapper.readValue(body, Map.class);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private void handleConsumeError(Message message, Channel channel, long deliveryTag, Exception e) throws IOException {
        int retryCount;
        try {
            Map<String, Object> notifyMessage = parseMessage(message);
            String messageId = (String) notifyMessage.get("messageId");
            if (messageId == null) {
                log.error("Notify message failed and has no messageId, send to DLQ");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            String messageType = (String) notifyMessage.get("messageType");
            Long businessId = toLong(notifyMessage.get("businessId"));

            MqMessageLog logRecord = mqMessageLogMapper.selectOne(
                    new LambdaQueryWrapper<MqMessageLog>()
                            .eq(MqMessageLog::getMessageId, messageId));

            if (logRecord == null) {
                logRecord = new MqMessageLog();
                logRecord.setMessageId(messageId);
                logRecord.setMessageType(messageType);
                logRecord.setBusinessId(businessId);
                logRecord.setConsumeStatus("CONSUME_FAILED");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setSendRetryCount(0);
                logRecord.setConsumeRetryCount(1);
                logRecord.setCreateTime(LocalDateTime.now());
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.insert(logRecord);
                retryCount = 1;
            } else {
                retryCount = nextConsumeRetryCount(logRecord);
                logRecord.setConsumeStatus("CONSUME_FAILED");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setConsumeRetryCount(retryCount);
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(logRecord);
            }
        } catch (Exception parseException) {
            log.error("Failed to record notify consume error, send to DLQ: {}", parseException.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        if (retryCount < 3) {
            channel.basicNack(deliveryTag, false, true);
        } else {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private int nextConsumeRetryCount(MqMessageLog record) {
        return (record.getConsumeRetryCount() == null ? 0 : record.getConsumeRetryCount()) + 1;
    }
}
