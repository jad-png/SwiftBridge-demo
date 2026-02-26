# SwiftBridge - Production Deployment Guide

## Overview

This guide covers deployment to production environments, including security hardening, performance optimization, and operational best practices.

---

## Pre-Deployment Checklist

### Security

- [ ] **Database Password**
  - [ ] Change `POSTGRES_PASSWORD` in docker-compose.yml
  - [ ] Update `SPRING_DATASOURCE_PASSWORD` in Orchestrator config
  - [ ] Store password in secure vault (AWS Secrets Manager, Vault, etc.)
  - [ ] Follow password policy: min 16 chars, mixed case, numbers, symbols

- [ ] **JWT Configuration**
  - [ ] Enable JWT: `APP_SECURITY_JWT_ENABLED=true`
  - [ ] Generate strong secret key (min 32 bytes)
  - [ ] Rotate secret key every 90 days
  - [ ] Store secret in environment variable, never in code

- [ ] **HTTPS/TLS**
  - [ ] Generate or obtain SSL certificate
  - [ ] Configure Nginx reverse proxy with TLS
  - [ ] Enable HSTS headers
  - [ ] Use TLS 1.2+ only

- [ ] **Network Security**
  - [ ] Restrict database access to application server only
  - [ ] Implement firewall rules
  - [ ] Use VPC security groups in cloud environments
  - [ ] Disable public IP on database servers

- [ ] **Application Security**
  - [ ] Enable CORS restrictions
  - [ ] Implement rate limiting (DDoS protection)
  - [ ] Add input validation and sanitization
  - [ ] Implement API key authentication for external clients
  - [ ] Enable security headers (CSP, X-Frame-Options, etc.)

### Performance

- [ ] **Database Optimization**
  - [ ] Create indexes on frequently queried columns
  - [ ] Enable query caching
  - [ ] Configure connection pooling appropriately
  - [ ] Set up monitoring and alerting for slow queries
  - [ ] Enable auto-vacuum

- [ ] **JVM Tuning**
  - [ ] Set appropriate heap sizes based on available memory
  - [ ] Configure G1GC for large heaps (> 4GB)
  - [ ] Enable GC logging: `-XX:+PrintGCDetails -XX:+PrintGCDateStamps`
  - [ ] Monitor GC pauses regularly

- [ ] **Load Balancing**
  - [ ] Deploy multiple instances of Orchestrator Service
  - [ ] Configure load balancer with health checks
  - [ ] Implement persistent session storage if needed
  - [ ] Use sticky sessions cautiously (prefer stateless)

- [ ] **Caching**
  - [ ] Deploy Redis for distributed caching
  - [ ] Cache SWIFT message templates
  - [ ] Cache conversion rules
  - [ ] Cache frequently accessed data
  - [ ] Implement cache invalidation strategy

### Infrastructure

- [ ] **Container Registry**
  - [ ] Set up Docker registry (ECR, Artifactory, DockerHub)
  - [ ] Push built images with proper version tags
  - [ ] Implement image scanning for vulnerabilities
  - [ ] Set up image retention policies

- [ ] **Orchestration Platform**
  - [ ] Kubernetes cluster (recommended for production)
  - [ ] Or Docker Swarm for smaller deployments
  - [ ] Configure resource requests and limits
  - [ ] Set up auto-scaling policies

- [ ] **Monitoring & Logging**
  - [ ] Set up Prometheus for metrics collection
  - [ ] Deploy Grafana for visualization
  - [ ] Configure ELK Stack (Elasticsearch, Logstash, Kibana)
  - [ ] Set up centralized logging
  - [ ] Configure log retention and archival

- [ ] **Backup & Disaster Recovery**
  - [ ] Automated daily PostgreSQL backups
  - [ ] Off-site backup replication (S3, Azure Blob)
  - [ ] Test backup restoration monthly
  - [ ] Document recovery procedures
  - [ ] Define RTO/RPO targets

### Compliance

- [ ] **Data Protection**
  - [ ] GDPR compliance for EU customers
  - [ ] Data encryption at rest (PostgreSQL)
  - [ ] Data encryption in transit (TLS)
  - [ ] PCI-DSS if handling payment data

- [ ] **Audit & Compliance**
  - [ ] Enable PostgreSQL audit logging
  - [ ] Implement request/response logging
  - [ ] Maintain audit trail for 7+ years (FINRA requirement)
  - [ ] Regular security audits

- [ ] **Documentation**
  - [ ] Architecture documentation
  - [ ] Operational runbooks
  - [ ] Incident response procedures
  - [ ] Change management process

---

## Kubernetes Deployment (Recommended)

### Prerequisites
- Kubernetes cluster (1.24+)
- kubectl CLI
- Helm (optional, for templating)

### Deployment Manifests

#### Namespace and Secrets

```yaml
# swiftbridge-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: swiftbridge
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-credentials
  namespace: swiftbridge
type: Opaque
stringData:
  username: swiftbridge_user
  password: YOUR_SECURE_PASSWORD_HERE
---
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
  namespace: swiftbridge
type: Opaque
stringData:
  secret-key: YOUR_JWT_SECRET_KEY_HERE
```

#### PostgreSQL StatefulSet

