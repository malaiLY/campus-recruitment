package com.campus.recruitment.module.message.vo;

import lombok.Data;

@Data
public class UnreadCountVO {
    private Long unreadCount;

    public UnreadCountVO(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
