# Bước 1: Build project bằng Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng bằng Java
FROM openjdk:17-jdk-slim
COPY --from=build /target/quanliPT-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
