package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String username;

    private String module;

    private String operation;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_ip")
    private String requestIp;

    @TableField("request_param")
    private String requestParam;

    @TableField("result_status")
    private String resultStatus;

    @TableField("error_message")
    private String errorMessage;

    @TableField("cost_time")
    private Long costTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
