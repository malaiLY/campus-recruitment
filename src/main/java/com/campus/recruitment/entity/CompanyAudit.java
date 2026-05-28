package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("company_audit")
public class CompanyAudit {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("audit_status")
    private String auditStatus;

    @TableField("audit_reason")
    private String auditReason;

    @TableField("auditor_id")
    private Long auditorId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
