# SRE Analytics Platform - Quick Reference Guide

## 🚀 Overview

This quick reference provides essential information for developers working with the SRE Analytics platform in the ecommerce-microservices project.

---

## 📂 Key Files & Locations

### Python Components
```bash
# AppDynamics Integration
/Users/shiva/Projects/ecommerce-microservices/appdynamics_metrics.py

# Demo Data Generator
/Users/shiva/Projects/ecommerce-microservices/demo_metrics_data.py

# Load Testing
/Users/shiva/Projects/ecommerce-microservices/load-generator/scripts/realistic_load_test.py
/Users/shiva/Projects/ecommerce-microservices/load-generator/scripts/health_check.py
```

### Java Services
```bash
# Core Service
/Users/shiva/Projects/ecommerce-microservices/intelligent-monitoring-service/

# Key Java Files
src/main/java/com/ecommerce/monitoring/
├── service/
│   ├── MonitoringEventService.java
│   ├── ErrorPatternAnalysisService.java
│   ├── AutomatedFixingService.java
│   ├── AppDynamicsIntegrationService.java
│   ├── OpenTelemetryIntegrationService.java
│   └── CrossPlatformCorrelationService.java
├── entity/
│   ├── MonitoringEvent.java
│   └── ErrorPattern.java
└── IntelligentMonitoringApplication.java
```

### Reports
```bash
# Sample Reports
/Users/shiva/Projects/ecommerce-microservices/reports/examples/
├── sample_sre_performance_report.html
├── sample_sre_performance_report.pdf
├── sample_sre_metrics_data.json
└── README.md
```

---

## 🔧 Quick Start Commands

### AppDynamics Metrics Collection

```bash
# Set up environment
cd /Users/shiva/Projects/ecommerce-microservices
source appdynamics-env/bin/activate  # If using venv

# Set credentials (required)
export APPDYNAMICS_CLIENT_ID="your-client-id"
export APPDYNAMICS_CLIENT_SECRET="your-client-secret"
export APPDYNAMICS_CONTROLLER_URL="https://your-controller.saas.appdynamics.com"

# Run metrics collection
python appdynamics_metrics.py

# With command-line options
python appdynamics_metrics.py --hours 24 --format excel --output metrics.xlsx
```

### Generate Demo Reports

```bash
# Generate demo performance data
python demo_metrics_data.py

# Output: demo_performance_report.csv
```

### Load Testing

```bash
# Health check
python load-generator/scripts/health_check.py

# Run load test (using Locust)
cd load-generator/scripts
locust -f realistic_load_test.py --host=http://localhost:8080

# With environment variables
export TARGET_BASE_URL="http://api-gateway:8080"
export CONCURRENT_USERS=20
export SPAWN_RATE=3
export TEST_DURATION=300

python realistic_load_test.py
```

### Intelligent Monitoring Service

```bash
# Build and run
cd intelligent-monitoring-service
./mvnw clean install
./mvnw spring-boot:run

# With specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=appdynamics

# Docker
docker-compose up intelligent-monitoring-service
```

---

## 📊 Current Metrics & SLOs

### Tracked Metrics (18 total)

| Service | Metrics |
|---------|---------|
| **ProductService** | availability, latency_p95, error_rate |
| **UserService** | availability, latency_p95, error_rate |
| **OrderService** | availability, latency_p95, error_rate |
| **PaymentService** | availability, latency_p95, error_rate |
| **CartService** | availability, latency_p95, error_rate |
| **AuthService** | availability, latency_p95, error_rate |

### SLO Targets

```yaml
availability:
  slo_target: 99.9%
  sla_target: 99.9%
  unit: "%"

latency_p95:
  slo_target: 200
  sla_target: 500
  unit: "ms"

error_rate:
  slo_target: 0.1
  sla_target: 1.0
  unit: "%"
```

### Health Status Criteria

```python
# From demo_metrics_data.py
status_criteria = {
    'Healthy': {
        'availability': '>= 99.9%',
        'latency_p95': '< 200ms',
        'error_rate': '< 0.1%'
    },
    'At_Risk': {
        'availability': '99.5-99.9%',
        'latency_p95': '200-500ms',
        'error_rate': '0.1-1.0%'
    },
    'Breached': {
        'availability': '< 99.5%',
        'latency_p95': '> 500ms',
        'error_rate': '> 1.0%'
    }
}
```

---

## 🔍 Error Pattern Analysis

### Supported Error Types

From `ErrorPatternAnalysisService.java`:

