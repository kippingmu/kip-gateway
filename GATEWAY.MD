# Gateway 功能需求文档

## 一、核心功能

### 1. 路由转发
- [x] 基于路径的路由匹配（Path Predicate）
- [x] 负载均衡路由（lb://service-name）
- [x] 服务发现集成（Nacos Discovery）
- [ ] 基于请求头的路由（Header Predicate）
- [ ] 基于请求参数的路由（Query Predicate）
- [ ] 基于时间的路由（After/Before/Between Predicate）
- [ ] 路由权重配置（灰度发布）
- [ ] 动态路由管理（通过 Nacos 配置中心）

### 2. 请求过滤

#### 2.1 全局过滤器
- [x] 请求日志记录（RequestLoggingFilter）
  - 记录请求方法、路径、客户端 IP
  - 记录响应状态码、耗时
  - 生成并传递 TraceId
- [x] 身份认证（AuthenticationFilter）
  - JWT Token 验证
  - 白名单路径配置
  - 认证失败统一响应
- [x] 权限校验（AuthorizationFilter）
  - 基于角色的权限控制（RBAC）
  - 路径权限配置
  - 权限不足统一响应
- [x] 请求限流（RateLimitFilter）
  - 基于 IP 的限流
  - 令牌桶算法实现
  - 可配置 QPS
- [x] 跨域处理（CorsConfig）
- [ ] 请求/响应体修改
- [ ] 请求重试（RetryFilter）

#### 2.2 路由级过滤器
- [x] 路径前缀去除（StripPrefix）
- [ ] 请求头添加/修改（AddRequestHeader）
- [ ] 响应头添加/修改（AddResponseHeader）
- [ ] 请求参数添加（AddRequestParameter）
- [ ] 重定向（RedirectTo）
- [ ] 熔断降级（CircuitBreaker）

### 3. 流量控制与保护

#### 3.1 Sentinel 集成
- [x] Sentinel Dashboard 集成
- [x] 流控规则配置（从 Nacos 加载）
- [x] 降级规则配置（从 Nacos 加载）
- [x] API 分组管理
- [x] 自定义限流异常处理
- [ ] 热点参数限流
- [ ] 系统自适应限流
- [ ] 授权规则配置

#### 3.2 熔断降级
- [x] Sentinel 熔断降级处理器
- [x] 降级响应定制
- [ ] 服务熔断配置
- [ ] 降级策略配置
- [ ] 熔断恢复机制

### 4. 安全防护

#### 4.1 认证授权
- [x] JWT Token 认证
- [x] OAuth2 集成
- [ ] API Key 认证
- [ ] 多租户隔离
- [ ] Token 刷新机制
- [ ] 黑名单/白名单管理

#### 4.2 安全防护
- [ ] SQL 注入防护
- [ ] XSS 攻击防护
- [ ] CSRF 防护
- [x] IP 黑白名单
- [ ] 请求签名验证
- [ ] 敏感数据脱敏

### 5. 监控与可观测性

#### 5.1 日志
- [x] 访问日志（Access Log）
- [x] 错误日志（Error Log）
- [x] TraceId 链路追踪
- [ ] 慢请求日志
- [ ] 审计日志

#### 5.2 指标监控
- [x] Prometheus 指标导出
- [x] Actuator 健康检查
- [ ] 请求 QPS 统计
- [ ] 响应时间统计
- [ ] 错误率统计
- [ ] 服务可用性监控

#### 5.3 链路追踪
- [x] TraceId 生成与传递
- [x] Micrometer Tracing 集成
- [x] Zipkin 集成
- [x] 分布式链路追踪

### 6. 管理功能

#### 6.1 路由管理
- [x] 健康检查接口
- [x] 路由列表查询
- [x] 路由详情查询
- [x] 动态添加路由
- [x] 动态修改路由
- [x] 动态删除路由
- [x] 路由刷新

#### 6.2 配置管理
- [x] Nacos 配置中心集成
- [x] 配置热更新
- [ ] 配置版本管理
- [ ] 配置回滚

#### 6.3 运维管理
- [ ] 优雅停机
- [ ] 流量录制与回放
- [x] 灰度发布支持
- [ ] A/B 测试支持

### 7. 性能优化

