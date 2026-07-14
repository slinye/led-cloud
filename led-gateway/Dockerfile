FROM led-management-backend:latest
WORKDIR /app
COPY target/led-gateway-1.0.0.jar /app/app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
