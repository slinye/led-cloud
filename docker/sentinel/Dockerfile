FROM led-management-backend:latest

# Download Sentinel Dashboard 1.8.6 official jar
RUN wget -q https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar -O /app/sentinel-dashboard.jar \
    && echo "Sentinel Dashboard downloaded"

WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-Dcsp.sentinel.dashboard.server=localhost:8080", "-Dproject.name=sentinel-dashboard", "-jar", "sentinel-dashboard.jar"]