```java
// Automatically detected and fixed
ERROR_TEMPLATES = {
    "NullPointerException": {
        fixes: ["Add null checks", "Use Optional"],
        automated: true
    },
    "SQLException": {
        fixes: ["Add @Retryable", "Circuit breaker"],
        automated: true
    },
    "RestClientException": {
        fixes: ["Circuit breaker", "Retry logic"],
        automated: true
    },
    "BeanCreationException": {
        fixes: ["Add @Component", "Fix dependencies"],
        automated: true
    },
    "OutOfMemoryError": {
        fixes: ["Optimize memory", "Add pagination"],
        automated: false  // Manual analysis required
    }
}
```

### Error Signature Generation

```java
// Pattern: service|errorType|location|stackTrace
// Example: product-service|NullPointerException|ProductController.getProduct|...
// MD5 Hash: a3f7b2c1...
```

### Automated Fix Flow

```
1. Error Detected → 2. Pattern Match → 3. Confidence Check → 4. Apply Fix
                                              ↓
                                         (>= 80% confidence)
                                              ↓
5. Create Branch → 6. Modify Code → 7. Run Tests → 8. Create PR
```

---

## 🔗 Integration Points

### AppDynamics

```java
// OAuth2 Authentication
GET /controller/api/oauth/access_token
Headers:
  Content-Type: application/vnd.appd.cntrl+protobuf;v=1
Body:
  grant_type: client_credentials
  client_id: ${CLIENT_ID}
  client_secret: ${CLIENT_SECRET}

// Get Applications
GET /controller/rest/applications
Headers:
  Authorization: Bearer ${ACCESS_TOKEN}
  Accept: application/json

// Get Metrics
GET /controller/rest/applications/{appId}/metric-data
Params:
  metric-path: Application Infrastructure Performance|*|*
  time-range-type: BETWEEN_TIMES
  start-time: ${START_TIME_MS}
  end-time: ${END_TIME_MS}
```

### OpenTelemetry

```python
# From realistic_load_test.py
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

# Initialize
resource = Resource.create({
    "service.name": "load-generator",
    "deployment.environment": "docker"
})

trace.set_tracer_provider(TracerProvider(resource=resource))

# Export to collector
otlp_exporter = OTLPSpanExporter(
    endpoint="http://otel-collector:4317",
    insecure=True
)
```

### MongoDB Collections

```javascript
// Collections used
db.intelligent_monitoring_events  // Monitoring events (30-day TTL)
db.error_patterns                 // Error pattern signatures
db.automated_fixes                // Auto-fix history
db.audit_events                   // Audit trail

// Key Indexes
db.intelligent_monitoring_events.createIndex({ source: 1, timestamp: -1 })
db.intelligent_monitoring_events.createIndex({ serviceName: 1, eventType: 1 })
db.intelligent_monitoring_events.createIndex({ correlationId: 1 })
db.error_patterns.createIndex({ signature: 1 }, { unique: true })
```

---

## 🛠️ Common Tasks

### Add New Metric

**1. Update Data Model** (`MonitoringEvent.java`)
```java
private Map<String, Object> metrics;

// Add to metrics map
metrics.put("new_metric_name", value);
```

**2. Update Collection** (`AppDynamicsIntegrationService.java`)
```java
// Add to metric paths
metric_paths.add("Application Infrastructure Performance|*|New Metric");
```

**3. Update Processing** (`MonitoringEventService.java`)
```java
// Add metric calculation
public Map<String, Object> calculateNewMetric(String serviceName) {
    // Implementation
}
```

### Add New Error Pattern

**1. Add Template** (`ErrorPatternAnalysisService.java`)
```java
ERROR_TEMPLATES.put("NewException", new ErrorPatternTemplate(
    "NewException",
    Arrays.asList("Cause 1", "Cause 2"),
    Arrays.asList("Fix 1", "Fix 2"),
    "Description",
    true  // Has automated fix
));
```

**2. Implement Fix** (`AutomatedFixingService.java`)
```java
case "NewException":
    modified = fixNewException(cu, methodName, pattern);
    break;

private boolean fixNewException(CompilationUnit cu, String methodName, ErrorPattern pattern) {
    // Fix implementation
}
```

### Create Custom Report

**1. Query Metrics**
```python
import pymongo
from datetime import datetime, timedelta

client = pymongo.MongoClient('mongodb://localhost:27017/')
db = client['monitoring']

# Get last 24h metrics
since = datetime.now() - timedelta(hours=24)
events = db.intelligent_monitoring_events.find({
    'timestamp': {'$gte': since},
    'serviceName': 'product-service'
})
```

