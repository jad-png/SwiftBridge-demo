# SwiftBridge - Quick Start Guide (5-Minute Setup)

## The Fastest Way to Get Everything Running

### Prerequisites Check (1 minute)

```powershell
# Check Docker installation
docker --version
docker-compose --version

# Verify you're in the SwiftBridge root directory
Get-ChildItem | Select-Object Name   # Should see: docker-compose.yml, README.md, etc.
```

---

## Option 1: Docker Compose (Easiest - 3 minutes)

```powershell
# 1. Start all services
docker-compose up -d

# 2. Wait 10 seconds for services to initialize
Start-Sleep -Seconds 10

# 3. Verify everything is running
docker-compose ps

# 4. Check if they're healthy
docker-compose logs postgres-db | Select-Object -Last 5
docker-compose logs orchestrator-service | Select-Object -Last 5
docker-compose logs core-converter-service | Select-Object -Last 5
```

### Test the Services

```powershell
# Test Orchestrator API
Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" | Select-Object StatusCode

# Test Converter API  
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/actuator/health" | Select-Object StatusCode

# Test PostgreSQL
docker exec swiftbridge_postgres psql -U swiftbridge_user -d swiftbridge -c "SELECT 1;"
```

### Stop Everything

```powershell
# Stop services (keeps data)
docker-compose down

# Remove everything including data
docker-compose down -v
```

---

## Option 2: Using the CLI Helper Script (Recommended on Windows)

```powershell
# Make script executable
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Start everything
.\swiftbridge-cli.ps1 -Command up

# Check status
.\swiftbridge-cli.ps1 -Command status

# View logs
.\swiftbridge-cli.ps1 -Command logs

# Run health checks
.\swiftbridge-cli.ps1 -Command test

# Stop everything
.\swiftbridge-cli.ps1 -Command down
```

---

## Option 3: Local Development (Maven + Java)

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL installed locally (or Docker)

### Steps

```bash
# 1. Start PostgreSQL in Docker (background)
docker run -d \
  --name postgres-dev \
  -e POSTGRES_DB=swiftbridge \
  -e POSTGRES_USER=swiftbridge_user \
  -e POSTGRES_PASSWORD=SwiftBridge@123!Secure \
  -p 5432:5432 \
  postgres:16-alpine

# 2. Build both services
cd Orchestrator-Service
mvn clean package -DskipTests

cd ../Core-Converter-Service
mvn clean package -DskipTests

# 3. Terminal 1 - Start Converter Service
cd Core-Converter-Service
java -jar target/core-converter-service-*.jar

# 4. Terminal 2 - Start Orchestrator Service
cd Orchestrator-Service
java -jar target/orchestrator-service-*.jar

# 5. Test the APIs
curl http://localhost:8080/api/actuator/health
curl http://localhost:8081/api/v1/actuator/health
```

---

## Service URLs & Credentials

```
┌─────────────────────────────────────────────────────────────────┐
│                        SERVICE ENDPOINTS                         │
├─────────────────────────────────────────────────────────────────┤
│ Service                    URL                  Port             │
├─────────────────────────────────────────────────────────────────┤
│ Orchestrator Service       http://localhost:8080/api             │
│ Converter Service          http://localhost:8081/api/v1          │
│ PostgreSQL Database        localhost:5432                        │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                     DATABASE CREDENTIALS                         │
├─────────────────────────────────────────────────────────────────┤
│ Username:                  swiftbridge_user                      │
│ Password:                  SwiftBridge@123!Secure                │
│ Database:                  swiftbridge                           │
│ Host:                      localhost (or postgres-db in Docker)  │
│ Port:                      5432                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Health Check Endpoints

```bash
# Orchestrator Service - Health
curl http://localhost:8080/api/actuator/health

# Orchestrator Service - Application Info
curl http://localhost:8080/api/actuator/info

# Core Converter Service - Health
curl http://localhost:8081/api/v1/actuator/health

