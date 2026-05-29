package com.campus.recruitment.module.message.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.campus.recruitment.module.message.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyMessageConsumer {

    private final MessageService messageService;
    private final MqMessageLogMapper mqMessageLogMapper;

    @RabbitListener(queues = RabbitMQConstants.NOTIFY_QUEUE)
    public void handleMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            Map<String, Object> notifyMessage = parseMessage(message);
            String messageId = (String) notifyMessage.get("messageId");

            if (messageId == null) {
                log.warn("MQ消息缺少messageId，跳过消费");
                channel.basicAck(deliveryTag, false);
                return;
            }

            MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                    new LambdaQueryWrapper<MqMessageLog>()
                            .eq(MqMessageLog::getMessageId, messageId));

            if (existingLog != null && "SUCCESS".equals(existingLog.getConsumeStatus())) {
                log.info("消息已消费，跳过: messageId={}", messageId);
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
                MqMessageLog updateRecord = new MqMessageLog();
                updateRecord.setId(existingLog.getId());
                updateRecord.setConsumeStatus("SUCCESS");
                updateRecord.setConsumeTime(LocalDateTime.now());
                updateRecord.setRetryCount(0);
                updateRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(updateRecord);
            } else {
                MqMessageLog logRecord = new MqMessageLog();
                logRecord.setMessageId(messageId);
                logRecord.setMessageType(messageType);
                logRecord.setBusinessId(businessId);
                logRecord.setConsumeStatus("SUCCESS");
                logRecord.setConsumeTime(LocalDateTime.now());
                logRecord.setRetryCount(0);
                logRecord.setCreateTime(LocalDateTime.now());
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.insert(logRecord);
            }

            channel.basicAck(deliveryTag, false);
            log.info("消息消费成功: messageId={}, receiverId={}", messageId, receiverId);

        } catch (Exception e) {
            log.error("消息消费失败: {}", e.getMessage(), e);
            handleConsumeError(message, channel, deliveryTag, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Message message) throws IOException {
        String body = new String(message.getBody(), "UTF-8");
        log.info("收到MQ消息: {}", body);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(body, Map.class);
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
        try {
            Map<String, Object> notifyMessage = parseMessage(message);
            String messageId = (String) notifyMessage.get("messageId");
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
                logRecord.setConsumeStatus("FAIL");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setRetryCount(1);
                logRecord.setCreateTime(LocalDateTime.now());
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.insert(logRecord);
            } else {
                logRecord.setConsumeStatus("FAIL");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setRetryCount(logRecord.getRetryCount() + 1);
                logRecord.setUpdateTime(LocalDateTime.now());
                mqMessageLogMapper.updateById(logRecord);
            }
        } catch (Exception parseException) {
            log.error("记录消费失败日志异常: {}", parseException.getMessage());
        }

        channel.basicNack(deliveryTag, false, false);
    }
}
