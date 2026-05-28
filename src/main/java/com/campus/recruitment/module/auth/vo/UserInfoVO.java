package com.campus.recruitment.module.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String userType;

    private List<String> roles;

    private List<String> permissions;
}
