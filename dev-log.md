# 校园智能招聘与面试预约平台 - 开发记录

> 开发日期：2026-05-18 ~ 2026-05-20
> 技术栈：Spring Boot 3.2.5 + MyBatis-Plus + Redis + RabbitMQ + Elasticsearch + MinIO + MySQL 8.0

---

## 一、项目概述

### 1.1 项目背景

校园智能招聘与面试预约平台是一个面向高校学生的综合性招聘系统，涵盖简历管理、岗位浏览、企业申请、面试预约等核心功能。

### 1.2 技术亮点

- **Redis Lua 脚本防超卖**：面试预约场次原子扣减库存
- **RabbitMQ 异步消息通知**：投递成功、审核结果、预约成功通知解耦
- **Elasticsearch 全文检索**：岗位关键词搜索、多条件筛选
- **MinIO 对象存储**：简历文件、企业资质、头像存储
- **AOP 操作日志**：异步线程池记录用户操作
- **RBAC 权限模型**：用户-角色-菜单权限，Redis 缓存权限
- **JWT + Redis 登录态**：分布式会话管理

---

## 二、环境搭建过程

### 2.1 中间件选型

| 中间件 | 版本 | 端口 | 用途 |
|--------|------|------|------|
| MySQL | 8.0 | 3306 | 核心业务数据存储 |
| Redis | 7 | 6379 | 缓存、会话、防超卖库存 |
| RabbitMQ | 3.13.7-management | 5672/15672 | 异步消息通知 |
| Elasticsearch | 8.13.4 | 9200 | 岗位全文检索 |
| MinIO | latest | 9990/9090 | 对象存储 |

### 2.2 Docker 安装与配置

#### 2.2.1 遇到的问题

1. **Docker Hub 被墙**：默认的 `rabbitmq:3-management` 等镜像无法拉取
2. **镜像源失效**：尝试多个国内镜像源（阿里云、腾讯云、163、华为云）均无法拉取
3. **Docker daemon 未启动**：需要先启动 Docker Desktop

#### 2.2.2 解决方案

使用华为云镜像加速：`swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/`

```yaml
# 修改后的 docker-compose.yml 镜像配置
rabbitmq:
  image: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/rabbitmq:3.13.7-management
elasticsearch:
  image: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/elasticsearch:8.13.4
mysql:
  image: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/mysql:8.0
redis:
  image: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:7-alpine
minio:
  image: swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/minio:latest
```

#### 2.2.3 容器启动命令

```bash
# RabbitMQ
docker run -d --name campus-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=campus \
  -e RABBITMQ_DEFAULT_PASS=campus123 \
  --restart always \
  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/rabbitmq:3.13.7-management

# Elasticsearch
docker run -d --name campus-es \
  -p 9200:9200 \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  --restart always \
  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/elasticsearch:8.13.4

# MinIO
docker start minio  # 已有容器，端口映射 9990:9000, 9090:9090
```

#### 2.2.4 配置调整

由于 MinIO 已有容器端口映射为 `9990:9000`，需要修改项目配置：

```yaml
# src/main/resources/application-dev.yml
app:
  minio:
    endpoint: http://localhost:9990  # 原为 9000
    access-key: minioadmin
    secret-key: minioadmin
```

### 2.3 RabbitMQ 本地安装尝试

尝试本地安装 RabbitMQ 时遇到的问题：

1. **Erlang 依赖**：RabbitMQ 运行需要 Erlang 环境
2. **网络限制**：GitHub 无法访问，无法下载安装包
3. **最终方案**：使用 Docker 方式运行，避免本地安装复杂性

---

## 三、项目结构

