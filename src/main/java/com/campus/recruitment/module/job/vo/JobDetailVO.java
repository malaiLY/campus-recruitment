package com.campus.recruitment.module.job.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDetailVO {

    private Long id;

    private Long companyId;

    private String companyName;

    private String title;

    private String category;

    private String city;

    private Integer salaryMin;

    private Integer salaryMax;

    private String salaryUnit;

    private String education;

    private String experience;

    private List<String> tags;

    private String status;

    private Integer viewCount;

    private Integer applyCount;

    private Integer favoriteCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String description;

    private String requirement;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private boolean isFavorited;
}
