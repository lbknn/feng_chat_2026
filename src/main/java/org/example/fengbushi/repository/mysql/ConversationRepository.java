package org.example.fengbushi.repository.mysql;

import org.example.fengbushi.entity.mysql.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    List<Conversation> findByUserIdOrderByUpdateTimeDesc(Long userId);
    
    Optional<Conversation> findByUserIdAndTargetIdAndType(Long userId, Long targetId, String type);
    
    void deleteByUserIdAndTargetIdAndType(Long userId, Long targetId, String type);
}