```
campus-recruitment/
├── pom.xml
├── deploy/
│   └── docker-compose.yml          # Docker 容器编排
├── dev-log.md                      # 本开发记录
├── src/main/java/com/campus/recruitment/
│   ├── CampusRecruitmentApplication.java  # Spring Boot 启动类
│   ├── common/                     # 通用模块
│   │   ├── annotation/            # 自定义注解
│   │   │   ├── RequirePermission.java   # 权限校验注解
│   │   │   ├── RequireLogin.java       # 登录校验注解
│   │   │   └── OperationLog.java       # 操作日志注解
│   │   ├── aspect/                # AOP 切面
│   │   │   └── OperationLogAspect.java  # 操作日志切面
│   │   ├── constant/              # 常量
│   │   │   ├── RedisConstants.java     # Redis Key 常量
│   │   │   └── RabbitMQConstants.java  # MQ Exchange/Queue 常量
│   │   ├── context/               # 上下文
│   │   │   └── LoginUserContext.java   # 登录用户上下文（ThreadLocal）
│   │   ├── enums/                 # 状态枚举
│   │   │   ├── ApplicationStatus.java    # 投递状态
│   │   │   ├── CompanyAuditStatus.java   # 企业审核状态
│   │   │   ├── FileBizType.java          # 文件业务类型
│   │   │   ├── InterviewBookingStatus.java # 预约状态
│   │   │   ├── InterviewSlotStatus.java  # 面试场次状态
│   │   │   ├── JobStatus.java            # 岗位状态
│   │   │   ├── MenuType.java             # 菜单类型
│   │   │   ├── MessageType.java          # 消息类型
│   │   │   ├── UserStatus.java           # 用户状态
│   │   │   └── UserType.java             # 用户类型
│   │   ├── exception/             # 异常处理
│   │   │   ├── BizException.java         # 业务异常
│   │   │   ├── ErrorCode.java            # 错误码
│   │   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   │   └── result/                # 统一响应
│   │       ├── R.java                   # 统一响应类
│   │       └── PageResult.java          # 分页响应类
│   ├── config/                    # 配置类
│   │   ├── AsyncConfig            # 异步线程池配置
│   │   ├── MinioConfig            # MinIO 配置
│   │   ├── MybatisPlusConfig      # MyBatis-Plus 配置
│   │   ├── RabbitMQConfig         # MQ 队列配置
│   │   ├── RedisConfig            # Redis 配置
│   │   ├── RedisScriptConfig      # Redis Lua 脚本配置
│   │   └── WebMvcConfig           # Web MVC 配置
│   ├── entity/                    # 数据库实体（23 个）
│   ├── mapper/                    # MyBatis-Plus Mapper（23 个）
│   ├── interceptor/               # 拦截器
│   │   ├── AuthInterceptor        # JWT 认证拦截
│   │   └── PermissionInterceptor  # 权限校验拦截
│   └── module/                    # 业务模块（11 个）
│       ├── auth/                  # 认证模块
│       │   ├── controller/AuthController.java
│       │   ├── service/AuthService.java
│       │   ├── service/impl/AuthServiceImpl.java
│       │   ├── dto/LoginRequest.java
│       │   ├── dto/RegisterRequest.java
│       │   ├── vo/LoginVO.java
│       │   ├── vo/RegisterVO.java
│       │   └── vo/UserInfoVO.java
│       ├── student/               # 学生模块
│       │   ├── controller/StudentController.java
│       │   ├── service/StudentService.java
│       │   ├── service/impl/StudentServiceImpl.java
│       │   ├── dto/StudentProfileDTO.java
│       │   └── vo/StudentProfileVO.java
│       ├── company/               # 企业模块
│       │   ├── controller/CompanyController.java
│       │   ├── service/CompanyService.java
│       │   ├── service/impl/CompanyServiceImpl.java
│       │   ├── dto/CompanyCertificationDTO.java
│       │   ├── dto/CompanyProfileDTO.java
│       │   ├── vo/CertificationStatusVO.java
│       │   └── vo/CompanyProfileVO.java
│       ├── file/                  # 文件模块
│       │   ├── controller/FileController.java
│       │   ├── service/FileService.java
│       │   ├── service/impl/FileServiceImpl.java
│       │   └── vo/FileUploadVO.java
│       ├── resume/                # 简历模块
│       │   ├── controller/ResumeController.java
│       │   ├── service/ResumeService.java
│       │   ├── service/impl/ResumeServiceImpl.java
│       │   ├── dto/CreateResumeRequest.java
│       │   └── vo/ResumeVO.java
│       ├── job/                   # 岗位模块
│       │   ├── controller/JobController.java
│       │   ├── controller/CompanyJobController.java
│       │   ├── service/JobService.java
│       │   ├── service/impl/JobServiceImpl.java
│       │   ├── dto/CreateJobRequest.java
│       │   ├── dto/UpdateJobRequest.java
│       │   ├── dto/JobQueryDTO.java
│       │   ├── vo/JobDetailVO.java
│       │   └── vo/JobVO.java
│       ├── application/           # 投递模块
│       │   ├── controller/ApplicationController.java
│       │   ├── controller/CompanyApplicationController.java
│       │   ├── service/ApplicationService.java
│       │   ├── service/impl/ApplicationServiceImpl.java
│       │   ├── dto/CreateApplicationRequest.java
│       │   ├── dto/UpdateApplicationStatusRequest.java
│       │   ├── vo/ApplicationStatusVO.java
│       │   ├── vo/ApplicationVO.java
│       │   └── vo/MyApplicationVO.java
│       ├── interview/             # 面试模块
│       │   ├── controller/InterviewController.java
│       │   ├── controller/CompanyInterviewController.java
│       │   ├── service/InterviewService.java
│       │   ├── service/impl/InterviewServiceImpl.java
│       │   ├── dto/BookInterviewRequest.java
│       │   ├── dto/CreateInterviewSlotRequest.java
│       │   ├── vo/InterviewBookingVO.java
│       │   ├── vo/InterviewSlotVO.java
│       │   └── vo/MyBookingVO.java
│       ├── message/               # 消息模块
│       │   ├── controller/MessageController.java
│       │   ├── service/MessageService.java
│       │   ├── service/impl/MessageServiceImpl.java
│       │   ├── consumer/NotifyMessageConsumer.java
│       │   ├── vo/MessageVO.java
│       │   └── vo/UnreadCountVO.java
│       ├── search/                # 搜索模块（ES）
│       │   ├── controller/SearchController.java
│       │   ├── service/JobSearchService.java
│       │   ├── service/impl/JobSearchServiceImpl.java
│       │   ├── repository/JobSearchRepository.java
│       │   ├── document/JobDocument.java
│       │   └── consumer/JobEsSyncConsumer.java
│       └── admin/                 # 后台管理
│           ├── controller/AdminController.java
│           ├── service/AdminService.java
│           ├── service/impl/AdminServiceImpl.java
│           ├── dto/AuditRequest.java
│           └── vo/DashboardVO.java
└── src/main/resources/
    ├── application.yml            # 主配置（端口、MyBatis-Plus、Swagger、日志）
    ├── application-dev.yml        # 开发环境配置（数据库、Redis、MQ、ES、MinIO）
    └── lua/
        └── interview_booking.lua  # 面试预约防超卖 Lua 脚本
```

