package org.example.fengbushi.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 好友关系实体类 - 存储在MySQL中
 * 双向存储：A加B，存两条记录
 */
@Data
@Entity
@Table(name = "friend")
public class Friend {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "friend_id", nullable = false)
    private Long friendId;
    
    @Column(length = 50)
    private String remark; // 备注
    
    @Column(length = 50)
    private String group_name = "默认分组"; // 分组
    
    @Column(nullable = false)
    private Boolean blocked = false; // 是否拉黑
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(unique = true, name = "uk_user_friend")
    private String userFriendKey; // 唯一约束：userId_friendId
}
