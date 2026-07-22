# ============================================
# LED Cloud - Sentinel Dashboard (熔断限流控制台)
# 端口: 8080
# ============================================
FROM eclipse-temurin:8-jdk-alpine

RUN wget -q https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar \
    -O /app/sentinel-dashboard.jar

WORKDIR /app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=30s \
  CMD wget -q --spider http://localhost:8080 || exit 1

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=8080 \
  -Dcsp.sentinel.dashboard.server=localhost:8080 \
  -Dproject.name=sentinel-dashboard \
  ${JAVA_OPTS} -jar /app/sentinel-dashboard.jar"]
