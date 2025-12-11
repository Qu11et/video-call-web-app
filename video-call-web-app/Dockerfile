# 1. Build Stage: Dùng Maven để build ra file .jar
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Run Stage: Chạy file .jar bằng Java nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Biến môi trường cho LiveKit URL (để sửa lỗi trả về localhost)
ENV LIVEKIT_URL=ws://localhost:7880

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

