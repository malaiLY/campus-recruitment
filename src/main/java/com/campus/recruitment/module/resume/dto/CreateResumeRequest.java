package com.campus.recruitment.module.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateResumeRequest {
    @NotBlank(message = "简历名称不能为空")
    private String resumeName;

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    private boolean isDefault;
}
