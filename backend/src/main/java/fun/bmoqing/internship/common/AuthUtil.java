package fun.bmoqing.internship.common;

import fun.bmoqing.internship.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtil {

    private AuthUtil() {
    }

    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }

    public static Long currentUserId() {
        User user = currentUser();
        return user == null ? null : user.getId();
    }

    public static boolean hasRole(String... roles) {
        User user = currentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        for (String role : roles) {
            if (user.getRole().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}
