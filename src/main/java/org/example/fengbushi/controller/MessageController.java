package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mongodb.Message;
import org.example.fengbushi.service.CrossServerClient;
import org.example.fengbushi.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    private final CrossServerClient crossServerClient;
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ApiResponse<Message> sendMessage(
            @RequestParam Long convId,
            @RequestParam Long senderId,
            @RequestParam String msgType,
            @RequestParam String content) {
        try {
            Message message = messageService.sendMessage(convId, senderId, msgType, content);
            return ApiResponse.success(message);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取会话消息（分页）
     */
    @GetMapping("/list/{convId}")
    public ApiResponse<List<Message>> getMessages(
            @PathVariable Long convId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer unreadCount,
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(required = false) Long userId) {
        try {
            List<Message> messages;
            if (onlyUnread && unreadCount != null && unreadCount > 0 && userId != null) {
                // 🔥 查询本地未读消息（serverAddress == null）
                List<Message> localMessages = messageService.getLocalUnreadMessages(convId, userId);
                
                // 🔥 查询外地消息位置（按 convId 查询）
                List<Message> remoteLocations = messageService.getRemoteMessageLocations(convId, userId);
                List<Message> allMessages = new ArrayList<>(localMessages);
                
                // 获取外地消息内容
                for (Message location : remoteLocations) {
                    String msgId = location.getMsgId();
                    String serverAddress = location.getServerAddress();
                    String currentServer = System.getenv("SERVER_ADDRESS");
                    
                    // 如果消息不在本地服务器，跨服务器获取
                    if (currentServer == null || !currentServer.equals(serverAddress)) {
                        Message remoteMsg = crossServerClient.fetchMessageFromServer(serverAddress, msgId);
                        if (remoteMsg != null) {
                            allMessages.add(remoteMsg);
                        }
                    }
                }
                
                messages = allMessages;
            } else {
                // 加载所有消息（分页，排除位置记录）
                Page<Message> pageResult = messageService.getRealMessages(convId, page, size);
                messages = pageResult.getContent();
            }
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 标记会话中所有消息为已读
     */
    @PutMapping("/read-all/{convId}")
    public ApiResponse<Void> markAllAsRead(@PathVariable Long convId, @RequestParam Long userId) {
        try {
            messageService.markAllMessagesAsRead(convId, userId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 撤回消息
     */
    @PutMapping("/revoke/{msgId}")
    public ApiResponse<Void> revokeMessage(@PathVariable String msgId) {
        try {
            messageService.revokeMessage(msgId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{msgId}")
    public ApiResponse<Void> markAsRead(@PathVariable String msgId) {
        try {
            messageService.markAsRead(msgId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 🔥 获取单条消息（供跨服务器调用，路径与历史 {@code /api/offline-message/fetch} 并存）
     */
    @GetMapping("/fetch/{msgId}")
    public ApiResponse<Message> fetchMessage(@PathVariable String msgId) {
        log.info("🔵 跨服务器获取消息 /api/message/fetch: msgId={}", msgId);
        return messageService.prepareCrossServerFetchMessage(msgId);
    }
}
