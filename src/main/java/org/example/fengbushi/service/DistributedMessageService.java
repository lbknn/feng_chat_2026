package org.example.fengbushi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.entity.mongodb.Message;
import org.example.fengbushi.repository.mongodb.MessageRepository;
import org.example.fengbushi.websocket.ChatWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 分布式消息服务
 * 通过 Redis Pub/Sub 实现跨服务器消息路由
 */
@Slf4j
@Service
public class DistributedMessageService implements MessageListener {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private UserSessionService userSessionService;
    
    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String CHANNEL_PREFIX = "chat:server:";
    private static final String OFFLINE_CHANNEL = "chat:offline:broadcast";
    
    @PostConstruct
    public void init() {
        String serverAddress = getCurrentServerAddress();
        String channel = CHANNEL_PREFIX + serverAddress;
        
        messageListenerContainer.addMessageListener(this, 
            ChannelTopic.of(channel));
        
        // 订阅离线消息广播频道
        messageListenerContainer.addMessageListener(this,
            ChannelTopic.of(OFFLINE_CHANNEL));
        
        log.info("📡 启动分布式消息监听: channel={}, offline={}", channel, OFFLINE_CHANNEL);
    }
    
    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            byte[] channelBytes = message.getChannel();
            String channel = channelBytes != null
                    ? new String(channelBytes, StandardCharsets.UTF_8)
                    : (pattern != null ? new String(pattern, StandardCharsets.UTF_8) : "");
            
            log.info("🔵 ========== Redis消息监听 ==========");
            log.info("🔵 收到Redis消息 - 频道: {}, 内容: {}", channel, body);
            
            // 离线消息广播处理
            if (OFFLINE_CHANNEL.equals(channel)) {
                log.info("🔵 处理离线消息广播: {}", body);
                handleOfflineBroadcast(body);
                return;
            }
            
            // 在线消息转发处理
            DistributedMessage distMsg = objectMapper.readValue(body, DistributedMessage.class);
            
            log.info("📨 收到跨服务器消息: type={}, receiverId={}", distMsg.getType(), distMsg.getReceiverId());
            
