package com.campus.recruitment.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginVO {

    private String token;

    private LocalDateTime expireTime;

    private Long userId;

    private String username;

    private String userType;

    private List<String> permissions;
}
