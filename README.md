# EasyMeeting - 视频会议信令服务端

基于 **Spring Boot + Netty + WebRTC** 的视频会议信令服务器后端，支持多人实时音视频会议、会议管理、用户体系等功能。

现在仓库中已经额外提供了一个独立的 `frontend/` 浏览器演示，用来结合当前后端的 REST 接口和 WebSocket 信令完成 **WebRTC P2P 视频传输** 验证。

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
| Frontend Demo | Vite + Vanilla JS |

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

### 前端 WebRTC Demo
- 浏览器中直接采集摄像头和麦克风
- 通过现有 `/api/account/*` 和 `/api/meeting/*` 接口完成登录、建会、入会
- 通过现有 `/ws` 信令通道转发 `offer / answer / candidate`
- 在浏览器之间建立 **P2P 音视频连接**
- 支持展示本地视频、远端视频、参会者列表和调试日志
- 支持退出会议与结束会议后的本地资源清理

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
- Node.js 18+（用于前端 demo）

### 1. 数据库初始化

创建数据库 `easymeeting` 并导入 SQL 脚本（位于 `sql/` 目录，如果你本地仓库没有该目录，需要自行补齐初始化 SQL）。

### 2. 修改后端配置

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

### 3. 启动后端

```bash
mvn clean package -DskipTests
java -jar target/easymeeting-1.0.jar
```

或直接在 IDE 中运行 `EasyMeetingApplication.java`。

### 4. 启动前端 Demo

```bash
cd frontend
npm install
npm run dev
```

默认启动后访问：

```text
http://localhost:5173
```

默认配置下：
- API Base：`/api`（通过 Vite 代理到 `http://localhost:5050`）
- WS Base：`ws://localhost:6061`

## WebRTC P2P 传输说明

这个项目中的 **WebSocket 不传输视频流本身**，只负责 **WebRTC 信令交换**：

- `offer`
- `answer`
- `ICE candidate`

实际的音视频媒体流：
- 由浏览器通过 WebRTC 建立 `RTCPeerConnection`
- 优先走浏览器之间的 **P2P 直连**
- 本仓库当前未内置 TURN 服务，因此复杂网络环境下可能无法成功建立媒体链路

### 信令流程

1. 用户登录后拿到 `token`
2. 浏览器连接：`ws://host:6061/ws?token=<token>`
3. 主持人创建会议后调用 `joinMeeting`
4. 参会者通过 `preJoinMeeting + joinMeeting` 进入房间
5. 后端广播 `ADD_MEETING_ROOM`
6. 新加入者向房间中已有成员逐个发送 `offer`
7. 对方返回 `answer`
8. 双方继续交换 `candidate`
9. 浏览器之间建立 P2P 媒体连接并显示远端视频

### 前端发送的信令结构

前端通过 WebSocket 发送的数据结构与后端 `PeerConnectionDataDto` 对齐：

```json
{
  "token": "user-token",
  "sendUserId": "sender-id",
  "receiveUserId": "receiver-id",
  "signalType": "offer",
  "signalData": "{...json string...}"
}
```

其中 `signalType` 支持：
- `offer`
- `answer`
- `candidate`
- `heartbeat`（前端为了兼容后端 20 秒空闲断连而补的保活消息）

## Demo 使用流程

建议使用两个不同浏览器窗口或两个不同浏览器进行测试。

### 方式一：主持人创建会议
1. 打开 `http://localhost:5173`
2. 注册两个账号，分别在两个浏览器中登录
3. 在浏览器 A 中点击“打开摄像头/麦克风”
4. 在浏览器 A 中填写会议信息，点击“创建会议并入会”
5. 浏览器 A 会连接 WebSocket，并等待其他成员加入

### 方式二：参会者加入会议
1. 在浏览器 B 中点击“打开摄像头/麦克风”
2. 输入会议号、昵称、密码（如有）
3. 点击“加入会议”
4. 前端会先调用 `preJoinMeeting`，再调用 `joinMeeting`
5. 加入成功后，浏览器 B 会主动向已有成员发起 `offer`
6. 双方交换 `answer` 和 `candidate` 后看到远端视频

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
| POST | `/preJoinMeeting` | 加入前校验会议号/密码，并写入当前会议状态 | ✓ |
| POST | `/joinMeeting` | 正式加入会议 | ✓ |
| POST | `/exitMeeting` | 退出会议 | ✓ |
| POST | `/finishMeeting` | 结束会议（主持人） | ✓ |
| POST | `/getCurrentMeeting` | 获取当前会议信息 | ✓ |
| POST | `/kickOutMeeting` | 踢出成员 | ✓ |
| POST | `/blackMeeting` | 将成员加入黑名单 | ✓ |

### WebSocket 连接

```text
ws://host:6061/ws?token=<your_token>
```

## 项目结构

```text
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

frontend/
├── index.html                       # 前端演示页面
├── package.json                     # 前端依赖与脚本
├── vite.config.js                   # Vite 开发代理配置
└── src/
    ├── api.js                       # REST 接口封装
    ├── main.js                      # 页面逻辑、状态管理、事件绑定
    ├── signaling.js                 # WebSocket 信令客户端
    ├── webrtc.js                    # RTCPeerConnection 管理
    └── styles.css                   # 页面样式
```

## 已实现的前端能力

本次新增的前端 Demo 做了这些事情：

- 新增独立 `frontend/` 目录，不侵入现有 Java 后端结构
- 使用现有后端登录、建会、入会、离会、结束会议接口
- 使用现有后端 `/ws` 信令服务完成 WebRTC 协商
- 在前端实现了 `offer / answer / candidate` 的发送与接收
- 在前端实现了“新加入者主动发 offer”的建链策略
- 为每个远端用户维护一个独立的 `RTCPeerConnection`
- 在 UI 中展示本地视频、远端视频、参会者和调试日志
- 增加了 WebSocket keepalive，避免后端空闲 20 秒断开连接
- 通过 `npm run build` 验证前端可以成功构建

## 已知限制

- 当前前端是 **Mesh P2P** 模式，多人会议人数较多时带宽和 CPU 压力会上升
- 当前仓库未提供 TURN 服务器，跨复杂 NAT 环境时音视频直连可能失败
- 后端当前更偏向演示/实验性质，部分踢人/房间清理逻辑需要后续继续完善
- `quickMeeting` 接口返回的是 `meetingId`，前端目前无法直接从该接口得到会议号，因此主持人创建会议后，会议号展示仍依赖后端后续补充或从其他接口读取

## License

MIT
