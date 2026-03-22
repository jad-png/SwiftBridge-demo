FROM maven:3.9.6-eclipse-temurin-17-alpine AS base
FROM base AS dependencies

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

FROM dependencies AS builder

COPY src ./src

RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="SwiftBridge Team"
LABEL description="SwiftBridge Core Converter Service - ISO 20022 to MT103 Conversion Engine"
LABEL version="1.0.0"

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -g 1000 swiftbridge && \
    adduser -u 1000 -G swiftbridge -s /sbin/nologin -D swiftbridge && \
    chown -R swiftbridge:swiftbridge /app

USER swiftbridge

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=10s --retries=5 --start-period=20s \
    CMD curl -f http://localhost:8081/api/v1/actuator/health || exit 1

ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
