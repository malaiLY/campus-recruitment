package com.campus.recruitment.module.interview.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewBookingVO {

    private Long id;
    private Long slotId;
    private Long applicationId;
    private Long studentId;
    private String studentName;
    private String school;
    private Long jobId;
    private String jobTitle;
    private String status;
    private LocalDateTime bookingTime;
}
