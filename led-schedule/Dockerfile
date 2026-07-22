# ============================================
# LED Cloud - 调度服务 (排程 + 播放 + 报表)
# 端口: 9004
# ============================================
ARG BASE_IMAGE=led-cloud-base:latest
FROM ${BASE_IMAGE}

COPY target/led-schedule-1.0.0.jar /app/app.jar

EXPOSE 9004

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=60s \
  CMD wget -q --spider http://localhost:9004/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
