package org.example.fengbushi.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 群成员实体类 - 存储在MySQL中
 */
@Data
@Entity
@Table(name = "group_member", uniqueConstraints = {
    @UniqueConstraint(name = "uk_group_user", columnNames = {"group_id", "user_id"})
})
public class GroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "group_id", nullable = false)
    private Long groupId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(length = 20, nullable = false)
    private String role = "member"; // owner/admin/member
    
    @CreationTimestamp
    @Column(name = "join_time", updatable = false)
    private LocalDateTime joinTime;
}
