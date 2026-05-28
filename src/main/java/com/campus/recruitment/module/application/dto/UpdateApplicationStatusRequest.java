package com.campus.recruitment.module.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private String reason;
}
