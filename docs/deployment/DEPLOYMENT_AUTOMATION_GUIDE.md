# E-commerce Platform - Deployment Automation Guide

**Version**: 1.0.0
**Last Updated**: 2025-10-21
**Target Audience**: DevOps Engineers, SREs, Platform Engineers

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Deployment Scripts](#deployment-scripts)
4. [Staging Deployment](#staging-deployment)
5. [Production Deployment](#production-deployment)
6. [Smoke Tests](#smoke-tests)
7. [Rollback Procedures](#rollback-procedures)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

---

## Overview

The E-commerce Platform includes comprehensive deployment automation scripts for staging and production environments. These scripts support multiple deployment methods:

- **Docker Compose** - Local development and staging
- **Kubernetes** - Production-grade orchestration
- **Helm** - Kubernetes package management

### Key Features

✅ **Multi-Environment Support** - Staging and production configurations
✅ **Production Safeguards** - Confirmation required, no 'latest' tags
✅ **Automated Health Checks** - Verify service availability
✅ **Comprehensive Smoke Tests** - 10 automated test scenarios
✅ **Rollback Capability** - Quick recovery from failed deployments
✅ **Zero-Downtime Deployments** - Kubernetes rolling updates

---

## Prerequisites

### Required Tools

- **Docker** 20.10+ and Docker Compose v2
- **kubectl** 1.25+ (for Kubernetes deployments)
- **helm** 3.10+ (for Helm deployments)
- **curl** (for health checks and smoke tests)
- **bash** 4.0+ (deployment scripts)

### Access Requirements

**Staging Environment**:
- Docker daemon access
- kubectl access to staging cluster (if using Kubernetes)

**Production Environment**:
- Docker Hub credentials (if using container registry)
- kubectl access to production cluster with appropriate RBAC
- Helm access (if using Helm deployments)

### Environment Variables

Create `.env` file with required credentials:

```bash
# MongoDB Configuration
MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=<secure_password>

# Redis Configuration
REDIS_PASSWORD=<redis_password>

# JWT Configuration
JWT_SECRET=<jwt_secret>

# AppDynamics Configuration (optional)
APPDYNAMICS_CONTROLLER_HOST_NAME=<controller_host>
APPDYNAMICS_AGENT_ACCOUNT_NAME=<account_name>
APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=<access_key>
```

---

## Deployment Scripts

### Script Locations

All deployment scripts are located in `scripts/deployment/`:

```
scripts/deployment/
├── deploy-staging.sh      # Staging environment deployment
├── deploy-production.sh   # Production environment deployment
└── smoke-tests.sh         # Automated smoke tests
```

### Script Permissions

Ensure scripts are executable:

```bash
chmod +x scripts/deployment/*.sh
```

---

## Staging Deployment

### Quick Start

**Docker Compose Deployment** (Default):
```bash
# Deploy to staging with Docker Compose
./scripts/deployment/deploy-staging.sh
```

**Kubernetes Deployment**:
```bash
# Deploy to Kubernetes staging cluster
DEPLOY_METHOD=kubernetes \
NAMESPACE=ecommerce-staging \
IMAGE_TAG=main-abc1234 \
./scripts/deployment/deploy-staging.sh
```

**Helm Deployment**:
```bash
# Deploy using Helm
DEPLOY_METHOD=helm \
NAMESPACE=ecommerce-staging \
IMAGE_TAG=v1.0.0 \
./scripts/deployment/deploy-staging.sh
```

### Configuration Options

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `DEPLOY_METHOD` | `docker-compose` | Deployment method (docker-compose, kubernetes, helm) |
| `NAMESPACE` | `ecommerce-staging` | Kubernetes namespace |
| `IMAGE_TAG` | `latest` | Docker image tag |
| `DOCKER_REGISTRY` | `docker.io` | Container registry |
| `DOCKER_USERNAME` | Required | Docker Hub username |

### Deployment Flow

1. **Validation**
   - Checks required tools (docker, kubectl, helm)
   - Validates .env file exists
   - Verifies manifests/charts are present

2. **Build Phase**
   - Builds all microservices with Maven
   - Creates Docker images

3. **Deployment Phase**
   - Deploys services based on chosen method
   - Applies configurations
   - Waits for services to start (45 seconds)

4. **Health Checks**
   - Tests Eureka Server
   - Tests API Gateway
   - Tests all microservices

5. **Smoke Tests**
   - Runs comprehensive automated tests
   - Validates end-to-end functionality

### Expected Output

```
🚀 Starting deployment to STAGING environment...

📋 Deployment Configuration:
  Environment: STAGING
  Namespace: ecommerce-staging
  Image Tag: latest
  Deployment Method: docker-compose

🐳 Deploying with Docker Compose...
🔨 Building all services...
📥 Pulling latest images...
🚀 Starting services...
⏳ Waiting for services to start...

🏥 Health Check Results:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ eureka-server (port 8761)
✅ api-gateway (port 8080)
✅ user-service (port 8082)
✅ product-service (port 8081)
✅ cart-service (port 8083)
✅ order-service (port 8084)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Docker Compose deployment complete

🎉 Staging deployment completed successfully!

🧪 Running smoke tests...
✅ All smoke tests PASSED!
```

---

## Production Deployment

### Production Safeguards

Production deployments have **strict requirements**:

1. ✅ **Explicit Confirmation** - Must set `PRODUCTION_DEPLOY_CONFIRMED=true`
2. ✅ **No 'latest' Tags** - Must specify version tag (e.g., v1.0.0)
3. ✅ **Docker Username Required** - Cannot use default placeholder
4. ✅ **Interactive Confirmation** - Must type "DEPLOY" to confirm
5. ✅ **Cluster Verification** - Confirms correct Kubernetes context

### Quick Start

```bash
# Production deployment with Kubernetes (recommended)
PRODUCTION_DEPLOY_CONFIRMED=true \
DEPLOY_METHOD=kubernetes \
IMAGE_TAG=v1.0.0 \
DOCKER_USERNAME=mycompany \
NAMESPACE=ecommerce \
./scripts/deployment/deploy-production.sh
```

### Production Deployment Flow

1. **Pre-Deployment Validation**
   - Checks `PRODUCTION_DEPLOY_CONFIRMED=true`
   - Validates `IMAGE_TAG` is not 'latest'
   - Validates `DOCKER_USERNAME` is configured
   - Confirms Kubernetes cluster context

2. **Interactive Confirmation**
   - Displays deployment configuration
   - Requires typing "DEPLOY" to proceed

3. **Backup Phase**
   - Backs up current Kubernetes deployment
   - Backs up Helm release values (if using Helm)
   - Saves to timestamped files

4. **Deployment Phase**
   - Applies Kubernetes manifests
   - Updates image tags to specific version
   - Waits for rolling update (10-minute timeout)

5. **Verification Phase**
   - Runs production smoke tests
   - Validates all health endpoints
   - Checks response times

6. **Post-Deployment**
   - Displays deployment summary
   - Provides rollback instructions
   - Lists post-deployment tasks

### Example Production Deployment

```bash
$ PRODUCTION_DEPLOY_CONFIRMED=true \
  DEPLOY_METHOD=kubernetes \
  IMAGE_TAG=v1.2.0 \
  DOCKER_USERNAME=acme \
  ./scripts/deployment/deploy-production.sh

🚀 Starting deployment to PRODUCTION environment...
⚠️  WARNING: This will deploy to PRODUCTION

📋 Deployment Configuration:
  Environment: PRODUCTION
  Namespace: ecommerce
  Image Tag: v1.2.0
  Docker Registry: docker.io/acme
  Deployment Method: kubernetes

⚠️  You are about to deploy to PRODUCTION with image tag: v1.2.0

Are you absolutely sure? (type 'DEPLOY' to confirm): DEPLOY

✅ Confirmation received, proceeding with deployment...

☸️  Deploying to Kubernetes (PRODUCTION)...

🔍 Current Kubernetes context:
arn:aws:eks:us-east-1:123456789:cluster/production

Is this the correct PRODUCTION cluster? (yes/no): yes

💾 Backing up current deployment...
📦 Applying Kubernetes manifests...
🏷️  Updating image tags to v1.2.0...
  Updating user-service...
  Updating product-service...
  Updating cart-service...
  Updating order-service...
  Updating api-gateway...
  Updating eureka-server...

⏳ Waiting for deployment to complete (timeout: 10 minutes)...
deployment "user-service" successfully rolled out
deployment "product-service" successfully rolled out
deployment "cart-service" successfully rolled out
deployment "order-service" successfully rolled out
deployment "api-gateway" successfully rolled out
deployment "eureka-server" successfully rolled out

🔍 Verifying pod status...
[Pod status output]

✅ Kubernetes deployment complete

🎉 Production deployment completed successfully!

🧪 Running production smoke tests...
✅ All smoke tests passed

📊 Deployment Summary:
  ✅ Environment: PRODUCTION
  ✅ Deployment Method: kubernetes
  ✅ Image Tag: v1.2.0
  ✅ Namespace: ecommerce
```

### Rollback on Failure

If smoke tests fail, you'll be prompted to rollback:

```
❌ Smoke tests failed!

Do you want to rollback? (yes/no): yes

🔄 Initiating rollback...
Rolling back Kubernetes deployments...
✅ Rollback initiated
```

---

## Smoke Tests

### Overview

The smoke test suite includes **10 comprehensive tests** to validate deployment health:

1. **Eureka Service Discovery** - Verifies service registration
2. **API Gateway Health** - Tests gateway endpoint
3. **User Service Health** - Validates authentication service
4. **Product Service Health** - Tests catalog service
5. **Cart Service Health** - Validates shopping cart
6. **Order Service Health** - Tests order processing
7. **Database Connectivity** - Verifies MongoDB connection
8. **Monitoring Stack** - Checks Prometheus and Grafana
9. **End-to-End Workflow** - Tests API Gateway routing
10. **Response Time Validation** - Ensures performance SLA

### Running Smoke Tests

**Standalone Execution**:
```bash
# Test staging environment
./scripts/deployment/smoke-tests.sh staging

# Test production environment
./scripts/deployment/smoke-tests.sh production
```

**With Custom Ports**:
```bash
# Custom ports for staging
API_GATEWAY_PORT=8081 \
EUREKA_PORT=8761 \
./scripts/deployment/smoke-tests.sh staging
```

### Test Output

**Success**:
```
🧪 Running smoke tests for staging environment...

1️⃣  Testing Eureka Server...
   ✅ Eureka Server is UP
   Registered services:
     - API-GATEWAY
     - CART-SERVICE
     - ORDER-SERVICE
     - PRODUCT-SERVICE
     - USER-SERVICE

2️⃣  Testing API Gateway...
   ✅ API Gateway health check PASSED

3️⃣  Testing User Service...
   ✅ User Service health check PASSED

[... additional tests ...]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All smoke tests PASSED!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Test Summary:
  Environment: staging
  Total Tests: 10
  Passed: 10
  Failed: 0
```

**Failure**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ 2 smoke test(s) FAILED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Test Summary:
  Environment: staging
  Total Tests: 10
  Passed: 8
  Failed: 2

🔍 Troubleshooting:
  1. Check service logs: docker-compose logs <service-name>
  2. Verify all containers are running: docker-compose ps
  3. Check Eureka dashboard: http://localhost:8761
  4. Review health endpoints individually
```

---

## Rollback Procedures

### Kubernetes Rollback

**Automatic Rollback** (via deployment script):
```bash
# If smoke tests fail, script will prompt for rollback
```

**Manual Rollback**:
```bash
# Rollback all deployments in namespace
kubectl rollout undo deployment --all -n ecommerce

# Rollback specific deployment
kubectl rollout undo deployment user-service -n ecommerce

# Rollback to specific revision
kubectl rollout undo deployment user-service --to-revision=2 -n ecommerce

# Check rollout history
kubectl rollout history deployment user-service -n ecommerce
```

### Helm Rollback

```bash
# List releases
helm list -n ecommerce

# Rollback to previous release
helm rollback ecommerce -n ecommerce

# Rollback to specific revision
helm rollback ecommerce 3 -n ecommerce

# View release history
helm history ecommerce -n ecommerce
```

### Docker Compose Rollback

```bash
# Stop current deployment
docker compose down

# Restore from backup
docker compose -f docker-compose.yml up -d

# Check logs
docker compose logs -f
```

---

## Troubleshooting

### Common Issues

#### 1. Services Not Starting

**Symptom**: Health checks fail after deployment

**Diagnosis**:
```bash
# Check container status
docker compose ps

# View logs
docker compose logs <service-name>

# Check Eureka registration
curl http://localhost:8761/eureka/apps
```

**Solutions**:
- Wait longer (services may need 60-90 seconds)
- Check .env file credentials
- Verify MongoDB and Redis are running
- Check for port conflicts

#### 2. Database Connection Errors

**Symptom**: Services fail with MongoDB connection errors

**Diagnosis**:
```bash
# Check MongoDB container
docker ps | grep mongo

# Test MongoDB connection
docker exec -it mongodb mongosh -u admin -p
```

**Solutions**:
- Verify MONGO_ROOT_PASSWORD in .env
- Ensure MongoDB container started first
- Check MongoDB connection string in application.yml

#### 3. Image Tag Not Found

**Symptom**: Kubernetes deployment fails with ImagePullBackOff

**Diagnosis**:
```bash
# Check pod events
kubectl describe pod <pod-name> -n ecommerce

# Verify image exists
docker pull <registry>/<username>/ecommerce-user-service:<tag>
```

**Solutions**:
- Verify image was built and pushed
- Check IMAGE_TAG matches pushed image
- Verify DOCKER_USERNAME is correct
- Check image registry credentials

#### 4. Eureka Registration Timeout

**Symptom**: Services don't appear in Eureka dashboard

**Diagnosis**:
```bash
# Check Eureka logs
docker compose logs eureka-server

# Check service logs for registration errors
docker compose logs user-service | grep eureka
```

**Solutions**:
- Wait 60 seconds for registration
- Verify eureka.client.service-url.defaultZone is correct
- Check network connectivity between services
- Restart services if needed

#### 5. Smoke Tests Failing

**Symptom**: Deployment successful but smoke tests fail

**Diagnosis**:
```bash
# Run tests with verbose output
bash -x scripts/deployment/smoke-tests.sh staging

# Check individual health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
```

**Solutions**:
- Verify correct ports in environment variables
- Check firewall rules
- Ensure services have fully started
- Review service logs for errors

---

## Best Practices

### Development Workflow

1. **Feature Development**
   ```bash
   # Develop on feature branch
   git checkout -b feature/new-feature

   # Test locally with Docker Compose
   docker compose up -d

   # Run smoke tests
   ./scripts/deployment/smoke-tests.sh staging
   ```

2. **Staging Deployment**
   ```bash
   # Merge to main
   git checkout main && git merge feature/new-feature

   # Deploy to staging
   IMAGE_TAG=main-$(git rev-parse --short HEAD) \
   ./scripts/deployment/deploy-staging.sh

   # Validate with smoke tests
   ./scripts/deployment/smoke-tests.sh staging
   ```

3. **Production Release**
   ```bash
   # Tag release
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0

   # Build and push images
   # (automated via CI/CD)

   # Deploy to production
   PRODUCTION_DEPLOY_CONFIRMED=true \
   DEPLOY_METHOD=kubernetes \
   IMAGE_TAG=v1.0.0 \
   ./scripts/deployment/deploy-production.sh
   ```

### Security Recommendations

✅ **Never commit .env files** - Always gitignore
✅ **Use strong passwords** - Generate with `openssl rand -base64 32`
✅ **Rotate credentials** - Regularly update production secrets
✅ **Limit access** - Use RBAC for Kubernetes namespaces
✅ **Use secrets management** - Kubernetes Secrets or external vaults
✅ **Enable TLS** - HTTPS for all production endpoints
✅ **Monitor deployments** - Set up alerts for failed deployments

### Performance Optimization

- Use **health check intervals** appropriately (30s for staging, 10s for production)
- Configure **resource limits** in Kubernetes manifests
- Enable **horizontal pod autoscaling** for production
- Use **readiness and liveness probes** correctly
- Monitor **response times** and set SLA thresholds

---

## Additional Resources

- [Architecture Documentation](../architecture/ARCHITECTURE.md)
- [Deployment Strategy](../DEPLOYMENT_STRATEGY.md)
- [Monitoring Guide](../../monitoring/MONITORING_STRATEGY.md)
- [Troubleshooting Guide](../guides/TRACING_ACCESS_GUIDE.md)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-21
**Maintained By**: Platform Engineering Team
