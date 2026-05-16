# Fengbushi 分布式聊天系统 - PPT大纲

## 封面页
- **项目名称**: Fengbushi 分布式聊天系统
- **副标题**: 基于 Spring Boot + WebSocket 的实时通讯解决方案
- **汇报人**: [姓名]
- **日期**: 2026年4月

---

## 目录
1. 项目概述
2. 技术栈介绍
3. 系统架构设计
4. 核心功能模块
5. 数据库设计
6. 分布式部署方案
7. 关键技术实现
8. 项目总结与展望

---

## 第一部分：项目概述

### 1.1 项目背景
- 即时通讯在现代应用中的重要性
- 传统单体架构的局限性
- 分布式系统的优势与挑战

### 1.2 项目简介
- **项目名称**: Fengbushi（分布式聊天系统）
- **核心定位**: 支持高并发的实时通讯平台
- **主要特性**:
  - 单聊/群聊功能
  - 好友管理系统
  - 文件传输
  - WebSocket 实时消息推送
  - 服务自动发现与负载均衡

### 1.3 应用场景
- 企业内部沟通工具
- 在线客服系统
- 社交应用即时通讯模块
- 游戏内聊天系统

---

## 第二部分：技术栈介绍

### 2.1 后端技术栈
#### 核心框架
- **Spring Boot 3.4.4**
  - 快速开发框架
  - 约定优于配置
  - 内嵌 Tomcat 服务器
  
- **Spring Cloud 2024.0.0**
  - 微服务生态体系
  - 服务治理解决方案

#### 数据持久层
- **Spring Data JPA**
  - ORM 框架，简化数据库操作
  - 基于 Hibernate 实现
  
- **MySQL 8.0**
  - 关系型数据库
  - 存储用户、好友、群组等结构化数据
  
- **MongoDB**
  - NoSQL 文档数据库
  - 存储海量聊天记录（非结构化数据）
  
- **Redis**
  - 内存数据库
  - 会话管理、缓存、在线状态

#### 安全与验证
- **Spring Security**
  - BCrypt 密码加密
  - 认证授权框架
  
- **Spring Validation**
  - 参数校验
  - 数据合法性检查

#### 通信协议
- **WebSocket**
  - 全双工通信协议
  - 实时消息推送
  
- **HTTP/RESTful API**
  - 常规业务接口

#### 辅助工具
- **Lombok**
  - 简化 Java 代码（getter/setter/构造器等）
  
- **Maven**
  - 项目构建与依赖管理

### 2.2 前端技术栈
- **原生 JavaScript (ES6+)**
  - 无框架依赖，轻量级实现
  
- **HTML5 + CSS3**
  - 响应式界面设计
  
- **WebSocket API**
  - 浏览器端实时通信

### 2.3 基础设施
- **Consul 1.17.2**
  - 服务注册与发现
  - 健康检查
  - 分布式配置管理
  
- **Nginx**
  - 反向代理
  - 负载均衡
  - 静态资源服务

- **JDK 17**
  - Long Term Support (LTS) 版本
  - 性能优化与新特性

---

## 第三部分：系统架构设计

### 3.1 整体架构图
```
┌─────────────────────────────────────────────┐
│              客户端 (Browser)                 │
└──────────────┬──────────────────────────────┘
               │ HTTP + WebSocket
┌──────────────▼──────────────────────────────┐
│          Nginx (负载均衡器)                   │
│         192.168.50.100 (Linux)               │
└──────┬───────────────────────┬──────────────┘
       │                       │
┌──────▼──────┐       ┌───────▼──────┐
│ Server 101  │       │ Server 102   │
│ Windows     │       │ Windows      │
│ Spring Boot │       │ Spring Boot  │
│ MongoDB     │       │ MongoDB      │
└──────┬──────┘       └───────┬──────┘
       │                      │
       └──────────┬───────────┘
                  │
         ┌────────▼────────┐
         │   Consul Server  │
         │  服务注册与发现   │
         └────────┬────────┘
                  │
         ┌────────▼────────┐
         │   MySQL 8.0      │
         │  共享数据库       │
         │ 192.168.50.1    │
         └─────────────────┘
```

### 3.2 分层架构
#### 表现层 (Controller)
- RESTful API 接口
- WebSocket 端点
- 统一异常处理

#### 业务层 (Service)
- 用户服务 (UserService)
- 消息服务 (MessageService)
- 好友服务 (FriendService)
- 群组服务 (GroupService)
- 会话服务 (ConversationService)
- 文件存储服务 (FileStorageService)

#### 数据访问层 (Repository)
- MySQL Repository (JPA)
  - UserRepository
  - FriendRepository
  - GroupChatRepository
  - GroupMemberRepository
  - ConversationRepository
  
- MongoDB Repository
  - MessageRepository

#### 基础设施层
- WebSocket 配置
- 跨域配置 (CORS)
- 安全配置 (Security)
- 全局异常处理

