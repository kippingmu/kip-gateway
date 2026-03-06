# Gateway API 使用文档

## 1. IP 黑白名单管理 API

### 1.1 添加 IP 到黑名单
```bash
curl -X POST http://10.42.0.125:9527/gateway/ip/blacklist \
  -H "Content-Type: application/json" \
  -d '{"ip": "192.168.1.100"}'

# 支持 CIDR 格式
curl -X POST http://10.42.0.125:9527/gateway/ip/blacklist \
  -H "Content-Type: application/json" \
  -d '{"ip": "192.168.1.0/24"}'
```

### 1.2 从黑名单移除 IP
```bash
curl -X DELETE http://10.42.0.125:9527/gateway/ip/blacklist/192.168.1.100
```

### 1.3 查询黑名单
```bash
curl http://10.42.0.125:9527/gateway/ip/blacklist
```

### 1.4 清空黑名单
```bash
curl -X DELETE http://10.42.0.125:9527/gateway/ip/blacklist
```

### 1.5 白名单操作（同黑名单）
```bash
# 添加到白名单
curl -X POST http://10.42.0.125:9527/gateway/ip/whitelist \
  -H "Content-Type: application/json" \
  -d '{"ip": "10.0.0.0/8"}'

# 查询白名单
curl http://10.42.0.125:9527/gateway/ip/whitelist

# 从白名单移除
curl -X DELETE http://10.42.0.125:9527/gateway/ip/whitelist/10.0.0.1

# 清空白名单
curl -X DELETE http://10.42.0.125:9527/gateway/ip/whitelist
```

## 2. 灰度发布管理 API

### 2.1 配置灰度规则
```bash
curl -X POST http://10.42.0.125:9527/gateway/gray/user-service \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "grayVersion": "v2.0",
    "weight": 20,
    "grayUserIds": "user001,user002,user003",
    "grayHeaders": "X-Test-Group=beta;X-Region=cn-north"
  }'
```

参数说明：
- `enabled`: 是否启用灰度发布
- `grayVersion`: 灰度版本标识
- `weight`: 灰度流量权重（0-100），表示百分比
- `grayUserIds`: 灰度用户 ID 列表，逗号分隔
- `grayHeaders`: 灰度请求头规则，格式：`header1=value1;header2=value2`

### 2.2 查询灰度配置
```bash
curl http://10.42.0.125:9527/gateway/gray/user-service
```

### 2.3 更新灰度权重
```bash
curl -X PUT http://10.42.0.125:9527/gateway/gray/user-service/weight \
  -H "Content-Type: application/json" \
  -d '{"weight": 50}'
```

### 2.4 启用/禁用灰度发布
```bash
# 启用
curl -X PUT http://10.42.0.125:9527/gateway/gray/user-service/toggle \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'

# 禁用
curl -X PUT http://10.42.0.125:9527/gateway/gray/user-service/toggle \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### 2.5 删除灰度配置
```bash
curl -X DELETE http://10.42.0.125:9527/gateway/gray/user-service
```

### 2.6 灰度发布使用示例

#### 场景 1：基于权重的灰度发布
```bash
# 配置 20% 流量到灰度版本
curl -X POST http://10.42.0.125:9527/gateway/gray/user-service \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "grayVersion": "v2.0",
    "weight": 20
  }'
```

#### 场景 2：指定用户灰度
```bash
# 只有特定用户访问灰度版本
curl -X POST http://10.42.0.125:9527/gateway/gray/user-service \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "grayVersion": "v2.0",
    "grayUserIds": "user001,user002"
  }'

# 客户端请求时需要携带用户 ID
curl http://10.42.0.125:9527/api/user/profile \
  -H "X-User-Id: user001"
```

#### 场景 3：基于请求头的灰度
```bash
# 配置特定请求头的请求路由到灰度版本
curl -X POST http://10.42.0.125:9527/gateway/gray/user-service \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "grayVersion": "v2.0",
    "grayHeaders": "X-Test-Group=beta"
  }'

# 客户端请求
curl http://10.42.0.125:9527/api/user/profile \
  -H "X-Test-Group: beta"
