package com.campus.recruitment.module.student.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentProfileVO {

    private Long id;

    private Long userId;

    private String realName;

    private String gender;

    private String school;

    private String major;

    private String grade;

    private String education;

    private String city;

    private String jobIntention;

    private String advantage;

    private List<String> skills;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
