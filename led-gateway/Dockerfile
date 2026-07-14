ARG BASE_IMAGE=led-management-backend:latest
FROM ${BASE_IMAGE}
WORKDIR /app
COPY target/led-gateway-1.0.0.jar /app/app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