```

## 3. 请求缓存管理 API

### 3.1 清除指定路径的缓存
```bash
curl -X DELETE "http://10.42.0.125:9527/gateway/cache/path?path=/api/user"
```

### 3.2 清除所有缓存
```bash
curl -X DELETE http://10.42.0.125:9527/gateway/cache/all
```

### 3.3 查询所有缓存键
```bash
curl http://10.42.0.125:9527/gateway/cache/keys
```

### 3.4 查询缓存统计信息
```bash
curl http://10.42.0.125:9527/gateway/cache/stats
```

### 3.5 缓存配置说明

在 `application-dev.yml` 中配置：
```yaml
gateway:
  cache:
    enabled: true
    ttl: 300  # 缓存过期时间（秒）
    paths: /api/user/**,/api/product/**  # 需要缓存的路径
```

缓存响应头：
- `X-Cache-Hit: true` - 表示命中缓存
- 未命中缓存时不会有此响应头

## 4. OAuth2 认证

### 4.1 配置 OAuth2

在 `application-dev.yml` 中配置：
```yaml
gateway:
  oauth2:
    enabled: true
    whitelist: /actuator/**,/gateway/**,/api/public/**,/api/auth/**

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://10.42.0.125:8080/auth/realms/kip
          jwk-set-uri: http://10.42.0.125:8080/auth/realms/kip/protocol/openid-connect/certs
```

### 4.2 使用 OAuth2 Token 访问

```bash
# 获取 Access Token（示例，具体取决于 OAuth2 服务器）
curl -X POST http://10.42.0.125:8080/auth/realms/kip/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=gateway-client" \
  -d "client_secret=secret" \
  -d "username=user" \
  -d "password=password"

# 使用 Access Token 访问受保护的资源
curl http://10.42.0.125:9527/api/user/profile \
  -H "Authorization: Bearer <access_token>"
```

### 4.3 OAuth2 认证流程

1. 客户端向 OAuth2 服务器请求 Access Token
2. 客户端携带 Access Token 访问网关
3. 网关验证 Token 的有效性
4. 验证通过后，网关将用户信息添加到请求头：
   - `X-User-Id`: 用户 ID
   - `X-User-Name`: 用户名
   - `X-User-Email`: 用户邮箱
5. 后端服务可以直接从请求头获取用户信息

## 5. 链路追踪

### 5.1 配置 Zipkin

在 `application-dev.yml` 中配置：
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 采样率（0.0-1.0）
  zipkin:
    tracing:
      endpoint: http://10.42.0.125:9411/api/v2/spans
```

### 5.2 查看链路追踪

1. 启动 Zipkin 服务器：
```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

2. 访问 Zipkin UI：
```
http://10.42.0.125:9411
```

3. 发送请求后，可以在 Zipkin UI 中查看完整的调用链路

### 5.3 自定义 Span 标签

网关自动添加以下标签：
- `gateway.path`: 请求路径
- `gateway.method`: 请求方法
- `gateway.remote-addr`: 客户端 IP
- `gateway.status`: 响应状态码
- `gateway.error`: 错误类型（如果有）
- `gateway.error.message`: 错误消息（如果有）

## 6. 配置优先级

所有功能的配置优先级：
1. Redis 动态配置（最高优先级）
2. Nacos 配置中心
3. application-{profile}.yml
4. application.yml

## 7. 完整配置示例

```yaml
spring:
  data:
    redis:
      host: 10.42.0.125
      port: 6379
      password:
      database: 0

gateway:
  # IP 黑白名单
  ip-filter:
    enabled: true
    mode: blacklist  # blacklist 或 whitelist
    static-blacklist: 192.168.1.100,10.0.0.0/8
    static-whitelist: 172.16.0.0/12

  # 请求缓存
  cache:
    enabled: true
    ttl: 300
    paths: /api/user/**,/api/product/**

  # 灰度发布
  gray:
    enabled: true

  # OAuth2
  oauth2:
    enabled: true
    whitelist: /actuator/**,/gateway/**,/api/public/**

  # 限流
  rate-limit:
    enabled: true
    default-qps: 100

# 链路追踪
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://10.42.0.125:9411/api/v2/spans
```

## 8. 监控指标

所有功能都会记录详细的日志，包含 TraceId，便于问题排查。

建议监控的指标：
- IP 黑名单拦截次数
- 灰度流量分布
- 缓存命中率
- OAuth2 认证失败次数
- 链路追踪采样率

## 9. 最佳实践

### 9.1 IP 黑白名单
- 使用 CIDR 格式管理 IP 段，减少配置数量
- 黑名单模式适合大部分场景
- 白名单模式适合高安全要求的场景

### 9.2 灰度发布
- 从小流量开始（5%-10%）
- 逐步增加流量比例
- 监控灰度版本的错误率和性能
- 确认稳定后再全量发布

### 9.3 请求缓存
- 只缓存 GET 请求
- 设置合理的过期时间
- 对于频繁变化的数据不建议缓存
- 及时清理失效缓存

### 9.4 OAuth2
- 使用 HTTPS 传输 Token
- 设置合理的 Token 过期时间
- 定期轮换客户端密钥
- 白名单路径不要过多

### 9.5 链路追踪
- 生产环境建议降低采样率（0.1-0.3）
- 定期清理 Zipkin 历史数据
- 关注慢请求和异常请求
