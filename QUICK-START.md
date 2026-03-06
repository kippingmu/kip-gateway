# 快速启动指南

## 前置条件

1. **JDK 21**
2. **Maven 3.6+**
3. **Redis 服务器**（用于缓存、IP 黑白名单、灰度发布）
4. **Nacos 服务器**（用于服务注册和配置中心）
5. **Sentinel Dashboard**（可选，用于流量控制）
6. **Zipkin 服务器**（可选，用于链路追踪）

## 启动步骤

### 1. 启动 Redis

```bash
# 使用 Docker 启动 Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 或使用本地 Redis
redis-server
```

### 2. 启动 Nacos

```bash
# 使用 Docker 启动 Nacos（单机模式）
docker run -d --name nacos \
  -e MODE=standalone \
  -p 8848:8848 \
  nacos/nacos-server:latest

# 访问 Nacos 控制台
# http://localhost:8848/nacos
# 默认用户名/密码：nacos/nacos
```

### 3. 启动 Sentinel Dashboard（可选）

```bash
# 下载 Sentinel Dashboard
wget https://github.com/alibaba/Sentinel/releases/download/1.8.9/sentinel-dashboard-1.8.9.jar

# 启动
java -jar sentinel-dashboard-1.8.9.jar --server.port=8080

# 访问 Sentinel 控制台
# http://localhost:8080
# 默认用户名/密码：sentinel/sentinel
```

### 4. 启动 Zipkin（可选）

```bash
# 使用 Docker 启动 Zipkin
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin

# 访问 Zipkin UI
# http://localhost:9411
```

### 5. 配置网关

编辑 `app/gateway-web/src/main/resources/application-dev.yml`：

```yaml
spring:
  data:
    redis:
      host: localhost  # 修改为你的 Redis 地址
      port: 6379

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # 修改为你的 Nacos 地址
      config:
        server-addr: localhost:8848

    sentinel:
      transport:
        dashboard: localhost:8080  # 修改为你的 Sentinel 地址

management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans  # 修改为你的 Zipkin 地址
```

### 6. 编译项目

```bash
cd /Users/xiaoshichuan/ide/idea/kip-gateway
mvn clean package -DskipTests
```

### 7. 启动网关

```bash
cd app/gateway-web
java -jar target/app.jar --spring.profiles.active=dev
```

或使用 Maven 启动：

```bash
mvn spring-boot:run -pl app/gateway-web -Dspring-boot.run.profiles=dev
```

### 8. 验证启动

访问健康检查接口：

```bash
curl http://localhost:9527/actuator/health
```

预期响应：

```json
{
  "status": "UP"
}
```

## 功能验证

### 1. 查询路由列表

```bash
curl http://localhost:9527/gateway/routes
```

### 2. 测试 IP 黑白名单

```bash
# 添加 IP 到黑名单
curl -X POST http://localhost:9527/gateway/ip/blacklist \
  -H "Content-Type: application/json" \
  -d '{"ip": "192.168.1.100"}'

# 查询黑名单
curl http://localhost:9527/gateway/ip/blacklist
```

### 3. 测试灰度发布

```bash
# 配置灰度规则（假设有 user-service）
curl -X POST http://localhost:9527/gateway/gray/user-service \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "grayVersion": "v2.0",
    "weight": 20
  }'

# 查询灰度配置
curl http://localhost:9527/gateway/gray/user-service
```

### 4. 测试请求缓存

首先在配置文件中启用缓存：

```yaml
gateway:
  cache:
    enabled: true
    ttl: 300
    paths: /api/test/**
```

然后测试：

```bash
# 第一次请求（未命中缓存）
curl -v http://localhost:9527/api/test/data

# 第二次请求（命中缓存，响应头包含 X-Cache-Hit: true）
curl -v http://localhost:9527/api/test/data
```

### 5. 查看链路追踪

访问 Zipkin UI：http://localhost:9411

发送几个请求后，可以在 Zipkin 中查看完整的调用链路。

## 功能开关

所有功能都可以通过配置文件开关：

```yaml
gateway:
  # IP 黑白名单
  ip-filter:
    enabled: true  # 设置为 false 禁用

  # 请求缓存
  cache:
    enabled: false  # 设置为 true 启用

  # 灰度发布
  gray:
    enabled: false  # 设置为 true 启用

  # OAuth2
  oauth2:
    enabled: false  # 设置为 true 启用

  # 限流
  rate-limit:
    enabled: true  # 设置为 false 禁用

# 链路追踪
management:
  tracing:
    enabled: true  # 设置为 false 禁用
```

## 常见问题

### 1. Redis 连接失败

确保 Redis 服务已启动，并且配置的地址和端口正确。

```bash
# 测试 Redis 连接
redis-cli ping
```

### 2. Nacos 连接失败

确保 Nacos 服务已启动，并且配置的地址和端口正确。

```bash
# 访问 Nacos 控制台
curl http://localhost:8848/nacos
```

### 3. 端口冲突

如果 9527 端口被占用，可以修改配置：

```yaml
server:
  port: 8080  # 修改为其他端口
```

### 4. OAuth2 配置

如果不使用 OAuth2，请确保配置中 `gateway.oauth2.enabled=false`。

### 5. 内存不足

如果启动时内存不足，可以调整 JVM 参数：

```bash
java -Xms512m -Xmx1024m -jar target/app.jar
```

## 监控端点

网关提供了以下监控端点（需要在配置中启用）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

访问监控端点：

- 健康检查：http://localhost:9527/actuator/health
- 应用信息：http://localhost:9527/actuator/info
- 指标数据：http://localhost:9527/actuator/metrics
- Prometheus：http://localhost:9527/actuator/prometheus

## 日志

日志文件位置：`logs/gateway.log`

可以通过配置调整日志级别：

```yaml
logging:
  level:
    root: INFO
    xyz.kip: DEBUG  # 网关日志级别
```

## 下一步

- 阅读 [API-USAGE.md](API-USAGE.md) 了解详细的 API 使用方法
- 阅读 [P1-FEATURES-SUMMARY.md](P1-FEATURES-SUMMARY.md) 了解功能实现细节
- 阅读 [GATEWAY.MD](../../GATEWAY.MD) 了解完整的功能清单

## 技术支持

如有问题，请查看：

1. 日志文件：`logs/gateway.log`
2. Nacos 控制台：http://localhost:8848/nacos
3. Sentinel 控制台：http://localhost:8080
4. Zipkin UI：http://localhost:9411
