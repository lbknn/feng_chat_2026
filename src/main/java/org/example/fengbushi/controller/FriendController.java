package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mysql.User;
import org.example.fengbushi.service.FriendService;
import org.example.fengbushi.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    private final MessageService messageService;
    
    /**
     * 添加好友
     */
    @PostMapping("/add")
    public ApiResponse<Void> addFriend(
            @RequestParam Long userId,
            @RequestParam Long friendId,
            @RequestParam(required = false) String remark) {
        try {
            friendService.addFriend(userId, friendId, remark);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/list/{userId}")
    public ApiResponse<List<Map<String, Object>>> getFriends(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> friends = friendService.getFriends(userId);
            return ApiResponse.success(friends);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteFriend(
            @RequestParam Long userId,
            @RequestParam Long friendId) {
        try {
            friendService.deleteFriend(userId, friendId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 设置备注
     */
    @PutMapping("/remark")
    public ApiResponse<Void> setRemark(
            @RequestParam Long userId,
            @RequestParam Long friendId,
            @RequestParam String remark) {
        try {
            friendService.setRemark(userId, friendId, remark);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 拉黑/取消拉黑
     */
    @PutMapping("/block")
    public ApiResponse<Void> blockFriend(
            @RequestParam Long userId,
            @RequestParam Long friendId,
            @RequestParam boolean blocked) {
        try {
            friendService.blockFriend(userId, friendId, blocked);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友信息（包含备注）
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getFriendInfo(
            @RequestParam Long userId,
            @RequestParam Long friendId) {
        try {
            Map<String, Object> info = friendService.getFriendInfo(userId, friendId);
            return ApiResponse.success(info);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友申请列表
     */
    @GetMapping("/requests/{userId}")
    public ApiResponse<List<Map<String, Object>>> getFriendRequests(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> requests = messageService.getFriendRequests(userId);
            return ApiResponse.success(requests);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 拒绝好友申请（删除MongoDB中的申请消息）
     */
    @DeleteMapping("/reject")
    public ApiResponse<Void> rejectFriendRequest(
            @RequestParam Long userId,
            @RequestParam Long fromUserId) {
        try {
            friendService.rejectFriendRequest(userId, fromUserId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
