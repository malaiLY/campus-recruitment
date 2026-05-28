package com.campus.recruitment.module.resume.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeVO {
    private Long id;
    private String resumeName;
    private Long fileId;
    private String originalName;
    private Long fileSize;
    private Integer isDefault;
    private LocalDateTime createTime;
}