### 3.3 数据流向
```
用户发送消息
    ↓
WebSocket 接收
    ↓
消息服务处理
    ↓
MongoDB 持久化
    ↓
推送给目标用户
```

---

## 第四部分：核心功能模块

### 4.1 用户模块
- **用户注册**
  - 用户名唯一性校验
  - BCrypt 密码加密存储
  
- **用户登录**
  - 身份验证
  - 会话管理（Redis）
  
- **用户信息管理**
  - 头像上传
  - 个人资料修改

### 4.2 好友模块
- **添加好友**
  - 好友请求发送
  - 好友请求接受/拒绝
  
- **好友列表**
  - 在线状态显示
  - 好友搜索
  
- **删除好友**

### 4.3 聊天模块
#### 单聊功能
- 一对一实时消息
- 消息历史记录
- 已读未读状态

#### 群聊功能
- 创建群组
- 邀请成员
- 群消息广播
- 群管理（踢人、解散）

#### 消息类型
- 文本消息
- 图片消息
- 文件消息
- 系统通知

### 4.4 文件传输模块
- 文件上传（最大 10MB）
- 文件下载
- 图片预览
- 文件存储管理

### 4.5 会话管理模块
- 会话列表
- 最后一条消息预览
- 未读消息计数
- 会话排序（按时间）

---

## 第五部分：数据库设计

### 5.1 MySQL 数据库（关系型数据）

#### 用户表 (user)
```
- id (BIGINT, 主键, 雪花算法生成)
- username (VARCHAR, 唯一)
- password (VARCHAR, BCrypt加密)
- nickname (VARCHAR)
- avatar (VARCHAR)
- create_time (DATETIME)
- update_time (DATETIME)
```

#### 好友表 (friend)
```
- id (BIGINT, 主键)
- user_id (BIGINT, 外键)
- friend_id (BIGINT, 外键)
- status (TINYINT, 好友状态)
- create_time (DATETIME)
```

#### 群组表 (group_chat)
```
- id (BIGINT, 主键)
- group_name (VARCHAR)
- owner_id (BIGINT, 群主)
- avatar (VARCHAR)
- create_time (DATETIME)
```

#### 群成员表 (group_member)
```
- id (BIGINT, 主键)
- group_id (BIGINT, 外键)
- user_id (BIGINT, 外键)
- role (TINYINT, 角色：群主/管理员/成员)
- join_time (DATETIME)
```

#### 会话表 (conversation)
```
- id (VARCHAR, 主键, 组合ID)
- type (TINYINT, 会话类型：单聊/群聊)
- participant_ids (VARCHAR, 参与者ID列表)
- last_message (VARCHAR)
- last_message_time (DATETIME)
- unread_count (INT)
```

### 5.2 MongoDB 数据库（非结构化数据）

#### 消息集合 (message)
```json
{
  "_id": "ObjectId",
  "messageId": "String (雪花算法)",
  "conversationId": "String",
  "senderId": "Long",
  "receiverId": "Long (单聊) / null (群聊)",
  "groupId": "Long (群聊) / null (单聊)",
  "messageType": "String (TEXT/IMAGE/FILE/SYSTEM)",
  "content": "String",
  "fileUrl": "String (可选)",
  "timestamp": "Date",
  "status": "String (SENT/DELIVERED/READ)"
}
```

### 5.3 Redis 缓存
- **用户会话**: `session:{userId}` → 用户登录信息
- **在线状态**: `online:{userId}` → WebSocket 连接信息
- **消息队列**: `message_queue:{userId}` → 离线消息暂存

---

## 第六部分：分布式部署方案

### 6.1 服务器架构

| 服务器 | IP地址 | 操作系统 | 部署服务 |
|--------|--------|----------|----------|
| 负载均衡器 | 192.168.50.100 | Linux | Nginx + Consul Server |
| 后端节点1 | 192.168.50.101 | Windows Server | Spring Boot + MongoDB + Consul Agent |
| 后端节点2 | 192.168.50.102 | Windows Server | Spring Boot + MongoDB + Consul Agent |
| 数据库服务器 | 192.168.50.1 | - | MySQL 8.0 |

### 6.2 服务注册与发现
#### Consul 工作流程
```
1. Spring Boot 应用启动
        ↓
2. 向 Consul Server 注册服务
        ↓
3. 定期发送心跳（健康检查）
        ↓
4. Nginx 通过 Consul 获取可用服务列表
        ↓
5. 负载均衡转发请求
```

#### 配置示例
```properties
spring.cloud.consul.host=192.168.50.100
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.enabled=true
spring.cloud.consul.discovery.prefer-ip-address=true
```

### 6.3 负载均衡策略
- **Nginx 配置**
  - 轮询算法 (Round Robin)
  - WebSocket 长连接支持
  - 健康检查机制

### 6.4 数据一致性保证
- **MySQL**: 共享数据库，保证用户数据一致性
- **MongoDB**: 各节点独立，聊天记录分散存储
- **Redis**: 会话共享（可扩展为 Redis Cluster）

