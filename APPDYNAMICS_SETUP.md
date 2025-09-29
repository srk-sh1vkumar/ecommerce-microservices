# AppDynamics OAuth2 Integration Setup Guide

## Overview
This guide explains how to configure the intelligent monitoring service to connect to AppDynamics using OAuth2 authentication for pulling snapshot data and business transaction metrics.

## Prerequisites

1. **AppDynamics SaaS Controller Access**
   - Controller URL (e.g., `mycompany.saas.appdynamics.com`)
   - Valid account with API access permissions

2. **OAuth2 Client Credentials**
   - Client ID (from AppDynamics OAuth2 settings)
   - Client Secret (from AppDynamics OAuth2 settings)

## Step 1: Create OAuth2 Client in AppDynamics

### In AppDynamics Controller:

1. **Navigate to OAuth2 Settings**:
   ```
   Settings > OAuth2 > Client Credentials
   ```

2. **Create New Client**:
   - Name: `ecommerce-monitoring-service`
   - Description: `Intelligent monitoring service for automated error detection`
   - Scopes: `read` (minimum required)
   - Grant Type: `client_credentials`

3. **Save Client Credentials**:
   - Copy the generated **Client ID**
   - Copy the generated **Client Secret**
   - ⚠️ **Important**: Save the client secret immediately - it won't be shown again!

## Step 2: Configure Environment Variables

### Option A: Using .env file

Create or update `.env.appdynamics` file:

```bash
# AppDynamics Controller Configuration
APPDYNAMICS_CONTROLLER_HOST_NAME=your-account.saas.appdynamics.com
APPDYNAMICS_CONTROLLER_PORT=443

# OAuth2 Authentication (Primary Method)
APPDYNAMICS_OAUTH2_CLIENT_ID=your-oauth2-client-id
APPDYNAMICS_OAUTH2_CLIENT_SECRET=your-oauth2-client-secret

# Application Configuration
APPDYNAMICS_APPLICATION_NAME=ecommerce-microservices
```

### Option B: Using Docker Environment Variables

Update your `docker-compose.yml`:

```yaml
intelligent-monitoring-service:
  environment:
    - APPDYNAMICS_CONTROLLER_HOST_NAME=your-account.saas.appdynamics.com
    - APPDYNAMICS_OAUTH2_CLIENT_ID=your-oauth2-client-id
    - APPDYNAMICS_OAUTH2_CLIENT_SECRET=your-oauth2-client-secret
    - APPDYNAMICS_APPLICATION_NAME=ecommerce-microservices
```

### Option C: Using System Environment Variables

```bash
export APPDYNAMICS_CONTROLLER_HOST_NAME="your-account.saas.appdynamics.com"
export APPDYNAMICS_OAUTH2_CLIENT_ID="your-oauth2-client-id"
export APPDYNAMICS_OAUTH2_CLIENT_SECRET="your-oauth2-client-secret"
export APPDYNAMICS_APPLICATION_NAME="ecommerce-microservices"
```

## Step 3: Verify Configuration

### Start the Monitoring Service

```bash
# Using Docker Compose
docker-compose up intelligent-monitoring-service

# Or build and run locally
cd intelligent-monitoring-service
./mvnw spring-boot:run
```

### Check Health Status

```bash
# Basic health check
curl http://localhost:8090/api/monitoring/appdynamics/health

# Expected response:
{
  "configured": true,
  "healthy": true,
  "status": "UP",
  "authentication": {
    "hasToken": true,
    "tokenType": "Bearer",
    "isExpired": false,
    "expiresAt": "2024-01-15T10:30:00",
    "secondsUntilExpiry": 3542
  }
}
```

### Test API Connectivity

```bash
curl -X POST http://localhost:8090/api/monitoring/appdynamics/test-connection

# Expected response:
{
  "connected": true,
  "responseTime": 245,
  "status": "CONNECTED",
  "message": "Successfully connected to AppDynamics API"
}
```

## Step 4: Monitor Data Collection

### View Token Information

```bash
curl http://localhost:8090/api/monitoring/appdynamics/token/info

# Response includes token expiry and status
{
  "hasToken": true,
  "tokenType": "Bearer",
  "isExpired": false,
  "expiresAt": "2024-01-15T10:30:00",
  "secondsUntilExpiry": 3542,
  "minutesUntilExpiry": 59
}
```

