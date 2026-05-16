package org.example.fengbushi.service;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.GroupInfoDTO;
import org.example.fengbushi.entity.mysql.GroupChat;
import org.example.fengbushi.entity.mysql.GroupMember;
import org.example.fengbushi.repository.mysql.GroupChatRepository;
import org.example.fengbushi.repository.mysql.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    
    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    /**
     * 创建群聊
     */
    @Transactional
    public GroupChat createGroup(Long ownerId, String groupName, String avatar) {
        GroupChat group = new GroupChat();
        group.setGroupName(groupName);
        group.setOwnerId(ownerId);
        group.setAvatar(avatar);
        
        group = groupChatRepository.save(group);
        
        // 群主自动成为成员
        GroupMember member = new GroupMember();
        member.setGroupId(group.getGroupId());
        member.setUserId(ownerId);
        member.setRole("owner");
        groupMemberRepository.save(member);
        
        return group;
    }
    
    /**
     * 添加群成员
     */
    @Transactional
    public void addMember(Long groupId, Long userId, String role) {
        // 检查是否已在群中
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new RuntimeException("用户已在群中");
        }
        
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role != null ? role : "member");
        
        groupMemberRepository.save(member);
    }
    
    /**
     * 移除群成员
     */
    @Transactional
    public void removeMember(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("用户不在群中"));
        
        // 不允许移除群主
        if ("owner".equals(member.getRole())) {
            throw new RuntimeException("不能移除群主");
        }
        
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
    }
    
    /**
     * 修改群成员职务
     */
    @Transactional
    public void updateMemberRole(Long groupId, Long userId, String role) {
        // 验证职务合法性
        if (!"owner".equals(role) && !"admin".equals(role) && !"member".equals(role)) {
            throw new RuntimeException("无效的职务类型");
        }
        
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("用户不在群中"));
        
        // 不允许直接修改群主职务（只能转让）
        if ("owner".equals(member.getRole()) && !"owner".equals(role)) {
            throw new RuntimeException("不能直接修改群主职务，请使用转让群主功能");
        }
        
        member.setRole(role);
        groupMemberRepository.save(member);
    }
    
    /**
     * 获取群成员列表
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }
    
    /**
     * 获取用户加入的群列表
     */
    public List<GroupInfoDTO> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        // 从群对象列表中提取群id
        List<Long> groupIds = memberships.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toList());
        
        if (groupIds.isEmpty()) {
            return List.of();
        }
        
        List<GroupChat> groups = groupChatRepository.findAllById(groupIds);
        
        // 转换为DTO并设置成员数量
        return groups.stream().map(group -> {
            GroupInfoDTO dto = new GroupInfoDTO();
            dto.setGroupId(group.getGroupId());
            dto.setGroupName(group.getGroupName());
            dto.setOwnerId(group.getOwnerId());
            dto.setAnnouncement(group.getAnnouncement());
            dto.setAvatar(group.getAvatar());
            dto.setCreatedAt(group.getCreatedAt() != null ? group.getCreatedAt().toString() : null);
            dto.setMemberCount(groupMemberRepository.countByGroupId(group.getGroupId()));
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * 更新群公告
     */
    @Transactional
    public void updateAnnouncement(Long groupId, String announcement) {
        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        
        group.setAnnouncement(announcement);
        groupChatRepository.save(group);
    }
    
    /**
     * 更新群信息（名称、头像、公告）
     */
    @Transactional
    public GroupChat updateGroup(Long groupId, String groupName, String avatar, String announcement) {
        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        
        if (groupName != null && !groupName.trim().isEmpty()) {
            group.setGroupName(groupName);
        }
        if (avatar != null) {
            group.setAvatar(avatar);
        }
        if (announcement != null) {
            group.setAnnouncement(announcement);
        }
        
        return groupChatRepository.save(group);
    }
    
    /**
     * 获取群详情（包含成员列表）
     */
    public java.util.Map<String, Object> getGroupDetail(Long groupId) {
        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));
        
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        long memberCount = groupMemberRepository.countByGroupId(groupId);
        
        java.util.Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("group", group);
        detail.put("members", members);
        detail.put("memberCount", memberCount);
        
        return detail;
    }
}
