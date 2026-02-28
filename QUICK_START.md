# 🚀 Spring Cloud Gateway 快速启动指南

本文档提供快速启动和测试Spring Cloud Gateway的说明。

## 📋 前置条件

- JDK 21+
- Maven 3.9+
- Docker & Docker Compose（推荐用于快速启动依赖）
- curl 或 Postman（用于API测试）

## ⚡ 快速启动（本地开发）

### 步骤1：启动依赖服务

```bash
# 方式一：使用Docker Compose（推荐）
docker-compose up -d

# 验证所有服务已启动
docker ps
```

此命令会启动：
- **Nacos** (端口 8848) - 服务注册与发现、配置中心
- **Sentinel Dashboard** (端口 8080) - 流量监控
- **Redis** (端口 6379) - 可选缓存
- **Gateway** (端口 8888) - Spring Cloud Gateway本身

### 步骤2：启动网关应用（本地IDE运行）

```bash
# 方式一：IDE中运行 GatewayBootApplication
# 点击IDE中的Run按钮即可

# 方式二：使用Maven运行
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# 方式三：使用jar运行
java -jar gateway-web/target/app.jar --spring.profiles.active=dev
```

### 步骤3：验证网关启动成功

```bash
# 检查网关健康状态
curl http://localhost:8888/gateway/health

# 预期响应：
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "status": "UP",
#     "timestamp": 1709114400000,
#     "version": "1.0.0"
#   },
#   "timestamp": 1709114400000
# }
```

## 🧪 API 测试

### 1. 网关健康检查

```bash
curl http://localhost:8888/gateway/health
```

### 2. 获取所有路由

```bash
curl http://localhost:8888/gateway/routes
```

### 3. 添加新路由

```bash
curl -X POST http://localhost:8888/gateway/routes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-route",
    "uri": "http://httpbin.org",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/test/**"
        }
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {
          "parts": "1"
        }
      }
    ]
  }'
```

### 4. 获取网关统计信息

```bash
curl http://localhost:8888/gateway/stats
```

### 5. 删除路由

```bash
curl -X DELETE http://localhost:8888/gateway/routes/test-route
```

## 🔐 认证测试

### 测试认证过滤器

不提供Token时应该返回401错误：

```bash
# 应该返回401 Unauthorized
curl http://localhost:8888/api/user/profile

# 响应：
# {
#   "code": 401,
#   "message": "Missing authentication token"
# }
```

提供有效Token时：

```bash
# 使用有效的Bearer Token
curl -H "Authorization: Bearer your-jwt-token-here" \
  http://localhost:8888/api/user/profile
```

## 📊 监控和可观测性

### 1. 查看网关日志

```bash
# 查看实时日志（本地IDE运行时）
tail -f logs/gateway.log

# 查看特定TraceId的日志
grep "traceId=xxx" logs/gateway.log

# 查看错误日志
tail -f logs/gateway-error.log

# 查看访问日志
tail -f logs/gateway-access.log
```

### 2. Sentinel控制台

访问 http://localhost:8080 查看：
- 实时流量统计
- 限流、熔断规则
- 监控数据

### 3. Nacos控制台

访问 http://localhost:8848/nacos 
- 默认用户名: nacos
- 默认密码: nacos

查看：
- 已注册的服务
- 配置列表
- 命名空间和分组

### 4. Actuator端点

```bash
# 健康检查
curl http://localhost:8888/actuator/health

# 所有指标
curl http://localhost:8888/actuator/metrics

# 网关路由信息
curl http://localhost:8888/actuator/gateway/routes

# Prometheus格式指标
curl http://localhost:8888/actuator/prometheus
```

## 📝 常见操作

### 查看完整的请求链路

网关会自动为每个请求生成TraceId，用于完整的链路追踪：

```bash
# 请求时指定TraceId
curl -H "X-Trace-Id: my-trace-123" \
  http://localhost:8888/gateway/health

# 响应头中包含TraceId
# X-Trace-Id: my-trace-123

# 在日志中查找这个TraceId
grep "traceId=my-trace-123" logs/gateway.log
```

### 调整日志级别

编辑 `application.yml` 修改日志级别：

```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG  # 改为DEBUG查看详细日志
    xyz.kip: DEBUG
```

### 修改限流规则

编辑 `SentinelGatewayConfig.java` 中的规则：

```java
// 修改user-service的QPS限制为200
GatewayFlowRule rule1 = new GatewayFlowRule("user-service")
    .setCount(200)  // 改为200请求/秒
    .setIntervalSec(1);
```

## 🐛 故障排查

### 问题1：网关无法启动

**症状**：启动时出现连接异常

**解决方案**：
```bash
# 检查Nacos是否运行
docker ps | grep nacos

# 重启所有服务
docker-compose restart
```

### 问题2：请求返回503错误

**症状**：路由转发时返回503

**可能原因**：
- 后端服务未注册到Nacos
- 服务名称不匹配

**解决方案**：
```bash
# 检查Nacos中注册的服务
curl http://localhost:8848/nacos/v1/ns/service/list

# 查看网关日志
tail -f logs/gateway.log
```

### 问题3：限流返回429错误

**症状**：请求返回 "Too many requests"

这是正常的限流行为。

**解决方案**：
- 降低请求频率
- 或修改限流规则增加阈值

### 问题4：TraceId丢失

**症状**：日志中没有TraceId

**原因**：RequestLoggingFilter未被正确加载

**解决方案**：
1. 确保RequestLoggingFilter被扫描（在main包下）
2. 检查@Component注解是否存在
3. 查看启动日志确认过滤器已加载

## 🔧 开发建议

### 1. 添加自定义过滤器

创建新的过滤器类：

```java
@Component
@Slf4j
public class CustomFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 你的逻辑
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -95;  // 优先级（越小越先执行）
    }
}
```

### 2. 测试路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my-service
          uri: lb://my-service
          predicates:
            - Path=/api/my/**
          filters:
            - StripPrefix=2
```

### 3. 动态更新路由

```java
@Autowired
private DynamicRouteLoader dynamicRouteLoader;

public void updateRoutes() {
    dynamicRouteLoader.addRoute(new RouteDefinition());
    dynamicRouteLoader.removeRoute("route-id");
}
```

## 📚 更多资源

- [官方文档](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
- [Spring Cloud文档](https://spring.io/projects/spring-cloud)
- [Nacos使用文档](https://nacos.io/zh-cn/docs/quick-start.html)
- [Sentinel文档](https://github.com/alibaba/Sentinel/wiki)

## ✅ 检查清单

启动前验证：

- [ ] JDK 21已安装
- [ ] Maven已安装
- [ ] Docker & Docker Compose已安装
- [ ] 项目代码已克隆/解压
- [ ] 依赖已下载（mvn clean compile）

启动后验证：

- [ ] Nacos服务已启动（http://localhost:8848）
- [ ] Sentinel Dashboard已启动（http://localhost:8080）
- [ ] 网关已启动（http://localhost:8888）
- [ ] 健康检查通过
- [ ] 日志正常输出

## 📞 技术支持

如有问题，请检查：
1. 应用日志文件 (`logs/gateway.log`)
2. 网关启动输出
3. Docker容器日志
4. Nacos和Sentinel的状态

