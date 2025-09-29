# AppDynamics Integration Setup Guide

## Overview

The ecommerce microservices platform is configured to support **AppDynamics Java Agent 25.7** (latest version) for comprehensive application performance monitoring.

## Current Status

### ✅ **FULLY IMPLEMENTED:**
- **Official AppDynamics Image**: Using `appdynamics/java-agent:25.7.0.37201`
- **Latest Agent Version**: AppDynamics Java Agent 25.7.0.37201 (latest official build)
- **Complete Environment Configuration**: All required AppDynamics environment variables
- **Multi-stage Docker Build**: Efficient agent integration from official image
- **Per-Service Configuration**: Each microservice has proper tier/node naming
- **Agent Loading Confirmed**: Successfully loading and running

### ⚠️ **REQUIRES SETUP:**
- Controller connection credentials (hostname, account, access key)
- Account access keys for reporting to AppDynamics dashboard

## Quick Setup Options

### Option 1: Configure Controller Connection (RECOMMENDED)

AppDynamics agent is already installed from official image. Just configure controller connection:

```bash
# Set controller credentials as environment variables
export APPDYNAMICS_CONTROLLER_HOST_NAME="<your-controller-host>.saas.appdynamics.com"
export APPDYNAMICS_AGENT_ACCOUNT_NAME="<your-account-name>" 
export APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY="<your-access-key>"

# Update docker-compose.yml or create docker-compose.override.yml:
version: '3.8'
services:
  order-service:
    environment:
      - APPDYNAMICS_CONTROLLER_HOST_NAME=${APPDYNAMICS_CONTROLLER_HOST_NAME}
      - APPDYNAMICS_AGENT_ACCOUNT_NAME=${APPDYNAMICS_AGENT_ACCOUNT_NAME}
      - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=${APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY}

# Restart services
docker-compose restart order-service
```

### Option 2: Manual Agent Installation

1. **Download AppDynamics Java Agent 25.7** from AppDynamics portal
2. **Place agent ZIP file** in each service directory:
   ```bash
   # Example for order-service
   cp AppServerAgent-25.7.0.zip /Users/shiva/Projects/ecommerce-microservices/order-service/
   ```
3. **Update Dockerfile** to copy local file:
   ```dockerfile
   # Replace download step with:
   COPY AppServerAgent-25.7.0.zip /tmp/AppServerAgent.zip
   RUN mkdir -p /opt/appdynamics && \
       unzip /tmp/AppServerAgent.zip -d /opt/appdynamics && \
       rm /tmp/AppServerAgent.zip && \
       chown -R appuser:appgroup /opt/appdynamics
   ```

### Option 3: Using AppDynamics SaaS/On-Premises

For enterprise installations with existing AppDynamics setup:

1. **Get download URL** from your AppDynamics admin
2. **Update docker-compose.yml** with environment variables:
   ```yaml
   order-service:
     environment:
       - APPDYNAMICS_CONTROLLER_HOST_NAME=<your-controller>
       - APPDYNAMICS_AGENT_ACCOUNT_NAME=<your-account>
       - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=<your-key>
   ```

## Configuration Details

### Environment Variables (Already Configured)

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `APPDYNAMICS_AGENT_APPLICATION_NAME` | `"ecommerce-microservices"` | Application name in AppDynamics |
| `APPDYNAMICS_AGENT_TIER_NAME` | `"order-service"` | Service tier name |
| `APPDYNAMICS_AGENT_NODE_NAME` | `"order-service-${HOSTNAME}"` | Unique node identifier |
| `APPDYNAMICS_CONTROLLER_HOST_NAME` | `""` | **REQUIRED**: Your controller hostname |
| `APPDYNAMICS_CONTROLLER_PORT` | `"443"` | Controller port (443 for SaaS) |
| `APPDYNAMICS_CONTROLLER_SSL_ENABLED` | `"true"` | Use SSL for controller connection |
| `APPDYNAMICS_AGENT_ACCOUNT_NAME` | `""` | **REQUIRED**: Your account name |
| `APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY` | `""` | **REQUIRED**: Your access key |

### Service Tiers Configuration

Each microservice is configured with appropriate tier names:

| Service | Tier Name | Node Name Pattern |
|---------|-----------|-------------------|
| **API Gateway** | `api-gateway` | `api-gateway-{hostname}` |
| **User Service** | `user-service` | `user-service-{hostname}` |
| **Product Service** | `product-service` | `product-service-{hostname}` |
| **Cart Service** | `cart-service` | `cart-service-{hostname}` |
| **Order Service** | `order-service` | `order-service-{hostname}` |
| **Eureka Server** | `eureka-server` | `eureka-server-{hostname}` |

## Verification

### Check Agent Status

```bash
# Check if agent is loaded
docker logs order-service | head -1

# Expected outputs:
# With Agent: "Starting with AppDynamics Agent 25.7..."
# Without Agent: "Starting without AppDynamics Agent (agent not found)..."
```

### Agent Files Location

Inside containers:
- **Agent Directory**: `/opt/appdynamics/`
- **Agent JAR**: `/opt/appdynamics/javaagent.jar`
- **Logs Directory**: `/app/logs/`
- **Runtime Directory**: `/opt/appdynamics/runtime/`

### Controller Registration

Once properly configured, services should appear in AppDynamics dashboard under:
- **Application**: `ecommerce-microservices`
- **Tiers**: `api-gateway`, `user-service`, `product-service`, `cart-service`, `order-service`

## Troubleshooting

### Common Issues

1. **"Starting without AppDynamics Agent"**
   - Agent JAR not found at `/opt/appdynamics/javaagent.jar`
   - Build with `APPDYNAMICS_ENABLED=true`

2. **Download Failures**
   - Invalid download URL or authentication required
   - Use manual installation method

3. **Agent Not Reporting**
   - Check controller connection settings
   - Verify account credentials
   - Check network connectivity to controller

### Enable Debug Logging

Add to `JAVA_OPTS`:
```bash
-Dappdynamics.agent.logs.level=DEBUG
```

## Production Deployment

### Docker Compose Override

Create `docker-compose.appdynamics.yml`:

```yaml
version: '3.8'
services:
  order-service:
    build:
      context: ./order-service
      args:
        APPDYNAMICS_ENABLED: "true"
        APPDYNAMICS_DOWNLOAD_URL: "${APPDYNAMICS_DOWNLOAD_URL}"
    environment:
      - APPDYNAMICS_CONTROLLER_HOST_NAME=${APPDYNAMICS_CONTROLLER_HOST_NAME}
      - APPDYNAMICS_AGENT_ACCOUNT_NAME=${APPDYNAMICS_AGENT_ACCOUNT_NAME}
      - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=${APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY}
  
  # Repeat for all services...
```

Deploy with:
```bash
docker-compose -f docker-compose.yml -f docker-compose.appdynamics.yml up -d
```

## Next Steps

1. **Obtain AppDynamics Credentials** from your organization
2. **Choose Setup Method** (download URL, manual, or enterprise)
3. **Configure Controller Connection** with proper credentials
4. **Test Agent Registration** in AppDynamics dashboard
5. **Set Up Monitoring Dashboards** and alerts

---

**Note**: This setup provides enterprise-grade APM capabilities with distributed tracing, real-time performance metrics, and comprehensive monitoring across all microservices.