# SwiftBridge - Service-Oriented Architecture (SOA)

## Detailed Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT / CONSUMER                         │
│                    (Browser, Mobile, Third-party)               │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP/REST
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                 ORCHESTRATOR SERVICE (Port 8080)                │
│                                                                  │
│ • Public REST API Gateway                                       │
│ • JWT Security (Stubbed for PoC)                                │
│ • Transaction Metadata Management                               │
│ • Request Validation & Routing                                  │
│ • Error Handling & Logging                                      │
│                                                                  │
│ Dependencies: PostgreSQL, Core-Converter-Service                │
└──────────────┬──────────────────────────────┬───────────────────┘
               │                              │
    Database   │                              │ RPC Call (HTTP)
    Query      │                              │
               │                              │
┌──────────────▼──────────────┐   ┌──────────▼────────────────────┐
│  POSTGRESQL DATABASE         │   │ CORE CONVERTER SERVICE         │
│  (Port 5432)                 │   │ (Port 8081)                    │
│                              │   │                                │
│  • Transaction History       │   │ • ISO 20022 XML Parsing       │
│  • Metadata Storage          │   │ • MT103 TXT Generation         │
│  • Schema Management         │   │ • Conversion Logic             │
│  • Data Integrity            │   │ • High-Throughput Processing  │
│                              │   │ • Stateless Design             │
│  User: swiftbridge_user      │   │ • No Database Dependency       │
│  Password: ***SECURE***      │   │                                │
│  DB: swiftbridge             │   │ Dependencies: None             │
└──────────────────────────────┘   └────────────────────────────────┘
```

## Service Communication Flow

### Request Lifecycle:

```
1. Client Request (SWIFT XML)
        │
        ▼
2. Orchestrator (REST API)
   ├─ Validate JWT Token
   ├─ Parse Request
   ├─ Generate Transaction ID
   └─ Call Core Converter
        │
        ▼
3. Core Converter Service
   ├─ Parse ISO 20022 XML
   ├─ Transform to MT103
   ├─ Validate Output
   └─ Return Result
        │
        ▼
4. Orchestrator (Process Result)
   ├─ Store in PostgreSQL
   ├─ Audit Log
   ├─ Generate Response
   └─ HTTP 200 OK
        │
        ▼
5. Client Receives Result (MT103 TXT)
```

## Docker Network Architecture

```
┌────────────────────────────────────────────────────────────┐
│              Docker Bridge Network                         │
│        (swiftbridge_network: 172.20.0.0/16)               │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │ postgres-db (172.20.0.2)                         │    │
│  │ - Internal DNS: postgres-db                      │    │
│  │ - Container Port: 5432                           │    │
│  │ - Host Port: 5432                                │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │ orchestrator-service (172.20.0.3)                │    │
│  │ - Internal DNS: orchestrator-service             │    │
│  │ - Container Port: 8080                           │    │
│  │ - Host Port: 8080                                │    │
│  │ - Connects to: postgres-db:5432                  │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │ core-converter-service (172.20.0.4)              │    │
│  │ - Internal DNS: core-converter-service           │    │
│  │ - Container Port: 8081                           │    │
│  │ - Host Port: 8081                                │    │
│  │ - No external dependencies                       │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Orchestrator Service
- **Framework:** Spring Boot 3.2.0
- **Language:** Java 17
- **Build Tool:** Maven 3.9
- **Database:** PostgreSQL 16
- **Key Dependencies:**
  - Spring Web (REST)
  - Spring Data JPA (ORM)
  - PostgreSQL Driver (Database)
  - Spring WebFlux (Async HTTP Client)
  - Jackson (JSON Processing)
- **Port:** 8080
- **Package:** `com.swiftbridge.orchestrator`

### Core Converter Service
- **Framework:** Spring Boot 3.2.0
- **Language:** Java 17
- **Build Tool:** Maven 3.9
- **Database:** None (Stateless)
- **Key Dependencies:**
  - Spring Web (REST)
  - Jackson XML (XML Processing)
  - Jackson Databind (JSON Processing)
- **Port:** 8081
- **Package:** `com.swiftbridge.converter`

### Infrastructure
- **Containerization:** Docker & Docker Compose
- **Database:** PostgreSQL 16 (Alpine)
- **Base Image:** Eclipse Temurin 17-JRE (Alpine)
- **Build Image:** Maven 3.9 + Temurin 17 (Alpine)

