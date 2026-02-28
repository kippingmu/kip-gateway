# 🏗️ Spring Cloud Gateway 架构设计文档

## 系统架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Requests                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Cloud Gateway                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Global Filter Chain (Request → Response)           │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ 1. RequestLoggingFilter (优先级 -100)              │   │
│  │    └─ 记录请求日志、生成TraceId                    │   │
│  │                                                     │   │
│  │ 2. AuthenticationFilter (优先级 -99)              │   │
│  │    └─ 验证Token、JWT                              │   │
│  │                                                     │   │
│  │ 3. AuthorizationFilter (优先级 -98)               │   │
│  │    └─ 检查权限、RBAC                               │   │
│  │                                                     │   │
│  │ 4. SentinelGatewayFilter (优先级 HIGHEST)        │   │
│  │    └─ 限流、熔断、降级                              │   │
│  │                                                     │   │
│  │ 5. Gateway Route Filter (内置)                    │   │
│  │    └─ 路由转发、负载均衡                            │   │
│  │                                                     │   │
│  │ 6. GlobalExceptionHandler (异常处理)             │   │
│  │    └─ 统一异常处理、错误响应                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Route Definition                                   │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ - From application.yml (Static Routes)             │   │
│  │ - From DynamicRouteLoader (Dynamic Routes)         │   │
│  │ - From Nacos Config (External Routes)              │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────┬────────────────────┬──────────────┬────────────┘
             │                    │              │
             ▼                    ▼              ▼
    ┌────────────────┐  ┌──────────────┐  ┌──────────────┐
    │ Backend        │  │ Backend      │  │ Backend      │
    │ Service 1      │  │ Service 2    │  │ Service N    │
    │ (Load-         │  │              │  │              │
    │  Balanced)     │  │              │  │              │
    └────────────────┘  └──────────────┘  └──────────────┘
```

## 完整的请求处理流程

```
1. 客户端请求
   ↓
2. 请求进入GatewayBootApplication
   ↓
3. 过滤器链执行（按优先级）
   │
   ├─→ RequestLoggingFilter
   │    ├─ 提取或生成TraceId
   │    ├─ 记录请求信息（方法、路径、客户端IP）
   │    └─ 添加TraceId到响应头
   │
   ├─→ AuthenticationFilter
   │    ├─ 检查Authorization头
   │    ├─ 验证Token格式
   │    ├─ 调用validateToken()验证
   │    ├─ 如果失败返回401 Unauthorized
   │    └─ 如果成功添加userId到请求属性
   │
   ├─→ AuthorizationFilter
   │    ├─ 检查权限（hasPermission）
   │    ├─ 检查是否在白名单中
   │    ├─ 如果失败返回403 Forbidden
   │    └─ 如果成功继续
   │
   ├─→ SentinelGatewayFilter (Sentinel限流)
   │    ├─ 获取限流规则
   │    ├─ 执行限流检查
   │    ├─ 如果限流返回429 Too Many Requests
   │    └─ 如果通过继续处理
   │
   └─→ Route Filter (Spring Cloud Gateway内置)
        ├─ 路由匹配（predicates）
        ├─ 应用路由过滤器（filters）
        ├─ 负载均衡器选择后端实例
        ├─ 转发请求到后端服务
        └─ 获取响应
   
4. 响应处理（反向过滤器链）
   │
   ├─ 获取响应状态码
   ├─ 记录响应日志
   ├─ 计算处理时间
   ├─ 访问日志记录
   └─ 返回到客户端

5. 异常处理（任何步骤发生异常）
   │
   └─→ GlobalExceptionHandler
        ├─ 捕获异常
        ├─ 转换为ApiResponse格式
        ├─ 返回JSON错误响应
        └─ 记录错误日志
```

## 模块化设计

### 1. 过滤器模块 (`filter/`)

```
RequestLoggingFilter
├─ 职责: 日志记录、TraceId生成和传递
├─ 优先级: -100（最先执行）
├─ 依赖: TraceIdUtil、Logback
└─ 输出: 日志文件、TraceId

AuthenticationFilter
├─ 职责: 身份认证、Token验证
├─ 优先级: -99
├─ 依赖: HttpHeaders、TraceIdUtil
└─ 输出: userId（成功）/ 401响应（失败）

AuthorizationFilter
├─ 职责: 权限检查、RBAC
├─ 优先级: -98
├─ 依赖: TraceIdUtil、配置规则
└─ 输出: 继续处理（成功）/ 403响应（失败）
```

### 2. 配置模块 (`config/`)

```
GatewayCorsConfig
├─ 职责: CORS跨域配置
├─ 功能: 允许的来源、方法、头部
└─ 输出: CorsConfigurationSource Bean