# Core Converter Service - Metrics
curl http://localhost:8081/api/v1/actuator/metrics
```

---

## Expected Docker Output

```
CONTAINER ID   IMAGE                              STATUS              PORTS
abc123         swiftbridge_postgres               Up 1 min            0.0.0.0:5432->5432/tcp
def456         swiftbridge_orchestrator           Up 1 min (healthy)  0.0.0.0:8080->8080/tcp
ghi789         swiftbridge_converter              Up 1 min (healthy)  0.0.0.0:8081->8081/tcp
```

---

## Common Troubleshooting

### Problem: "Port already in use"
```powershell
# Find what's using the port
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

# Kill the process
Stop-Process -Id <PID> -Force

# Or use different ports - edit docker-compose.yml
```

### Problem: "Cannot connect to database"
```powershell
# Check if PostgreSQL container is running
docker ps | Select-String postgres

# Check logs
docker logs swiftbridge_postgres

# Restart PostgreSQL
docker restart swiftbridge_postgres
```

### Problem: "Services failing to start"
```powershell
# View detailed logs
docker-compose logs

# Rebuild images
docker-compose build --no-cache

# Start fresh
docker-compose down -v
docker-compose up -d
```

### Problem: "Out of memory"
```powershell
# Check Docker resource limits
# Windows: Docker Desktop Settings → Resources

# Increase in docker-compose.yml
# deploy:
#   resources:
#     limits:
#       memory: 2G
```

---

## Next Steps After Setup

### 1. Explore the Architecture
- Read [ARCHITECTURE.md](ARCHITECTURE.md) for detailed design
- Review [docker-compose.yml](docker-compose.yml) for service configuration
- Check [application.yml](Orchestrator-Service/src/main/resources/application.yml) files

### 2. Customize Configuration
```bash
# Copy environment template
Copy-Item .env.example .env.local

# Edit with your settings
# DO NOT commit .env.local to Git!
```

### 3. Test the APIs
```bash
# Check Orchestrator health
Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" | ConvertFrom-Json

# Get application info
Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/info" | ConvertFrom-Json

# Check database connectivity
Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health/db" | ConvertFrom-Json
```

### 4. Monitor Services
```bash
# Real-time logs
docker-compose logs -f

# Logs for specific service
docker-compose logs -f orchestrator-service

# Container statistics
docker stats
```

### 5. Database Access
```SQL
-- Connect using any PostgreSQL client
-- psql, DBeaver, pgAdmin, or IDE tools

-- Your connection string:
-- Host: localhost (or postgres-db from Docker)
-- Port: 5432
-- Database: swiftbridge
-- Username: swiftbridge_user
-- Password: SwiftBridge@123!Secure
```

---

## Document Reference

| Document | Purpose |
|----------|---------|
| [README.md](README.md) | Complete setup and usage guide |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Detailed system architecture |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Production deployment guide |
| [docker-compose.yml](docker-compose.yml) | Service orchestration configuration |
| [swiftbridge-cli.ps1](swiftbridge-cli.ps1) | Windows management script |

---

## Key Commands Reference

```powershell
# Start/Stop
docker-compose up -d              # Start all services
docker-compose down               # Stop all services
docker-compose down -v            # Stop and remove data

# Monitoring
docker-compose ps                 # List containers
docker-compose logs -f            # Stream all logs
docker-compose logs orchestrator  # Service-specific logs

# Management
docker-compose build              # Build images
docker-compose pull               # Pull latest images
docker-compose restart            # Restart all services
docker-compose restart postgres-db  # Restart specific service

# Cleanup
docker-compose down -v            # Remove everything
docker system prune               # Clean up Docker resources
```

---

## Support & Resources

- **Docker Documentation:** https://docs.docker.com
- **Spring Boot Documentation:** https://spring.boot.io
- **PostgreSQL Documentation:** https://www.postgresql.org/docs
- **SWIFT Standards:** https://www.swift.com/standards

---

## ⏱️ Typical Setup Timeline

| Phase | Time | Action |
|-------|------|--------|
| 1 | 1 min | Verify prerequisites (Docker, Docker Compose) |
| 2 | 2 min | Run `docker-compose up -d` |
| 3 | 1 min | Wait for services to become healthy |
| 4 | 1 min | Verify with health check endpoints |
| **TOTAL** | **~5 minutes** | **Complete working environment** |

---

**Version:** 1.0.0  
**Last Updated:** February 2026  
**Ready to get started?** Run: `docker-compose up -d`
