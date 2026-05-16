package org.example.fengbushi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.entity.mongodb.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * 跨服务器HTTP调用服务
 */
@Slf4j
@Service
public class CrossServerClient {
    
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 与 Spring MVC 默认 JSON 行为对齐：支持 {@code java.time}，且忽略未知字段（跨版本节点兼容）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    /**
     * 从远程服务器获取单条消息
     */
    public Message fetchMessageFromServer(String serverAddress, String msgId) {
        try {
            String url = "http://" + serverAddress + "/api/offline-message/fetch/" + msgId;
            log.info("🌐 请求远程服务器: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("⚠️ 从服务器 {} 获取消息失败: HTTP {}", serverAddress, response.getStatusCode());
                return null;
            }
            ApiResponse<Message> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Message>>() {});
            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                return apiResponse.getData();
            }
            log.warn("⚠️ 从服务器 {} 获取消息业务失败: msgId={}, code={}, message={}",
                    serverAddress, msgId, apiResponse.getCode(), apiResponse.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ 跨服务器获取消息异常: server={}, msgId={}", serverAddress, msgId, e);
            return null;
        }
    }
    
    /**
     * 从远程服务器批量获取用户离线消息
     */
    public List<Message> fetchBatchMessagesFromServer(String serverAddress, Long userId) {
        try {
            String url = "http://" + serverAddress + "/api/offline-message/fetch-batch/" + userId;
            log.info("🌐 请求远程服务器批量消息: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("⚠️ 从服务器 {} 批量获取消息失败: HTTP {}", serverAddress, response.getStatusCode());
                return List.of();
            }
            ApiResponse<Message[]> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<ApiResponse<Message[]>>() {});
            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                return Arrays.asList(apiResponse.getData());
            }
            log.warn("⚠️ 从服务器 {} 批量获取消息业务失败: code={}, message={}",
                    serverAddress, apiResponse.getCode(), apiResponse.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("❌ 跨服务器批量获取消息异常: server={}, userId={}", serverAddress, userId, e);
            return List.of();
        }
    }
}
