package org.example.fengbushi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mongodb.Message;
import org.example.fengbushi.repository.mongodb.MessageRepository;
import org.example.fengbushi.util.SnowflakeIdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final DistributedMessageService distributedMessageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 发送消息
     */
    public Message sendMessage(Long convId, Long senderId, String msgType, String content) {
        return sendMessageWithExtra(convId, senderId, msgType, content, null);
    }
    
    /**
     * 发送消息（支持额外信息，如文件URL）
     */
    public Message sendMessageWithExtra(Long convId, Long senderId, String msgType, String content, String extra) {
        return sendMessageWithExtra(convId, senderId, msgType, content, extra, false);
    }
    
    /**
     * 发送消息（支持阅后即焚）
     */
    public Message sendMessageWithExtra(Long convId, Long senderId, String msgType, String content, String extra, boolean ephemeral) {
        Message message = new Message();
        message.setMsgId(snowflakeIdGenerator.nextStringId());
        message.setConvId(convId);
        message.setSenderId(senderId);
        message.setMsgType(msgType);
        message.setContent(content);
        message.setSendTime(LocalDateTime.now());
        message.setStatus("sent");
        message.setExtra(extra); // 设置额外信息（文件URL等）
        message.setEphemeral(ephemeral); // 🔥 设置是否为阅后即焚
        message.setRead(false); // 🔥 默认设为未读，由接收方查看时标记为已读
        
        // 🔥 本地消息不记录 serverAddress（保持 null）
        // 外地消息位置会通过广播机制单独存储
        
        return messageRepository.save(message);
    }
    
    /**
     * 获取会话消息列表（分页）
     */
    public Page<Message> getMessages(Long convId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sendTime"));
        return messageRepository.findByConvIdOrderBySendTimeDesc(convId, pageRequest);
    }
    
    /**
     * 🔥 获取会话消息列表（分页，排除位置记录）
     */
    public Page<Message> getRealMessages(Long convId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sendTime"));
        Page<Message> allPage = messageRepository.findByConvIdOrderBySendTimeDesc(convId, pageRequest);
        
        // 过滤掉位置记录
        List<Message> realMessages = allPage.getContent().stream()
                .filter(msg -> !msg.isLocationRecord())
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(realMessages, pageRequest, realMessages.size());
    }
    
    /**
     * 获取未读消息（只返回别人发给当前用户的消息）
     */
    public List<Message> getUnreadMessages(Long convId, int unreadCount, Long userId) {
        List<Message> allMessages = messageRepository.findByConvIdOrderBySendTimeAsc(convId);
        // 🔥 过滤出：1. 未读的 2. 且不是自己发的
        return allMessages.stream()
                .filter(msg -> !msg.isRead() && !msg.getSenderId().equals(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 🔥 获取本地未读消息（serverAddress 为 null）
     */
    public List<Message> getLocalUnreadMessages(Long convId, Long userId) {
        List<Message> allMessages = messageRepository.findByConvIdOrderBySendTimeAsc(convId);
        return allMessages.stream()
                .filter(msg -> !msg.isRead() 
                            && !msg.getSenderId().equals(userId)
                            && msg.getServerAddress() == null
                            && !msg.isLocationRecord())  // 🔥 排除位置记录
                .collect(Collectors.toList());
    }
    
    /**
     * 🔥 获取外地消息位置（isLocationRecord = true）
     */
    public List<Message> getRemoteMessageLocations(Long convId, Long userId) {
        List<Message> allMessages = messageRepository.findByConvIdOrderBySendTimeAsc(convId);
        return allMessages.stream()
                .filter(msg -> msg.getSenderId().equals(userId)  // 🔥 senderId 存储的是 userId
                            && msg.isLocationRecord())  // 🔥 只查位置记录
                .collect(Collectors.toList());
    }
    
    /**
     * 标记消息为已读
     */
    public void markMessageAsRead(String msgId) {
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        if (!message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
            log.info("✅ 标记消息为已读: msgId={}", msgId);
        }
    }

    /**
     * 本地存在则标记已读（用于上线推送：正文可能仅在对方 Mongo，本地无文档时不抛错）。
     */
    public void markMessageAsReadIfPresent(String msgId) {
        messageRepository.findById(msgId).ifPresent(message -> {
            if (!message.isRead()) {
                message.setRead(true);
                messageRepository.save(message);
                log.info("✅ 标记消息为已读: msgId={}", msgId);
            }
        });
    }

    /**
     * 删除指定用户身上的「外地位置」占位（远程已不可拉取时清理，避免反复请求）。
     */
    public void deleteLocationRecordForUser(String msgId, Long userId) {
        messageRepository.findById(msgId).ifPresent(doc -> {
            if (doc.isLocationRecord() && userId.equals(doc.getSenderId())) {
                messageRepository.deleteById(msgId);
                log.info("🗑️ 已清理无效外地位置记录: msgId={}, userId={}", msgId, userId);
            }
        });
    }
    
    /**
     * 标记会话中所有消息为已读（仅标记非本人发送的消息，排除位置记录）
     */
    public void markAllMessagesAsRead(Long convId, Long userId) {
        List<Message> messages = messageRepository.findByConvIdOrderBySendTimeAsc(convId);
        int count = 0;
        for (Message msg : messages) {
            // 🔥 只将别人发给自己的消息标记为已读，排除位置记录
            if (!msg.isLocationRecord() && !msg.isRead() && !msg.getSenderId().equals(userId)) {
                msg.setRead(true);
                messageRepository.save(msg);
                count++;
            }
        }
        if (count > 0) {
            log.info("✅ 用户 {} 标记 {} 条消息为已读: convId={}", userId, count, convId);
        }
    }
    
    /**
     * 获取会话所有消息
     */
    public List<Message> getAllMessages(Long convId) {
        return messageRepository.findByConvIdOrderBySendTimeAsc(convId);
    }
    
    /**
     * 🔥 根据ID获取消息
     */
    public Message getMessageById(String msgId) {
        return messageRepository.findById(msgId).orElse(null);
    }

    /**
     * 跨服拉取单条消息：读库、标记已读、广播删除各地「位置记录」，供 HTTP 与集群节点调用。
     */
    public ApiResponse<Message> prepareCrossServerFetchMessage(String msgId) {
        try {
            Message message = getMessageById(msgId);
            if (message == null) {
                log.warn("⚠️ 消息不存在: msgId={}", msgId);
                return ApiResponse.error("消息不存在");
            }
            if (message.isLocationRecord()) {
                log.warn("⚠️ msgId 对应为位置记录非正文: msgId={}", msgId);
                return ApiResponse.error("消息不存在");
            }
            markMessageAsRead(msgId);
            distributedMessageService.broadcastDeleteOfflineMessage(msgId);
            log.info("✅ 跨服务器获取消息成功: msgId={}", msgId);
            return ApiResponse.success(message);
        } catch (Exception e) {
            log.error("❌ 跨服务器获取消息失败: msgId={}", msgId, e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 撤回消息
     */
    public void revokeMessage(String msgId) {
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        message.setStatus("revoked");
        messageRepository.save(message);
    }
    
    /**
     * 标记消息为已读
     */
    public void markAsRead(String msgId) {
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        message.setStatus("read");
        messageRepository.save(message);
    }
    
    /**
     * 获取好友申请列表
     */
    public List<Map<String, Object>> getFriendRequests(Long userId) {
        // 查询所有发送给该用户的好友申请消息
        List<Message> allMessages = messageRepository.findAll();
        
        return allMessages.stream()
                .filter(msg -> "friend_request".equals(msg.getMsgType()))
                .filter(msg -> {
                    // 解析extra获取targetId
                    try {
                        if (msg.getExtra() != null) {
                            Map<String, Object> extra = objectMapper.readValue(msg.getExtra(), Map.class);
                            Object targetId = extra.get("targetId");
                            if (targetId instanceof Number) {
                                return ((Number) targetId).longValue() == userId;
                            }
                        }
                    } catch (Exception e) {
                        return false;
                    }
                    return false;
                })
                .sorted((a, b) -> b.getSendTime().compareTo(a.getSendTime()))
                .map(msg -> {
                    Map<String, Object> request = new HashMap<>();
                    request.put("fromUserId", msg.getSenderId());
                    request.put("content", msg.getContent());
                    request.put("timestamp", msg.getSendTime());
                    
                    // 解析extra获取申请人信息
                    try {
                        if (msg.getExtra() != null) {
                            Map<String, Object> extra = objectMapper.readValue(msg.getExtra(), Map.class);
                            request.put("fromUsername", extra.get("fromUsername"));
                            request.put("fromNickname", extra.get("fromNickname"));
                            request.put("fromAvatar", extra.get("fromAvatar"));
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                    
                    return request;
                })
                .collect(Collectors.toList());
    }
}
