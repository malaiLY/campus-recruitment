# Campus Recruitment - 校园智能招聘与面试预约平台

> 基于 Spring Boot 3 的校园招聘后端系统，打通「岗位发布 → 审核 → 搜索 → 投递 → 面试预约 → 消息通知」完整业务闭环。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-orange)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-yellow)
![MinIO](https://img.shields.io/badge/MinIO-Object%20Storage-black)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

---

## 项目简介

校园招聘场景中，岗位信息分散、投递状态不透明、面试预约效率低。本项目构建了一个面向校园的招聘与面试预约平台，服务于**学生**、**企业 HR** 和**平台管理员**三类角色。

### 核心业务流程

```
岗位发布 → 岗位审核 → 岗位搜索 → 简历投递 → 企业筛选 → 面试邀约 → 面试预约 → 消息通知
```

---

## 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.5 | 后端框架 |
| MyBatis-Plus | 3.5.7 | ORM 增强 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存、登录态、库存扣减 |
| RabbitMQ | 3.x | 异步消息通知 |
| Elasticsearch | 8.x | 岗位全文搜索 |
| MinIO | 8.5.9 | 对象文件存储 |
| springdoc-openapi | 2.5.0 | Swagger / OpenAPI 文档 |
| Docker Compose | v2 | 中间件一键编排 |

---

## 项目亮点

### 业务亮点

- 完整招聘闭环：学生从搜索岗位到投递、预约面试全流程可演示
- 企业审核机制：企业认证 + 岗位审核，杜绝虚假岗位
- 投递状态流转：已投递 → 已查看 → 邀约面试 → 已预约 / 不合适
- 面试预约系统：企业创建场次，学生自主预约，Redis Lua 原子防超卖
- 消息通知：投递、审核、预约结果通过 RabbitMQ 异步站内信通知

### 技术亮点

| 技术点 | 落地场景 | 面试可讲点 |
|---|---|---|
| Redis Lua | 面试预约 | 原子判断库存并扣减，防止高并发超卖 |
| RabbitMQ | 异步通知 | 投递/审核/预约解耦，messageId 消费幂等 |
| Elasticsearch | 岗位搜索 | 关键词检索、多条件筛选、MQ 异步同步 |
| MinIO | 文件存储 | 简历/资质/头像存储，下载前数据权限校验 |
| RBAC 权限 | 接口鉴权 | 用户-角色-菜单模型 + 自定义注解 + AOP |
| 唯一索引 | 数据兜底 | 防重复投递、防重复预约、MQ 幂等 |
| Docker Compose | 本地部署 | MySQL/Redis/MQ/ES/MinIO 一键启动 |

---

## 系统架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────────────────┐
│   学生/HR    │────▶│  Vue 3 前端   │────▶│      Spring Boot 3 API      │
│   管理员     │     │  (可选 Nginx) │     │                             │
└─────────────┘     └──────────────┘     │  ┌───────────┐ ┌──────────┐ │
                                          │  │ Auth/RBAC │ │  业务模块 │ │
                                          │  └───────────┘ └──────────┘ │
                                          └──────┬──┬──┬──┬──┬─────────┘
                                                 │  │  │  │  │
                            ┌────────────────────┘  │  │  │  └──────────────┐
                            ▼                       ▼  │  ▼                 ▼
                       ┌────────┐            ┌──────┐│┌──────────┐   ┌──────────┐
                       │ MySQL  │            │Redis │││   MinIO  │   │    ES    │
                       └────────┘            └──────┘│└──────────┘   └──────────┘
                                                     ▼
                                              ┌───────────┐
                                              │ RabbitMQ  │
                                              └─────┬─────┘
                                                    │
                                          ┌─────────┴─────────┐
                                          ▼                   ▼
                                    通知消费者           ES 同步消费者
```

### 后端分层

```
Controller (接口层) → Service (业务层) → Mapper (数据访问层)
                         │
                    ┌────┼────┬────────┐
                    ▼    ▼    ▼        ▼
                  Redis  MQ  MinIO    ES
```

---

## 功能模块

| 模块 | 功能 |
|---|---|
| **用户认证** | 注册、登录、JWT Token、登出 |
| **RBAC 权限** | 用户-角色-菜单模型，`@RequireLogin` / `@RequirePermission` 注解鉴权 |
| **学生模块** | 个人资料、技能标签、求职意向 |
| **企业模块** | 企业资料、企业认证、审核状态管理 |
| **文件模块** | 简历/资质/头像上传至 MinIO，元数据入库 |
| **简历模块** | 创建简历、设默认简历、逻辑删除 |
| **岗位模块** | CRUD、提交审核、下架、收藏、ES 搜索 |
| **投递模块** | 投递岗位、状态流转、防重复投递 |
| **面试模块** | 创建场次、学生预约、Redis Lua 防超卖 |
| **消息模块** | 站内信、未读计数、标记已读、MQ 幂等消费 |
| **后台管理** | 数据看板、用户管理、企业/岗位审核、操作日志 |

---

## 快速启动

### 环境要求

| 环境 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| Docker & Docker Compose | 24+ / v2 |

### 1. 克隆项目

```bash
git clone https://github.com/malaiLY/campus-recruitment.git
cd campus-recruitment
```

### 2. 启动中间件

```bash
cd deploy
docker compose up -d
```

启动后各服务访问地址：

| 服务 | 地址 | 账号/密码 |
|---|---|---|
| MySQL | `localhost:3306` | `root / root` |
| Redis | `localhost:6379` | 无密码 |
| RabbitMQ 管理台 | `http://localhost:15672` | `campus / campus123` |
| MinIO 控制台 | `http://localhost:9001` | `minioadmin / minioadmin` |
| Elasticsearch | `http://localhost:9200` | 无认证 |

### 3. 初始化数据库

MySQL 容器启动时会自动执行 `campus-recruitment-plan/校园招聘数据库设计包/V001__初始化数据库结构.sql` 初始化表结构和基础数据。如需手动执行：

```bash
mysql -uroot -proot < campus-recruitment-plan/校园招聘数据库设计包/V001__初始化数据库结构.sql
```

### 4. 启动后端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/campus-recruitment-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 5. 访问接口文档

```
http://localhost:8080/api/swagger-ui/index.html
```

OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

---

## 项目结构

```
campus-recruitment/
├── pom.xml
├── deploy/
│   └── docker-compose.yml              # 中间件编排
├── campus-recruitment-plan/             # 项目文档、数据库设计、接口文档
│   ├── 校园招聘项目说明.md
│   ├── 校园招聘产品需求文档.md
│   ├── 校园招聘完整PRD与研发交付文档.md
│   ├── 校园招聘开发路线图.md
│   ├── 校园招聘数据库设计包/
│   ├── 校园招聘接口文档包/
│   └── 校园招聘低保真原型图.md
└── src/main/
    ├── java/com/campus/recruitment/
    │   ├── CampusRecruitmentApplication.java
    │   ├── common/                     # 通用模块
    │   │   ├── annotation/             #   自定义注解 (@RequireLogin, @RequirePermission)
    │   │   ├── aspect/                 #   AOP 切面 (操作日志)
    │   │   ├── constant/               #   常量 (Redis Key, MQ 路由)
    │   │   ├── context/                #   线程上下文 (LoginUserContext)
    │   │   ├── enums/                  #   枚举 (状态、类型)
    │   │   ├── exception/              #   统一异常处理
    │   │   └── result/                 #   统一响应封装 (R, PageResult)
    │   ├── config/                     # 配置类 (Redis, MQ, MinIO, MyBatis-Plus)
    │   ├── entity/                     # 数据库实体
    │   ├── mapper/                     # MyBatis-Plus Mapper
    │   ├── interceptor/                # 拦截器 (认证、权限)
    │   └── module/                     # 业务模块
    │       ├── admin/                  #   后台管理
    │       ├── application/            #   投递管理
    │       ├── auth/                   #   认证登录
    │       ├── company/                #   企业模块
    │       ├── file/                   #   文件上传
    │       ├── interview/              #   面试预约
    │       ├── job/                    #   岗位管理
    │       ├── message/                #   消息通知
    │       ├── resume/                 #   简历管理
    │       ├── search/                 #   ES 搜索
    │       └── student/                #   学生模块
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        └── lua/
            └── interview_booking.lua   # Redis Lua 防超卖脚本
```

每个业务模块遵循统一分层：`controller` → `service` → `service/impl`，以及 `dto`（请求）、`vo`（响应）。

---

## 核心技术方案

### 面试预约防超卖 (Redis Lua)

面试场次有固定名额，高并发预约时需保证不超卖。方案：

1. 创建场次时将名额同步到 Redis
2. 预约时执行 Lua 脚本，原子完成「判断重复 → 判断库存 → 扣减 → 写去重标记」
3. Lua 成功后写 MySQL，数据库唯一索引兜底

```lua
-- 返回值: -2=重复预约, -1=名额不足, >=0=扣减成功(剩余库存)
if redis.call('exists', KEYS[2]) == 1 then return -2 end
local stock = tonumber(redis.call('get', KEYS[1]) or '0')
if stock <= 0 then return -1 end
redis.call('decr', KEYS[1])
redis.call('set', KEYS[2], ARGV[1], 'EX', ARGV[2])
return stock - 1
```

### RabbitMQ 消费幂等

通知消息携带 `messageId`，消费前查询 `mq_message_log` 是否已处理，避免重复生成站内信。

### ES 岗位搜索同步

岗位审核通过/编辑/下架时发送 MQ，消费者异步同步到 Elasticsearch，失败时记录日志定时补偿。

---

## 核心接口

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | `/api/auth/register` | 注册 |
| 认证 | POST | `/api/auth/login` | 登录 |
| 认证 | GET | `/api/auth/me` | 当前用户信息 |
| 岗位 | GET | `/api/jobs` | 岗位列表(公开) |
| 岗位 | GET | `/api/jobs/{id}` | 岗位详情 |
| 投递 | POST | `/api/applications` | 投递岗位 |
| 投递 | GET | `/api/applications/my` | 我的投递 |
| 面试 | POST | `/api/interview/bookings` | 预约面试 |
| 面试 | GET | `/api/interview/bookings/my` | 我的预约 |
| 消息 | GET | `/api/messages/unread-count` | 未读消息数 |
| 管理 | GET | `/api/admin/dashboard` | 数据看板 |
| 管理 | PUT | `/api/admin/companies/{id}/audit` | 审核企业 |
| 管理 | PUT | `/api/admin/jobs/{id}/audit` | 审核岗位 |

完整接口文档详见 Swagger UI 或 `campus-recruitment-plan/校园招聘接口文档包/`。

---

## 统一响应格式

```json
// 普通响应
{ "code": 0, "message": "success", "data": {} }

// 分页响应
{ "code": 0, "message": "success", "data": { "records": [], "total": 100, "pageNum": 1, "pageSize": 10 } }
```

---

## License

本项目用于学习、校招展示和个人作品集，可按需自行修改。