```yaml
# postgres-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: swiftbridge
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: swiftbridge
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: password
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U swiftbridge_user
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U swiftbridge_user
          initialDelaySeconds: 10
          periodSeconds: 5
  volumeClaimTemplates:
  - metadata:
      name: postgres-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: "standard"
      resources:
        requests:
          storage: 50Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: swiftbridge
spec:
  ports:
  - port: 5432
    targetPort: 5432
  clusterIP: None
  selector:
    app: postgres
```

#### Orchestrator Service Deployment

```yaml
# orchestrator-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orchestrator-service
  namespace: swiftbridge
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: orchestrator-service
  template:
    metadata:
      labels:
        app: orchestrator-service
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - orchestrator-service
              topologyKey: kubernetes.io/hostname
      containers:
      - name: orchestrator
        image: YOUR_REGISTRY/orchestrator-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/swiftbridge
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-credentials
              key: password
        - name: APP_CORE_CONVERTER_URL
          value: http://core-converter-service:8081
        - name: APP_SECURITY_JWT_ENABLED
          value: "true"
        - name: APP_SECURITY_JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret-key
        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: orchestrator-service
  namespace: swiftbridge
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
  selector:
    app: orchestrator-service
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: orchestrator-hpa
  namespace: swiftbridge
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: orchestrator-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Core Converter Service Deployment

```yaml
# converter-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: core-converter-service
  namespace: swiftbridge
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: core-converter-service
  template:
    metadata:
      labels:
        app: core-converter-service
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - core-converter-service
              topologyKey: kubernetes.io/hostname
      containers:
      - name: converter
        image: YOUR_REGISTRY/core-converter-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: JAVA_OPTS
          value: "-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
        - name: APP_CONVERTER_VALIDATION_MODE
          value: "strict"
        - name: APP_CONVERTER_BATCH_ENABLED
          value: "true"
        - name: APP_CONVERTER_BATCH_SIZE
          value: "50"
        resources:
          requests:
            memory: "256Mi"
            cpu: "1000m"
          limits:
            memory: "512Mi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /api/v1/actuator/health
            port: 8081
          initialDelaySeconds: 40
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/v1/actuator/health
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: core-converter-service
  namespace: swiftbridge
spec:
  type: ClusterIP
  ports:
  - port: 8081
    targetPort: 8081
    protocol: TCP
  selector:
    app: core-converter-service
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: converter-hpa
  namespace: swiftbridge
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: core-converter-service
  minReplicas: 5
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 75
```

### Deployment Steps

```bash
# 1. Create namespace
kubectl apply -f swiftbridge-namespace.yaml

# 2. Deploy PostgreSQL
kubectl apply -f postgres-statefulset.yaml

# 3. Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod \
  -l app=postgres -n swiftbridge --timeout=300s

# 4. Deploy Orchestrator Service
kubectl apply -f orchestrator-deployment.yaml

# 5. Deploy Core Converter Service
kubectl apply -f converter-deployment.yaml

# 6. Verify all pods are running
kubectl get pods -n swiftbridge

# 7. Get service endpoints
kubectl get svc -n swiftbridge

# 8. Port forward for testing (optional)
kubectl port-forward svc/orchestrator-service 8080:80 -n swiftbridge
```

---

## AWS ECS Deployment (Docker)

### ECR Setup

```bash
# Create ECR repositories
aws ecr create-repository --repository-name swiftbridge/orchestrator-service
aws ecr create-repository --repository-name swiftbridge/core-converter-service

# Build and push images
docker build -t orchestrator-service:1.0.0 ./Orchestrator-Service
docker tag orchestrator-service:1.0.0 YOUR_ACCOUNT.dkr.ecr.REGION.amazonaws.com/swiftbridge/orchestrator-service:1.0.0
docker push YOUR_ACCOUNT.dkr.ecr.REGION.amazonaws.com/swiftbridge/orchestrator-service:1.0.0
```

---

## Monitoring Setup

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'orchestrator-service'
    static_configs:
      - targets: ['orchestrator-service:8080']
    metrics_path: '/api/actuator/prometheus'

  - job_name: 'core-converter-service'
    static_configs:
      - targets: ['core-converter-service:8081']
    metrics_path: '/api/v1/actuator/prometheus'
```

### Grafana Dashboard

Import dashboard IDs:
- 11378 - Spring Boot Micrometer
- 11462 - Spring Boot Metrics

---

## Post-Deployment Validation

```bash
# Check service health
curl https://api.swiftbridge.com/api/actuator/health

# Verify database connectivity
curl https://api.swiftbridge.com/api/actuator/health/db

# Check converter service
curl https://api.swiftbridge.com/internal/converter/health

# Run smoke tests
bash tests/smoke-tests.sh

# Monitor logs
kubectl logs -f deployment/orchestrator-service -n swiftbridge
```

---

## Rollback Procedures

### Kubernetes Rollback

```bash
# Check rollout history
kubectl rollout history deployment/orchestrator-service -n swiftbridge

# Rollback to previous version
kubectl rollout undo deployment/orchestrator-service -n swiftbridge

# Rollback to specific revision
kubectl rollout undo deployment/orchestrator-service \
  --to-revision=2 -n swiftbridge
```

---

## Maintenance Windows

### Database Maintenance
- Scheduled during low-traffic hours
- Update statistics: `ANALYZE`
- Vacuum dead tuples: `VACUUM`
- Reindex: `REINDEX`
- Backup before maintenance

### Application Updates
- Blue-green deployment
- Canary rollout (5% → 25% → 100%)
- Automated rollback on health check failure

---

**Version:** 1.0.0  
**Last Updated:** February 2026
