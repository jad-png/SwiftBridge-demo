FROM maven:3.9.6-eclipse-temurin-17-alpine AS base
FROM base AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="SwiftBridge Team"
LABEL description="SwiftBridge Orchestrator Service - REST API Gateway & Transaction Manager"
LABEL version="1.0.0"

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -g 1000 swiftbridge && \
    adduser -u 1000 -G swiftbridge -s /sbin/nologin -D swiftbridge && \
    chown -R swiftbridge:swiftbridge /app

USER swiftbridge

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=30s \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
