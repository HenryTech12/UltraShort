FROM openjdk:17-jdk-slim
ADD target/ultrashort.jar ultrashort.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]