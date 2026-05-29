package com.campus.recruitment.module.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyProfileDTO {

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称不超过128字符")
    private String companyName;

    @Size(max = 64)
    private String industry;

    @Size(max = 64)
    private String scale;

    @Size(max = 64)
    private String city;

    @Size(max = 255)
    private String address;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64)
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    private Long licenseFileId;
}
