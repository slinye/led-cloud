FROM led-management-backend:latest
WORKDIR /app
COPY target/led-device-1.0.0.jar /app/app.jar
EXPOSE 9002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
