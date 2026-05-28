package com.campus.recruitment.interceptor;

import com.campus.recruitment.common.annotation.RequirePermission;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.common.constant.RedisConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }

        String requiredPerm = requirePermission.value();
        LoginUserContext.LoginUser user = LoginUserContext.get();
        if (user == null) {
            sendForbidden(response, "未登录");
            return false;
        }

        if ("ADMIN".equals(user.getUserType())) {
            return true;
        }

        String permKey = RedisConstants.USER_PERMISSION_PREFIX + user.getUserId();
        Set<String> permissions = stringRedisTemplate.opsForSet().members(permKey);
        if (permissions == null || !permissions.contains(requiredPerm)) {
            sendForbidden(response, "无权限访问");
            return false;
        }

        return true;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(ErrorCode.FORBIDDEN.getCode(), message)));
    }
}
