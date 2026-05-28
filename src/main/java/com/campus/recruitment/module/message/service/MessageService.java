package com.campus.recruitment.module.message.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.module.message.vo.MessageVO;
import com.campus.recruitment.module.message.vo.UnreadCountVO;

public interface MessageService {

    PageResult<MessageVO> getMyMessages(Integer pageNum, Integer pageSize, String messageType);

    void markAsRead(Long messageId);

    void deleteMessage(Long messageId);

    UnreadCountVO getUnreadCount();

    void saveMessage(String messageId, Long receiverId, Long senderId, String messageType,
                     String title, String content, String businessType, Long businessId);
}
