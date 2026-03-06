# 配置文件说明

## 配置文件结构

项目采用**最小化本地配置 + Nacos 集中管理**的策略：

```
Nacos 配置中心 (最高优先级，支持热更新)
    ↓
application-{profile}.yml (只包含 Nacos 连接配置)
    ↓
application.yml (通用基础配置)
```

## 配置文件职责

### 1. application.yml (通用基础配置)

**职责：** 所有环境共享的最基础配置

**包含内容：**
- Spring 应用基础配置（应用名称、配置导入）
- Nacos 配置导入设置
- Gateway 服务发现配置
- Jackson 序列化配置
- Management 端点配置

**特点：**
- 极简配置，只包含必需项
- 不包含任何环境特定的值
- 配置项稳定，几乎不修改

### 2. application-dev.yml (开发环境配置)

**职责：** 只包含 Nacos 连接配置和必要的基础配置

**包含内容：**
- Nacos 服务器地址和配置
- 服务端口号
- 本地日志配置

**特点：**
- **极简原则**：只保留 Nacos 连接配置
- 其他所有配置都放在 Nacos 中
- 需要重启才能生效

**为什么这样设计？**
- ✅ 配置集中管理，便于维护
- ✅ 支持热更新，无需重启
- ✅ 环境隔离，避免配置泄露
- ✅ 版本控制，配置变更可追溯

### 3. nacos-todo.yml (Nacos 配置中心)

**职责：** 包含所有业务配置，支持热更新

**包含内容：**
- Redis 连接配置
- OAuth2 配置
- Gateway 路由配置
- Sentinel 配置和规则数据源
- 链路追踪配置
- 网关功能开关（限流、IP 过滤、缓存、灰度发布）
- 日志级别配置

**特点：**
- 支持热更新，修改后无需重启网关
- 适合频繁变化的配置
- 可以针对不同环境创建不同的配置文件
- 配置变更有历史记录

**使用方式：**
1. 登录 Nacos 控制台：http://10.42.0.125:8848/nacos
2. 进入"配置管理" -> "配置列表"
3. 创建配置：
   - Data ID: `kip-gateway-dev.yml` (开发环境)
   - Group: `DEFAULT_GROUP`
   - 配置格式: `YAML`
   - 配置内容: 复制 nacos-todo.yml 的内容
4. 发布配置

## 配置分层原则

### 本地配置文件（application.yml / application-dev.yml）

**只包含：**
- ✅ Nacos 连接配置（必须）
- ✅ 服务端口号（必须）
- ✅ 应用名称（必须）
- ✅ 本地日志配置（可选）

**不包含：**
- ❌ Redis 配置
- ❌ 路由配置
- ❌ Sentinel 配置
- ❌ 功能开关
- ❌ 业务参数

### Nacos 配置中心（nacos-todo.yml）

**包含：**
- ✅ 所有中间件配置（Redis、Sentinel、Zipkin）
- ✅ 所有路由配置
- ✅ 所有功能开关
- ✅ 所有业务参数
- ✅ 日志级别配置

## 配置合并示例

假设启动命令为：`java -jar app.jar --spring.profiles.active=dev`

### 第一步：加载 application.yml
```yaml
spring:
  application:
    name: kip-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
```

### 第二步：加载 application-dev.yml
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.42.0.125:8848
      config:
        server-addr: 10.42.0.125:8848

server:
  port: 9527
