# Deployment and Production Strategy

## Overview

This document outlines comprehensive deployment strategies for the e-commerce microservices application, covering local development, staging, and production environments with focus on scalability, reliability, and security.

## Deployment Architecture

### Multi-Environment Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                    Production Environment                        │
│  ┌───────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ Load Balancer │  │   CDN/WAF    │  │  Monitoring Stack    │ │
│  │   (HAProxy/   │  │ (CloudFlare/ │  │   (Prometheus/       │ │
│  │    Nginx)     │  │   AWS WAF)   │  │    Grafana)          │ │
│  └───────┬───────┘  └──────┬───────┘  └──────────────────────┘ │
│          │                 │                                    │
│  ┌───────▼─────────────────▼────────────────────────────────┐   │
│  │              Kubernetes Cluster                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │   Node 1    │ │   Node 2    │ │      Node 3         │ │   │
│  │  │ (Master +   │ │  (Worker)   │ │     (Worker)        │ │   │
│  │  │  Worker)    │ │             │ │                     │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Database Cluster                           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │ MongoDB     │ │ MongoDB     │ │    MongoDB          │ │   │
│  │  │ Primary     │ │ Secondary   │ │   Secondary         │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Staging Environment                          │
│              (Mirror of Production - Scaled Down)               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                  Development Environment                        │
│              (Docker Compose - Local/Remote)                    │
└─────────────────────────────────────────────────────────────────┘
```

## Container Strategy

### Docker Optimization

#### Multi-stage Dockerfile Example

```dockerfile
# Build stage
FROM maven:3.9-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user (Alpine Linux syntax)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Install required packages (Alpine Linux syntax)
RUN apk add --no-cache curl

# Copy application
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Application configuration
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Image Security Scanning

```yaml
# Docker image security scan in CI/CD
- name: Scan Docker image
  uses: azure/container-scan@v0
  with:
    image-name: ecommerce/user-service:${{ github.sha }}
    severity-threshold: HIGH
    
- name: Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ecommerce/user-service:${{ github.sha }}
    format: 'sarif'
    output: 'trivy-results.sarif'
```

## Kubernetes Deployment

### Namespace Organization

```yaml
# environments/production/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-prod
  labels:
    environment: production
    team: platform
---
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce-staging
  labels:
    environment: staging
    team: platform
```

### Service Deployment Template

```yaml
# k8s/user-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: ecommerce-prod
  labels:
    app: user-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        version: v1
    spec:
      serviceAccountName: user-service
      containers:
      - name: user-service
        image: ecommerce/user-service:1.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: mongodb-credentials
              key: uri
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        securityContext:
          runAsNonRoot: true
          runAsUser: 1001
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
            - ALL
        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: tmp
        emptyDir: {}
      - name: logs
        emptyDir: {}
      nodeSelector:
        workload-type: application
      tolerations:
      - key: "application"
        operator: "Equal"
        value: "true"
        effect: "NoSchedule"
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: ecommerce-prod
  labels:
    app: user-service
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: user-service
```

### Horizontal Pod Autoscaler

```yaml
# k8s/user-service/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: ecommerce-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
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
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

### Ingress Configuration

```yaml
# k8s/ingress/api-gateway-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-gateway-ingress
  namespace: ecommerce-prod
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://ecommerce.example.com"
    nginx.ingress.kubernetes.io/enable-cors: "true"
spec:
  tls:
  - hosts:
    - api.ecommerce.example.com
    secretName: api-gateway-tls
  rules:
  - host: api.ecommerce.example.com
    http:
      paths:
      - path: /api/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 80
      - path: /api/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 80
      - path: /api/cart
        pathType: Prefix
        backend:
          service:
            name: cart-service
            port:
              number: 80
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 80
```

## Database Deployment

### MongoDB Replica Set

```yaml
# k8s/mongodb/mongodb-replica-set.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mongodb
  namespace: ecommerce-prod
spec:
  serviceName: mongodb-headless
  replicas: 3
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
      - name: mongodb
        image: mongo:7.0
        command:
        - mongod
        - --replSet
        - rs0
        - --auth
        - --keyFile
        - /etc/mongodb-keyfile/keyfile
        ports:
        - containerPort: 27017
        env:
        - name: MONGO_INITDB_ROOT_USERNAME
          valueFrom:
            secretKeyRef:
              name: mongodb-credentials
              key: username
        - name: MONGO_INITDB_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mongodb-credentials
              key: password
        volumeMounts:
        - name: mongodb-data
          mountPath: /data/db
        - name: mongodb-keyfile
          mountPath: /etc/mongodb-keyfile
          readOnly: true
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command:
            - mongo
            - --eval
            - "db.adminCommand('ping')"
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - mongo
            - --eval
            - "db.runCommand('ping').ok"
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: mongodb-keyfile
        secret:
          secretName: mongodb-keyfile
          defaultMode: 0600
  volumeClaimTemplates:
  - metadata:
      name: mongodb-data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: fast-ssd
      resources:
        requests:
          storage: 100Gi
