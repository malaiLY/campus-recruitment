package com.campus.recruitment.module.auth.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.auth.dto.LoginRequest;
import com.campus.recruitment.module.auth.dto.RegisterRequest;
import com.campus.recruitment.module.auth.service.AuthService;
import com.campus.recruitment.module.auth.vo.LoginVO;
import com.campus.recruitment.module.auth.vo.RegisterVO;
import com.campus.recruitment.module.auth.vo.TokenRefreshVO;
import com.campus.recruitment.module.auth.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public R<RegisterVO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return R.ok(authService.login(request, ip, userAgent));
    }

    @PostMapping("/logout")
    @RequireLogin
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    @RequireLogin
    public R<UserInfoVO> getCurrentUser() {
        return R.ok(authService.getCurrentUser());
    }

    @GetMapping("/permissions")
    @RequireLogin
    public R<List<String>> getPermissions() {
        return R.ok(authService.getPermissions());
    }

    @PostMapping("/refresh-token")
    @RequireLogin
    public R<TokenRefreshVO> refreshToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return R.fail(401, "未提供 Token");
        }
        return R.ok(authService.refreshToken(token));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
