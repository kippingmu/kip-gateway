# Kip Gateway - Spring Cloud Gateway 网关

基于 Spring Cloud Gateway 的微服务网关，集成了 Nacos、Sentinel、Redis 等组件，提供路由转发、限流、认证、灰度发布等功能。

## ✨ 功能特性

### 已实现功能 (P1)

- ✅ **IP 黑白名单** - 支持 CIDR 格式，动态配置
- ✅ **链路追踪** - Micrometer Tracing + Zipkin
- ✅ **请求缓存** - 基于 Redis 的响应缓存
- ✅ **灰度发布** - 支持权重、用户 ID、请求头三种策略
- ✅ **OAuth2 集成** - 支持 OAuth2 资源服务器模式

### 核心功能

- ✅ **路由转发** - 基于路径的动态路由
- ✅ **服务发现** - 集成 Nacos Discovery
- ✅ **负载均衡** - 客户端负载均衡
- ✅ **请求限流** - 基于 IP 的令牌桶限流
- ✅ **熔断降级** - Sentinel 流量控制
- ✅ **身份认证** - JWT Token 认证
- ✅ **权限校验** - 基于角色的权限控制
- ✅ **跨域处理** - CORS 配置
- ✅ **日志记录** - 请求日志和 TraceId

## 🚀 快速开始

### 前置条件

- JDK 21+
- Maven 3.6+
- Redis (用于缓存、IP 黑白名单、灰度发布)
- Nacos (用于服务注册和配置中心)

### 编译项目

```bash
mvn clean package -DskipTests
```

### 启动方式

#### 方式 1：使用启动脚本（推荐）

```bash
./start.sh
```

然后选择启动模式：
1. 开发环境 (使用 Nacos)
2. 本地环境 (不使用 Nacos，快速启动)
3. 开发环境 (禁用 Nacos)

#### 方式 2：直接启动

**开发环境（使用 Nacos）：**
```bash
java -jar app/gateway-web/target/app.jar --spring.profiles.active=dev
```

**本地环境（不使用 Nacos）：**
```bash
java -jar app/gateway-web/target/app.jar --spring.profiles.active=local
```

### 验证启动

```bash
# 健康检查
curl http://localhost:9527/actuator/health

# 查看路由
curl http://localhost:9527/actuator/gateway/routes
```

## 📖 文档

- [快速启动指南](STARTUP-GUIDE.md) - 启动前准备和常见问题
- [配置文件说明](CONFIG-GUIDE.md) - 配置文件结构和最佳实践
- [API 使用文档](app/gateway-web/API-USAGE.md) - 完整的 API 使用说明
- [功能实现总结](P1-FEATURES-SUMMARY.md) - P1 功能实现细节
- [模块结构说明](app/gateway-web/MODULE-STRUCTURE.md) - 代码结构说明

## 🔧 配置说明

### 配置文件结构

```
application.yml              # 通用基础配置
application-dev.yml          # 开发环境配置（只包含 Nacos 连接）
application-local.yml        # 本地环境配置（不使用 Nacos）
nacos-todo.yml              # Nacos 配置模板（需上传到 Nacos）
```

### Nacos 配置

**重要：** 使用开发环境启动前，需要在 Nacos 中创建配置：

1. 登录 Nacos 控制台：http://10.42.0.125:8848/nacos
2. 创建配置：
   - Data ID: `kip-gateway-dev.yml`
   - Group: `DEFAULT_GROUP`
   - 配置内容: 复制 `nacos-todo.yml` 的内容

详细步骤请参考 [STARTUP-GUIDE.md](STARTUP-GUIDE.md)

## 🎯 管理接口

### 路由管理

```bash
# 查询所有路由
GET /gateway/routes

# 添加路由
POST /gateway/routes

# 删除路由
DELETE /gateway/routes/{id}

# 刷新路由
POST /gateway/routes/refresh
```

### IP 黑白名单管理

```bash
# 添加 IP 到黑名单
POST /gateway/ip/blacklist
Body: {"ip": "192.168.1.100"}

# 查询黑名单
GET /gateway/ip/blacklist

# 添加 IP 到白名单
POST /gateway/ip/whitelist
Body: {"ip": "10.0.0.0/8"}

# 查询白名单
GET /gateway/ip/whitelist
```

### 灰度发布管理

