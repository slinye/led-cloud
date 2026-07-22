# ============================================
# LED Cloud 后端基础镜像 (JDK8 Alpine)
# 所有 Java 微服务均继承此镜像
# 构建: docker build -t led-cloud-base:latest ./docker/java-base
# ============================================
FROM eclipse-temurin:8-jdk-alpine

# 时区 + 常用工具 + 字体 + libgcc/libstdc++ (Java AWT 字体库依赖)
RUN apk add --no-cache tzdata curl wget fontconfig ttf-dejavu libgcc libstdc++ \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata \
    && fc-cache -fv

WORKDIR /app

# JVM 参数 (容器运行时可通过 -e JAVA_OPTS=... 覆盖)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/ \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=Asia/Shanghai"

RUN mkdir -p /app/logs /app/uploads
