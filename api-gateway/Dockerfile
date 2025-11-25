# BUILD STAGE

FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml và tải dependencies
COPY pom.xml ./
RUN mvn dependency:resolve -B -V || true

# Copy toàn bộ source và build
COPY src ./src
RUN mvn clean package -DskipTests

# RUNTIME STAGE

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Copy jar từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms256m", "-Xmx256m", "-jar", "app.jar"]