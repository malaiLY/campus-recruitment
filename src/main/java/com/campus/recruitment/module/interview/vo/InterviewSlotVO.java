package com.campus.recruitment.module.interview.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewSlotVO {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long companyId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer remainCount;
    private String interviewType;
    private String location;
    private String status;
}
