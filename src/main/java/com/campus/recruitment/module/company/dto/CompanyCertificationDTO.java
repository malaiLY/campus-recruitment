package com.campus.recruitment.module.company.dto;

import lombok.Data;

@Data
public class CompanyCertificationDTO {

    private String companyName;

    private String industry;

    private String scale;

    private String city;

    private String address;

    private String contactName;

    private String contactPhone;

    private Long licenseFileId;
}