**2. Generate Report**
```python
import pandas as pd

# Convert to DataFrame
df = pd.DataFrame(list(events))

# Export
df.to_csv('custom_report.csv', index=False)
df.to_excel('custom_report.xlsx', index=False)
```

### Add Integration

**1. Create Integration Service** (`NewIntegrationService.java`)
```java
@Service
public class NewIntegrationService {

    @Value("${new-tool.api.url}")
    private String apiUrl;

    public List<MonitoringEvent> fetchData() {
        // Implementation
    }
}
```

**2. Schedule Collection** (`MonitoringScheduler.java`)
```java
@Scheduled(fixedRate = 60000)  // Every minute
public void collectFromNewTool() {
    List<MonitoringEvent> events = newIntegrationService.fetchData();
    monitoringEventService.saveEvents(events);
}
```

---

## 📈 Performance Optimization Tips

### Python

```python
# Use async for parallel API calls
import asyncio
import aiohttp

async def collect_metrics_async():
    async with aiohttp.ClientSession() as session:
        tasks = [fetch_metric(session, app) for app in apps]
        results = await asyncio.gather(*tasks)
    return results

# Use connection pooling
from requests.adapters import HTTPAdapter

session = requests.Session()
adapter = HTTPAdapter(pool_connections=10, pool_maxsize=20)
session.mount('http://', adapter)
session.mount('https://', adapter)
```

### Java

```java
// Use @Async for background processing
@Async
public CompletableFuture<List<MonitoringEvent>> collectMetricsAsync() {
    List<MonitoringEvent> events = fetchEvents();
    return CompletableFuture.completedFuture(events);
}

// Enable caching
@Cacheable(value = "metrics", key = "#serviceName")
public List<MonitoringEvent> getServiceMetrics(String serviceName) {
    // Expensive operation
}

// Use database indexes
@Indexed(name = "service_time_idx")
private String serviceName;

@Indexed(expireAfterSeconds = 2592000)  // 30-day TTL
private LocalDateTime timestamp;
```

### Database

```javascript
// Compound indexes for common queries
db.intelligent_monitoring_events.createIndex({
    serviceName: 1,
    eventType: 1,
    timestamp: -1
})

// Aggregation pipeline for better performance
db.intelligent_monitoring_events.aggregate([
    { $match: { serviceName: "product-service" } },
    { $group: {
        _id: "$eventType",
        count: { $sum: 1 },
        avgSeverity: { $avg: "$metrics.value" }
    }}
])
```

---

## 🐛 Troubleshooting

### Common Issues

#### AppDynamics Authentication Failed
```bash
# Check credentials
echo $APPDYNAMICS_CLIENT_ID
echo $APPDYNAMICS_CLIENT_SECRET

# Test authentication
curl -X POST "https://bny-ucf.saas.appdynamics.com/controller/api/oauth/access_token" \
  -H "Content-Type: application/vnd.appd.cntrl+protobuf;v=1" \
  -d "grant_type=client_credentials&client_id=${CLIENT_ID}&client_secret=${SECRET}"
```

#### MongoDB Connection Issues
```bash
# Check MongoDB status
docker ps | grep mongo

# Test connection
mongosh mongodb://localhost:27017/monitoring --eval "db.stats()"

# Check logs
docker logs intelligent-monitoring-service | grep -i mongo
```

#### Load Test Not Starting
```bash
# Check dependencies
pip list | grep -E "locust|faker|opentelemetry"

# Verify target is reachable
curl http://api-gateway:8080/health

# Check OpenTelemetry collector
curl http://otel-collector:13133/health
```

#### Pattern Analysis Not Working
```bash
# Check error event format
curl http://localhost:8080/api/monitoring/events?eventType=error | jq

# Verify stack trace is present
db.intelligent_monitoring_events.findOne({ eventType: "error" })

# Check pattern repository
db.error_patterns.find().pretty()
```

### Debug Mode

```bash
# Python
export LOG_LEVEL=DEBUG
python appdynamics_metrics.py

# Java
./mvnw spring-boot:run -Dspring-boot.run.arguments=--logging.level.com.ecommerce.monitoring=DEBUG

# Docker
docker-compose logs -f intelligent-monitoring-service
```

---

## 📚 API Reference

### Monitoring Events API

