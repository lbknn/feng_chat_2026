package org.example.fengbushi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.repository.mongodb.MessageRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消息清理定时器
 * 每分钟删除一次已读消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupScheduler {

    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cleanupReadMessages() {
        try {
            log.info("🧹 开始执行已读消息清理任务...");
            
            // 查询所有 isRead 为 true 的消息
            Query query = new Query(Criteria.where("isRead").is(true));
            long count = mongoTemplate.count(query, org.example.fengbushi.entity.mongodb.Message.class);
            
            if (count > 0) {
                mongoTemplate.remove(query, org.example.fengbushi.entity.mongodb.Message.class);
                log.info("✅ 已清理 {} 条已读消息", count);
            } else {
                log.debug("ℹ️ 没有需要清理的已读消息");
            }
        } catch (Exception e) {
            log.error("❌ 清理已读消息失败", e);
        }
    }
}
