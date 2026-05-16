package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.websocket.ChatWebSocket;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 用于验证系统状态
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("onlineUsers", ChatWebSocket.getOnlineCount());
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(data);
    }
    
    /**
     * 获取系统信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, String>> info() {
        Map<String, String> info = new HashMap<>();
        info.put("application", "Fengbushi Chat System");
        info.put("version", "1.0.0");
        info.put("description", "实时聊天软件后端系统");
        
        return ApiResponse.success(info);
    }
}
