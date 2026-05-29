package com.campus.recruitment.module.search.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.campus.recruitment.module.search.service.JobSearchService;
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
public class JobEsSyncConsumer {

    private final JobSearchService jobSearchService;
    private final JobMapper jobMapper;
    private final ObjectMapper objectMapper;
    private final MqMessageLogMapper mqMessageLogMapper;

    @RabbitListener(queues = RabbitMQConstants.JOB_ES_QUEUE)
    public void handleMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            Map<String, Object> body = parseMessage(message);
            String messageId = (String) body.get("messageId");
            String action = (String) body.get("action");
            Long jobId = toLong(body.get("jobId"));

            if (jobId == null) {
                log.warn("ES sync message is missing jobId, ack and skip");
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (messageId != null) {
                MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                        new LambdaQueryWrapper<MqMessageLog>()
                                .eq(MqMessageLog::getMessageId, messageId));

                if (existingLog != null && "CONSUMED".equals(existingLog.getConsumeStatus())) {
                    log.info("ES sync message already consumed, skip: messageId={}, action={}, jobId={}",
                            messageId, action, jobId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            switch (action) {
                case "save":
                    Job jobForSave = jobMapper.selectById(jobId);
                    if (jobForSave != null) {
                        jobSearchService.saveJob(jobForSave);
                    } else {
                        log.warn("Job does not exist, skip ES save: jobId={}", jobId);
                    }
                    break;
                case "delete":
                    jobSearchService.deleteJob(jobId);
                    break;
                case "update":
                    Job jobForUpdate = jobMapper.selectById(jobId);
                    if (jobForUpdate != null) {
                        jobSearchService.syncJob(jobForUpdate);
                    } else {
                        log.warn("Job does not exist, skip ES update: jobId={}", jobId);
                    }
                    break;
                default:
                    log.warn("Unknown ES sync action: action={}", action);
                    break;
            }

            if (messageId != null) {
                MqMessageLog existingForUpdate = mqMessageLogMapper.selectOne(
                        new LambdaQueryWrapper<MqMessageLog>()
                                .eq(MqMessageLog::getMessageId, messageId));
                if (existingForUpdate != null) {
                    existingForUpdate.setConsumeStatus("CONSUMED");
                    existingForUpdate.setConsumeTime(LocalDateTime.now());
                    existingForUpdate.setConsumeRetryCount(0);
                    existingForUpdate.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.updateById(existingForUpdate);
                } else {
                    MqMessageLog logRecord = new MqMessageLog();
                    logRecord.setMessageId(messageId);
                    logRecord.setMessageType("JOB_ES_SYNC");
                    logRecord.setBusinessId(jobId);
                    logRecord.setConsumeStatus("CONSUMED");
                    logRecord.setSendRetryCount(0);
                    logRecord.setConsumeRetryCount(0);
                    logRecord.setConsumeTime(LocalDateTime.now());
                    logRecord.setCreateTime(LocalDateTime.now());
                    logRecord.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.insert(logRecord);
                }
            }

            channel.basicAck(deliveryTag, false);
            log.info("ES sync message consumed: action={}, jobId={}", action, jobId);

        } catch (JsonProcessingException jsonEx) {
            log.error("ES sync message is invalid JSON, send to DLQ: {}", jsonEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (NumberFormatException formatEx) {
            log.error("ES sync message has invalid numeric field, send to DLQ: {}", formatEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("ES sync message consume failed: {}", e.getMessage(), e);
            handleConsumeError(message, channel, deliveryTag, e);
        }
    }

    private void handleConsumeError(Message message, Channel channel, long deliveryTag, Exception e) throws IOException {
        int retryCount;
        try {
            Map<String, Object> body = parseMessage(message);
            String messageId = (String) body.get("messageId");
            if (messageId == null) {
                log.error("ES sync message failed and has no messageId, send to DLQ");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            Long jobId = toLong(body.get("jobId"));

            MqMessageLog logRecord = mqMessageLogMapper.selectOne(
                    new LambdaQueryWrapper<MqMessageLog>()
                            .eq(MqMessageLog::getMessageId, messageId));

            if (logRecord == null) {
                logRecord = new MqMessageLog();
                logRecord.setMessageId(messageId);
                logRecord.setMessageType("JOB_ES_SYNC");
                logRecord.setBusinessId(jobId);
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
            log.error("Failed to record ES sync consume error, send to DLQ: {}", parseException.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        if (retryCount < 3) {
            channel.basicNack(deliveryTag, false, true);
        } else {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Message message) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received ES sync message: {}", body);
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

    private int nextConsumeRetryCount(MqMessageLog record) {
        return (record.getConsumeRetryCount() == null ? 0 : record.getConsumeRetryCount()) + 1;
    }
}
