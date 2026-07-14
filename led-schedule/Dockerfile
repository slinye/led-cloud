FROM led-management-backend:latest
RUN mkdir -p /app/uploads
WORKDIR /app
COPY target/led-schedule-1.0.0.jar /app/app.jar
EXPOSE 9004
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
