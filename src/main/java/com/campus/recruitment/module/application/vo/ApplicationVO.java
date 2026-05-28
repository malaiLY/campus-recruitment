package com.campus.recruitment.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String school;
    private String major;
    private String education;
    private Long jobId;
    private String jobTitle;
    private Long companyId;
    private String companyName;
    private Long resumeId;
    private String status;
    private LocalDateTime applyTime;
}