DynamicRouteLoader
├─ 职责: 动态路由管理
├─ 功能: 加载、添加、删除路由
├─ 实现: RouteDefinitionRepository接口
└─ 输出: 动态路由列表

SentinelGatewayConfig
├─ 职责: Sentinel限流配置
├─ 功能: 初始化限流规则
├─ 依赖: Sentinel、GatewayFlowRule
└─ 输出: 限流规则
```

### 3. 控制器模块 (`controller/`)

```
GatewayManagementController
├─ /gateway/health (GET)      → 健康检查
├─ /gateway/routes (GET)      → 获取所有路由
├─ /gateway/routes/{id} (GET) → 获取指定路由
├─ /gateway/routes (POST)     → 添加新路由
├─ /gateway/routes/{id} (DELETE) → 删除路由
└─ /gateway/stats (GET)       → 统计信息
```

### 4. 数据流转模块 (`dto/`)

```
ApiResponse<T>
├─ code: Integer (响应码)
├─ message: String (响应消息)
├─ data: T (响应数据)
├─ timestamp: Long (时间戳)
└─ traceId: String (链路ID)
```

### 5. 异常处理模块 (`handler/` + `exception/`)

```
GatewayException
├─ code: Integer
├─ message: String
└─ cause: Throwable

GlobalExceptionHandler (ErrorWebExceptionHandler)
├─ 捕获所有异常
├─ 转换为JSON格式
└─ 返回统一的错误响应
```

## 数据流向

### 请求数据流

```
HTTP Request
    ↓
RequestLoggingFilter (添加TraceId)
    ↓
AuthenticationFilter (验证Token)
    ↓
AuthorizationFilter (检查权限)
    ↓
SentinelGatewayFilter (限流检查)
    ↓
Route Filter (路由转发)
    ↓
LoadBalancer (负载均衡)
    ↓
Backend Service
```

### 响应数据流

```
Backend Response
    ↓
Route Filter (处理响应)
    ↓
RequestLoggingFilter (记录响应日志)
    ↓
ClientResponse with TraceId Header
```

### 异常数据流

```
任何异常（在过滤器链任何位置）
    ↓
GlobalExceptionHandler
    ↓
JSON Error Response with TraceId
```

## 配置管理流程

```
┌──────────────────┐
│  application.yml │  ← Spring Boot主配置
└────────┬─────────┘
         ↓
┌──────────────────────────┐
│  application-dev.yml     │  ← 开发环境
│  application-prod.yml    │  ← 生产环境
└────────┬─────────────────┘
         ↓
┌──────────────────────────┐
│  Nacos Config Server     │  ← 远程配置
│  - gateway-routes.yml    │
│  - sentinel-rules        │
└────────┬─────────────────┘
         ↓
┌──────────────────────────┐
│  Spring Cloud Gateway    │
│  - 路由加载              │
│  - 规则更新              │
│  - 配置刷新              │
└──────────────────────────┘
```

## 限流降级工作流

```
Request Arrives
    ↓
SentinelGatewayFilter
    ├─ Get Flow Rule for Route
    ├─ Check Current QPS
    │
    ├─ If QPS < Limit
    │  └─ PASS → Continue
    │
    └─ If QPS >= Limit
       ├─ Check Circuit Breaker
       │
       ├─ If Closed (Normal)
       │  └─ BLOCK → Return 429
       │
       ├─ If Open (Circuit Break)
       │  └─ Return Fallback Response
       │
       └─ If Half-Open (Recovery)
           ├─ Try Request
           └─ Update State
```

## 服务发现与负载均衡

```
┌──────────────────────┐
│   Nacos Registry     │
│                      │
│  Services:           │
│  - service-1: [...]  │
│  - service-2: [...]  │
│  - service-n: [...]  │
└─────────┬────────────┘
          │
          ↓
┌──────────────────────────────┐
│  LoadBalancer                │
│  (Spring Cloud LoadBalancer) │
│                              │
│  Strategies:                 │
│  - Round Robin (轮询)        │
│  - Random (随机)             │
│  - Least Connections (最少)  │
│  - IP Hash (哈希)            │
└─────────┬────────────────────┘
          │
          ↓
