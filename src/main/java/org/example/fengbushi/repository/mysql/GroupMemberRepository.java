package org.example.fengbushi.repository.mysql;

import org.example.fengbushi.entity.mysql.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    List<GroupMember> findByGroupId(Long groupId);
    
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
    
    List<GroupMember> findByUserId(Long userId);
    
    long countByGroupId(Long groupId);
}
