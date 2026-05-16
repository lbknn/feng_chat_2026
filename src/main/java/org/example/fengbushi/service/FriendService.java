package org.example.fengbushi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.entity.mysql.Friend;
import org.example.fengbushi.entity.mysql.User;
import org.example.fengbushi.repository.mongodb.MessageRepository;
import org.example.fengbushi.repository.mysql.FriendRepository;
import org.example.fengbushi.repository.mysql.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {
    
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    
    /**
     * 添加好友（双向存储）
     */
    @Transactional
    public void addFriend(Long userId, Long friendId, String remark) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        
        // 检查好友是否存在
        if (!userRepository.existsById(friendId)) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查是否已经是好友
        String key1 = userId + "_" + friendId;
        if (friendRepository.findByUserFriendKey(key1).isPresent()) {
            throw new RuntimeException("已经是好友关系");
        }
        
        // 创建双向好友关系
        Friend friend1 = new Friend();
        friend1.setUserId(userId);
        friend1.setFriendId(friendId);
        friend1.setRemark(remark);
        friend1.setUserFriendKey(userId + "_" + friendId);
        
        Friend friend2 = new Friend();
        friend2.setUserId(friendId);
        friend2.setFriendId(userId);
        friend2.setRemark(null);
        friend2.setUserFriendKey(friendId + "_" + userId);
        
        friendRepository.save(friend1);
        friendRepository.save(friend2);
        
        // 删除MongoDB中的好友申请消息
        try {
            Long convId = Math.min(userId, friendId) * 100000L + Math.max(userId, friendId);
            messageRepository.deleteByConvIdAndMsgType(convId, "friend_request");
            log.info("✅ 已删除好友申请消息: convId={}, userId={}, friendId={}", convId, userId, friendId);
        } catch (Exception e) {
            log.warn("⚠️ 删除好友申请消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 拒绝好友申请（删除MongoDB中的申请消息）
     */
    @Transactional
    public void rejectFriendRequest(Long userId, Long fromUserId) {
        try {
            Long convId = Math.min(userId, fromUserId) * 100000L + Math.max(userId, fromUserId);
            messageRepository.deleteByConvIdAndMsgType(convId, "friend_request");
            log.info("✅ 已拒绝并删除好友申请消息: convId={}, userId={}, fromUserId={}", convId, userId, fromUserId);
        } catch (Exception e) {
            log.warn("⚠️ 拒绝好友申请失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取好友列表
     */
    public List<Map<String, Object>> getFriends(Long userId) {
        List<Friend> friends = friendRepository.findByUserIdAndBlockedFalse(userId);
        
        List<Long> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());
        
        List<User> users = userRepository.findAllById(friendIds);
        
        // 将用户信息和备注合并
        return users.stream().map(user -> {
            Map<String, Object> friendInfo = new HashMap<>();
            friendInfo.put("userId", user.getUserId());
            friendInfo.put("username", user.getUsername());
            friendInfo.put("nickname", user.getNickname());
            friendInfo.put("avatar", user.getAvatar());
            friendInfo.put("status", user.getStatus());
            
            // 查找对应的备注
            String key = userId + "_" + user.getUserId();
            friendRepository.findByUserFriendKey(key).ifPresent(friend -> {
                friendInfo.put("remark", friend.getRemark());
            });
            
            return friendInfo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        String key1 = userId + "_" + friendId;
        String key2 = friendId + "_" + userId;
        
        friendRepository.deleteByUserFriendKey(key1);
        friendRepository.deleteByUserFriendKey(key2);
    }
    
    /**
     * 设置备注
     */
    @Transactional
    public void setRemark(Long userId, Long friendId, String remark) {
        String key = userId + "_" + friendId;
        Friend friend = friendRepository.findByUserFriendKey(key)
                .orElseThrow(() -> new RuntimeException("好友关系不存在"));
        
        friend.setRemark(remark);
        friendRepository.save(friend);
    }
    
    /**
     * 拉黑/取消拉黑
     */
    @Transactional
    public void blockFriend(Long userId, Long friendId, boolean blocked) {
        String key = userId + "_" + friendId;
        Friend friend = friendRepository.findByUserFriendKey(key)
                .orElseThrow(() -> new RuntimeException("好友关系不存在"));
        
        friend.setBlocked(blocked);
        friendRepository.save(friend);
    }
    
    /**
     * 获取好友信息（包含备注）
     */
    public Map<String, Object> getFriendInfo(Long userId, Long friendId) {
        // 获取好友关系
        String key = userId + "_" + friendId;
        Friend friend = friendRepository.findByUserFriendKey(key)
                .orElseThrow(() -> new RuntimeException("好友关系不存在"));
        
        // 获取好友用户信息
        User user = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getUserId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("status", user.getStatus());
        info.put("remark", friend.getRemark());
        
        return info;
    }
}
