# SwiftBridge - Service-Oriented Architecture PoC
## A SWIFT ISO 20022 to Legacy MT103 Conversion Platform
### This is a demo version of SwiftBridge, built with AI and guided by the repository’s owner.
---

## 📋 Architecture Overview

This is a production-ready PoC for **SwiftBridge**, a B2B SaaS platform that converts ISO 20022 (pacs.008 XML) messages to Legacy MT103 (TXT) messages.

### Components:

1. **Orchestrator-Service (Port 8080)**
   - REST API Gateway for public consumption
   - JWT security (stubbed for PoC)
   - Transaction metadata persistence (PostgreSQL)
   - Communicates with Core-Converter-Service

2. **Core-Converter-Service (Port 8081)**
   - Lightweight, stateless conversion engine
   - Processes XML → TXT transformations
   - High-throughput, low-latency design
   - Internal communication only (no database)

3. **PostgreSQL Database (Port 5432)**
   - Persists transaction history
   - Used ONLY by Orchestrator-Service
   - Auto-initialized via Docker

---

## 🚀 Quick Start Guide

### Prerequisites:
- **Docker & Docker Compose** (latest versions)
- **Java 17+** (for local development)
- **Maven 3.9+** (for building outside Docker)
- **Git** (optional, for version control)

### Option A: Run Everything with Docker Compose (Recommended)

```bash
# 1. Navigate to the SwiftBridge directory
cd /path/to/SwiftBridge

# 2. Build images and start all services
docker-compose up -d

# 3. Verify all services are running
docker-compose ps

# 4. Check logs
docker-compose logs -f

# 5. Test the Orchestrator API
curl -X GET http://localhost:8080/api/actuator/health

# 6. Test the Converter API
curl -X GET http://localhost:8081/api/v1/actuator/health

# 7. Stop all services
docker-compose down

# Optional: Remove volumes (clears database)
docker-compose down -v
```

---

## 🔧 Local Development Setup (Without Docker)

### Step 1: Maven Scaffolding Commands (If Starting from Scratch)

#### Using Spring Boot CLI:
```bash
# Install Spring Boot CLI (if not already installed)
# https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing.cli

# Create Orchestrator Service
spring project create \
  --build maven \
  --java-version 17 \
  --language java \
  --name orchestrator-service \
  --type maven-project \
  Orchestrator-Service

cd Orchestrator-Service

# Add dependencies
spring project add --dependency web,data-jpa,postgresql
```

#### Using Maven Directly:
```bash
# Create Orchestrator Service
mvn archetype:generate \
  -DgroupId=com.swiftbridge \
  -DartifactId=orchestrator-service \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd orchestrator-service

# Then manually add Spring Boot parent and dependencies to pom.xml
```

#### Using Spring Initializr (Recommended):
Visit: https://start.spring.io

**Orchestrator-Service Configuration:**
- **Project:** Maven Project
- **Language:** Java
- **Spring Boot Version:** 3.2.0
- **Group:** com.swiftbridge
- **Artifact:** orchestrator-service
- **Name:** Orchestrator Service
- **Description:** SwiftBridge Orchestrator Service
- **Java Version:** 17
- **Dependencies:** Spring Web, Spring Data JPA, PostgreSQL Driver, Spring WebFlux

**Core-Converter-Service Configuration:**
- **Project:** Maven Project
- **Language:** Java
- **Spring Boot Version:** 3.2.0
- **Group:** com.swiftbridge
- **Artifact:** core-converter-service
- **Name:** Core Converter Service
- **Description:** SwiftBridge Core Converter Service
- **Java Version:** 17
- **Dependencies:** Spring Web, Jackson XML

---

### Step 2: Build Both Services Locally

```bash
# Build Orchestrator Service
cd Orchestrator-Service
mvn clean package -DskipTests
# Output: target/orchestrator-service-1.0.0.jar

# Build Core Converter Service
cd ../Core-Converter-Service
mvn clean package -DskipTests
# Output: target/core-converter-service-1.0.0.jar
```

### Step 3: Run PostgreSQL (Using Docker Only)

```bash
# Run PostgreSQL container only
docker run -d \
  --name swiftbridge_postgres \
  --network swiftbridge_network \
  -e POSTGRES_DB=swiftbridge \
  -e POSTGRES_USER=swiftbridge_user \
  -e POSTGRES_PASSWORD=SwiftBridge@123!Secure \
  -p 5432:5432 \
  postgres:16-alpine

# Create Docker network (if it doesn't exist)
docker network create swiftbridge_network
```

### Step 4: Run Services Locally

```bash
# Terminal 1: Start Core Converter Service
cd Core-Converter-Service
java -Xmx256m -Xms128m -jar target/core-converter-service-1.0.0.jar

# Terminal 2: Start Orchestrator Service
cd Orchestrator-Service
java \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/swiftbridge \
  -Dspring.datasource.username=swiftbridge_user \
  -Dspring.datasource.password=SwiftBridge@123!Secure \
  -Dapp.core-converter.url=http://localhost:8081 \
  -Xmx512m -Xms256m \
  -jar target/orchestrator-service-1.0.0.jar
```

---

## 📡 API Testing & Verification

### Health Check Endpoints

```bash
# Orchestrator Service Health
curl -X GET http://localhost:8080/api/actuator/health

# Core Converter Service Health
curl -X GET http://localhost:8081/api/v1/actuator/health

# Application Info
curl -X GET http://localhost:8080/api/actuator/info
curl -X GET http://localhost:8081/api/v1/actuator/info
```

### Docker Service Verification

