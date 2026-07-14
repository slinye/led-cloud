FROM led-management-backend:latest
WORKDIR /app
COPY target/led-auth-1.0.0.jar /app/app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
