package com.campus.recruitment.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenRefreshVO {

    private String token;

    private LocalDateTime expireTime;

    private Long userId;

    private String username;

    private String userType;
}