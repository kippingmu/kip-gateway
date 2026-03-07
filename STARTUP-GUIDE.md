# 启动指南

## 当前配置策略

为了方便快速启动，当前 `application-dev.yml` 包含了所有必要的配置，**Nacos 配置中心已临时禁用**。

这样可以直接启动，无需先配置 Nacos。

## 快速启动

### 方式 1：直接启动（推荐）

```bash
java -jar app/gateway-web/target/app.jar --spring.profiles.active=dev
```

或使用 Maven：

```bash
mvn spring-boot:run -pl app/gateway-web -Dspring-boot.run.profiles=dev
```

### 方式 2：使用启动脚本

```bash
./start.sh
```

选择选项 1（开发环境）

### 方式 3：IDEA 启动

在 IDEA 中运行 `GatewayBootApplication`，配置 VM options：
```
-Dspring.profiles.active=dev
```

## 验证启动

启动成功后，访问：

```bash
# 健康检查
curl http://localhost:9527/actuator/health

# 查看路由
curl http://localhost:9527/actuator/gateway/routes
```

预期响应：
```json
{
  "status": "UP"
}
```

## 启用 Nacos 配置（可选）

如果需要使用 Nacos 配置中心实现配置热更新，按以下步骤操作：

### 1. 在 Nacos 中创建配置

1. 登录 Nacos 控制台：http://10.42.0.125:8848/nacos
   - 用户名：nacos
   - 密码：nacos

2. 进入"配置管理" -> "配置列表"

3. 点击右上角"+"创建配置：
   - **Data ID**: `kip-gateway-dev.yml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**: 复制 `nacos-todo.yml` 的内容

4. 点击"发布"

### 2. 启用 Nacos 配置

修改 `application-dev.yml`：

```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: true  # 改为 true
```

### 3. 重启网关

```bash
java -jar app/gateway-web/target/app.jar --spring.profiles.active=dev
```

### 4. 验证 Nacos 配置

```bash
# 查看配置信息
curl http://localhost:9527/actuator/env | grep nacos
```

## 配置说明

### 当前配置（Nacos 禁用）

所有配置都在 `application-dev.yml` 中：
- ✅ Redis 配置
- ✅ Sentinel 配置
- ✅ 链路追踪配置
- ✅ 网关功能配置
- ✅ 日志配置

**优点：**
- 快速启动，无需配置 Nacos
- 配置集中在一个文件
- 适合本地开发和调试

**缺点：**
- 不支持配置热更新
- 修改配置需要重启

### 启用 Nacos 后

配置分为两部分：
- `application-dev.yml`：只包含 Nacos 连接配置
- Nacos 配置中心：包含所有业务配置

**优点：**
- 支持配置热更新
- 配置集中管理
- 环境隔离

**缺点：**
- 需要先配置 Nacos
- 依赖 Nacos 服务

## 依赖服务

### 必需服务

- **Redis** - 用于缓存、IP 黑白名单、灰度发布
  ```bash
  # Docker 启动
  docker run -d --name redis -p 6379:6379 redis:latest
  ```

### 可选服务

- **Nacos** - 服务注册和配置中心
  ```bash
  # Docker 启动
  docker run -d --name nacos \
    -e MODE=standalone \
    -p 8848:8848 \
    nacos/nacos-server:latest
  ```

- **Sentinel Dashboard** - 流量控制面板
  ```bash
  # 下载并启动
  java -jar sentinel-dashboard-1.8.9.jar --server.port=8080
  ```

- **Zipkin** - 链路追踪
  ```bash
  # Docker 启动
  docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin
  ```

## 功能开关

在 `application-dev.yml` 中可以开关各项功能：

```yaml
gateway:
  rate-limit:
    enabled: true  # 限流
  ip-filter:
    enabled: true  # IP 过滤
  cache:
    enabled: false  # 请求缓存
  gray:
    enabled: false  # 灰度发布
  oauth2:
    enabled: false  # OAuth2 认证
```

## 常见问题

### 1. Redis 连接失败

**错误信息：**
```
Unable to connect to Redis
```

**解决方法：**
- 检查 Redis 是否启动：`redis-cli ping`
- 检查 Redis 地址和端口是否正确
- 临时禁用 Redis 相关功能：
  ```yaml
  gateway:
    cache:
      enabled: false
    gray:
      enabled: false
  ```

### 2. Sentinel 连接失败

**错误信息：**
```
Failed to connect to Sentinel Dashboard
```

**解决方法：**
- 检查 Sentinel Dashboard 是否启动
- 临时禁用 Sentinel：
  ```yaml
  spring:
    cloud:
      sentinel:
        enabled: false
  ```

### 3. 端口被占用

**错误信息：**
```
Port 9527 is already in use
```

**解决方法：**
- 修改端口：
  ```yaml
  server:
    port: 8080  # 改为其他端口
  ```

### 4. Nacos 配置不生效

**原因：**
- Nacos 配置中心被禁用
- Nacos 中没有创建配置文件

**解决方法：**
- 检查 `spring.cloud.nacos.config.enabled` 是否为 true
- 确认 Nacos 中已创建 `kip-gateway-dev.yml` 配置

## 配置迁移

### 从本地配置迁移到 Nacos

1. 复制 `application-dev.yml` 中的业务配置
2. 在 Nacos 中创建 `kip-gateway-dev.yml`
3. 粘贴配置内容
4. 发布配置
5. 修改 `application-dev.yml`，启用 Nacos 配置
6. 删除 `application-dev.yml` 中的业务配置
7. 重启网关

### 从 Nacos 迁移到本地配置

1. 从 Nacos 中导出 `kip-gateway-dev.yml` 配置
2. 复制到 `application-dev.yml`
3. 禁用 Nacos 配置：`spring.cloud.nacos.config.enabled: false`
4. 重启网关

## 监控端点

启动后可以访问以下监控端点：

```bash
# 健康检查
curl http://localhost:9527/actuator/health

# 应用信息
curl http://localhost:9527/actuator/info

# 指标数据
curl http://localhost:9527/actuator/metrics

# 网关路由
curl http://localhost:9527/actuator/gateway/routes

# 环境配置
curl http://localhost:9527/actuator/env
```

## 日志

日志文件位置：`logs/gateway.log`

实时查看日志：
```bash
tail -f logs/gateway.log
```

## 下一步

- 查看 [API-USAGE.md](app/gateway-web/API-USAGE.md) 了解 API 使用
- 查看 [CONFIG-GUIDE.md](CONFIG-GUIDE.md) 了解配置详情
- 查看 [P1-FEATURES-SUMMARY.md](P1-FEATURES-SUMMARY.md) 了解功能实现