```bash
# Get all events
GET /api/monitoring/events

# Filter by service
GET /api/monitoring/events?serviceName=product-service

# Filter by time range
GET /api/monitoring/events?since=2025-10-01T00:00:00&until=2025-10-06T23:59:59

# Get by correlation ID
GET /api/monitoring/events/correlation/{correlationId}

# Create event
POST /api/monitoring/events
Content-Type: application/json
{
  "source": "frontend",
  "eventType": "error",
  "severity": "high",
  "serviceName": "product-service",
  "description": "Failed to load products",
  "stackTrace": "...",
  "metrics": { ... }
}
```

### Error Patterns API

```bash
# Get all patterns
GET /api/monitoring/patterns

# Get pattern by signature
GET /api/monitoring/patterns/{signature}

# Get fixable patterns
GET /api/monitoring/patterns/fixable?confidenceThreshold=0.8

# Validate pattern
POST /api/monitoring/patterns/{id}/validate
{
  "isValid": true
}
```

### Correlation API

```bash
# Get correlation statistics
GET /api/monitoring/correlation/stats?since=2025-10-01T00:00:00

# Get correlated events
GET /api/monitoring/correlation/{correlationId}

# Trigger correlation analysis
POST /api/monitoring/correlation/analyze
{
  "since": "2025-10-01T00:00:00"
}
```

### Health & Status

```bash
# Service health
GET /actuator/health

# Metrics
GET /actuator/metrics

# AppDynamics integration status
GET /api/appdynamics/health

# OpenTelemetry integration status
GET /api/opentelemetry/health
```

---

## 🔐 Security Best Practices

### Credentials Management

```bash
# Never commit credentials
echo "*.env" >> .gitignore
echo "*secrets*" >> .gitignore

# Use environment variables
export APPDYNAMICS_CLIENT_ID="$(cat ~/.secrets/appdynamics_client_id)"
export APPDYNAMICS_CLIENT_SECRET="$(cat ~/.secrets/appdynamics_secret)"

# Or use secrets management
kubectl create secret generic appdynamics-creds \
  --from-literal=client-id=$CLIENT_ID \
  --from-literal=client-secret=$SECRET
```

### API Security

```java
// HTTPS only
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.requiresChannel()
            .anyRequest()
            .requiresSecure();
        return http.build();
    }
}
```

### Data Privacy

```java
// Mask sensitive data
public MonitoringEvent sanitizeEvent(MonitoringEvent event) {
    if (event.getStackTrace() != null) {
        event.setStackTrace(
            maskPII(event.getStackTrace())
        );
    }
    return event;
}
```

---

## 📊 Sample Queries

### Find High-Severity Errors (Last Hour)

```javascript
db.intelligent_monitoring_events.find({
    eventType: "error",
    severity: { $in: ["critical", "high"] },
    timestamp: { $gte: new Date(Date.now() - 3600000) }
}).sort({ timestamp: -1 })
```

### Calculate Error Rate by Service

```javascript
db.intelligent_monitoring_events.aggregate([
    {
        $match: {
            timestamp: { $gte: new Date(Date.now() - 86400000) }
        }
    },
    {
        $group: {
            _id: "$serviceName",
            totalEvents: { $sum: 1 },
            errorEvents: {
                $sum: { $cond: [{ $eq: ["$eventType", "error"] }, 1, 0] }
            }
        }
    },
    {
        $project: {
            serviceName: "$_id",
            errorRate: {
                $multiply: [
                    { $divide: ["$errorEvents", "$totalEvents"] },
                    100
                ]
            }
        }
    }
])
```

### Find Patterns Needing Attention

```javascript
db.error_patterns.find({
    $or: [
        { severity: "critical" },
        { occurrenceCount: { $gte: 10 } }
    ],
    hasAutomatedFix: true,
    confidenceScore: { $gte: 0.8 }
}).sort({ occurrenceCount: -1 })
```

---

## 🚨 Alerting Examples

### Critical Error Rate

```java
@Scheduled(fixedRate = 60000)
public void checkErrorRate() {
    LocalDateTime since = LocalDateTime.now().minusMinutes(5);

    for (String service : services) {
        double errorRate = monitoringEventService.getErrorRate(service, since);

        if (errorRate > 5.0) {  // 5% threshold
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                "High error rate in " + service + ": " + errorRate + "%"
            );
        }
    }
}
```

### SLO Breach

