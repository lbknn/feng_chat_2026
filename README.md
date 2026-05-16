# Fengbushi 分布式聊天系统

## 项目结构

```
fengbushi/
├── deploy/                  # 部署脚本和配置
│   ├── start-backend.ps1     # 后端启动脚本
│   ├── stop-backend.ps1      # 后端停止脚本
│   ├── check-backend.ps1     # 状态检查脚本
│   ├── consul-template.py    # Consul 自动配置脚本
│   ├── START_COMMANDS.txt    # 启动命令文档
│   ├── server-commands.txt # Server 桌面命令
├── src/                     # 源代码
├── target/                  # 编译输出
└── pom.xml                  # Maven 配置
```

## 技术栈

- **后端**: Spring Boot 3.4.4 + Spring Cloud Consul
- **数据库**: MySQL 8.0 (共享) + MongoDB (每台服务器独立)
- **服务发现**: Consul 1.17.2
- **负载均衡**: Nginx
- **即时通讯**: WebSocket
- **部署**: Windows Server + Linux

## 服务器架构

| 服务器 | IP | 角色 | 服务 |
|--------|-----|------|------|
| 负载均衡器 | 192.168.50.100 | Linux | Nginx + Consul Server |
| 后端节点 1 | 192.168.50.101 | Windows | Spring Boot + MongoDB |
| 后端节点 2 | 192.168.50.102 | Windows | Spring Boot + MongoDB |
| 数据库 | 192.168.50.1 | - | MySQL 8.0 |

## 快速开始

### 本地开发

```bash
mvn clean package -DskipTests
java -jar target/fengbushi-0.0.1-SNAPSHOT.jar
```

### 部署到服务器

详见 `deploy/START_COMMANDS.txt`

## 配置文件

- `application.properties` - 本地开发配置

## 核心功能

- 用户注册/登录
- 好友管理
- 单聊/群聊
- 文件传输
- WebSocket 实时消息
- 服务自动发现与负载均衡