            if ("forward".equals(distMsg.getType())) {
                ChatWebSocket.sendToUser(distMsg.getReceiverId(), distMsg.getWsMessage());
            }
        } catch (Exception e) {
            log.error("❌ 处理Redis消息失败", e);
        }
    }
    
    /**
     * 处理离线消息广播
     */
    private void handleOfflineBroadcast(String body) throws Exception {
        Map<String, Object> data = objectMapper.readValue(body, Map.class);
        String action = (String) data.get("action");
        
        if ("register".equals(action)) {
            // 注册离线消息位置：{userId, serverAddress, msgId, convId}
            Long userId = ((Number) data.get("userId")).longValue();
            String serverAddress = (String) data.get("serverAddress");
            String msgId = (String) data.get("msgId");
            Long convId = null;
            if (data.get("convId") instanceof Number) {
                convId = ((Number) data.get("convId")).longValue();
            }
            
            // 🔥 创建外地消息位置记录（存 MongoDB）
            Message locationMsg = new Message();
            locationMsg.setMsgId(msgId);
            locationMsg.setConvId(convId);  // 🔥 使用真实的 convId
            locationMsg.setSenderId(userId);  // 用 senderId 存储 userId
            locationMsg.setServerAddress(serverAddress);
            locationMsg.setRead(false);
            locationMsg.setSendTime(java.time.LocalDateTime.now());
            locationMsg.setLocationRecord(true);  // 🔥 标记为位置记录
            
            // 检查是否已存在，不存在则保存
            if (!messageRepository.findById(msgId).isPresent()) {
                messageRepository.save(locationMsg);
                log.info("📝 保存外地消息位置: userId={}, server={}, msgId={}, convId={}", userId, serverAddress, msgId, convId);
            }
            
        } else if ("delete".equals(action)) {
            // 仅删除「位置记录」，绝不能删 msgId 相同的正文（各节点 Mongo 独立，误删会导致跨服拉取 消息不存在）
            String msgId = (String) data.get("msgId");
            log.info("🔵 ========== 处理删除广播 ==========");
            log.info("🔵 收到删除广播: msgId={}", msgId);
            
            try {
                messageRepository.findById(msgId).ifPresent(doc -> {
                    if (doc.isLocationRecord()) {
                        messageRepository.deleteById(msgId);
                        log.info("🗑️ 删除外地消息位置成功: msgId={}", msgId);
                    } else {
                        log.debug("跳过删除非位置记录（保留正文）: msgId={}", msgId);
                    }
                });
            } catch (Exception e) {
                log.error("❌ 删除外地消息位置失败: msgId={}", msgId, e);
            }
        } else {
            log.warn("⚠️ 未知的广播动作: {}", action);
        }
    }
    
    /**
     * 发送消息到远程服务器的用户
     * @param receiverId 接收者ID
     * @param wsMessage WebSocket消息
     */
    public void sendToRemoteUser(Long receiverId, ChatWebSocket.WsMessage wsMessage) {
        String serverAddress = userSessionService.getUserServer(receiverId);
        
        if (serverAddress == null) {
            log.warn("⚠️ 用户 {} 不在线任何服务器", receiverId);
            return;
        }
        
        String currentServer = getCurrentServerAddress();
        if (currentServer.equals(serverAddress)) {
            // 🔥 用户在本地服务器，直接从 ONLINE_USERS 获取会话发送（避免死循环）
            log.info("🔄 用户在本地服务器，直接发送: userId={}", receiverId);
            // 注意：这里不能调用 ChatWebSocket.sendToUser()，会导致死循环
            // 因为 sendToUser() 发现用户不在线时会再次调用 sendToRemoteUser()
            return;
        }
        
        try {
            DistributedMessage distMsg = new DistributedMessage();
            distMsg.setType("forward");
            distMsg.setReceiverId(receiverId);
            distMsg.setWsMessage(wsMessage);
            
            String channel = CHANNEL_PREFIX + serverAddress;
            String json = objectMapper.writeValueAsString(distMsg);
            
            redisTemplate.convertAndSend(channel, json);
            log.info("📤 转发消息到服务器 {}: userId={}", serverAddress, receiverId);
        } catch (Exception e) {
            log.error("❌ 发送跨服务器消息失败: userId={}", receiverId, e);
        }
    }
    
    /**
     * 广播离线消息位置
     */
    public void broadcastOfflineMessage(Long userId, String msgId, Long convId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "register");
            data.put("userId", userId);
            data.put("serverAddress", getCurrentServerAddress());
            data.put("msgId", msgId);
            data.put("convId", convId);  // 🔥 添加 convId
            
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.convertAndSend(OFFLINE_CHANNEL, json);
            
            log.info("📢 广播离线消息: userId={}, msgId={}, convId={}", userId, msgId, convId);
        } catch (Exception e) {
            log.error("❌ 广播离线消息失败", e);
        }
    }
    
    /**
     * 广播删除离线消息
     */
    public void broadcastDeleteOfflineMessage(String msgId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "delete");
            data.put("msgId", msgId);
            
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.convertAndSend(OFFLINE_CHANNEL, json);
            
            log.info("📢 广播删除离线消息: msgId={}", msgId);
        } catch (Exception e) {
            log.error("❌ 广播删除离线消息失败", e);
        }
    }
    
    /**
     * 获取离线消息所在服务器
     */
    public String getOfflineMessageServer(Long userId, String msgId) {
        return OfflineMessageCache.getServer(userId, msgId);
    }
    
    /**
     * 获取当前服务器地址
     */
    private String getCurrentServerAddress() {
        String addr = System.getenv("SERVER_ADDRESS");
        if (addr == null || addr.isEmpty()) {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                String port = System.getProperty("server.port", "8080");
                addr = ip + ":" + port;
            } catch (Exception e) {
                addr = "localhost:8080";
            }
        }
        return addr;
    }
    
    @Data
    public static class DistributedMessage {
        private String type;
        private Long receiverId;
        private ChatWebSocket.WsMessage wsMessage;
    }
}