```bash
# View all running containers
docker ps

# View specific service logs
docker-compose logs orchestrator-service
docker-compose logs core-converter-service
docker-compose logs postgres-db

# Stream logs in real-time
docker-compose logs -f

# Execute command inside container
docker exec -it swiftbridge_orchestrator ls -la /app

# Check network connectivity
docker network inspect swiftbridge_network
```

### Database Verification

```bash
# Connect to PostgreSQL
docker exec -it swiftbridge_postgres psql \
  -U swiftbridge_user \
  -d swiftbridge \
  -c "SELECT version();"

# List tables
docker exec -it swiftbridge_postgres psql \
  -U swiftbridge_user \
  -d swiftbridge \
  -c "\dt"
```

---

## 🔐 Configuration Management

### Environment Variables for Orchestrator Service

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/swiftbridge
SPRING_DATASOURCE_USERNAME=swiftbridge_user
SPRING_DATASOURCE_PASSWORD=SwiftBridge@123!Secure

# Core Converter Service URL (internal)
APP_CORE_CONVERTER_URL=http://core-converter-service:8081

# JWT Configuration (stubbed)
APP_SECURITY_JWT_ENABLED=false
APP_SECURITY_JWT_SECRET=your-secret-key-change-in-production

# JVM Optimization
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

### Environment Variables for Core Converter Service

```bash
# JVM Optimization (high-throughput)
JAVA_OPTS=-Xmx256m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100

# Validation Mode (strict|lenient|warning)
APP_CONVERTER_VALIDATION_MODE=strict

# Batch Processing
APP_CONVERTER_BATCH_ENABLED=true
APP_CONVERTER_BATCH_SIZE=50
```

---

## 📊 Performance Tuning

### Orchestrator Service (REST API Gateway)
- **Max Threads:** 200
- **Min Spare Threads:** 10
- **Connection Pool:** 10 max, 5 min idle
- **Heap Memory:** 512MB (production: 1GB+)

### Core Converter Service (Conversion Engine)
- **Max Threads:** 250
- **Min Spare Threads:** 20
- **Batch Size:** 50 messages per batch
- **Heap Memory:** 256MB (optimized for GC)

### PostgreSQL (Database)
- **Max Connections:** 100 (default, sufficient for PoC)
- **Shared Buffers:** 128MB (Alpine default)
- **Work Memory:** 4MB per operation

---

## 🐛 Troubleshooting

### Services Won't Start

**Problem:** `Connection refused` or `Cannot connect to database`

**Solution:**
```bash
# Ensure PostgreSQL is ready
docker-compose logs postgres-db

# Wait for PostgreSQL to be healthy
docker-compose ps  # Check STATUS column

# Restart specific service
docker-compose restart orchestrator-service
```

### Port Conflicts

**Problem:** `Address already in use`

**Solution:**
```bash
# Find what's using the port
# Windows:
netstat -ano | findstr :8080

# Linux/Mac:
lsof -i :8080

# Change port in docker-compose.yml
# Change: ports: - "8080:8080" to "8090:8080"
```

### Database Connection Issues

**Problem:** `org.postgresql.util.PSQLException: Connection to localhost:5432 refused`

**Solution:**
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check database credentials in application.yml
# Default: swiftbridge_user / SwiftBridge@123!Secure

# Reset database volume
docker-compose down -v
docker-compose up -d
```

### Out of Memory Errors

**Problem:** `java.lang.OutOfMemoryError`

**Solution:**
```bash
# Increase JVM heap in docker-compose.yml
JAVA_OPTS: "-Xmx1g -Xms512m"  # Orchestrator
JAVA_OPTS: "-Xmx512m -Xms256m"  # Converter

# Or restart with more memory
docker-compose down
docker-compose up -d
```

---

## 📈 Monitoring & Metrics

### Actuator Endpoints

**Orchestrator Service:**
```bash
# Health
curl http://localhost:8080/api/actuator/health

# Metrics
curl http://localhost:8080/api/actuator/metrics

# Specific metric
curl http://localhost:8080/api/actuator/metrics/jvm.memory.used
```

**Core Converter Service:**
```bash
# Health
curl http://localhost:8081/api/v1/actuator/health

# Metrics
curl http://localhost:8081/api/v1/actuator/metrics
```

### Container Resource Usage

```bash
# Real-time stats
docker stats

# Historical stats
docker container stats --no-stream
```

---

## 🔄 CI/CD Integration

### GitHub Actions Example

```yaml
name: Build & Deploy SwiftBridge

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: |
          cd Orchestrator-Service && mvn clean package
          cd ../Core-Converter-Service && mvn clean package
      
      - name: Build Docker images
        run: docker-compose build
      
      - name: Run tests with Docker Compose
        run: docker-compose up --exit-code-from orchestrator-service
```

---

## 🚢 Production Deployment Checklist

- [ ] Update PostgreSQL password in all configuration files
- [ ] Enable JWT security (set `APP_SECURITY_JWT_ENABLED=true`)
- [ ] Configure secret key for JWT tokens
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (don't auto-create tables)
- [ ] Enable HTTPS/TLS for all services
- [ ] Configure resource limits and CPU quotas
- [ ] Set up health check monitoring and alerting
- [ ] Configure log aggregation (ELK, Datadog, etc.)
- [ ] Implement rate limiting and API throttling
- [ ] Set up database backups
- [ ] Configure network policies and firewall rules
- [ ] Implement service mesh (Istio, Linkerd) for advanced routing

---

## 📝 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [SWIFT Standard Formats](https://www.swift.com/)
- [ISO 20022 XML Standard](https://www.iso.org/standard/81090.html)

---

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review service logs: `docker-compose logs -f`
3. Check application health endpoints
4. Verify network connectivity between services

---

**Version:** 1.0.0  
**Last Updated:** February 2026  
**Created by:** SwiftBridge Architecture Team
