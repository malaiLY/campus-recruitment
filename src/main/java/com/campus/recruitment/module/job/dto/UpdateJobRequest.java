package com.campus.recruitment.module.job.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateJobRequest {

    @NotBlank(message = "职位名称不能为空")
    private String title;

    private String category;

    @NotBlank(message = "工作城市不能为空")
    private String city;

    private Integer salaryMin;

    private Integer salaryMax;

    @NotBlank(message = "薪资单位不能为空")
    private String salaryUnit;

    private String education;

    private String experience;

    @NotBlank(message = "职位描述不能为空")
    private String description;

    @NotBlank(message = "职位要求不能为空")
    private String requirement;

    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
