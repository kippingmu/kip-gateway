# Gateway 模块结构说明

## 目录结构

```
xyz.kip.gateway/
├── config/                 # 配置类
│   ├── CorsConfig.java                    # 跨域配置
│   ├── GatewayGlobalConfig.java           # 网关全局配置
│   ├── SentinelGatewayConfig.java         # Sentinel 限流熔断配置
│   └── NacosConfigRefreshListener.java    # Nacos 配置热更新监听器
│
├── controller/             # 控制器层
│   └── GatewayManagementController.java   # 网关管理接口（路由管理、健康检查）
│
├── service/                # 服务层
│   └── RouteManagementService.java        # 路由管理服务
│
├── filter/                 # 过滤器层
│   ├── RequestLoggingFilter.java          # 请求日志过滤器（Order: -100）
│   ├── AuthenticationFilter.java          # 身份认证过滤器（Order: -99）
│   ├── AuthorizationFilter.java           # 权限校验过滤器（Order: -98）
│   └── RateLimitFilter.java               # 限流过滤器（Order: -50）
│
├── handler/                # 处理器层
│   ├── GlobalExceptionHandler.java        # 全局异常处理器
│   └── SentinelFallbackHandler.java       # Sentinel 熔断降级处理器
│
├── model/                  # 数据模型层
│   └── RouteDefinitionVO.java             # 路由定义视图对象
│
├── exception/              # 异常定义
│   └── GatewayException.java              # 网关自定义异常
│
└── util/                   # 工具类
    └── TraceIdUtil.java                   # TraceId 工具类
```

## 模块职责

### 1. Config 配置层
负责各种配置的初始化和管理：
- **CorsConfig**: 跨域资源共享配置
- **SentinelGatewayConfig**: Sentinel 限流规则、API 分组、熔断降级配置
- **NacosConfigRefreshListener**: 监听 Nacos 配置变化，自动刷新路由

### 2. Controller 控制层
提供 RESTful API 接口：
- **GatewayManagementController**:
  - GET /gateway/health - 健康检查
  - GET /gateway/routes - 查询所有路由
  - POST /gateway/routes - 添加路由
  - PUT /gateway/routes/{id} - 更新路由
  - DELETE /gateway/routes/{id} - 删除路由
  - POST /gateway/routes/refresh - 刷新路由

### 3. Service 服务层
业务逻辑处理：
- **RouteManagementService**: 路由的增删改查、刷新等操作

### 4. Filter 过滤器层
请求处理链，按 Order 值从小到大执行：

| Order | 过滤器 | 功能 |
|-------|--------|------|
| -100 | RequestLoggingFilter | 记录请求日志、生成 TraceId |
| -99 | AuthenticationFilter | JWT Token 认证 |
| -98 | AuthorizationFilter | 权限校验 |
| -50 | RateLimitFilter | 基于 IP 的限流 |

### 5. Handler 处理器层
异常和降级处理：
- **GlobalExceptionHandler**: 全局异常统一处理
- **SentinelFallbackHandler**: Sentinel 限流/熔断时的降级响应

### 6. Model 模型层
数据传输对象：
- **RouteDefinitionVO**: 路由定义的视图对象，用于 API 响应

### 7. Exception 异常层
自定义异常：
- **GatewayException**: 网关业务异常

### 8. Util 工具层
通用工具类：
- **TraceIdUtil**: TraceId 生成和管理（基于 ThreadLocal）

## 功能实现清单

### ✅ 已实现功能

1. **路由转发**
   - 基于路径的路由匹配
   - 负载均衡路由（lb://）
   - 服务发现集成（Nacos）

2. **请求过滤**
   - 请求日志记录（含 TraceId）
   - JWT 身份认证
   - 权限校验
   - 请求限流（基于 IP + 令牌桶算法）

3. **流量控制**
   - Sentinel 集成
   - 流控规则配置
   - 降级规则配置
   - API 分组管理
   - 自定义限流响应

4. **安全防护**
   - JWT Token 认证
   - 白名单路径配置
   - 跨域处理（CORS）

5. **监控与可观测性**
   - 访问日志
   - TraceId 链路追踪
   - Prometheus 指标导出
   - Actuator 健康检查

6. **管理功能**
   - 路由查询接口
   - 路由动态管理（增删改）
   - 路由刷新
   - Nacos 配置热更新

7. **异常处理**
   - 全局异常处理
   - 统一错误响应格式
   - Sentinel 熔断降级处理

## 配置说明

### 限流配置
```yaml
gateway:
  rate-limit:
    enabled: true        # 是否启用限流
    default-qps: 100     # 默认 QPS 限制
```

### Nacos 配置热更新
```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: true
        refresh-enabled: true  # 启用配置自动刷新
```

## API 使用示例

### 1. 查询所有路由
```bash
curl http://10.42.0.125:9527/gateway/routes
```

### 2. 添加路由
```bash
curl -X POST http://10.42.0.125:9527/gateway/routes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "user-service",
    "uri": "lb://kip-user-service",
    "predicates": [
      {
        "name": "Path",
        "args": {"pattern": "/api/user/**"}
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {"parts": "2"}
      }
    ],
    "order": 0
  }'
```

### 3. 刷新路由
```bash
curl -X POST http://10.42.0.125:9527/gateway/routes/refresh
```

## 扩展点

### 1. 自定义过滤器
实现 `GlobalFilter` 和 `Ordered` 接口：
```java
@Component
public class CustomFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 自定义逻辑
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -10; // 设置执行顺序
    }
}
```

### 2. 自定义限流策略
扩展 `RateLimitFilter`，支持：
- 基于用户的限流
- 基于路径的限流
- 基于请求参数的限流

### 3. 自定义认证方式
扩展 `AuthenticationFilter`，支持：
- OAuth2 认证
- API Key 认证
- 多租户认证

## 性能优化建议

1. **限流器缓存**: 使用 Guava LoadingCache 缓存限流器实例
2. **异步处理**: 使用 Reactor 响应式编程，避免阻塞
3. **连接池优化**: 配置合理的连接池大小
4. **日志异步**: 使用异步日志框架（Logback AsyncAppender）

## 监控指标

建议监控以下指标：
- 请求 QPS
- 平均响应时间
- P95/P99 响应时间
- 错误率
- 限流次数
- 熔断次数
- 服务可用性
