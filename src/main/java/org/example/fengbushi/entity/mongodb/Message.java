package org.example.fengbushi.entity.mongodb;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 消息实体类 - 存储在MongoDB中
 * 使用雪花算法生成全局唯一ID
 */
@Data
@Document(collection = "message")
public class Message {
    
    @Id
    private String msgId; // 雪花算法生成的全局唯一ID
    
    @Indexed
    private Long convId; // 会话ID
    
    @Indexed
    private Long senderId; // 发送者ID
    
    @Indexed
    private String msgType; // text/img/file/audio
    
    private String content; // 消息内容
    
    @Indexed
    private LocalDateTime sendTime; // 发送时间
    
    private String status = "sent"; // sent/read/revoked
    
    private String extra; // 扩展信息（如图片URL、文件路径等）
    
    private boolean ephemeral = false; // 是否为阅后即焚消息（转发后删除）
    
    private boolean isRead = false; // 消息是否已读（接收方查看后标记为 true）
    
    private String serverAddress; // 🔥 消息存储的服务器地址（用于分布式查询）
    
    private boolean isLocationRecord = false; // 🔥 是否为外地消息位置记录
}
