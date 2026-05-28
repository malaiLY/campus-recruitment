package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String username;

    @TableField("user_type")
    private String userType;

    @TableField("login_ip")
    private String loginIp;

    @TableField("user_agent")
    private String userAgent;

    private String status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("login_time")
    private LocalDateTime loginTime;
}
