ARG BASE_IMAGE=led-management-backend:latest
FROM ${BASE_IMAGE}
RUN mkdir -p /app/uploads
WORKDIR /app
COPY target/led-content-1.0.0.jar /app/app.jar
EXPOSE 9003
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

