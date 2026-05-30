package com.campus.recruitment.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationDetailVO {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private Long companyId;

    private String companyName;

    private String companyLogo;

    private Long resumeId;

    private String status;

    private LocalDateTime applyTime;

    private List<ApplicationStatusLogVO> statusLogs;
}
