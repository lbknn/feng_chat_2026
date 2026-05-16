package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mysql.Conversation;
import org.example.fengbushi.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {
    
    private final ConversationService conversationService;
    
    /**
     * 获取用户的会话列表（兼容前端调用）
     */
    @GetMapping("/list")
    public ApiResponse<List<Conversation>> getConversationsByParam(@RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                List<Conversation> conversations = conversationService.getConversations(userId);
                return ApiResponse.success(conversations);
            } else {
                return ApiResponse.success(List.of());
            }
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的会话列表
     */
    @GetMapping("/list/{userId}")
    public ApiResponse<List<Conversation>> getConversations(@PathVariable Long userId) {
        try {
            List<Conversation> conversations = conversationService.getConversations(userId);
            return ApiResponse.success(conversations);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 创建或更新会话
     */
    @PostMapping("/create")
    public ApiResponse<Conversation> createOrUpdateConversation(
            @RequestParam Long userId,
            @RequestParam Long targetId,
            @RequestParam String type,
            @RequestParam String lastMsg) {
        try {
            Conversation conversation = conversationService.createOrUpdateConversation(
                    userId, targetId, type, lastMsg);
            return ApiResponse.success(conversation);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 清除未读数
     */
    @PutMapping("/clear-unread")
    public ApiResponse<Void> clearUnreadCount(
            @RequestParam Long userId,
            @RequestParam Long targetId,
            @RequestParam String type) {
        try {
            conversationService.clearUnreadCount(userId, targetId, type);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteConversation(
            @RequestParam Long userId,
            @RequestParam Long targetId,
            @RequestParam String type) {
        try {
            conversationService.deleteConversation(userId, targetId, type);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
