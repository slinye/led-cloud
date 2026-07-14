# ============================================
# LED Cloud 后端基础镜像 (JDK8 + 常用工具)
# 所有 Java 微服务 Dockerfile 都以该镜像为基础
# ============================================
FROM eclipse-temurin:8-jdk-alpine

# 时区设置
RUN apk add --no-cache tzdata curl wget \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# JVM 优化参数（可通过环境变量覆盖）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

WORKDIR /app
