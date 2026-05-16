package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.dto.GroupInfoDTO;
import org.example.fengbushi.entity.mysql.GroupChat;
import org.example.fengbushi.entity.mysql.GroupMember;
import org.example.fengbushi.service.GroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {
    
    private final GroupService groupService;
    
    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public ApiResponse<GroupChat> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            GroupChat group = groupService.createGroup(request.getOwnerId(), request.getGroupName(), request.getAvatar());
            
            // 添加成员
            if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
                for (Long memberId : request.getMemberIds()) {
                    try {
                        groupService.addMember(group.getGroupId(), memberId, "member");
                    } catch (Exception e) {
                        // 忽略单个成员添加失败,继续添加其他成员
                        System.err.println("添加成员" + memberId + "失败: " + e.getMessage());
                    }
                }
            }
            
            return ApiResponse.success(group);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 创建群聊请求DTO
     */
    public static class CreateGroupRequest {
        private Long ownerId;
        private String groupName;
        private String avatar;
        private java.util.List<Long> memberIds;
        
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public java.util.List<Long> getMemberIds() { return memberIds; }
        public void setMemberIds(java.util.List<Long> memberIds) { this.memberIds = memberIds; }
    }
    
    /**
     * 添加群成员
     */
    @PostMapping("/member/add")
    public ApiResponse<Void> addMember(
            @RequestParam Long groupId,
            @RequestParam Long userId,
            @RequestParam(required = false) String role) {
        try {
            groupService.addMember(groupId, userId, role);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 移除群成员
     */
    @DeleteMapping("/member/remove")
    public ApiResponse<Void> removeMember(
            @RequestParam Long groupId,
            @RequestParam Long userId) {
        try {
            groupService.removeMember(groupId, userId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 修改群成员职务
     */
    @PutMapping("/member/role")
    public ApiResponse<Void> updateMemberRole(
            @RequestParam Long groupId,
            @RequestParam Long userId,
            @RequestParam String role) {
        try {
            groupService.updateMemberRole(groupId, userId, role);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取群成员列表
     */
    @GetMapping("/members/{groupId}")
    public ApiResponse<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<GroupMember> members = groupService.getGroupMembers(groupId);
            return ApiResponse.success(members);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取群聊列表（兼容前端调用）
     */
    @GetMapping("/list")
    public ApiResponse<List<GroupInfoDTO>> getGroups(@RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                List<GroupInfoDTO> groups = groupService.getUserGroups(userId);
                return ApiResponse.success(groups);
            } else {
                // 如果没有传userId，返回空列表
                return ApiResponse.success(List.of());
            }
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户加入的群列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<GroupInfoDTO>> getUserGroups(@PathVariable Long userId) {
        try {
            List<GroupInfoDTO> groups = groupService.getUserGroups(userId);
            return ApiResponse.success(groups);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 更新群公告
     */
    @PutMapping("/announcement")
    public ApiResponse<Void> updateAnnouncement(
            @RequestParam Long groupId,
            @RequestParam String announcement) {
        try {
            groupService.updateAnnouncement(groupId, announcement);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 更新群信息（名称、头像、公告）
     */
    @PutMapping("/update")
    public ApiResponse<GroupChat> updateGroup(
            @RequestParam Long groupId,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) String announcement) {
        try {
            GroupChat group = groupService.updateGroup(groupId, groupName, avatar, announcement);
            return ApiResponse.success(group);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取群详情（包含成员列表）
     */
    @GetMapping("/{groupId}/detail")
    public ApiResponse<Map<String, Object>> getGroupDetail(@PathVariable Long groupId) {
        try {
            Map<String, Object> detail = groupService.getGroupDetail(groupId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
