# Redis 连接问题解决方案

## 问题诊断

**错误信息：**
```
Unable to connect to Redis
Connection refused: /10.42.0.125:6379
```

**根本原因：**
Redis 服务正在运行，但配置为只监听 `127.0.0.1:6379`（本地回环地址），不接受来自其他机器的远程连接。

**验证：**
```bash
# Redis 服务状态
systemctl status redis-server
# 输出显示：redis-server 127.0.0.1:6379

# 端口检查
nc -zv 10.42.0.125 6379
# 输出：Connection refused
```

## 解决方案

### 方案 1：修改 Redis 配置允许远程连接（推荐用于开发环境）

#### 步骤 1：修改 Redis 配置文件

```bash
# 登录到 Redis 服务器
ssh xiaoshichuan@10.42.0.125

# 备份配置文件
sudo cp /etc/redis/redis.conf /etc/redis/redis.conf.backup

# 编辑配置文件
sudo vi /etc/redis/redis.conf
```

#### 步骤 2：修改 bind 配置

找到 `bind` 配置行，修改为：

**选项 A：允许所有 IP 连接（开发环境）**
```conf
# 注释掉原来的 bind 127.0.0.1
# bind 127.0.0.1

# 允许所有 IP 连接
bind 0.0.0.0
```

**选项 B：只允许特定 IP 连接（更安全）**
```conf
# 允许本地和内网 IP 连接
bind 127.0.0.1 10.42.0.125
```

#### 步骤 3：修改 protected-mode（如果需要）

找到 `protected-mode` 配置：

```conf
# 如果没有设置密码，需要关闭保护模式
protected-mode no
```

或者设置密码（推荐）：

```conf
# 保持保护模式开启
protected-mode yes

# 设置密码
requirepass your_strong_password_here
```

#### 步骤 4：重启 Redis 服务

```bash
sudo systemctl restart redis-server

# 验证 Redis 监听地址
sudo ss -tlnp | grep 6379
# 应该看到 0.0.0.0:6379 或 10.42.0.125:6379
```

#### 步骤 5：测试连接

```bash
# 从网关服务器测试
nc -zv 10.42.0.125 6379
# 应该输出：Connection to 10.42.0.125 port 6379 [tcp/*] succeeded!

# 或使用 redis-cli 测试
redis-cli -h 10.42.0.125 -p 6379 ping
# 应该输出：PONG
```

#### 步骤 6：更新网关配置（如果设置了密码）

如果设置了 Redis 密码，需要更新 `application-dev.yml`：

```yaml
spring:
  data:
    redis:
      host: 10.42.0.125
      port: 6379
      password: your_strong_password_here  # 取消注释并填写密码
      database: 0
```

### 方案 2：使用 Docker 启动 Redis（推荐用于快速测试）

如果不想修改现有 Redis 配置，可以用 Docker 启动一个新的 Redis 实例：

```bash
# 登录到 Redis 服务器
ssh xiaoshichuan@10.42.0.125

# 启动 Redis（监听所有接口，使用不同端口避免冲突）
docker run -d \
  --name redis-gateway \
  -p 6380:6379 \
  redis:latest \
  redis-server --bind 0.0.0.0 --protected-mode no

# 验证
docker ps | grep redis-gateway
```

然后更新网关配置：

```yaml
spring:
  data:
    redis:
      host: 10.42.0.125
      port: 6380  # 使用新端口
```

### 方案 3：使用本地 Redis（最简单）

在网关服务器本地启动 Redis：

```bash
# 使用 Docker 在本地启动
docker run -d \
  --name redis-local \
  -p 6379:6379 \
  redis:latest

# 或使用 Homebrew 安装（macOS）
brew install redis
brew services start redis
```

然后更新网关配置：

```yaml
spring:
  data:
    redis:
      host: localhost  # 或 127.0.0.1
      port: 6379
```

### 方案 4：临时禁用 Redis 相关功能

如果暂时不需要 Redis 功能，可以禁用：

在 `application-dev.yml` 中添加：

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration

gateway:
  rate-limit:
    enabled: false  # 禁用限流
  ip-filter:
    enabled: false  # 禁用 IP 过滤
  cache:
    enabled: false  # 禁用缓存
  gray:
    enabled: false  # 禁用灰度发布
```

## 安全建议

### 生产环境配置

1. **使用强密码**
   ```conf
   requirepass $(openssl rand -base64 32)
   ```

2. **限制访问 IP**
   ```conf
   bind 127.0.0.1 10.42.0.0/24
   ```

3. **启用保护模式**
   ```conf
   protected-mode yes
   ```

4. **配置防火墙**
   ```bash
   # 只允许特定 IP 访问 Redis 端口
   sudo ufw allow from 10.42.0.0/24 to any port 6379
   ```

5. **禁用危险命令**
   ```conf
   rename-command FLUSHDB ""
   rename-command FLUSHALL ""
   rename-command CONFIG ""
   ```

### 开发环境配置

开发环境可以简化配置：

```conf
bind 0.0.0.0
protected-mode no
# 或者设置简单密码
# requirepass dev123456
```

## 验证清单

- [ ] Redis 服务正在运行
- [ ] Redis 监听正确的地址（0.0.0.0 或特定 IP）
- [ ] 防火墙允许 6379 端口
- [ ] 网关配置中的 Redis 地址和端口正确
- [ ] 如果设置了密码，网关配置中也配置了密码
- [ ] 可以从网关服务器连接到 Redis

## 快速验证命令

```bash
# 1. 检查 Redis 监听地址
ssh xiaoshichuan@10.42.0.125 "sudo ss -tlnp | grep 6379"

# 2. 测试端口连通性
nc -zv 10.42.0.125 6379

# 3. 测试 Redis 连接
redis-cli -h 10.42.0.125 -p 6379 ping

# 4. 如果设置了密码
redis-cli -h 10.42.0.125 -p 6379 -a your_password ping
```

## 推荐方案

**对于当前情况，推荐使用方案 1：**

1. 修改 Redis 配置允许远程连接
2. 设置密码保护
3. 更新网关配置

这样既能正常使用 Redis 功能，又保证了一定的安全性。

## 下一步

选择一个方案后：

1. 执行相应的配置修改
2. 重启 Redis 服务
3. 验证连接
4. 重启网关服务
5. 测试功能

如有问题，请查看 Redis 日志：
```bash
sudo journalctl -u redis-server -f
```
