# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy file cấu hình trước để cache dependency
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source và build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage (chỉ cần JRE, không cần JDK)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy jar đã build
COPY --from=build /app/target/java-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
