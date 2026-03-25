# EasyMeeting - 视频会议信令服务端

基于 **Spring Boot + Netty + WebRTC** 的视频会议信令服务器后端，支持多人实时音视频会议、会议管理、用户体系等功能。

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 1.8 |
| Spring Boot | 2.7.18 |
| Netty | 4.1.50 |
| MySQL | 8.0.23 |
| Redis / Redisson | 3.12.3 |
| RabbitMQ (amqp-client) | 5.14.2 |
| MyBatis | 1.3.2 |
| Lombok | 1.18.22 |

## 架构概览

```
┌─────────────────────────────────────────┐
│              客户端 (前端)               │
│   HTTP REST API        WebSocket        │
└────────┬────────────────────┬───────────┘
         │  port 5050         │  port 6061
         ▼                    ▼
┌─────────────────┐  ┌──────────────────────┐
│  Spring Boot    │  │  Netty WebSocket      │
│  REST 接口层    │  │  信令服务器 (/ws)     │
│                 │  │  - Token 鉴权         │
│  /api/account   │  │  - 心跳检测           │
│  /api/meeting   │  │  - WebRTC 信令转发    │
│  /api/userInfo  │  └──────────┬───────────┘
└────────┬────────┘             │
         │                      │
         ▼                      ▼
┌─────────────────────────────────────────┐
│           消息总线 (可切换)              │
│     Redis Pub/Sub  或  RabbitMQ         │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
   MySQL             Redis
  (持久化)          (缓存/Token)
```

## 核心功能

### 用户模块
- 图形验证码生成 (`/api/account/checkCode`)
- 用户注册 (`/api/account/register`)
- 用户登录 (`/api/account/login`)
- 用户信息管理、联系人管理

### 会议模块
- **即时会议**：快速创建，支持个人会议号或随机会议号
- **预约会议**：查看今日会议列表
- **加入会议**：支持密码验证加入
- **会议管理**：踢出成员、加入黑名单、结束会议
- **实时状态**：查询当前进行中会议

### WebSocket 信令
- 基于 Netty 的 WebSocket 服务器，路径 `/ws`
- 支持 WebRTC 的 Offer/Answer/ICE Candidate 信令转发
- Token 鉴权拦截器
- 心跳检测（20秒空闲检测）
- Peer 连接数据转发

### 消息通道（可配置）
通过 `message.handle.channel` 配置项切换：
- `redis`：使用 Redis Pub/Sub（默认）
- `rabbitmq`：使用 RabbitMQ

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis
- RabbitMQ（可选，默认使用 Redis）

### 1. 数据库初始化

创建数据库 `easymeeting` 并导入 SQL 脚本（位于 `sql/` 目录）。

### 2. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
# HTTP 服务端口
server.port=5050
# WebSocket 信令端口
ws.port=6061

# 数据库
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/easymeeting?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=your_password

# Redis
spring.redis.host=127.0.0.1
spring.redis.port=6379

# 消息通道: redis 或 rabbitmq
message.handle.channel=redis

# 超级管理员邮箱
admin.emails=admin@example.com

# 项目文件目录
project.folder=/your/project/folder/
```

### 3. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/easymeeting-1.0.jar
```

或直接在 IDE 中运行 `EasyMeetingApplication.java`。

## API 接口一览

### 账号接口 `/api/account`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/checkCode` | 获取图形验证码 |
| POST | `/register` | 注册（email / nickName / password） |
| POST | `/login` | 登录（email / password） |

### 会议接口 `/api/meeting`

| 方法 | 路径 | 说明 | 需鉴权 |
|------|------|------|--------|
| POST | `/loadingMeeting` | 分页查询会议列表 | ✓ |
| POST | `/quickMeeting` | 快速创建会议 | ✓ |
| POST | `/joinMeeting` | 加入会议 | ✓ |
| POST | `/exitMeeting` | 退出会议 | ✓ |
| POST | `/finishMeeting` | 结束会议（主持人） | ✓ |
| POST | `/getCurrentMeeting` | 获取当前会议信息 | ✓ |
| POST | `/kickOutMeeting` | 踢出成员 | ✓ |
| POST | `/blackMeeting` | 将成员加入黑名单 | ✓ |

### WebSocket 连接

```
ws://host:6061/ws?token=<your_token>
```

## 项目结构

```
src/main/java/com/easymeeting/
├── EasyMeetingApplication.java      # 启动类
├── annotation/                      # 自定义注解（鉴权、限流）
├── aspect/                          # AOP 切面
├── config/                          # 配置类
├── controller/                      # REST 控制器
├── entity/
│   ├── constants/                   # 常量
│   ├── dto/                         # 数据传输对象
│   ├── enums/                       # 枚举
│   ├── po/                          # 数据库实体
│   ├── query/                       # 查询条件
│   └── vo/                          # 视图对象
├── exception/                       # 异常处理
├── mappers/                         # MyBatis Mapper
├── redis/                           # Redis 工具类
├── service/                         # 业务逻辑层
│   └── impl/
├── utils/                           # 工具类
└── websocket/
    ├── ChannelContextUtils.java     # Channel 上下文管理
    ├── InitRun.java                 # 启动时初始化
    ├── message/                     # 消息处理（Redis/RabbitMQ）
    └── netty/                       # Netty WebSocket 服务器
```

## License

MIT
