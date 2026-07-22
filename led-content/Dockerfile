# ============================================
# LED Cloud - 内容管理服务 (文件上传)
# 端口: 9003
# ============================================
ARG BASE_IMAGE=led-cloud-base:latest
FROM ${BASE_IMAGE}

COPY target/led-content-1.0.0.jar /app/app.jar

EXPOSE 9003

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=60s \
  CMD wget -q --spider http://localhost:9003/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
