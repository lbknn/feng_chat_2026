package org.example.fengbushi.repository.mysql;

import org.example.fengbushi.entity.mysql.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    
    List<GroupChat> findByOwnerId(Long ownerId);
}
