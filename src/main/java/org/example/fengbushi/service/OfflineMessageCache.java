package org.example.fengbushi.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 离线消息位置缓存
 * 记录每个用户的离线消息存储在哪个服务器
 */
@Slf4j
public class OfflineMessageCache {
    
    // userId -> Map<msgId, serverAddress>
    private static final Map<Long, Map<String, String>> USER_OFFLINE_MESSAGES = new ConcurrentHashMap<>();
    
    /**
     * 注册离线消息位置
     */
    public static void register(Long userId, String serverAddress, String msgId) {
        USER_OFFLINE_MESSAGES.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                             .put(msgId, serverAddress);
    }
    
    /**
     * 获取离线消息所在服务器
     */
    public static String getServer(Long userId, String msgId) {
        Map<String, String> messages = USER_OFFLINE_MESSAGES.get(userId);
        if (messages == null) {
            return null;
        }
        return messages.get(msgId);
    }
    
    /**
     * 删除离线消息缓存
     */
    public static void delete(String msgId) {
        // 遍历所有用户，删除该msgId的记录
        USER_OFFLINE_MESSAGES.forEach((userId, messages) -> {
            messages.remove(msgId);
            if (messages.isEmpty()) {
                USER_OFFLINE_MESSAGES.remove(userId);
            }
        });
    }
    
    /**
     * 获取用户的所有离线消息位置
     */
    public static Map<String, String> getUserOfflineMessages(Long userId) {
        return USER_OFFLINE_MESSAGES.getOrDefault(userId, new ConcurrentHashMap<>());
    }
    
    /**
     * 清除用户的所有离线消息缓存
     */
    public static void clearUser(Long userId) {
        USER_OFFLINE_MESSAGES.remove(userId);
    }
}
