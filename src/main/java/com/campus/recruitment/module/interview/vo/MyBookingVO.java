package com.campus.recruitment.module.interview.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyBookingVO {

    private Long id;
    private Long slotId;
    private String slotTitle;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String interviewType;
    private String location;
    private String status;
    private LocalDateTime bookingTime;
}