```

## CI/CD Pipeline

### GitOps with ArgoCD

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ecommerce

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Run tests
      run: mvn test
    
    - name: Security scan
      run: |
        mvn org.owasp:dependency-check-maven:check
        mvn spotbugs:check
  
  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    strategy:
      matrix:
        service: [user-service, product-service, cart-service, order-service, api-gateway, eureka-server]
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/${{ matrix.service }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: ./${{ matrix.service }}
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
  
  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: staging
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Update staging manifests
      run: |
        sed -i "s|IMAGE_TAG|${{ github.sha }}|g" k8s/staging/*.yaml
        
    - name: Deploy to staging
      uses: azure/k8s-deploy@v1
      with:
        manifests: |
          k8s/staging/
        images: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/user-service:${{ github.sha }}
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/product-service:${{ github.sha }}
        kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}
  
  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Update ArgoCD Application
      run: |
        # Update GitOps repository with new image tags
        curl -X POST \
          -H "Authorization: Bearer ${{ secrets.ARGOCD_TOKEN }}" \
          -H "Content-Type: application/json" \
          -d '{
            "spec": {
              "source": {
                "targetRevision": "${{ github.sha }}"
              }
            }
          }' \
          "https://argocd.example.com/api/v1/applications/ecommerce-prod"
```

### Blue-Green Deployment Strategy

```yaml
# k8s/deployment-strategies/blue-green.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: user-service-rollout
  namespace: ecommerce-prod
spec:
  replicas: 5
  strategy:
    blueGreen:
      activeService: user-service-active
      previewService: user-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: user-service-preview
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: user-service-active
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: ecommerce/user-service:1.0.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

## Environment Configuration

### Configuration Management with Helm

```yaml
# helm/ecommerce/values-production.yaml
global:
  environment: production
  imageRegistry: ghcr.io/company/ecommerce
  imagePullSecrets:
    - name: ghcr-secret

userService:
  replicaCount: 3
  image:
    tag: "1.0.0"
  resources:
    requests:
      memory: "256Mi"
      cpu: "100m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  
  config:
    jvm:
      heapSize: "384m"
      gcType: "G1GC"
    
    database:
      connectionPoolSize: 20
      maxIdleTime: 300
    
    security:
      jwtExpiration: 3600
      bcryptStrength: 12

mongodb:
  enabled: true
  architecture: replicaset
  replicaCount: 3
  auth:
    enabled: true
    existingSecret: mongodb-credentials
  persistence:
    enabled: true
    size: 100Gi
    storageClass: fast-ssd
  
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
  hosts:
    - host: api.ecommerce.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: api-gateway-tls
      hosts:
        - api.ecommerce.example.com

monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
  prometheusRule:
    enabled: true
```

### Environment-Specific Secrets

```bash
#!/bin/bash
# scripts/setup-secrets.sh

# Production secrets
kubectl create secret generic mongodb-credentials \
  --from-literal=username="$MONGODB_USERNAME" \
  --from-literal=password="$MONGODB_PASSWORD" \
  --from-literal=uri="$MONGODB_URI" \
  --namespace=ecommerce-prod

kubectl create secret generic jwt-secret \
  --from-literal=secret="$JWT_SECRET_KEY" \
  --namespace=ecommerce-prod

# TLS certificates
kubectl create secret tls api-gateway-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  --namespace=ecommerce-prod

# Image pull secrets
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username="$GITHUB_USERNAME" \
  --docker-password="$GITHUB_TOKEN" \
  --namespace=ecommerce-prod
```

## Load Balancing and Traffic Management

### Nginx Configuration

```nginx
# nginx/production.conf
upstream api_backend {
    least_conn;
    server k8s-node1:30080 max_fails=3 fail_timeout=30s;
    server k8s-node2:30080 max_fails=3 fail_timeout=30s;
    server k8s-node3:30080 max_fails=3 fail_timeout=30s;
}

server {
    listen 443 ssl http2;
    server_name api.ecommerce.example.com;
    
    ssl_certificate /etc/ssl/certs/ecommerce.crt;
    ssl_certificate_key /etc/ssl/private/ecommerce.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req zone=api burst=20 nodelay;
    
    # Timeouts
    proxy_connect_timeout 5s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
    
    location / {
        proxy_pass http://api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Circuit breaker
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
        proxy_next_upstream_tries 3;
        proxy_next_upstream_timeout 10s;
    }
    
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name api.ecommerce.example.com;
    return 301 https://$server_name$request_uri;
}
```

## Production Checklist

### Pre-Deployment Checklist

- [ ] All tests passing (unit, integration, e2e)
- [ ] Security scan completed (no high-severity vulnerabilities)
- [ ] Performance benchmarks met
- [ ] Database migrations tested
- [ ] Configuration validated
- [ ] Secrets properly configured
- [ ] SSL certificates installed
- [ ] Monitoring alerts configured
- [ ] Backup strategy implemented
- [ ] Rollback plan documented

### Post-Deployment Checklist

- [ ] All services healthy
- [ ] Database connections established
- [ ] External integrations working
- [ ] SSL certificate valid
- [ ] Monitoring dashboards showing green
- [ ] Log aggregation working
- [ ] Performance metrics within SLA
- [ ] Security headers present
- [ ] Rate limiting functional
- [ ] Error rates within threshold

## Deployment Strategies

### Rolling Deployment (Default)
- Zero downtime
- Gradual rollout
- Automatic rollback on failure
- Resource efficient

### Blue-Green Deployment
- Instant rollback capability
- Full environment testing
- Higher resource requirement
- Complete traffic switch

### Canary Deployment
- Risk mitigation
- Gradual traffic shifting
- A/B testing capability
- Complex configuration

This deployment strategy ensures reliable, secure, and scalable production deployment of the e-commerce microservices application.