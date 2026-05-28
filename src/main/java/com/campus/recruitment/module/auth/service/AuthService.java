package com.campus.recruitment.module.auth.service;

import com.campus.recruitment.module.auth.dto.LoginRequest;
import com.campus.recruitment.module.auth.dto.RegisterRequest;
import com.campus.recruitment.module.auth.vo.LoginVO;
import com.campus.recruitment.module.auth.vo.RegisterVO;
import com.campus.recruitment.module.auth.vo.TokenRefreshVO;
import com.campus.recruitment.module.auth.vo.UserInfoVO;

import java.util.List;

public interface AuthService {

    RegisterVO register(RegisterRequest request);

    LoginVO login(LoginRequest request, String ip, String userAgent);

    void logout();

    UserInfoVO getCurrentUser();

    List<String> getPermissions();

    TokenRefreshVO refreshToken(String oldToken);
}
