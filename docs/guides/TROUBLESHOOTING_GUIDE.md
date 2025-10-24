# E-commerce Platform - Comprehensive Troubleshooting Guide

**Version**: 1.0.0
**Last Updated**: 2025-10-24
**Target Audience**: Developers, DevOps Engineers, SREs

---

## Table of Contents

1. [Quick Diagnosis](#quick-diagnosis)
2. [Common Issues](#common-issues)
3. [Service-Specific Issues](#service-specific-issues)
4. [Infrastructure Issues](#infrastructure-issues)
5. [Performance Issues](#performance-issues)
6. [Database Issues](#database-issues)
7. [Network & Connectivity](#network--connectivity)
8. [Monitoring & Observability](#monitoring--observability)
9. [Deployment Issues](#deployment-issues)
10. [Production Incidents](#production-incidents)
11. [Debug Tools & Commands](#debug-tools--commands)
12. [Escalation Guide](#escalation-guide)

---

## Quick Diagnosis

### Health Check Commands

Run these commands first to quickly identify the problem area:

```bash
# Check all service health
for port in 8761 8080 8081 8082 8083 8084; do
  echo -n "Port $port: "
  curl -s http://localhost:$port/actuator/health | jq -r '.status' || echo "DOWN"
done

# Check Docker containers
docker-compose ps

# Check Eureka service registry
curl -s http://localhost:8761/eureka/apps | grep -o '<name>[^<]*</name>' | sed 's/<[^>]*>//g' | sort -u

# Check logs for errors
docker-compose logs --tail=50 | grep -i "error\|exception\|fail"

# Check resource usage
docker stats --no-stream

# Check database connectivity
docker exec mongodb mongosh --eval "db.adminCommand('ping')" || echo "MongoDB unreachable"
```

### Decision Tree

```
Service not responding?
├─ Container not running? → Check docker-compose ps → Restart service
├─ Container running but unhealthy?
│  ├─ Check logs → docker-compose logs <service>
│  ├─ Memory/CPU issues? → docker stats
│  └─ Database connectivity? → Check MongoDB/Redis
└─ Service responding but errors?
   ├─ Check Eureka registration
   ├─ Check API Gateway routing
   └─ Review application logs
```

---

## Common Issues

### Issue 1: Services Not Starting

**Symptoms**:
- Containers exit immediately after starting
- Health checks fail continuously
- Services don't register with Eureka

**Diagnosis**:
```bash
# Check container status
docker-compose ps

# View service logs
docker-compose logs <service-name>

# Check if ports are in use
lsof -i :8080 -i :8081 -i :8082 -i :8083 -i :8084

# Verify .env file exists
test -f .env && echo "✅ .env exists" || echo "❌ .env missing"
```

**Solutions**:

1. **Missing .env file**:
   ```bash
   # Create .env from template
   cp .env.example .env
   # Edit with actual credentials
   nano .env
   ```

2. **Port conflicts**:
   ```bash
   # Find process using port
   lsof -i :8080
   # Kill conflicting process
   kill -9 <PID>
   # Or change port in docker-compose.yml
   ```

3. **Dependency services not ready**:
   ```bash
   # Ensure MongoDB and Redis start first
   docker-compose up -d mongodb redis
   sleep 10
   # Then start application services
   docker-compose up -d
   ```

4. **Build failures**:
   ```bash
   # Rebuild specific service
   docker-compose build --no-cache <service-name>
   # Or rebuild all
   ./build-all.sh
   ```

---

### Issue 2: Service Not Registered with Eureka

**Symptoms**:
- Service shows as DOWN in Eureka dashboard
- Other services can't discover the service
- API Gateway returns 503 Service Unavailable

**Diagnosis**:
```bash
# Check Eureka dashboard
curl http://localhost:8761/eureka/apps | grep -A 5 "USER-SERVICE"

# Check service logs for registration errors
docker-compose logs user-service | grep -i eureka

# Verify network connectivity
docker exec user-service ping -c 3 eureka-server
```

**Solutions**:

1. **Eureka server not ready**:
   ```bash
   # Wait for Eureka to be fully up
   while ! curl -s http://localhost:8761/actuator/health | grep -q "UP"; do
     echo "Waiting for Eureka..."
     sleep 5
   done
   # Restart service to re-register
   docker-compose restart user-service
   ```

2. **Incorrect Eureka URL in service**:
   ```bash
   # Check service configuration
   docker exec user-service env | grep EUREKA
   # Should show: eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka
   ```

3. **Network issues**:
   ```bash
   # Verify Docker network
   docker network inspect ecommerce-microservices_default
   # Recreate network if needed
   docker-compose down
   docker-compose up -d
   ```

4. **Service startup too fast**:
   ```bash
   # Add delay in docker-compose.yml under service
   depends_on:
     eureka-server:
       condition: service_healthy
   ```

---

### Issue 3: API Gateway Not Routing

**Symptoms**:
- 404 Not Found when accessing service through gateway
- 503 Service Unavailable
- Gateway shows UP but routes don't work

**Diagnosis**:
```bash
# Check gateway health and routes
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/gateway/routes | jq

# Test direct service access (bypass gateway)
curl http://localhost:8082/actuator/health  # user-service
curl http://localhost:8081/actuator/health  # product-service

# Check gateway logs
docker-compose logs api-gateway | tail -50
```

**Solutions**:

1. **Routes not configured**:
   ```bash
   # Check gateway configuration
   docker exec api-gateway cat /app/resources/application.yml | grep -A 20 routes
   ```

2. **Service discovery failing**:
   ```bash
   # Verify services are registered
   curl http://localhost:8761/eureka/apps
   # Restart gateway to refresh routes
   docker-compose restart api-gateway
   ```

3. **Load balancer not working**:
   ```bash
   # Check Ribbon/LoadBalancer configuration
   docker-compose logs api-gateway | grep -i "loadbalancer\|ribbon"
   ```

---

### Issue 4: Database Connection Failures

**Symptoms**:
- Services can't connect to MongoDB
- "Connection refused" errors in logs
- Data not persisting

**Diagnosis**:
```bash
# Check MongoDB container
docker-compose ps mongodb

# Test MongoDB connectivity
docker exec mongodb mongosh --eval "db.adminCommand('ping')"

# Check MongoDB logs
docker-compose logs mongodb | tail -50

# Verify credentials
docker exec mongodb mongosh -u admin -p <password> --authenticationDatabase admin
```

**Solutions**:

1. **MongoDB not started**:
   ```bash
   # Start MongoDB
   docker-compose up -d mongodb
   # Wait for it to be ready
   sleep 10
   ```

2. **Wrong credentials**:
   ```bash
   # Check .env file
   grep MONGO .env
   # Update service connection strings
   # Restart services
   docker-compose restart user-service product-service cart-service order-service
   ```

3. **Database not initialized**:
   ```bash
   # Connect and check databases
   docker exec -it mongodb mongosh -u admin -p <password>
   > show dbs
   > use ecommerce
   > show collections
   ```

4. **Network issues**:
   ```bash
   # Test connectivity from service container
   docker exec user-service nc -zv mongodb 27017
   ```

---

### Issue 5: Redis Cache Not Working

**Symptoms**:
- Slow product queries despite caching
- Cache miss rate very high
- Redis connection errors

**Diagnosis**:
```bash
# Check Redis container
docker-compose ps redis

# Test Redis connectivity
docker exec redis redis-cli ping

# Check cache statistics
docker exec redis redis-cli INFO stats | grep keyspace

# Monitor cache operations
docker exec redis redis-cli MONITOR
```

**Solutions**:

1. **Redis not running**:
   ```bash
   # Start Redis
   docker-compose up -d redis
   ```

2. **Cache configuration issues**:
   ```bash
   # Check product-service configuration
   docker-compose logs product-service | grep -i cache
   ```

3. **TTL too short**:
   ```bash
   # Check TTL settings
   docker exec redis redis-cli TTL "product:*"
   # Update cache configuration in product-service application.yml
   ```

4. **Memory issues**:
   ```bash
   # Check Redis memory usage
   docker exec redis redis-cli INFO memory
   # Increase maxmemory if needed
   ```

---

### Issue 6: JWT Authentication Failures

**Symptoms**:
- 401 Unauthorized errors
- "Invalid JWT token" messages
- Token expiration too fast

**Diagnosis**:
```bash
# Test user registration and login
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123","firstName":"Test","lastName":"User"}'

curl -X POST http://localhost:8082/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123"}'

# Check JWT secret configuration
docker exec user-service env | grep JWT_SECRET

# Decode JWT token (without verification)
echo "<token>" | cut -d. -f2 | base64 -d | jq
```

**Solutions**:

1. **JWT secret mismatch**:
   ```bash
   # Ensure JWT_SECRET is set in .env
   grep JWT_SECRET .env
   # Restart all services to pick up new secret
   docker-compose restart
   ```

2. **Token expired**:
   ```bash
   # Check token expiration time in decoded JWT
   # Adjust expiration in user-service application.yml
   # Default is 24 hours
   ```

3. **Clock skew**:
   ```bash
   # Check system time in containers
   docker exec user-service date
   docker exec api-gateway date
   # Synchronize if different
   ```

---

### Issue 7: Circuit Breaker Open

**Symptoms**:
- Product service returns 503 immediately
- "Circuit breaker is OPEN" messages
- No retries even after service recovers

**Diagnosis**:
```bash
# Check product service health
curl http://localhost:8081/actuator/health | jq

# Check circuit breaker metrics
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | jq

# Check recent errors
docker-compose logs product-service | grep -i "circuit\|breaker"
```

**Solutions**:

1. **High error rate**:
   ```bash
   # Check what's causing errors
   docker-compose logs product-service | tail -100
   # Fix underlying issue (e.g., database connectivity)
   ```

2. **Reset circuit breaker**:
   ```bash
   # Circuit breaker will auto-recover after configured time
   # Or restart service to reset
   docker-compose restart product-service
   ```

3. **Adjust circuit breaker settings**:
   ```bash
   # Edit product-service application.yml
   # Increase failure rate threshold or wait duration
   # Rebuild and restart
   ```

---

## Service-Specific Issues

### User Service Issues

#### Issue: Password Validation Failing

**Symptoms**: Registration fails with "Password does not meet requirements"

**Solution**:
```bash
# Password must meet criteria:
# - Minimum 8 characters
# - At least one uppercase letter
# - At least one lowercase letter
# - At least one number
# - At least one special character

# Test with compliant password:
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecureP@ss123","firstName":"John","lastName":"Doe"}'
```

#### Issue: Email Already Registered

**Symptoms**: Registration fails with 409 Conflict

**Solution**:
```bash
# Check if user exists in database
docker exec mongodb mongosh -u admin -p <password> --eval "
  use ecommerce;
  db.users.findOne({email: 'user@example.com'});
"

# Delete test user if needed
docker exec mongodb mongosh -u admin -p <password> --eval "
  use ecommerce;
  db.users.deleteOne({email: 'user@example.com'});
"
```

---

### Product Service Issues

#### Issue: Cache Miss on Every Request

**Symptoms**: Slow queries despite Redis caching enabled

**Diagnosis**:
```bash
# Monitor Redis cache hits/misses
docker exec redis redis-cli INFO stats | grep keyspace_hits

# Check if caching is enabled
docker-compose logs product-service | grep -i cache
```

**Solution**:
```bash
# Verify @Cacheable annotations in code
# Check Redis keys
docker exec redis redis-cli KEYS "product:*"

# Manually test cache
curl http://localhost:8081/api/products/507f1f77bcf86cd799439011
curl http://localhost:8081/api/products/507f1f77bcf86cd799439011
# Second request should be faster
```

#### Issue: Search Not Returning Results

**Symptoms**: Product search returns empty results

**Solution**:
```bash
# Check if products exist
curl http://localhost:8081/api/products?page=0&size=10

# Test search with known product name
curl "http://localhost:8081/api/products/search?name=laptop"

# Check MongoDB text indexes
docker exec mongodb mongosh -u admin -p <password> --eval "
  use ecommerce;
  db.products.getIndexes();
"
```

---

### Cart Service Issues

#### Issue: Items Disappearing from Cart

**Symptoms**: Added items not persisting in cart

**Solution**:
```bash
# Check user authentication
# Cart is tied to user JWT token

# Verify cart data in database
docker exec mongodb mongosh -u admin -p <password> --eval "
  use ecommerce;
  db.carts.find({userId: 'USER_ID_HERE'}).pretty();
"

# Check session management
docker-compose logs cart-service | grep -i session
```

---

### Order Service Issues

#### Issue: Payment Processing Failing

**Symptoms**: Orders created but payment fails

**Diagnosis**:
```bash
# Check Stripe configuration
docker exec order-service env | grep STRIPE

# Check order service logs
docker-compose logs order-service | grep -i payment
```

**Solution**:
```bash
# Verify Stripe API key in .env
grep STRIPE_API_KEY .env

# Test with Stripe test tokens
# Use tok_visa for successful test payment
curl -X POST http://localhost:8084/api/orders \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"paymentToken":"tok_visa","shippingAddress":"123 Main St"}'
```

---

## Infrastructure Issues

### Docker Issues

#### Issue: Out of Disk Space

**Symptoms**: "no space left on device" errors

**Solution**:
```bash
# Check disk usage
df -h

# Clean up Docker resources
docker system prune -a --volumes

# Remove unused images
docker image prune -a

# Remove stopped containers
docker container prune

# Check Docker disk usage
docker system df
```

#### Issue: Docker Daemon Not Responding

**Symptoms**: "Cannot connect to Docker daemon"

**Solution**:
```bash
# Check Docker service
sudo systemctl status docker

# Restart Docker
sudo systemctl restart docker

# Check Docker socket
ls -la /var/run/docker.sock
```

---

### Kubernetes Issues

#### Issue: Pods Stuck in Pending

**Symptoms**: Pods not scheduling

**Diagnosis**:
```bash
# Check pod status
kubectl get pods -n ecommerce

# Describe pending pod
kubectl describe pod <pod-name> -n ecommerce

# Check node resources
kubectl top nodes
```

**Solution**:
```bash
# Increase node resources or scale down replicas
kubectl scale deployment user-service --replicas=1 -n ecommerce

# Check for resource limits
kubectl describe deployment user-service -n ecommerce
```

#### Issue: ImagePullBackOff

**Symptoms**: Pods can't pull Docker images

**Solution**:
```bash
# Check image exists
docker pull docker.io/<username>/ecommerce-user-service:<tag>

# Check image pull secrets
kubectl get secrets -n ecommerce

# Create image pull secret if needed
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=<username> \
  --docker-password=<password> \
  -n ecommerce
```

---

## Performance Issues

### Issue: High Response Times

**Symptoms**: APIs taking >1 second to respond

**Diagnosis**:
```bash
# Measure response times
time curl http://localhost:8080/api/products

# Check service metrics
curl http://localhost:8081/actuator/metrics/http.server.requests | jq

# Monitor with Apache Bench
ab -n 100 -c 10 http://localhost:8080/api/products
```

**Solutions**:

1. **Database query optimization**:
   ```bash
   # Check slow queries in MongoDB
   docker exec mongodb mongosh -u admin -p <password> --eval "
     db.setProfilingLevel(2);
     db.system.profile.find().sort({millis:-1}).limit(5).pretty();
   "
   ```

2. **Enable caching**:
   ```bash
   # Verify Redis caching is working
   docker stats redis
   ```

3. **Scale services**:
   ```bash
   # Increase replicas in Kubernetes
   kubectl scale deployment product-service --replicas=3 -n ecommerce
   ```

---

### Issue: High Memory Usage

**Symptoms**: Services crashing with OOMKilled

**Diagnosis**:
```bash
# Check memory usage
docker stats

# Check JVM heap usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used | jq
```

**Solution**:
```bash
# Increase container memory limits in docker-compose.yml
services:
  product-service:
    deploy:
      resources:
        limits:
          memory: 1G

# Adjust JVM heap size
environment:
  - JAVA_OPTS=-Xms512m -Xmx1024m
```

---

## Database Issues

### MongoDB Connection Pool Exhausted

**Symptoms**: "Connection timeout" errors under load

**Solution**:
```bash
# Check current connections
docker exec mongodb mongosh -u admin -p <password> --eval "
  db.serverStatus().connections
"

# Increase connection pool size in service application.yml
spring:
  data:
    mongodb:
      max-connection-pool-size: 100
```

---

### Data Inconsistency

**Symptoms**: Data not matching between services

**Diagnosis**:
```bash
# Check data in each collection
docker exec mongodb mongosh -u admin -p <password> --eval "
  use ecommerce;
  db.users.count();
  db.products.count();
  db.orders.count();
"
```

**Solution**:
```bash
# Implement eventual consistency patterns
# Check for failed events or messages
# Replay events if needed
```

---

## Network & Connectivity

### Issue: Inter-Service Communication Failing

**Symptoms**: Services can't reach each other

**Diagnosis**:
```bash
# Check Docker network
docker network inspect ecommerce-microservices_default

# Test connectivity between containers
docker exec user-service ping -c 3 product-service
docker exec user-service nc -zv product-service 8081
```

**Solution**:
```bash
# Ensure all services on same network
# Recreate network
docker-compose down
docker-compose up -d
```

---

### Issue: External API Calls Failing

**Symptoms**: Stripe/Email notifications not working

**Diagnosis**:
```bash
# Test external connectivity from container
docker exec order-service curl -I https://api.stripe.com

# Check proxy/firewall settings
docker exec order-service env | grep -i proxy
```

**Solution**:
```bash
# Configure proxy if needed
environment:
  - HTTP_PROXY=http://proxy:8080
  - HTTPS_PROXY=http://proxy:8080
```

---

## Monitoring & Observability

### Issue: Metrics Not Appearing in Prometheus

**Symptoms**: No data in Grafana dashboards

**Diagnosis**:
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health, lastError}'

# Check service metrics endpoint
curl http://localhost:8081/actuator/prometheus
```

**Solution**:
```bash
# Verify prometheus.yml configuration
docker exec prometheus cat /etc/prometheus/prometheus.yml

# Restart Prometheus
docker-compose restart prometheus

# Check service has actuator dependency
grep spring-boot-starter-actuator pom.xml
```

---

### Issue: Traces Not Showing in Tempo

**Symptoms**: No distributed traces available

**Diagnosis**:
```bash
# Check OpenTelemetry collector
docker-compose logs otel-collector

# Check Tempo
curl http://localhost:3200/ready

# Verify trace propagation
curl -v http://localhost:8080/api/products | grep traceparent
```

**Solution**:
```bash
# Ensure OpenTelemetry agent configured
# Check OTLP_ENDPOINT environment variable
docker exec user-service env | grep OTEL
```

---

## Deployment Issues

### Issue: Rolling Update Failing

**Symptoms**: New pods not becoming ready

**Diagnosis**:
```bash
# Check rollout status
kubectl rollout status deployment user-service -n ecommerce

# Check pod events
kubectl get events -n ecommerce --sort-by='.lastTimestamp'
```

**Solution**:
```bash
# Rollback to previous version
kubectl rollout undo deployment user-service -n ecommerce

# Fix issue and redeploy
```

---

### Issue: Smoke Tests Failing After Deployment

**Symptoms**: Deployment successful but tests fail

**Solution**:
```bash
# Run smoke tests manually
./scripts/deployment/smoke-tests.sh staging

# Check specific failing test
curl http://localhost:8080/actuator/health

# Review logs for errors
docker-compose logs -f
```

---

## Production Incidents

### Critical: Complete Service Outage

**Immediate Actions**:
```bash
1. Check service status
   kubectl get pods -n ecommerce

2. Check recent deployments
   kubectl rollout history deployment user-service -n ecommerce

3. Rollback if needed
   kubectl rollout undo deployment --all -n ecommerce

4. Check logs
   kubectl logs -l app=user-service -n ecommerce --tail=100

5. Notify stakeholders
   # Use incident management system
```

---

### High: Database Connection Loss

**Immediate Actions**:
```bash
1. Check MongoDB status
   kubectl get pods -n ecommerce | grep mongo

2. Check connectivity
   kubectl exec -it user-service -n ecommerce -- nc -zv mongodb 27017

3. Restart MongoDB if needed
   kubectl delete pod mongodb-0 -n ecommerce

4. Monitor recovery
   watch kubectl get pods -n ecommerce
```

---

## Debug Tools & Commands

### Useful Docker Commands

```bash
# View all container logs
docker-compose logs -f

# View specific service logs with timestamps
docker-compose logs -f --timestamps user-service

# Check container resource usage
docker stats

# Inspect container
docker inspect <container-id>

# Execute command in container
docker exec -it <container> /bin/sh

# Copy files from container
docker cp <container>:/path/to/file ./local/path
```

---

### Useful Kubernetes Commands

```bash
# Get pod logs
kubectl logs -f <pod-name> -n ecommerce

# Get previous pod logs (after crash)
kubectl logs <pod-name> -n ecommerce --previous

# Execute command in pod
kubectl exec -it <pod-name> -n ecommerce -- /bin/sh

# Port forward to pod
kubectl port-forward <pod-name> 8080:8080 -n ecommerce

# Check resource usage
kubectl top pods -n ecommerce
kubectl top nodes
```

---

### Useful MongoDB Commands

```bash
# Connect to MongoDB
docker exec -it mongodb mongosh -u admin -p <password>

# List databases
> show dbs

# Use database
> use ecommerce

# List collections
> show collections

# Count documents
> db.users.count()

# Find documents
> db.products.find().pretty()

# Check indexes
> db.products.getIndexes()

# Database stats
> db.stats()
```

---

### Useful Redis Commands

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Check all keys
> KEYS *

# Get key value
> GET product:123

# Check key TTL
> TTL product:123

# Monitor commands
> MONITOR

# Get info
> INFO
```

---

## Escalation Guide

### Severity Levels

**P0 - Critical** (Complete service outage):
- Immediate escalation to on-call engineer
- Incident commander required
- Executive notification

**P1 - High** (Major functionality broken):
- Escalate to service team lead
- Create incident ticket
- Start investigation immediately

**P2 - Medium** (Minor functionality impaired):
- Create support ticket
- Investigate during business hours
- Document in runbook

**P3 - Low** (Cosmetic or minor issues):
- Create enhancement ticket
- Schedule for next sprint
- Document workaround

---

### Contact Information

**On-Call Rotation**: See PagerDuty
**Service Owners**:
- User Service: Team Auth
- Product Service: Team Catalog
- Cart Service: Team Commerce
- Order Service: Team Commerce

**Escalation Path**:
1. Service Team → Team Lead
2. Team Lead → Engineering Manager
3. Engineering Manager → VP Engineering
4. VP Engineering → CTO

---

## Additional Resources

- [Deployment Automation Guide](../deployment/DEPLOYMENT_AUTOMATION_GUIDE.md)
- [API Documentation](../api/API_DOCUMENTATION_GUIDE.md)
- [Monitoring Strategy](../../monitoring/MONITORING_STRATEGY.md)
- [Architecture Documentation](../architecture/ARCHITECTURE.md)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-24
**Maintained By**: SRE Team
