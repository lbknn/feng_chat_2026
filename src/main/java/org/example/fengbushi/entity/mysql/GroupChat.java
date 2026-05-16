package org.example.fengbushi.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 群聊实体类 - 存储在MySQL中
 */
@Data
@Entity
@Table(name = "group_chat")
public class GroupChat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;
    
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId; // 群主ID
    
    @Column(length = 500)
    private String announcement; // 群公告
    
    @Column(length = 255)
    private String avatar; // 群头像
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
