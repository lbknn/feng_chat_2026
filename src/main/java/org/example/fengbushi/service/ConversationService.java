package org.example.fengbushi.service;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.entity.mysql.Conversation;
import org.example.fengbushi.repository.mysql.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * 获取用户的会话列表（按更新时间排序）
     */
    public List<Conversation> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }
    
    /**
     * 创建或更新会话（写扩散模式）
     */
    @Transactional
    public Conversation createOrUpdateConversation(Long userId, Long targetId, String type, String lastMsg) {
        Optional<Conversation> existing = conversationRepository
                .findByUserIdAndTargetIdAndType(userId, targetId, type);
        
        Conversation conversation;
        if (existing.isPresent()) {
            conversation = existing.get();
            conversation.setLastMsg(lastMsg);
            conversation.setUpdateTime(java.time.LocalDateTime.now());
        } else {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setType(type);
            conversation.setLastMsg(lastMsg);
            conversation.setUnreadCount(0);
        }
        
        return conversationRepository.save(conversation);
    }
    
    /**
     * 增加未读数
     */
    @Transactional
    public void incrementUnreadCount(Long userId, Long targetId, String type) {
        Optional<Conversation> conversation = conversationRepository
                .findByUserIdAndTargetIdAndType(userId, targetId, type);
        
        if (conversation.isPresent()) {
            Conversation conv = conversation.get();
            conv.setUnreadCount(conv.getUnreadCount() + 1);
            conversationRepository.save(conv);
        }
    }
    
    /**
     * 清除未读数
     */
    @Transactional
    public void clearUnreadCount(Long userId, Long targetId, String type) {
        Optional<Conversation> conversation = conversationRepository
                .findByUserIdAndTargetIdAndType(userId, targetId, type);
        
        if (conversation.isPresent()) {
            Conversation conv = conversation.get();
            conv.setUnreadCount(0);
            conversationRepository.save(conv);
        }
    }
    
    /**
     * 删除会话
     */
    @Transactional
    public void deleteConversation(Long userId, Long targetId, String type) {
        conversationRepository.deleteByUserIdAndTargetIdAndType(userId, targetId, type);
    }
}
