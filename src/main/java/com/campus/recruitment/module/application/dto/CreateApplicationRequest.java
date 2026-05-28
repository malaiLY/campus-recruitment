package com.campus.recruitment.module.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateApplicationRequest {

    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    private Long resumeId;
}
