package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private String messageId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("message_type")
    private String messageType;

    private String title;

    private String content;

    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private Long businessId;

    @TableField("read_status")
    private String readStatus;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
