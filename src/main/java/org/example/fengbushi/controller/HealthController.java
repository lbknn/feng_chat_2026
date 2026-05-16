package org.example.fengbushi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 用于负载均衡器和Consul进行服务健康探测
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    /**
     * 基础健康检查端点
     * Consul和Nginx会定期调用此接口检测服务是否可用
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "fengbushi-chat");
        
        return ResponseEntity.ok(health);
    }

    /**
     * 详细健康信息
     * 包含更多服务状态细节
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "fengbushi-chat");
        info.put("version", "1.0.0");
        info.put("description", "实时聊天服务");
        info.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(info);
    }
}
