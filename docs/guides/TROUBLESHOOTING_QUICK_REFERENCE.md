# Troubleshooting Quick Reference Card

**Print-friendly quick reference for common troubleshooting commands**

---

## 🚨 Quick Health Check (30 seconds)

```bash
# Check all services
for port in 8761 8080 8081 8082 8083 8084; do
  echo -n "Port $port: "
  curl -s http://localhost:$port/actuator/health | jq -r '.status' || echo "DOWN"
done

# Check containers
docker-compose ps

# Check Eureka registry
curl -s http://localhost:8761/eureka/apps | grep '<name>' | sed 's/<[^>]*>//g' | sort -u
```

---

## 📊 Service Status

```bash
# Check specific service health
curl http://localhost:8082/actuator/health | jq

# Check all registered services
curl http://localhost:8761/eureka/apps

# Check API Gateway routes
curl http://localhost:8080/actuator/gateway/routes | jq
```

---

## 🐳 Docker Commands

```bash
# View logs
docker-compose logs -f <service-name>
docker-compose logs --tail=50 | grep -i error

# Restart service
docker-compose restart <service-name>

# Rebuild service
docker-compose up -d --build <service-name>

# Check resource usage
docker stats --no-stream

# Clean up
docker system prune -a
```

---

## 🗄️ Database Commands

### MongoDB
```bash
# Connect
docker exec -it mongodb mongosh -u admin -p <password>

# Check databases
> show dbs
> use ecommerce
> show collections

# Count documents
> db.users.count()
> db.products.count()

# Find user
> db.users.findOne({email: "user@example.com"})
```

### Redis
```bash
# Connect
docker exec -it redis redis-cli

# Check keys
> KEYS *
> GET product:123
> TTL product:123

# Monitor commands
> MONITOR

# Clear all (careful!)
> FLUSHALL
```

---

## 🔍 Log Analysis

```bash
# Search for errors
docker-compose logs | grep -i "error\|exception\|fail"

# Watch logs in real-time
docker-compose logs -f | grep error

# Check specific service errors (last 100 lines)
docker-compose logs --tail=100 user-service | grep -i error

# Check MongoDB logs
docker-compose logs mongodb | tail -50
```

---

## 🌐 Network Debugging

```bash
# Test connectivity between services
docker exec user-service ping -c 3 product-service
docker exec user-service nc -zv product-service 8081

# Check ports in use
lsof -i :8080 -i :8081 -i :8082

# Kill process on port
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Test external API
docker exec order-service curl -I https://api.stripe.com
```

---

## 🔐 JWT Token Debug

```bash
# Register user
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","firstName":"Test","lastName":"User"}'

# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8082/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}' \
  | jq -r '.token')

# Decode JWT (payload only)
echo $TOKEN | cut -d. -f2 | base64 -d | jq

# Test with token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/users/profile
```

---

## ☸️ Kubernetes Commands

```bash
# Get pod status
kubectl get pods -n ecommerce

# Check pod logs
kubectl logs -f <pod-name> -n ecommerce

# Previous logs (after crash)
kubectl logs <pod-name> -n ecommerce --previous

# Describe pod
kubectl describe pod <pod-name> -n ecommerce

# Execute command
kubectl exec -it <pod-name> -n ecommerce -- /bin/sh

# Check resource usage
kubectl top pods -n ecommerce
kubectl top nodes

# Restart deployment
kubectl rollout restart deployment user-service -n ecommerce

# Rollback deployment
kubectl rollout undo deployment user-service -n ecommerce
```

---

## 📈 Performance Monitoring

```bash
# Check response times
time curl http://localhost:8080/api/products

# Load test
ab -n 100 -c 10 http://localhost:8080/api/products

# Check circuit breaker status
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | jq

# Check cache hit rate
docker exec redis redis-cli INFO stats | grep keyspace_hits
```

---

## 🔧 Common Fixes

### Service Won't Start
```bash
# Check .env exists
test -f .env && echo "✅ Exists" || echo "❌ Missing"

# Restart dependencies first
docker-compose up -d mongodb redis eureka-server
sleep 10
docker-compose up -d
```

### Eureka Registration Failed
```bash
# Wait for Eureka
while ! curl -s http://localhost:8761/actuator/health | grep -q "UP"; do
  sleep 5
done
# Restart service
docker-compose restart user-service
```

### Database Connection Failed
```bash
# Test MongoDB
docker exec mongodb mongosh --eval "db.adminCommand('ping')"

# Check credentials
grep MONGO .env

# Restart with dependencies
docker-compose restart mongodb
sleep 10
docker-compose restart user-service product-service cart-service order-service
```

### Circuit Breaker Open
```bash
# Check product service logs
docker-compose logs product-service | grep -i circuit

# Reset by restarting
docker-compose restart product-service
```

### High Memory Usage
```bash
# Check memory
docker stats --no-stream

# Restart service
docker-compose restart <service-name>

# Increase limits in docker-compose.yml
# deploy:
#   resources:
#     limits:
#       memory: 1G
```

---

## 🚀 Emergency Commands

### Complete Service Outage
```bash
# 1. Check all services
docker-compose ps

# 2. Check Eureka
curl http://localhost:8761

# 3. Restart all
docker-compose restart

# 4. If still down, full restart
docker-compose down && docker-compose up -d
```

### Database Lost Connection
```bash
# 1. Check MongoDB
docker-compose ps mongodb

# 2. Restart MongoDB
docker-compose restart mongodb

# 3. Wait for ready
sleep 10

# 4. Restart services
docker-compose restart user-service product-service cart-service order-service
```

### Kubernetes Pod CrashLoopBackOff
```bash
# 1. Check logs
kubectl logs <pod-name> -n ecommerce --previous

# 2. Describe pod
kubectl describe pod <pod-name> -n ecommerce

# 3. Delete pod (will recreate)
kubectl delete pod <pod-name> -n ecommerce

# 4. If persists, rollback
kubectl rollout undo deployment <deployment-name> -n ecommerce
```

---

## 📞 Escalation

### Severity Levels
- **P0 (Critical)**: Complete service outage → Escalate immediately
- **P1 (High)**: Major functionality broken → Escalate within 1 hour
- **P2 (Medium)**: Minor impairment → Create ticket, resolve in business hours
- **P3 (Low)**: Cosmetic issues → Create enhancement ticket

### Quick Contacts
- **On-Call Engineer**: See PagerDuty
- **Service Teams**: Check team channel
- **Emergency Hotline**: Internal wiki

---

## 🔗 Useful Links

- [Complete Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md)
- [API Documentation](../api/API_DOCUMENTATION_GUIDE.md)
- [Deployment Guide](../deployment/DEPLOYMENT_AUTOMATION_GUIDE.md)
- [Monitoring Strategy](../../monitoring/MONITORING_STRATEGY.md)

---

**Print This Page**: Keep this reference card handy for quick access to common commands.

**Last Updated**: 2025-10-24
