# 配置文件说明

## 配置文件结构

### 1. application.yml（通用配置）
包含所有环境共享的基础配置：
- 应用名称
- Jackson 序列化配置
- Actuator 管理端点配置
- Gateway 服务发现配置

### 2. application-dev.yml（开发环境）
开发环境特定配置：
- Nacos 服务地址：`10.42.0.125:8848`
- Sentinel Dashboard：`10.42.0.125:8080`
- 服务端口：`8888`
- 日志级别：DEBUG（便于开发调试）

### 3. application-prod.yml（生产环境）
生产环境特定配置：
- Nacos 集群地址（支持环境变量）
- Sentinel Dashboard 地址（支持环境变量）
- 服务端口（支持环境变量）
- 日志级别：INFO（生产环境）
- 使用独立的 namespace 隔离

### 4. nacos-todo.yml（Nacos 配置中心）
需要上传到 Nacos 配置中心的动态配置：
- Gateway 路由规则（支持热更新）
- Sentinel 规则数据源配置
- 业务动态配置

## 使用方式

### 本地开发
```bash
# 使用开发环境配置
java -jar gateway.jar --spring.profiles.active=dev
```

### 生产部署
```bash
# 使用生产环境配置，并通过环境变量注入关键参数
java -jar gateway.jar \
  --spring.profiles.active=prod \
  -DNACOS_SERVER_ADDR=nacos-cluster.prod.svc:8848 \
  -DNACOS_NAMESPACE=prod \
  -DSENTINEL_DASHBOARD=sentinel-dashboard.prod.svc:8080 \
  -DSERVER_PORT=8888
```

### Nacos 配置中心设置

1. 登录 Nacos 控制台
2. 进入"配置管理" -> "配置列表"
3. 创建配置：
   - **Data ID**: `kip-gateway.yml` 或 `kip-gateway-dev.yml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: `YAML`
   - **配置内容**: 复制 `nacos-todo.yml` 的内容

4. 确保 `application-{profile}.yml` 中的 `spring.cloud.nacos.config.enabled=true`

## 配置优先级

Spring Boot 配置加载优先级（从高到低）：
1. Nacos 配置中心（动态配置，支持热更新）
2. application-{profile}.yml（环境特定配置）
3. application.yml（通用配置）

## 配置分离原则

- **通用配置** → application.yml
- **环境差异** → application-{profile}.yml
- **动态配置** → Nacos 配置中心（nacos-todo.yml）

## 注意事项

1. 生产环境建议使用环境变量注入敏感配置
2. Nacos 配置中心的配置会覆盖本地配置
3. 路由规则建议在 Nacos 中管理，便于动态调整
4. 生产环境使用独立的 namespace 隔离不同环境的配置
5. 服务 IP 和主机名由 Nacos 客户端自动检测上报，无需手动配置（除非在特殊网络环境如 Docker/K8s 中自动检测失败）
