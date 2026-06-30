package com.example.evaluation.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TokenManager {

    private final Map<Long, String> userActiveToken = new ConcurrentHashMap<>();
    private final Set<String> blacklist = new CopyOnWriteArraySet<>();

    public void registerToken(Long userId, String token, long expirationMs) {
        String oldToken = userActiveToken.put(userId, token);
        if (oldToken != null && !oldToken.equals(token)) {
            addToBlacklist(oldToken, expirationMs);
            log.info("用户 [{}] 在其他设备登录，旧 Token 已踢下线", userId);
        }
    }

    public boolean isTokenValid(Long userId, String token) {
        String activeToken = userActiveToken.get(userId);
        return activeToken != null && activeToken.equals(token) && !blacklist.contains(token);
    }

    public void removeToken(Long userId, String token) {
        userActiveToken.remove(userId, token);
        addToBlacklist(token, 60000L);
        log.info("用户 [{}] 已登出", userId);
    }

    public void kickUser(Long userId) {
        String oldToken = userActiveToken.remove(userId);
        if (oldToken != null) {
            addToBlacklist(oldToken, 3600000L);
            log.info("用户 [{}] 已被管理员踢下线", userId);
        }
    }

    public String getActiveToken(Long userId) {
        return userActiveToken.get(userId);
    }

    private void addToBlacklist(String token, long ttlMs) {
        blacklist.add(token);
        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(ttlMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            blacklist.remove(token);
        }).start();
    }
}
