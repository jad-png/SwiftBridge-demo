# ============================================================================
# Dockerfile - Core Converter Service
# ============================================================================
# Multi-stage build for optimized production image
# Stage 1: Build application JAR using Maven
# Stage 2: Run JAR in lightweight Alpine JVM image
# ============================================================================

# ============================================================================
# STAGE 1: BUILD (Maven)
# ============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy Maven project files
COPY pom.xml .

# Download Maven dependencies (cached layer for faster builds)
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build the application JAR
RUN mvn clean package -DskipTests -q

# ============================================================================
# STAGE 2: RUNTIME (Alpine JVM)
# ============================================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="SwiftBridge Team"
LABEL description="SwiftBridge Core Converter Service - ISO 20022 to MT103 Conversion Engine"
LABEL version="1.0.0"

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup -g 1000 swiftbridge && \
    adduser -u 1000 -G swiftbridge -s /sbin/nologin -D swiftbridge && \
    chown -R swiftbridge:swiftbridge /app

# Switch to non-root user
USER swiftbridge

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=20s \
    CMD curl -f http://localhost:8081/api/v1/actuator/health || exit 1

# Set JVM options for optimal performance and high-throughput conversion
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
