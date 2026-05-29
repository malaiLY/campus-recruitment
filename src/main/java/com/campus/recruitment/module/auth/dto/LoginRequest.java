package com.campus.recruitment.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名不超过64字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码不超过128字符")
    private String password;

    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "STUDENT|COMPANY|ADMIN", message = "用户类型无效")
    private String userType;
}
