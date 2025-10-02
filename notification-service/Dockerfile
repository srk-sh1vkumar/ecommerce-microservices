# Multi-stage build for optimized image size
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy parent pom first
COPY ../pom.xml ../pom.xml
COPY ../common-lib ../common-lib

# Copy notification service files
COPY pom.xml .
COPY src ./src

# Build the application
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

# Extract layers
RUN mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Add non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy layers from builder
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8087

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