### 6.5 部署脚本
- `start-backend.ps1` - 后端服务启动
- `stop-backend.ps1` - 后端服务停止
- `check-backend.ps1` - 服务状态检查
- `consul-template.py` - Consul 配置自动生成

---

## 第七部分：关键技术实现

### 7.1 WebSocket 实时通信

#### WebSocket 配置
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocket(), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

#### 消息推送流程
```
1. 客户端建立 WebSocket 连接
        ↓
2. 服务端保存连接映射 (userId ↔ Session)
        ↓
3. 收到消息后查找目标用户连接
        ↓
4. 通过 Session 推送消息
        ↓
5. 客户端接收并渲染
```

#### 断线重连机制
- 心跳检测 (Ping/Pong)
- 自动重连逻辑
- 离线消息补偿

### 7.2 雪花算法 ID 生成
- **用途**: 生成分布式唯一 ID
- **优势**: 
  - 无需数据库自增
  - 高性能
  - 趋势递增
  
- **结构**:
  ```
  1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
  ```

### 7.3 会话 ID 生成算法
- **单聊**: `min(userId1, userId2)_max(userId1, userId2)`
- **群聊**: `group_{groupId}`
- **保证**: 前后端算法一致，避免会话混乱

### 7.4 密码安全
- **BCrypt 加密**
  - 加盐哈希
  - 不可逆加密
  - 防止彩虹表攻击

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode(rawPassword);
boolean matches = encoder.matches(rawPassword, hashedPassword);
```

### 7.5 全局异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error("系统异常: " + e.getMessage());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }
}
```

**优势**:
- 统一错误响应格式
- 减少重复代码
- 便于前端处理

### 7.6 跨域配置 (CORS)
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
```

### 7.7 文件存储
- **本地存储**: `uploads/` 目录
- **文件大小限制**: 10MB
- **文件类型校验**: 图片、文档等
- **访问路径**: `/api/files/{filename}`

---

## 第八部分：项目亮点与挑战

### 8.1 项目亮点
1. **分布式架构**
   - 支持水平扩展
   - 高可用性设计
   - 服务自动发现

2. **多数据库混合使用**
   - MySQL: 结构化数据
   - MongoDB: 海量聊天记录
   - Redis: 高速缓存

3. **实时通讯**
   - WebSocket 双向通信
   - 低延迟消息推送
   - 断线重连机制

4. **完善的异常处理**
   - 全局异常捕获
   - 统一响应格式
   - 详细日志记录

### 8.2 技术挑战与解决方案

#### 挑战1: 分布式会话管理
- **问题**: 多节点如何共享用户登录状态
- **解决**: Redis 集中存储会话信息

#### 挑战2: WebSocket 负载均衡
- **问题**: 长连接如何跨节点通信
- **解决**: 
  - 基于 userId 的粘性会话
  - 或通过消息中间件广播

#### 挑战3: 消息可靠性
- **问题**: 如何保证消息不丢失
- **解决**:
  - MongoDB 持久化
  - 离线消息队列
  - ACK 确认机制

#### 挑战4: 前后端会话ID一致性
- **问题**: 会话ID生成算法不一致导致聊天混乱
- **解决**: 统一定义生成规则，严格保持一致

### 8.3 性能优化
- **数据库索引**: 常用查询字段建立索引
- **连接池**: HikariCP 数据库连接池
- **懒加载**: JPA 关联关系优化
- **分页查询**: 避免一次性加载大量数据

---

## 第九部分：项目总结与展望

### 9.1 项目成果
- ✅ 实现了完整的即时通讯功能
- ✅ 搭建了分布式微服务架构
- ✅ 支持多节点负载均衡
- ✅ 保证了数据一致性和消息可靠性

### 9.2 技术收获
- Spring Boot 微服务开发经验
- WebSocket 实时通信技术
- 分布式系统设计与部署
- 多数据库混合架构实践

### 9.3 未来优化方向
1. **功能增强**
   - 语音/视频通话
   - 消息撤回功能
   - @提及功能
   - 表情包支持

2. **性能提升**
   - 引入 Kafka/RabbitMQ 消息队列
   - CDN 加速文件访问
   - 数据库读写分离

3. **安全性加强**
   - JWT Token 认证
   - HTTPS 加密传输
   - 敏感信息脱敏
   - 防刷限流机制

4. **运维监控**
   - Prometheus + Grafana 监控
   - ELK 日志分析
   - 自动化部署 (CI/CD)
   - 容器化部署 (Docker + Kubernetes)

### 9.4 学习建议
- 深入理解 Spring Cloud 微服务生态
- 掌握 WebSocket 协议原理
- 学习分布式系统设计模式
- 关注高并发场景下的性能优化

---

## Q&A 环节
- 欢迎提问与交流

---

## 附录：参考资源
- Spring Boot 官方文档: https://spring.io/projects/spring-boot
- WebSocket 规范: RFC 6455
- Consul 文档: https://www.consul.io/docs
- MongoDB 最佳实践: https://docs.mongodb.com/manual/core/

---

**谢谢观看！**