┌──────────────────────────┐
│  Backend Service         │
│  (Selected Instance)     │
└──────────────────────────┘
```

## 日志架构

```
Logback Configuration (logback-spring.xml)
│
├─ Console Appender
│  └─ Color Output for Development
│
├─ RollingFile Appender (gateway.log)
│  ├─ Size: 100MB per file
│  ├─ Retention: 30 days
│  └─ Compression: gzip
│
├─ Error File Appender (gateway-error.log)
│  ├─ Filter: ERROR level only
│  ├─ Size: 100MB per file
│  └─ Retention: 30 days
│
├─ Access File Appender (gateway-access.log)
│  ├─ Format: Simple log format
│  ├─ Size: 100MB per file
│  └─ Retention: 30 days
│
└─ Async Appender
   ├─ Queue Size: 512
   └─ Improves Performance
```

## 链路追踪设计

```
Request with TraceId
│
├─ Generate TraceId (if not present)
│  └─ Format: UUID (32 chars)
│
├─ Store in ThreadLocal
│  └─ TraceIdUtil.setTraceId(traceId)
│
├─ Pass to Response Header
│  └─ X-Trace-Id: {traceId}
│
├─ Log in All Filters
│  └─ "traceId={traceId}, ..."
│
├─ Forward to Backend (if needed)
│  └─ X-Trace-Id header
│
└─ Clear on Complete
   └─ TraceIdUtil.clear()

All related logs can be searched by TraceId
```

## 错误处理架构

```
Exceptions
│
├─ GatewayException (Custom)
│  ├─ code: Integer
│  └─ message: String
│
├─ ResponseStatusException (Spring)
│  ├─ status: HttpStatus
│  └─ reason: String
│
└─ Other Exceptions
   └─ Default: 500 Internal Server Error

All → GlobalExceptionHandler
│
├─ Convert to ApiResponse
├─ Add TraceId
├─ Set HTTP Status Code
└─ Return JSON Response
```

## 扩展点设计

### 1. 添加新过滤器

```java
@Component
@Slf4j
public class CustomFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Before processing
        return chain.filter(exchange).doFinally(signalType -> {
            // After processing
        });
    }
    
    @Override
    public int getOrder() {
        return -97;  // 在AuthZ之后，Sentinel之前
    }
}
```

优先级参考：
- RequestLogging: -100
- Authentication: -99
- Authorization: -98
- Custom: -97 to 0
- Sentinel: HIGHEST_PRECEDENCE

### 2. 自定义限流规则

编辑 `SentinelGatewayConfig.java`:

```java
GatewayFlowRule rule = new GatewayFlowRule("route-id")
    .setCount(100)           // QPS上限
    .setIntervalSec(1)       // 时间窗口（秒）
    .setControlBehavior(0);  // 0=reject, 1=warm up, 2=queue
```

### 3. 动态路由管理

```java
@Autowired
private DynamicRouteLoader routeLoader;

// 添加路由
routeLoader.addRoute(new RouteDefinition());

// 删除路由
routeLoader.removeRoute("route-id");

// 更新路由
routeLoader.updateRoutes(List.of(...));
```

## 性能考虑

### 1. 异步处理

- 日志异步写入（AsyncAppender）
- 响应式编程（Reactive/Mono）
- 非阻塞IO

### 2. 缓存策略

- Nacos服务发现缓存
- 配置缓存
- 可扩展: Redis缓存

### 3. 连接管理

- HTTP连接复用
- 服务发现连接池
- 负载均衡缓存

### 4. 监控指标

- RequestLoggingFilter计时
- Sentinel实时监控
- Micrometer指标
- Prometheus导出

## 安全设计

### 1. 认证层

- Bearer Token验证
- JWT支持（可扩展）
- 白名单机制

### 2. 授权层

- RBAC支持
- 路径级权限
- 动态权限加载

### 3. 数据保护

- HTTPS支持（可配置）
- TraceId审计
- 日志脱敏（可扩展）

### 4. 限流保护

- DDoS防护（Sentinel）
- 服务隔离
- 自动降级

## 测试覆盖

### 单元测试

- 过滤器测试
- 工具类测试
- 配置类测试

### 集成测试

- 完整请求链路
- 多路由转发
- 异常处理验证

### 性能测试

- 限流验证
- 响应延迟测试
- 吞吐量测试

---

## 总结

这个网关架构设计遵循以下原则：

1. **分层设计** - 清晰的职责划分
2. **高扩展性** - 易于添加新功能
3. **高可靠性** - 异常处理完善
4. **高可观测性** - 完整的日志和追踪
5. **生产就绪** - 包含所有生产需要的功能

