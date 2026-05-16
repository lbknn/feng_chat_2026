package org.example.fengbushi.controller;

import lombok.RequiredArgsConstructor;
import org.example.fengbushi.dto.ApiResponse;
import org.example.fengbushi.dto.LoginRequest;
import org.example.fengbushi.dto.RegisterRequest;
import org.example.fengbushi.entity.mysql.User;
import org.example.fengbushi.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> result = userService.login(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserInfo(@PathVariable Long userId) {
        try {
            User user = userService.getUserInfo(userId);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public ApiResponse<User> updateUserInfo(
            @PathVariable Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) String phone) {
        try {
            User user = userService.updateUserInfo(userId, nickname, avatar, phone);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 更新用户信息（兼容query参数方式）
     */
    @PutMapping("/update")
    public ApiResponse<User> updateUserInfoByParam(
            @RequestParam Long userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String email) {
        try {
            User user = userService.updateUserInfo(userId, nickname, avatar, phone);
            return ApiResponse.success(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/{userId}/logout")
    public ApiResponse<Void> logout(@PathVariable Long userId) {
        try {
            userService.logout(userId);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
