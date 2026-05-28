package com.campus.recruitment.module.student.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class StudentProfileDTO {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String gender;

    @NotBlank(message = "学校不能为空")
    private String school;

    @NotBlank(message = "专业不能为空")
    private String major;

    private String grade;

    private String education;

    private String city;

    private String jobIntention;

    private List<String> skills;
}
