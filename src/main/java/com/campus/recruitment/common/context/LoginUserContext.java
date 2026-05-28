package com.campus.recruitment.common.context;

import lombok.Data;

@Data
public class LoginUserContext {

    private static final ThreadLocal<LoginUser> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(LoginUser user) {
        THREAD_LOCAL.set(user);
    }

    public static LoginUser get() {
        return THREAD_LOCAL.get();
    }

    public static Long getUserId() {
        LoginUser user = THREAD_LOCAL.get();
        return user != null ? user.getUserId() : null;
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }

    @Data
    public static class LoginUser {
        private Long userId;
        private String username;
        private String userType;
    }
}
