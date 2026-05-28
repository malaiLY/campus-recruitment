package com.campus.recruitment.module.job.dto;

import lombok.Data;

@Data
public class JobQueryDTO {

    private String keyword;

    private String city;

    private Integer salaryMin;

    private Integer salaryMax;

    private String education;

    private String sort;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