### Force Token Refresh (if needed)

```bash
curl -X POST http://localhost:8090/api/monitoring/appdynamics/token/refresh

# Response confirms successful refresh
{
  "success": true,
  "message": "Token refreshed successfully",
  "refreshedAt": "2024-01-15T09:31:00"
}
```

## Step 5: Verify Data Collection

### Check Logs for Data Collection

```bash
# View intelligent monitoring service logs
docker logs intelligent-monitoring-service

# Look for successful data collection messages:
# "Fetched 15 business transactions from AppDynamics"
# "Fetched 8 error snapshots from AppDynamics" 
# "Comprehensive data collection completed: 23 events in 1250ms"
```

### Monitor Database for Collected Data

```bash
# Connect to MongoDB and check collected data
docker exec -it ecommerce-mongodb mongosh

use ecommerce

# Check monitoring events from AppDynamics
db.intelligent_monitoring_events.find({"source": "appdynamics"}).limit(5)

# Check error patterns detected
db.error_patterns.find({}).limit(5)

# Check cross-platform traces
db.cross_platform_traces.find({}).limit(5)
```

## Data Collection Schedule

The service automatically collects data on the following schedule:

| Data Type | Frequency | Endpoint |
|-----------|-----------|----------|
| Business Transactions | 1 minute | `/business-transactions` |
| Error Snapshots | 30 seconds | `/snapshots?severity=ERROR` |
| Performance Metrics | 1 minute | `/metric-data` |
| Health Violations | 30 seconds | `/healthrule-violations` |
| Comprehensive Analysis | 5 minutes | All endpoints + correlation |

## Troubleshooting

### Common Issues

1. **"OAuth2 Error: invalid_client"**
   ```
   Solution: Verify client ID and secret are correct
   Check: APPDYNAMICS_OAUTH2_CLIENT_ID and APPDYNAMICS_OAUTH2_CLIENT_SECRET
   ```

2. **"No valid OAuth2 token available"**
   ```
   Solution: Check network connectivity to AppDynamics controller
   Test: curl https://your-account.saas.appdynamics.com/controller/api/oauth/access_token
   ```

3. **"AppDynamics OAuth2 not configured"**
   ```
   Solution: Ensure environment variables are set correctly
   Check: All required environment variables are present
   ```

4. **"Token validation failed"**
   ```
   Solution: Token may be expired or invalid
   Action: Force refresh with POST /api/monitoring/appdynamics/token/refresh
   ```

### Debug Configuration

Enable debug logging by adding to `application.yml`:

```yaml
logging:
  level:
    com.ecommerce.monitoring.service.AppDynamicsAuthService: DEBUG
    com.ecommerce.monitoring.service.AppDynamicsIntegrationService: DEBUG
    org.springframework.web.client.RestTemplate: DEBUG
```

### Verify OAuth2 Client Setup

In AppDynamics Controller, verify:

1. **Client exists**: Settings > OAuth2 > Client Credentials
2. **Scopes are correct**: Minimum `read` scope required
3. **Client is enabled**: Status should be active
4. **Grant type**: Must be `client_credentials`

## Security Best Practices

1. **Secure Credential Storage**:
   - Never commit secrets to version control
   - Use environment variables or secure secret management
   - Rotate client secrets regularly

2. **Network Security**:
   - Ensure HTTPS communication to AppDynamics
   - Use TLS 1.2+ for all connections
   - Validate SSL certificates

3. **Monitoring**:
   - Monitor token expiry and refresh cycles
   - Set up alerts for authentication failures
   - Log security events for audit trails

## API Rate Limits

AppDynamics API has rate limits:

- **Default**: 100 requests per minute per client
- **Burst**: Up to 200 requests in short bursts
- **Monitoring**: Service automatically throttles requests

Configure rate limiting in `application-appdynamics.yml`:

```yaml
appdynamics:
  api:
    rate-limit: 100  # requests per minute
    retry-attempts: 3
    timeout: 30000  # 30 seconds
```

## Next Steps

1. **Monitor Dashboard**: Access admin dashboard to view AppDynamics integration status
2. **Configure Alerting**: Set up alerts for critical AppDynamics events
3. **Tune Collection**: Adjust collection intervals based on your needs
4. **Analyze Patterns**: Review automated error pattern detection and fixes

For additional support, check the intelligent monitoring service logs and AppDynamics controller logs.