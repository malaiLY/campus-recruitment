package com.campus.recruitment.module.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String messageId;
    private String messageType;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private Integer readStatus;
    private LocalDateTime createTime;
}
