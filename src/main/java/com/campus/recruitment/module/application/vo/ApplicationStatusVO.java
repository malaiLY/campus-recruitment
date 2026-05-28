package com.campus.recruitment.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationStatusVO {

    private Long applicationId;
    private String status;
    private LocalDateTime applyTime;
}
