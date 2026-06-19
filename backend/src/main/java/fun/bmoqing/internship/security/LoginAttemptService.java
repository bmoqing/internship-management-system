/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static class AttemptInfo {
        private int failCount;
        private LocalDateTime lockedUntil;
    }

    private final Map<String, AttemptInfo> attemptMap = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final int lockMinutes;

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") int lockMinutes
    ) {
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.lockMinutes = Math.max(lockMinutes, 1);
    }

    public boolean isBlocked(String key) {
        AttemptInfo info = attemptMap.get(key);
        if (info == null || info.lockedUntil == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(info.lockedUntil)) {
            attemptMap.remove(key);
            return false;
        }

        return true;
    }

    public void onLoginSuccess(String key) {
        attemptMap.remove(key);
    }

    public void onLoginFail(String key) {
        AttemptInfo info = attemptMap.computeIfAbsent(key, k -> new AttemptInfo());
        info.failCount++;
        if (info.failCount >= maxAttempts) {
            info.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    public int getRemainingAttempts(String key) {
        AttemptInfo info = attemptMap.get(key);
        if (info == null) {
            return maxAttempts;
        }
        if (isBlocked(key)) {
            return 0;
        }
        int remaining = maxAttempts - info.failCount;
        return Math.max(remaining, 0);
    }
}