```python
def check_slo_compliance(metrics):
    for metric in metrics:
        if metric['status'] == 'breached':
            error_budget_consumed = metric['error_budget_consumed']
            if error_budget_consumed > 100:
                send_alert(
                    level='CRITICAL',
                    message=f"SLO breached: {metric['service_name']} "
                           f"{metric['metric_name']} - "
                           f"{error_budget_consumed}% budget consumed"
                )
```

---

## 📝 Code Examples

### Load Test User Journey

```python
class RealisticEcommerceUser(HttpUser):
    wait_time = between(1, 5)

    @task(10)
    def execute_user_journey(self):
        journey = ["homepage", "browse_products", "view_product",
                  "add_to_cart", "checkout"]

        with tracer.start_span("user_journey") as span:
            for step in journey:
                self.execute_step(step)
                self.wait_between_actions()
```

### Error Pattern Detection

```java
public void analyzeErrorPattern(MonitoringEvent errorEvent) {
    String signature = generateErrorSignature(errorEvent);

    Optional<ErrorPattern> existingPattern =
        errorPatternRepository.findBySignature(signature);

    if (existingPattern.isPresent()) {
        updateExistingPattern(existingPattern.get(), errorEvent);
    } else {
        createNewPattern(signature, errorEvent);
    }

    // Trigger automated fix if ready
    if (shouldTriggerAutomatedFix(pattern)) {
        automatedFixingService.triggerAutomatedFix(pattern);
    }
}
```

### Cross-Platform Correlation

```java
public void correlateCrossPlatformEvents(LocalDateTime since) {
    List<MonitoringEvent> allEvents =
        monitoringEventRepository.findByTimestampAfter(since);

    Map<String, List<MonitoringEvent>> correlationGroups =
        groupEventsByCorrelation(allEvents);

    for (Map.Entry<String, List<MonitoringEvent>> entry :
         correlationGroups.entrySet()) {

        double confidence = calculateCorrelationConfidence(entry.getValue());

        if (confidence >= 0.7) {
            processCorrelationGroup(entry.getKey(), entry.getValue());
        }
    }
}
```

---

## 🎯 Next Steps

### For New Developers
1. ✅ Read the [Executive Summary](./SRE_ANALYTICS_EXECUTIVE_SUMMARY.md)
2. ✅ Review [Architecture Evolution](./SRE_ANALYTICS_ARCHITECTURE_EVOLUTION.md)
3. ✅ Study the sample reports in `/reports/examples/`
4. ✅ Run `demo_metrics_data.py` to understand data structure
5. ✅ Deploy local monitoring stack with Docker Compose

### For Contributors
1. 📚 Review [Enhancement Roadmap](./SRE_ANALYTICS_ENHANCEMENT_ROADMAP.md)
2. 🔧 Pick a Quick Win task (<1 week)
3. 🧪 Write tests (80% coverage target)
4. 📖 Update documentation
5. 🚀 Submit PR with comprehensive description

### For Operators
1. 📊 Set up monitoring dashboards
2. 🔔 Configure alerting rules
3. 📈 Establish SLO baselines
4. 🔍 Review error patterns weekly
5. 📝 Document incident response procedures

---

## 📞 Support & Resources

### Documentation
- **Roadmap**: [SRE_ANALYTICS_ENHANCEMENT_ROADMAP.md](./SRE_ANALYTICS_ENHANCEMENT_ROADMAP.md)
- **Architecture**: [SRE_ANALYTICS_ARCHITECTURE_EVOLUTION.md](./SRE_ANALYTICS_ARCHITECTURE_EVOLUTION.md)
- **Executive Summary**: [SRE_ANALYTICS_EXECUTIVE_SUMMARY.md](./SRE_ANALYTICS_EXECUTIVE_SUMMARY.md)

### External Resources
- [AppDynamics REST API Docs](https://docs.appdynamics.com/display/PRO45/Use+the+AppDynamics+REST+API)
- [OpenTelemetry Python](https://opentelemetry.io/docs/instrumentation/python/)
- [Spring Boot Monitoring](https://spring.io/guides/gs/actuator-service/)
- [MongoDB Performance](https://docs.mongodb.com/manual/administration/analyzing-mongodb-performance/)

### Tools & Utilities
- **API Testing**: Postman collection in `/docs/postman/`
- **Load Testing**: Locust web UI at `http://localhost:8089`
- **Database**: MongoDB Compass for visual inspection
- **Logging**: ELK stack at `http://localhost:5601`

---

**Last Updated**: October 6, 2025
**Version**: 1.0
**Maintained By**: SRE Platform Team
