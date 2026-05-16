package org.example.fengbushi.repository.mysql;

import org.example.fengbushi.entity.mysql.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    
    List<Friend> findByUserIdAndBlockedFalse(Long userId);
    
    Optional<Friend> findByUserFriendKey(String userFriendKey);
    
    void deleteByUserFriendKey(String userFriendKey);
    
    List<Friend> findByUserId(Long userId);
}
