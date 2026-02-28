# 多阶段构建
# 第一阶段：构建应用
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制pom文件
COPY pom.xml .
COPY gateway-common ./gateway-common
COPY gateway-web ./gateway-web

# 构建应用
RUN mvn clean package -DskipTests -pl gateway-web

# 第二阶段：运行应用
FROM eclipse-temurin:21-jre

WORKDIR /app

# 从第一阶段复制jar文件
COPY --from=builder /app/gateway-web/target/app.jar .

# 创建日志目录
RUN mkdir -p logs

# 暴露端口
EXPOSE 8888

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8888/gateway/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]

