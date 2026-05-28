package com.campus.recruitment.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditRequest {

    @NotBlank(message = "审核状态不能为空")
    private String auditStatus;

    private String reason;
}
