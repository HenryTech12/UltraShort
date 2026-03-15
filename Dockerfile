FROM eclipse-temurin:17-jdk
ADD target/ultrashort.jar ultrashort.jar
EXPOSE 8080
ENTRYPOINT ["java",  "-jar", "/app.jar"]