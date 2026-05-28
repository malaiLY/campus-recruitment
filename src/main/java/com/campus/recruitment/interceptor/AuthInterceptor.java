package com.campus.recruitment.interceptor;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.constant.RedisConstants;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.ttl-hours}")
    private int jwtTtlHours;

    private static final long REFRESH_THRESHOLD_HOURS = 2;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireLogin requireLogin = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (requireLogin == null) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            sendUnauthorized(response, "未登录");
            return false;
        }

        String tokenKey = RedisConstants.LOGIN_TOKEN_PREFIX + token;
        String userIdStr = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            sendUnauthorized(response, "登录已过期");
            return false;
        }

        Long userId = Long.parseLong(userIdStr);
        String usernameKey = "campus:login:user:" + userId;
        String username = stringRedisTemplate.opsForHash().get(usernameKey, "username").toString();
        String userType = stringRedisTemplate.opsForHash().get(usernameKey, "userType").toString();

        LoginUserContext.LoginUser loginUser = new LoginUserContext.LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(username);
        loginUser.setUserType(userType);
        LoginUserContext.set(loginUser);

        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        checkAndSetRefreshHeader(token, response);

        return true;
    }

    private void checkAndSetRefreshHeader(String token, HttpServletResponse response) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            long refreshThresholdMs = REFRESH_THRESHOLD_HOURS * 60 * 60 * 1000;

            if (remainingMs < refreshThresholdMs && remainingMs > 0) {
                response.setHeader("X-Token-Refresh-Needed", "true");
                response.setHeader("X-Token-Remaining-Seconds", String.valueOf(remainingMs / 1000));
                log.debug("Token 即将过期，提醒前端刷新: 剩余 {} 秒", remainingMs / 1000);
            }
        } catch (Exception e) {
            log.debug("解析 JWT Token 失败: {}", e.getMessage());
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String paramToken = request.getParameter("token");
        if (paramToken != null) {
            return paramToken;
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(ErrorCode.UNAUTHORIZED.getCode(), message)));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }
}
