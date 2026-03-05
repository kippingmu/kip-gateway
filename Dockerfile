# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven project files first for better layer caching
COPY pom.xml ./
COPY app/gateway-web/pom.xml gateway-web/
COPY app/common/gateway-common/pom.xml gateway-common/

# Pre-fetch dependencies (optional but speeds up iterative builds)
RUN mvn -q -DskipTests -pl gateway-web -am dependency:go-offline || true

# Copy sources and build
COPY app/gateway-web/src gateway-web/src
COPY app/common/gateway-common/src gateway-common/src

RUN mvn -q -DskipTests -pl gateway-web -am package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

COPY --from=builder /app/gateway-web/target/app.jar /app/app.jar

EXPOSE 8888

# If your environment restricts hostname lookup, you can set these at runtime:
#   -e SPRING_CLOUD_CLIENT_HOSTNAME=localhost -e SPRING_CLOUD_CLIENT_IP_ADDRESS=127.0.0.1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

