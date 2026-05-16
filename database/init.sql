-- ========================================
-- Fengbushi Chat Application - Database Init
-- ========================================

-- 创建数据库(如果不存在)
CREATE DATABASE IF NOT EXISTS fengbushi 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE fengbushi;

-- ========================================
-- 1. 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `phone` VARCHAR(20) COMMENT '手机号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `status` VARCHAR(20) DEFAULT 'offline' COMMENT '在线状态: online/offline',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_username (`username`),
    INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 好友关系表
-- ========================================
CREATE TABLE IF NOT EXISTS `friend` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `remark` VARCHAR(50) COMMENT '备注名',
    `group_name` VARCHAR(50) DEFAULT '默认分组' COMMENT '分组名称',
    `blocked` TINYINT(1) DEFAULT 0 COMMENT '是否拉黑: 0-否, 1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `user_friend_key` VARCHAR(100) UNIQUE COMMENT '唯一键: user_id_friend_id',
    UNIQUE KEY uk_user_friend (`user_friend_key`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_friend_id (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ========================================
-- 3. 群聊表
-- ========================================
CREATE TABLE IF NOT EXISTS `group_chat` (
    `group_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群名称',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `announcement` VARCHAR(500) COMMENT '群公告',
    `avatar` VARCHAR(255) COMMENT '群头像URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_owner_id (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ========================================
-- 4. 群成员表
-- ========================================
CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '群ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: owner/admin/member',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (`group_id`, `user_id`),
    INDEX idx_group_id (`group_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ========================================
-- 5. 会话表
-- ========================================
CREATE TABLE IF NOT EXISTS `conversation` (
    `conv_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    `type` VARCHAR(20) NOT NULL COMMENT '会话类型: single/group',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `target_id` BIGINT NOT NULL COMMENT '目标ID(对方用户ID或群ID)',
    `last_msg` VARCHAR(500) COMMENT '最后一条消息',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_user_target_type (`user_id`, `target_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ========================================
-- 6. 插入测试数据(可选)
-- ========================================

-- 测试用户 (密码都是: 123456)
INSERT INTO `user` (`username`, `nickname`, `password_hash`, `status`) VALUES
('admin', '管理员', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'offline'),
('test1', '测试用户1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'offline'),
('test2', '测试用户2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'offline'),
('test3', '测试用户3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'offline');

-- 测试好友关系 (test1和test2互为好友)
INSERT INTO `friend` (`user_id`, `friend_id`, `remark`, `group_name`, `blocked`, `user_friend_key`) VALUES
(2, 3, '我的好友2', '默认分组', 0, '2_3'),
(3, 2, '我的好友1', '默认分组', 0, '3_2');

-- 测试群聊
INSERT INTO `group_chat` (`group_name`, `owner_id`, `announcement`) VALUES
('测试群聊1', 2, '这是一个测试群聊'),
('技术交流群', 2, '欢迎交流技术问题');

-- 测试群成员
INSERT INTO `group_member` (`group_id`, `user_id`, `role`) VALUES
(1, 2, 'owner'),   -- test1是群主
(1, 3, 'member'),  -- test2是成员
(1, 4, 'member'),  -- test3是成员
(2, 2, 'owner'),   -- test1是群主
(2, 3, 'admin');   -- test2是管理员

-- ========================================
-- 完成提示
-- ========================================
SELECT '✅ Database initialization completed!' AS status;
SELECT COUNT(*) AS total_users FROM `user`;
SELECT COUNT(*) AS total_friends FROM `friend`;
SELECT COUNT(*) AS total_groups FROM `group_chat`;
