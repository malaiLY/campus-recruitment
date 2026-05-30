package com.campus.recruitment.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationStatusLogVO {

    private String beforeStatus;

    private String afterStatus;

    private String operatorType;

    private String reason;

    private LocalDateTime createTime;
}
