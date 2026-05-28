package com.campus.recruitment.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyApplicationVO {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String companyLogo;
    private String status;
    private LocalDateTime applyTime;
    private Long resumeId;
}
