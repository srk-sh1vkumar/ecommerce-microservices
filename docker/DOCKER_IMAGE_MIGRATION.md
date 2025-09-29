# Docker Base Image Migration Guide

## Migration from OpenJDK to Eclipse Temurin

### Overview
The `openjdk:17-jre-slim` Docker image has been deprecated and removed from Docker Hub. We have migrated to use `eclipse-temurin:17-jre-alpine` which is the official OpenJDK distribution maintained by the Eclipse Foundation.

### Changes Made

#### Base Images Updated
- **Build Stage**: `openjdk:17-jdk-slim` → `eclipse-temurin:17-jdk-alpine`
- **Runtime Stage**: `openjdk:17-jre-slim` → `eclipse-temurin:17-jre-alpine`

#### Package Manager Commands Updated
Since we moved from Debian-based (`slim`) to Alpine Linux, package management commands have been updated:

**Before (Debian/Ubuntu):**
```dockerfile
RUN apt-get update && \
    apt-get install -y curl unzip maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
```

**After (Alpine Linux):**
```dockerfile
RUN apk add --no-cache curl unzip maven
```

#### User Creation Commands Updated
Alpine Linux uses different syntax for user management:

**Before (Debian/Ubuntu):**
```dockerfile
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser
```

**After (Alpine Linux):**
```dockerfile
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
```

### Benefits of Eclipse Temurin

1. **Official OpenJDK Distribution**: Maintained by Eclipse Foundation
2. **Regular Security Updates**: Actively maintained with security patches
3. **Smaller Image Size**: Alpine Linux base provides smaller footprint
4. **Better Performance**: Optimized for containerized environments
5. **Long-term Support**: Guaranteed support lifecycle

### Image Size Comparison

| Image | Size | Base OS |
|-------|------|---------|
| `openjdk:17-jre-slim` | ~220MB | Debian |
| `eclipse-temurin:17-jre-alpine` | ~165MB | Alpine Linux |

**Size Reduction**: ~55MB smaller (25% reduction)

### Compatibility Notes

1. **Java Runtime**: Fully compatible - same OpenJDK 17 runtime
2. **Application Code**: No changes required
3. **JVM Arguments**: All existing JVM configurations remain valid
4. **AppDynamics Agent**: Fully compatible with Alpine Linux
5. **Health Checks**: All existing health check endpoints work

### Files Updated

1. `docker/Dockerfile.template` - Template for all services
2. `order-service/Dockerfile` - Order service specific Dockerfile
3. `docs/DEPLOYMENT_STRATEGY.md` - Documentation updated

### Verification Steps

After the migration, verify the changes work correctly:

```bash
# Build a service with the new base image
cd docker
./build-with-appdynamics.sh order-service 8084

# Verify the image uses the correct base
docker history ecommerce/order-service:latest

# Test the service starts correctly
docker run -d --name test-order-service -p 8084:8084 ecommerce/order-service:latest

# Check health endpoint
curl http://localhost:8084/actuator/health

# Clean up
docker stop test-order-service && docker rm test-order-service
```

### Rollback Strategy

If issues arise, you can quickly rollback by reverting the base images:

```dockerfile
# Temporary rollback (use only if necessary)
FROM openjdk:17-jdk-slim AS builder
# Runtime stage  
FROM openjdk:17-jre-slim
```

However, this is not recommended as the OpenJDK images are deprecated.

### Alternative Images

If Eclipse Temurin doesn't work for your environment, consider these alternatives:

1. **Amazon Corretto**: `amazoncorretto:17-alpine-jre`
2. **Microsoft OpenJDK**: `mcr.microsoft.com/openjdk/jdk:17-ubuntu`
3. **Red Hat UBI**: `registry.access.redhat.com/ubi8/openjdk-17-runtime`

### Security Considerations

1. **Reduced Attack Surface**: Alpine Linux has fewer packages by default
2. **Security Updates**: Eclipse Temurin receives regular security patches
3. **CVE Scanning**: Regularly scan images for vulnerabilities
4. **Base Image Updates**: Keep base images updated to latest versions

### Performance Impact

- **Startup Time**: Potentially faster due to smaller image size
- **Memory Usage**: Similar or slightly better
- **Network**: Faster image pulls due to smaller size
- **Storage**: Less disk space required

The migration to Eclipse Temurin provides better long-term maintainability and security while reducing image size and improving performance.