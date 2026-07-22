# ============================================
# LED Cloud - 认证中心
# 端口: 9001
# ============================================
ARG BASE_IMAGE=led-cloud-base:latest
FROM ${BASE_IMAGE}

COPY target/led-auth-1.0.0.jar /app/app.jar

EXPOSE 9001

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=60s \
  CMD wget -q --spider http://localhost:9001/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