- [ ] 连接池优化
- [x] 请求缓存
- [ ] 响应压缩
- [ ] 静态资源缓存
- [ ] 预热机制
- [ ] 请求合并（Request Collapsing）

### 8. 异常处理

- [x] 全局异常处理（GlobalExceptionHandler）
- [x] 统一错误响应格式
- [ ] 超时处理
- [ ] 服务不可用处理
- [ ] 自定义错误页面

## 二、技术栈

### 核心框架
- Spring Cloud Gateway 3.x
- Spring Boot 3.x
- Spring Cloud Alibaba

### 服务治理
- Nacos（服务注册与配置中心）
- Sentinel（流量控制与熔断降级）

### 监控与追踪
- Prometheus（指标监控）
- Spring Boot Actuator（健康检查）
- Logback（日志）

### 安全
- JWT（身份认证）
- Spring Security（可选）

## 三、部署架构

### 开发环境
- 单实例部署
- 本地 Nacos
- 本地 Sentinel Dashboard

### 生产环境
- 多实例部署（高可用）
- Nacos 集群
- Sentinel Dashboard 集群
- Kubernetes 部署
- 负载均衡（Nginx/SLB）

## 四、配置管理

### 配置文件结构
```
resources/
├── application.yml              # 通用配置
├── application-dev.yml          # 开发环境配置
├── application-prod.yml         # 生产环境配置
├── application-local.yml        # 本地配置（不提交）
├── nacos-todo.yml              # Nacos 配置模板
└── logback-spring.xml          # 日志配置
```

### 配置优先级
1. Nacos 配置中心（最高优先级，支持热更新）
2. application-local.yml（本地开发）
3. application-{profile}.yml（环境配置）
4. application.yml（基础配置）

## 五、开发规范

### 过滤器开发规范
1. 实现 `GlobalFilter` 和 `Ordered` 接口
2. 使用 `@Component` 注解注册为 Bean
3. 通过 `getOrder()` 方法设置执行顺序（数值越小越先执行）
4. 使用 TraceId 记录日志
5. 异常统一处理，返回标准错误格式

### 过滤器执行顺序
```
-100: RequestLoggingFilter（日志记录）
 -99: AuthenticationFilter（身份认证）
 -98: AuthorizationFilter（权限校验）
 -50: RateLimitFilter（限流）
 -10: CorsFilter（跨域处理）
   0: 默认过滤器
```

### 日志规范
- 使用 SLF4J + Logback
- 日志级别：ERROR > WARN > INFO > DEBUG
- 必须包含 TraceId
- 敏感信息脱敏

### 错误响应格式
```json
{
  "code": 401,
  "message": "认证失败",
  "traceId": "1234567890abcdef",
  "timestamp": 1709712000000
}
```

## 六、待实现功能优先级

### P0（高优先级）
1. ~~请求限流（RateLimitFilter）~~ ✅ 已完成
2. ~~跨域处理（CorsFilter）~~ ✅ 已完成
3. ~~路由管理接口（查询、刷新）~~ ✅ 已完成
4. ~~配置热更新~~ ✅ 已完成
5. ~~熔断降级~~ ✅ 已完成

### P1（中优先级）
1. ~~OAuth2 集成~~ ✅ 已完成
2. ~~链路追踪（Sleuth + Zipkin）~~ ✅ 已完成（使用 Micrometer Tracing）
3. ~~请求缓存~~ ✅ 已完成
4. ~~灰度发布支持~~ ✅ 已完成
5. ~~IP 黑白名单~~ ✅ 已完成

### P2（低优先级）
1. 流量录制与回放
2. A/B 测试
3. 请求合并
4. 静态资源缓存
5. 自定义错误页面

## 七、性能指标

### 目标指标
- 平均响应时间：< 50ms
- P99 响应时间：< 200ms
- QPS：> 10000
- 可用性：99.99%
- 错误率：< 0.01%

### 容量规划
- 单实例 QPS：2000-3000
- 生产环境实例数：≥ 3
- 最大并发连接数：10000

## 八、运维监控

### 监控指标
- 请求总数
- 请求成功率
- 平均响应时间
- P95/P99 响应时间
- 错误率
- 限流次数
- 熔断次数

### 告警规则
- 错误率 > 1%
- P99 响应时间 > 500ms
- 可用性 < 99.9%
- 限流次数异常增长
- 服务实例下线