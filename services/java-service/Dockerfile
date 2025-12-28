FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage
FROM gcr.io/distroless/java17-debian12
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

# CẤU HÌNH TỐI ƯU
# -XX:MaxRAMPercentage=75.0: Java sẽ dùng 75% RAM của Container cho Heap,
# 25% còn lại dành cho Non-Heap (Metaspace, Threads...) để tránh bị OS kill.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=50.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]