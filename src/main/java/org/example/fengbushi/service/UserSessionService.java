package org.example.fengbushi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户会话路由服务
 * 用于在分布式环境中追踪用户连接到哪个服务器实例
 */
@Slf4j
@Service
public class UserSessionService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String USER_SERVER_KEY = "user:server:";
    private static final long SESSION_TIMEOUT = 30 * 60; // 30分钟
    
    /**
     * 注册用户所在服务器
     * @param userId 用户ID
     * @param serverAddress 服务器地址(IP:Port)
     */
    public void registerUserServer(Long userId, String serverAddress) {
        try {
            String key = USER_SERVER_KEY + userId;
            redisTemplate.opsForValue().set(key, serverAddress, SESSION_TIMEOUT, TimeUnit.SECONDS);
            log.info("✅ 注册用户 {} 到服务器 {} (TTL={}s)", userId, serverAddress, SESSION_TIMEOUT);
        } catch (Exception e) {
            log.error("❌ Redis注册失败: userId={}, server={}", userId, serverAddress, e);
            throw e; // 抛出异常让调用者知道失败了
        }
    }
    
    /**
     * 获取用户所在服务器
     * @param userId 用户ID
     * @return 服务器地址,如果不存在返回null
     */
    public String getUserServer(Long userId) {
        try {
            String key = USER_SERVER_KEY + userId;
            String serverAddress = redisTemplate.opsForValue().get(key);
            
            if (serverAddress != null) {
                // 刷新过期时间
                redisTemplate.expire(key, SESSION_TIMEOUT, TimeUnit.SECONDS);
                log.debug("🔍 查询用户 {} 所在服务器: {}", userId, serverAddress);
            } else {
                log.debug("⚠️ 用户 {} 不在Redis中", userId);
            }
            
            return serverAddress;
        } catch (Exception e) {
            log.error("❌ Redis查询失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 注销用户会话
     * @param userId 用户ID
     */
    public void unregisterUserServer(Long userId) {
        try {
            String key = USER_SERVER_KEY + userId;
            Boolean deleted = redisTemplate.delete(key);
            log.info("✅ 注销用户 {} 的会话 (deleted={})", userId, deleted);
        } catch (Exception e) {
            log.error("❌ Redis注销失败: userId={}", userId, e);
            throw e;
        }
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return true-在线, false-离线
     */
    public boolean isUserOnline(Long userId) {
        return getUserServer(userId) != null;
    }
    
    /**
     * 更新会话心跳
     * @param userId 用户ID
     */
    public void updateHeartbeat(Long userId) {
        String key = USER_SERVER_KEY + userId;
        String serverAddress = redisTemplate.opsForValue().get(key);
        if (serverAddress != null) {
            redisTemplate.expire(key, SESSION_TIMEOUT, TimeUnit.SECONDS);
        }
    }
}
