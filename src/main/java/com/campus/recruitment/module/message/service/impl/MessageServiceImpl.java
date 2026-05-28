package com.campus.recruitment.module.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.Message;
import com.campus.recruitment.entity.MqMessageLog;
import com.campus.recruitment.mapper.MessageMapper;
import com.campus.recruitment.mapper.MqMessageLogMapper;
import com.campus.recruitment.module.message.service.MessageService;
import com.campus.recruitment.module.message.vo.MessageVO;
import com.campus.recruitment.module.message.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final MqMessageLogMapper mqMessageLogMapper;

    @Override
    public PageResult<MessageVO> getMyMessages(Integer pageNum, Integer pageSize, String messageType) {
        Long userId = LoginUserContext.getUserId();

        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, userId)
                .eq(messageType != null, Message::getMessageType, messageType)
                .orderByDesc(Message::getCreateTime);

        IPage<Message> messagePage = messageMapper.selectPage(page, queryWrapper);
        List<MessageVO> voList = messagePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, messagePage.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId) {
        Long userId = LoginUserContext.getUserId();

        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }

        if (!message.getReceiverId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该消息");
        }

        LambdaUpdateWrapper<Message> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Message::getId, messageId)
                .set(Message::getReadStatus, 1)
                .set(Message::getReadTime, LocalDateTime.now())
                .set(Message::getUpdateTime, LocalDateTime.now());
        messageMapper.update(null, updateWrapper);
    }

    @Override
    public void deleteMessage(Long messageId) {
        Long userId = LoginUserContext.getUserId();

        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "消息不存在");
        }

        if (!message.getReceiverId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该消息");
        }

        messageMapper.deleteById(messageId);
    }

    @Override
    public UnreadCountVO getUnreadCount() {
        Long userId = LoginUserContext.getUserId();

        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getReadStatus, 0);
        Long count = messageMapper.selectCount(queryWrapper);

        return new UnreadCountVO(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(String messageId, Long receiverId, Long senderId, String messageType,
                            String title, String content, String businessType, Long businessId) {
        Message existing = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>().eq(Message::getMessageId, messageId));
        if (existing != null) {
            return;
        }

        Message message = new Message();
        message.setMessageId(messageId);
        message.setReceiverId(receiverId);
        message.setSenderId(senderId);
        message.setMessageType(messageType);
        message.setTitle(title);
        message.setContent(content);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setDeleted(0);

        messageMapper.insert(message);
    }

    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setMessageId(message.getMessageId());
        vo.setMessageType(message.getMessageType());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setBusinessType(message.getBusinessType());
        vo.setBusinessId(message.getBusinessId());
        vo.setReadStatus(message.getReadStatus());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
