package com.campus.recruitment.module.company.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyProfileVO {

    private Long id;

    private Long userId;

    private String companyName;

    private String industry;

    private String scale;

    private String city;

    private String address;

    private String contactName;

    private String contactPhone;

    private Long licenseFileId;

    private String auditStatus;

    private String auditReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
