package org.example.fengbushi.repository.mongodb;

import org.example.fengbushi.entity.mongodb.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    List<Message> findByConvIdOrderBySendTimeAsc(Long convId);
    
    Page<Message> findByConvIdOrderBySendTimeDesc(Long convId, Pageable pageable);
    
    List<Message> findByConvIdAndSendTimeBetweenOrderBySendTimeAsc(
            Long convId, LocalDateTime startTime, LocalDateTime endTime);
    
    void deleteByConvId(Long convId);
    
    void deleteByConvIdAndMsgType(Long convId, String msgType);
}