```

### 第三步：从 Nacos 加载 kip-gateway-dev.yml
```yaml
spring:
  data:
    redis:
      host: 10.42.0.125
      port: 6379
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://kip-auth-service
          predicates:
            - Path=/api/auth/**

gateway:
  rate-limit:
    enabled: true
    default-qps: 100
```

### 最终生效的配置
三个配置文件合并后的完整配置。

## 配置修改流程

### 修改 Nacos 连接配置（需要重启）

1. 修改 `application-dev.yml` 中的 Nacos 地址
2. 提交代码
3. 重新打包部署
4. 重启应用

### 修改业务配置（无需重启）

1. 登录 Nacos 控制台
2. 找到配置：`kip-gateway-dev.yml`
3. 修改配置（如：调整限流 QPS、添加路由等）
4. 发布配置
5. 网关自动刷新（约 1-2 秒生效）

## 环境配置

### 开发环境
- 本地配置：`application-dev.yml`
- Nacos 配置：`kip-gateway-dev.yml`
- 启动命令：`--spring.profiles.active=dev`

### 生产环境
- 本地配置：`application-prod.yml`
- Nacos 配置：`kip-gateway-prod.yml`
- 启动命令：`--spring.profiles.active=prod`

### 本地环境（快速启动）
- 本地配置：`application-local.yml`
- 启动命令：`--spring.profiles.active=local`
- 特点：禁用 Nacos，所有配置都在本地

## 配置最佳实践

### 1. 什么配置放在本地？

**只放这些：**
- Nacos 服务器地址
- 服务端口号
- 应用名称
- 本地日志配置

**原则：** 能放 Nacos 的都放 Nacos

### 2. 什么配置放在 Nacos？

**放这些：**
- 所有中间件配置（Redis、Sentinel、Zipkin）
- 所有路由配置
- 所有功能开关
- 所有业务参数
- 日志级别

**原则：** 需要热更新的都放 Nacos

### 3. 配置优先级

```
Nacos 配置 > application-dev.yml > application.yml
```

如果同一个配置项在多个地方都有，Nacos 的配置会覆盖本地配置。

## 常见场景

### 场景 1：添加新路由

**操作步骤：**
1. 登录 Nacos 控制台
2. 编辑 `kip-gateway-dev.yml`
3. 在 `spring.cloud.gateway.routes` 下添加新路由
4. 发布配置
5. 网关自动刷新（无需重启）

### 场景 2：调整限流 QPS

**操作步骤：**
1. 登录 Nacos 控制台
2. 编辑 `kip-gateway-dev.yml`
3. 修改 `gateway.rate-limit.default-qps` 的值
4. 发布配置
5. 网关自动刷新（无需重启）

### 场景 3：启用灰度发布

**操作步骤：**
1. 登录 Nacos 控制台
2. 编辑 `kip-gateway-dev.yml`
3. 修改 `gateway.gray.enabled: true`
4. 发布配置
5. 网关自动刷新（无需重启）

### 场景 4：调整日志级别

**操作步骤：**
1. 登录 Nacos 控制台
2. 编辑 `kip-gateway-dev.yml`
3. 修改 `logging.level` 下的日志级别
4. 发布配置
5. 网关自动刷新（无需重启）

## 配置验证

启动后可以通过以下方式验证配置：

```bash
# 查看当前配置
curl http://localhost:9527/actuator/env

# 查看路由配置
curl http://localhost:9527/actuator/gateway/routes

# 查看健康状态
curl http://localhost:9527/actuator/health

# 查看 Nacos 配置是否加载
curl http://localhost:9527/actuator/configprops
```

## 常见问题

### 1. Nacos 配置不生效？

**检查清单：**
- [ ] Nacos 服务器是否可访问
- [ ] Data ID 是否正确（kip-gateway-dev.yml）
- [ ] Group 是否正确（DEFAULT_GROUP）
- [ ] `spring.cloud.nacos.config.enabled` 是否为 true
- [ ] `spring.cloud.nacos.config.refresh-enabled` 是否为 true

**验证方法：**
```bash
# 查看 Nacos 配置是否加载
curl http://localhost:9527/actuator/env | grep nacos
```

### 2. 配置修改后没有生效？

**可能原因：**
- Nacos 配置没有发布
- 网关没有开启配置刷新
- 配置项不支持热更新（如端口号）

**解决方法：**
- 确认 Nacos 配置已发布
- 检查 `spring.cloud.nacos.config.refresh-enabled: true`
- 如果是基础配置，需要重启网关

### 3. 如何禁用 Nacos 配置？

**方法 1：** 在 application-dev.yml 中设置
```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: false
```

**方法 2：** 启动参数
```bash
--spring.cloud.nacos.config.enabled=false
```

### 4. 本地开发如何快速启动？

使用 `application-local.yml`，禁用 Nacos：
```bash
java -jar app.jar --spring.profiles.active=local
```

## Sentinel 规则配置

### 流控规则 (sentinel-flow-rules)

在 Nacos 中创建配置：
- Data ID: `sentinel-flow-rules`
- Group: `DEFAULT_GROUP`
- 配置格式: `JSON`

```json
[
  {
    "resource": "auth-service",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

### 降级规则 (sentinel-degrade-rules)

在 Nacos 中创建配置：
- Data ID: `sentinel-degrade-rules`
- Group: `DEFAULT_GROUP`
- 配置格式: `JSON`

```json
[
  {
    "resource": "auth-service",
    "grade": 0,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  }
]
```

## 配置文件对比

| 配置项 | application.yml | application-dev.yml | nacos-todo.yml |
|--------|----------------|---------------------|----------------|
| 应用名称 | ✅ | ❌ | ❌ |
| Nacos 地址 | ❌ | ✅ | ❌ |
| 服务端口 | ❌ | ✅ | ❌ |
| Redis 配置 | ❌ | ❌ | ✅ |
| 路由配置 | ❌ | ❌ | ✅ |
| Sentinel 配置 | ❌ | ❌ | ✅ |
| 功能开关 | ❌ | ❌ | ✅ |
| 日志级别 | ❌ | ✅ (本地) | ✅ (动态) |

## 总结

**核心原则：**
1. 本地配置最小化，只保留 Nacos 连接配置
2. 业务配置全部放 Nacos，支持热更新
3. 配置分层清晰，职责明确
4. 环境隔离，配置安全
