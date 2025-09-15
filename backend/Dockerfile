# Multi-stage build for Zettix Backend
FROM maven:3.9.4-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r zettix && useradd -r -g zettix zettix

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for Gmail API
RUN mkdir -p /app/tokens && chown -R zettix:zettix /app

# Switch to non-root user
USER zettix

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8083/api/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