```bash
# 配置灰度规则
POST /gateway/gray/{serviceName}
Body: {
  "enabled": true,
  "grayVersion": "v2.0",
  "weight": 20,
  "grayUserIds": "user001,user002",
  "grayHeaders": "X-Test-Group=beta"
}

# 查询灰度配置
GET /gateway/gray/{serviceName}

# 更新灰度权重
PUT /gateway/gray/{serviceName}/weight
Body: {"weight": 50}
```

### 缓存管理

```bash
# 清除指定路径缓存
DELETE /gateway/cache/path?path=/api/user

# 清除所有缓存
DELETE /gateway/cache/all

# 查询缓存统计
GET /gateway/cache/stats
```

## 🏗️ 技术栈

- **Spring Boot 3.5.0**
- **Spring Cloud Gateway 4.3.0**
- **Spring Cloud Alibaba 2025.0.0.0**
- **Nacos 3.0.3** - 服务注册与配置中心
- **Sentinel 1.8.9** - 流量控制与熔断降级
- **Redis** - 缓存和分布式存储
- **Micrometer Tracing** - 链路追踪
- **Zipkin** - 分布式追踪系统

## 📊 监控

### Actuator 端点

```bash
# 健康检查
http://localhost:9527/actuator/health

# 应用信息
http://localhost:9527/actuator/info

# 指标数据
http://localhost:9527/actuator/metrics

# Prometheus 指标
http://localhost:9527/actuator/prometheus

# 网关路由
http://localhost:9527/actuator/gateway/routes
```

### Sentinel Dashboard

访问：http://10.42.0.125:8080

### Zipkin UI

访问：http://10.42.0.125:9411

## 🔐 安全

### 认证方式

- **JWT Token 认证** - 默认启用
- **OAuth2 认证** - 可选启用

### 安全防护

- **IP 黑白名单** - 支持 CIDR 格式
- **请求限流** - 基于 IP 的令牌桶算法
- **熔断降级** - Sentinel 流量控制

## 🎨 过滤器执行顺序

```
-100: RequestLoggingFilter（日志记录）
 -99: AuthenticationFilter（JWT 认证）
 -97: OAuth2AuthenticationFilter（OAuth2 认证）
 -98: AuthorizationFilter（权限校验）
 -95: IpBlackWhiteListFilter（IP 过滤）
 -90: GrayReleaseFilter（灰度发布）
 -80: RequestCacheFilter（请求缓存）
 -50: RateLimitFilter（限流）
 -10: CorsFilter（跨域处理）
   0: 默认过滤器
```

## 📝 日志

日志文件位置：`logs/gateway.log`

日志级别配置：
```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG
    xyz.kip: DEBUG
```

## 🐛 故障排查

### 常见问题

1. **dataId must be specified**
   - 原因：Nacos 中未创建配置文件
   - 解决：参考 [STARTUP-GUIDE.md](STARTUP-GUIDE.md) 创建配置

2. **connect to nacos server fail**
   - 原因：无法连接 Nacos 服务器
   - 解决：检查 Nacos 服务是否启动，网络是否连通

3. **Redis connection refused**
   - 原因：Redis 服务未启动
   - 解决：启动 Redis 服务

详细故障排查请参考 [STARTUP-GUIDE.md](STARTUP-GUIDE.md)

## 📦 项目结构

```
kip-gateway/
├── app/
│   └── gateway-web/              # 网关主模块
│       ├── src/main/java/
│       │   └── xyz/kip/gateway/
│       │       ├── config/       # 配置类
│       │       ├── controller/   # 控制器
│       │       ├── filter/       # 过滤器
│       │       ├── handler/      # 处理器
│       │       ├── model/        # 数据模型
│       │       ├── service/      # 服务层
│       │       └── util/         # 工具类
│       └── src/main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-local.yml
│           └── nacos-todo.yml
├── STARTUP-GUIDE.md              # 启动指南
├── CONFIG-GUIDE.md               # 配置说明
├── API-USAGE.md                  # API 文档
├── P1-FEATURES-SUMMARY.md        # 功能总结
└── start.sh                      # 启动脚本
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[MIT License](LICENSE)

## 📞 联系方式

如有问题，请查看文档或提交 Issue。

## Git Remote Policy

Use SSH for the Git remote. Do not use HTTPS remotes in this repository.

Correct remote:

git remote set-url origin git@github.com:kippingmu/kip-gateway.git
