package org.example.fengbushi.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 会话实体类 - 存储在MySQL中
 * 采用"写扩散"模式：发消息时更新所有相关用户的会话
 */
@Data
@Entity
@Table(name = "conversation")
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long convId;
    
    @Column(nullable = false, length = 20)
    private String type; // single/group
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 所属用户ID
    
    @Column(name = "target_id", nullable = false)
    private Long targetId; // 对方ID或群ID
    
    @Column(name = "last_msg", length = 500)
    private String lastMsg; // 最后一条消息
    
    @Column(name = "unread_count")
    private Integer unreadCount = 0; // 未读数
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
