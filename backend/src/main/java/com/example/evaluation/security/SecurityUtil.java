package com.example.evaluation.security;

import com.example.evaluation.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user.getUserId();
    }

    public static Integer getCurrentRole() {
        LoginUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user.getRole();
    }

    public static boolean isTeacher() {
        LoginUser user = getCurrentUser();
        return user != null && user.getRole() == 1;
    }

    public static boolean isStudent() {
        LoginUser user = getCurrentUser();
        return user != null && user.getRole() == 0;
    }
}
