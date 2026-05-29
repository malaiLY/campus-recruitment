package com.campus.recruitment.module.search.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.campus.recruitment.module.search.service.JobSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                log.warn("ES同步MQ消息缺少jobId，跳过消费");
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (messageId != null) {
                MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                        new LambdaQueryWrapper<MqMessageLog>()
                                .eq(MqMessageLog::getMessageId, messageId));

                if (existingLog != null && "CONSUMED".equals(existingLog.getConsumeStatus())) {
                    log.info("ES同步消息已消费，跳过: messageId={}, action={}, jobId={}", messageId, action, jobId);
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
                        log.warn("岗位不存在，无法保存到ES: jobId={}", jobId);
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
                        log.warn("岗位不存在，无法同步到ES: jobId={}", jobId);
                    }
                    break;
                default:
                    log.warn("未知的ES同步动作: action={}", action);
                    break;
            }

            if (messageId != null) {
                MqMessageLog existingForUpdate = mqMessageLogMapper.selectOne(
                        new LambdaQueryWrapper<MqMessageLog>()
                                .eq(MqMessageLog::getMessageId, messageId));
                if (existingForUpdate != null) {
                    existingForUpdate.setConsumeStatus("CONSUMED");
                    existingForUpdate.setConsumeTime(LocalDateTime.now());
                    existingForUpdate.setRetryCount(0);
                    existingForUpdate.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.updateById(existingForUpdate);
                } else {
                    MqMessageLog logRecord = new MqMessageLog();
                    logRecord.setMessageId(messageId);
                    logRecord.setMessageType("JOB_ES_SYNC");
                    logRecord.setBusinessId(jobId);
                    logRecord.setConsumeStatus("CONSUMED");
                    logRecord.setConsumeTime(LocalDateTime.now());
                    logRecord.setRetryCount(0);
                    logRecord.setCreateTime(LocalDateTime.now());
                    logRecord.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.insert(logRecord);
                }
            }

            channel.basicAck(deliveryTag, false);
            log.info("ES同步MQ消息消费成功: action={}, jobId={}", action, jobId);

        } catch (com.fasterxml.jackson.core.JsonProcessingException jsonEx) {
            log.error("ES同步消息反序列化失败，直接进入DLQ: {}", jsonEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("ES同步MQ消息消费失败: {}", e.getMessage(), e);
            handleConsumeError(message, channel, deliveryTag, e);
        }
    }

    private void handleConsumeError(Message message, Channel channel, long deliveryTag, Exception e) throws IOException {
        int retryCount = 0;
        try {
            Map<String, Object> body = parseMessage(message);
            String messageId = (String) body.get("messageId");
            String action = (String) body.get("action");
            Long jobId = toLong(body.get("jobId"));

            if (messageId != null) {
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
                    logRecord.setRetryCount(1);
                    logRecord.setCreateTime(LocalDateTime.now());
                    logRecord.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.insert(logRecord);
                    retryCount = 1;
                } else {
                    logRecord.setConsumeStatus("CONSUME_FAILED");
                    logRecord.setErrorMessage(e.getMessage());
                    logRecord.setRetryCount(logRecord.getRetryCount() + 1);
                    logRecord.setUpdateTime(LocalDateTime.now());
                    mqMessageLogMapper.updateById(logRecord);
                    retryCount = logRecord.getRetryCount();
                }
            }
        } catch (Exception parseException) {
            log.error("记录ES同步消费失败日志异常: {}", parseException.getMessage());
        }

        if (retryCount < 3) {
            channel.basicNack(deliveryTag, false, true);
        } else {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Message message) throws IOException {
        String body = new String(message.getBody(), "UTF-8");
        log.info("收到ES同步MQ消息: {}", body);
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
}
