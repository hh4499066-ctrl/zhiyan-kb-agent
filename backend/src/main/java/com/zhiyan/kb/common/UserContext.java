package com.zhiyan.kb.common;

public class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user;
    }

    public static Long userId() {
        return get().getId();
    }

    public static String role() {
        return get().getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