---

## 四、核心配置

### 4.1 application.yml（主配置）

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  tomcat:
    max-threads: 200
    accept-count: 100

spring:
  application:
    name: campus-recruitment-backend
  profiles:
    active: dev
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

springdoc:
  swagger-ui:
    path: /swagger-ui/index.html
  api-docs:
    path: /v3/api-docs

logging:
  level:
    com.campus.recruitment: debug
    com.baomidou.mybatisplus: debug
```

### 4.2 application-dev.yml（开发环境配置）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/campus_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456

  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 2000ms

  rabbitmq:
    host: localhost
    port: 5672
    username: campus
    password: campus123
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms

  elasticsearch:
    uris: http://localhost:9200

app:
  minio:
    endpoint: http://localhost:9990
    access-key: minioadmin
    secret-key: minioadmin
    bucket-resume: resume
    bucket-license: license
    bucket-avatar: avatar
    bucket-job-attachment: job-attachment

  jwt:
    secret: CampusRecruitment2026SecretKeyForJWTTokenGenerationMustBeLongEnough
    ttl-hours: 24

  security:
    exclude-paths:
      - /auth/register
      - /auth/login
      - /jobs
      - /jobs/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /swagger-resources/**
      - /webjars/**
```

---

## 五、数据库设计

### 5.1 核心表结构

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| sys_role | 角色表 |
| sys_menu | 菜单/权限表 |
| sys_user_role | 用户-角色关联表 |
| sys_role_menu | 角色-菜单关联表 |
| sys_config | 系统配置表 |
| sys_dict | 数据字典表 |
| student_profile | 学生档案 |
| student_skill | 学生技能标签 |
| company_profile | 企业信息 |
| company_audit | 企业审核记录 |
| job | 岗位信息 |
| job_tag | 岗位标签关联 |
| job_favorite | 收藏岗位 |
| job_application | 投递记录 |
| application_log | 投递状态变更日志 |
| resume | 简历信息 |
| interview_slot | 面试场次 |
| interview_booking | 面试预约记录 |
| message | 消息通知 |
| operation_log | 操作日志 |
| login_log | 登录日志 |
| mq_message_log | MQ 消息日志（幂等） |
| file_object | 文件对象记录 |

### 5.2 初始化脚本

位于：`campus-recruitment-plan/校园招聘数据库设计包/V001__初始化数据库结构.sql`

---

## 六、核心功能实现

### 6.1 用户认证

- **注册**：用户名/密码注册，BCrypt 加密密码
- **登录**：JWT Token 生成，Redis 存储登录态
- **登出**：清除 Redis 登录态