## Data Persistence

### PostgreSQL Schema (Managed by Orchest rator Service)

```sql
-- Tables managed by Spring JPA (auto-generated)
-- Examples of what will be created:

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    client_id VARCHAR(255),
    request_type VARCHAR(50),
    input_xml TEXT,
    output_txt TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE transaction_audit (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID,
    event_type VARCHAR(50),
    message TEXT,
    created_at TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    jwt_token TEXT,
    created_at TIMESTAMP
);
```

## Security Architecture

### Authentication (Stubbed for PoC)
- JWT payload validation (not yet implemented)
- Token extraction from `Authorization: Bearer <token>` header
- Token signature verification (to be implemented)

### Network Security
- Services communicate via private Docker network (172.20.0.0/16)
- Only necessary ports exposed to host
- Non-root user execution in containers
- Security options enabled in docker-compose

### Data Security (Production Checklist)
- [ ] Enable SSL/TLS for PostgreSQL connections
- [ ] Configure database password encryption
- [ ] Implement rate limiting on API endpoints
- [ ] Add API key authentication for external integrations
- [ ] Enable mTLS for service-to-service communication
- [ ] Implement message signing/verification

## Performance Characteristics

### Orchestrator Service
- **Target Throughput:** 1,000 requests/second
- **Latency:** p99 < 500ms
- **Heap Memory:** 512MB (production: 1GB)
- **Thread Pool:** 200 max
- **Database Connection Pool:** 10 connections

### Core Converter Service
- **Target Throughput:** 10,000 conversions/second
- **Latency:** p99 < 100ms
- **Heap Memory:** 256MB
- **Thread Pool:** 250 max
- **Batch Processing:** 50 messages/batch

### Bottleneck Analysis
- **I/O Bound:** Database queries (PostgreSQL)
- **CPU Bound:** XML parsing and transformation
- **Network Bound:** Service-to-service communication

## Deployment Patterns

### Single-Machine (PoC/Dev)
- All services on one Docker host
- Shared Docker network
- Local PostgreSQL volume storage
- Suitable for: Development, testing, demos

### Multi-Machine (Production)
- Kubernetes orchestration
- Distributed PostgreSQL (streaming replication)
- Service mesh (Istio)
- Load balancing (Nginx, HAProxy)
- Monitoring (Prometheus, Grafana)
- Logging (ELK Stack)

## Configuration Management

### Environment Variables (Docker Compose)
- Service connectivity strings
- Database credentials
- JWT configuration
- Application-specific settings

### Application Properties
- Spring framework settings
- Logging levels
- Performance tuning
- Health check configuration

### Secrets Management (Production)
- Use Kubernetes Secrets
- HashiCorp Vault
- AWS Secrets Manager
- Never hardcode passwords

## Scalability Considerations

### Horizontal Scaling
- **Orchestrator:** Stateless, easily scalable behind load balancer
- **Core Converter:** Stateless, easily scalable
- **PostgreSQL:** Connection pooling limits; consider read replicas

### Vertical Scaling
- Increase JVM heap size
- Increase thread pools
- Optimize database indexes
- Tune connection timeouts

### Caching Strategy
- Cache SWIFT message templates
- Cache conversion rules
- Cache transaction lookups (Redis)

## Monitoring & Observability

### Metrics Collection
- Spring Boot Actuator endpoints
- JVM metrics (memory, GC)
- Application-level metrics
- Database query performance

### Health Checks
- Liveness probe: `GET /actuator/health`
- Readiness probe: Database connectivity
- Custom health indicators: Service availability

### Logging
- Structured logging (JSON format recommended)
- Centralized log aggregation
- Correlation IDs for request tracing
- Audit logs for compliance

## Disaster Recovery

### Backup Strategy
- Daily PostgreSQL backups
- Backup retention: 30 days
- Test restore procedures monthly
- Off-site backup storage

### Recovery Procedures
1. **Data Loss:** Restore from latest backup
2. **Service Failure:** Kubernetes auto-restart
3. **Network Partition:** Circuit breaker pattern
4. **Database Unavailability:** Cache results temporarily

---

**Version:** 1.0.0  
**Last Updated:** February 2026
