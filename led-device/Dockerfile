# ============================================
# LED Cloud - 设备管理服务 (MQTT + WebSocket)
# 端口: 9002
# ============================================
ARG BASE_IMAGE=led-cloud-base:latest
FROM ${BASE_IMAGE}

COPY target/led-device-1.0.0.jar /app/app.jar

EXPOSE 9002

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=60s \
  CMD wget -q --spider http://localhost:9002/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