### 6.2 RBAC 权限

- 用户-角色-菜单三级权限模型
- 自定义 `@RequirePermission` 注解
- Redis 缓存用户权限，减少数据库查询
- 自定义 `@RequireLogin` 注解标记需要登录的接口

### 6.3 面试预约防超卖

```lua
-- lua/interview_booking.lua
local key = KEYS[1]
local count = tonumber(redis.call('GET', key) or '0')
if count > 0 then
    redis.call('DECR', key)
    return 1
end
return 0
```

通过 Redis Lua 脚本保证原子操作，防止并发预约导致超卖。

### 6.4 异步消息通知

```java
// RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {
    // 投递成功通知队列
    public static final String APPLY_SUCCESS_QUEUE = "apply.success";
    // 审核结果通知队列
    public static final String AUDIT_RESULT_QUEUE = "audit.result";
    // 预约成功通知队列
    public static final String BOOKING_SUCCESS_QUEUE = "booking.success";
}
```

### 6.5 Elasticsearch 岗位搜索

- 岗位数据同步到 ES（通过 MQ 异步同步）
- 关键词全文检索
- 多条件筛选（城市、薪资、类型等）
- 降级策略：ES 不可用时 fallback 到 MySQL 查询

### 6.6 AOP 操作日志

```java
// OperationLogAspect.java
@Aspect
@Component
public class OperationLogAspect {
    // 异步记录操作日志到数据库
}
```

---

## 七、Maven 编译记录

```bash
# 编译命令
mvn clean compile

# 编译结果
[INFO] BUILD SUCCESS
[INFO] Compiling 151 source files to target/classes
```

---

## 八、踩坑记录

### 8.1 Docker 镜像拉取失败

**问题**：`unable to fetch descriptor ... which reports content size of zero`

**原因**：Docker Hub 被墙，国内镜像源不稳定

**解决**：使用华为云镜像 `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/`

### 8.2 MinIO 端口不匹配

**问题**：已有 MinIO 容器端口映射为 `9990:9000`，项目配置为 `localhost:9000`

**解决**：修改 `application-dev.yml` 中的 MinIO endpoint 为 `http://localhost:9990`

### 8.3 Elasticsearch 环境变量格式

**问题**：PowerShell 中双引号未正确转义导致 `invalid reference format`

**解决**：使用 `docker run -e "key=value"` 格式，每个环境变量单独加引号

### 8.4 docker-compose.yml version 字段过时

**警告**：`the attribute version is obsolete, it will be ignored`

**说明**：Docker Compose V2 不再需要 `version` 字段，可安全移除

---

## 九、启动与运行

### 9.1 启动中间件

```bash
# 方式一：使用 docker compose（需要配置国内镜像源）
cd C:\Code\JavaCode\campus-recruitment\deploy
docker compose up -d

# 方式二：单独启动已有容器
docker start minio

# RabbitMQ
docker run -d --name campus-rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=campus -e RABBITMQ_DEFAULT_PASS=campus123 \
  --restart always \
  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/rabbitmq:3.13.7-management

# Elasticsearch
docker run -d --name campus-es -p 9200:9200 \
  -e "discovery.type=single-node" -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" --restart always \
  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/elasticsearch:8.13.4
```

### 9.2 初始化数据库

```bash
mysql -uroot -p123456 < campus-recruitment-plan/校园招聘数据库设计包/V001__初始化数据库结构.sql
```

### 9.3 编译项目

```bash
mvn clean compile
# 结果：BUILD SUCCESS，151 个源文件
```

### 9.4 启动后端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 9.5 访问接口文档

```
http://localhost:8080/api/swagger-ui/index.html
```

---

## 十、访问地址汇总

| 服务 | 地址 | 账号 |
|------|------|------|
| 后端 API | http://localhost:8080/api | - |
| Swagger UI | http://localhost:8080/api/swagger-ui/index.html | - |
| RabbitMQ 管理台 | http://localhost:15672 | campus / campus123 |
| MinIO Console | http://localhost:9090 | minioadmin / minioadmin |
| Elasticsearch | http://localhost:9200 | - |
| MySQL | localhost:3306 | root / 123456 |
| Redis | localhost:6379 | 无密码 |

---

## 十一、待完成事项

- [ ] 初始化 MySQL 数据库
- [ ] 启动 Spring Boot 应用并验证连接
- [ ] 测试各接口功能
- [ ] 补充单元测试

---

*本文档由 Vibe Coding 自动生成，记录开发全过程。*
