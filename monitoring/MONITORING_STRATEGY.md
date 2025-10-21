# Monitoring and Performance Strategy

## Overview

This document outlines a comprehensive monitoring, observability, and performance optimization strategy for the e-commerce microservices application, covering metrics collection, logging, distributed tracing, alerting, and performance tuning.

## Observability Stack

### Comprehensive Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Observability Architecture                        │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────────────┐ │
│  │   Metrics   │  │    Logs     │  │        Distributed Traces           │ │
│  │             │  │             │  │                                     │ │
│  │ Prometheus  │  │ ELK Stack   │  │ Grafana Tempo + OpenTelemetry      │ │
│  │   Grafana   │  │ Fluentd     │  │ (Primary Tracing Backend)           │ │
│  │             │  │             │  │                                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────────────────┘ │
│         │                │                           │                      │
│         └────────────────┼───────────────────────────┘                      │
│                          │                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                   Application Performance Monitoring                │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │                    AppDynamics Java Agent 25.6                 │ │   │
│  │  │  • Application Performance Monitoring (APM)                    │ │   │
│  │  │  • Business Transaction Monitoring                             │ │   │
│  │  │  • Code-level Visibility                                       │ │   │
│  │  │  • Database Performance                                        │ │   │
│  │  │  • Error Detection and Root Cause Analysis                    │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Application Services                            │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────────────┐ │   │
│  │  │User Service │ │Product Svc  │ │        Other Services           │ │   │
│  │  │             │ │             │ │                                 │ │   │
│  │  │ Micrometer  │ │ Micrometer  │ │       Micrometer                │ │   │
│  │  │  Logback    │ │  Logback    │ │        Logback                  │ │   │
│  │  │ OpenTelemetry│ │OpenTelemetry│ │     OpenTelemetry               │ │   │
│  │  │AppDynamics  │ │AppDynamics  │ │      AppDynamics                │ │   │
│  │  │   Agent     │ │   Agent     │ │        Agent                    │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## AppDynamics Integration

### AppDynamics Java Agent 25.6 Configuration

AppDynamics provides comprehensive Application Performance Monitoring (APM) with automatic instrumentation for Java applications.

#### Key Features
- **Automatic Code Instrumentation**: Zero-code changes required
- **Business Transaction Monitoring**: End-to-end transaction visibility
- **Database Performance Monitoring**: SQL query analysis and optimization
- **Error Detection**: Automatic error detection and root cause analysis
- **Real-time Dashboards**: Business and technical metrics
- **Baseline Detection**: Automatic performance baseline establishment

#### Docker Integration

All services are configured with AppDynamics Java Agent in the Docker images:

```dockerfile
# AppDynamics Java Agent is downloaded and configured in Dockerfile
ARG APPDYNAMICS_AGENT_VERSION=25.6.0
RUN curl -L "https://download.appdynamics.com/download/prox/download-file/java-jdk8/${APPDYNAMICS_AGENT_VERSION}/AppServerAgent-${APPDYNAMICS_AGENT_VERSION}.zip" \
    -o /tmp/AppServerAgent.zip && \
    mkdir -p /opt/appdynamics && \
    unzip /tmp/AppServerAgent.zip -d /opt/appdynamics

# JVM configuration with AppDynamics agent
ENV JAVA_OPTS="-javaagent:/opt/appdynamics/javaagent.jar \
    -Dappdynamics.agent.applicationName=ecommerce-microservices \
    -Dappdynamics.agent.tierName=${SERVICE_NAME} \
    -Dappdynamics.agent.nodeName=${SERVICE_NAME}-${HOSTNAME}"
```

#### Environment Configuration

```yaml
# docker-compose.yml - AppDynamics configuration
services:
  user-service:
    image: ecommerce/user-service:latest
    environment:
      - APPDYNAMICS_CONTROLLER_HOST_NAME=${APPDYNAMICS_CONTROLLER_HOST}
      - APPDYNAMICS_CONTROLLER_PORT=443
      - APPDYNAMICS_CONTROLLER_SSL_ENABLED=true
      - APPDYNAMICS_AGENT_ACCOUNT_NAME=${APPDYNAMICS_ACCOUNT_NAME}
      - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=${APPDYNAMICS_ACCESS_KEY}
      - APPDYNAMICS_AGENT_APPLICATION_NAME=ecommerce-microservices
      - APPDYNAMICS_AGENT_TIER_NAME=user-service
```

#### Kubernetes Deployment with AppDynamics

```yaml
# k8s/user-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  template:
    spec:
      containers:
      - name: user-service
        image: ecommerce/user-service:latest
        env:
        - name: APPDYNAMICS_CONTROLLER_HOST_NAME
          valueFrom:
            secretKeyRef:
              name: appdynamics-secret
              key: controller-host
        - name: APPDYNAMICS_AGENT_ACCOUNT_NAME
          valueFrom:
            secretKeyRef:
              name: appdynamics-secret
              key: account-name
        - name: APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: appdynamics-secret
              key: access-key
        - name: APPDYNAMICS_AGENT_APPLICATION_NAME
          value: "ecommerce-microservices"
        - name: APPDYNAMICS_AGENT_TIER_NAME
          value: "user-service"
        - name: APPDYNAMICS_AGENT_NODE_NAME
          value: "user-service-$(HOSTNAME)"
```

#### Custom Business Metrics for AppDynamics

```java
// AppDynamics custom metrics integration
@Component
public class AppDynamicsBusinessMetrics {
    
    public void recordBusinessTransaction(String transactionName, String userId, BigDecimal amount) {
        // AppDynamics automatically captures this as a business transaction
        MDC.put("userId", userId);
        MDC.put("amount", amount.toString());
        MDC.put("transactionType", transactionName);
        
        // Custom business data for AppDynamics
        if (isAppDynamicsActive()) {
            // Add business data to current transaction
            addBusinessData("user.id", userId);
            addBusinessData("transaction.amount", amount.toString());
            addBusinessData("transaction.type", transactionName);
        }
    }
    
    private void addBusinessData(String key, String value) {
        try {
            // AppDynamics API for adding business data
            Class<?> btClass = Class.forName("com.appdynamics.apm.appagent.api.AppdynamicsAgent");
            Method addBusinessDataMethod = btClass.getMethod("addBusinessData", String.class, String.class);
            addBusinessDataMethod.invoke(null, key, value);
        } catch (Exception e) {
            // AppDynamics not available, skip
        }
    }
    
    private boolean isAppDynamicsActive() {
        try {
            Class.forName("com.appdynamics.apm.appagent.api.AppdynamicsAgent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
```

## Grafana Tempo Integration

### Distributed Tracing with OpenTelemetry and Tempo

Grafana Tempo provides scalable distributed tracing storage and visualization integrated with Grafana dashboards.

#### OpenTelemetry Configuration

```yaml
# application.yml - OpenTelemetry configuration
otel:
  resource:
    attributes:
      service.name: ${spring.application.name}
      service.version: ${application.version:1.0.0}
      deployment.environment: ${spring.profiles.active:local}
  exporter:
    otlp:
      endpoint: http://tempo:4317
      protocol: grpc
      compression: gzip
  traces:
    exporter: otlp
  metrics:
    exporter: none  # Using Prometheus for metrics
  logs:
    exporter: none  # Using ELK for logs

management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of traces in production
    enabled: true
  otlp:
    tracing:
      endpoint: http://tempo:4317
```

#### Tempo Configuration

```yaml
# tempo/tempo.yml
server:
  http_listen_port: 3200
  grpc_listen_port: 9095

query_frontend:
  search:
    duration_slo: 5s
    throughput_bytes_slo: 1.073741824e+09
  trace_by_id:
    duration_slo: 5s

distributor:
  receivers:
    otlp:
      protocols:
        grpc:
          endpoint: 0.0.0.0:4317
        http:
          endpoint: 0.0.0.0:4318

ingester:
  max_block_duration: 5m

compactor:
  compaction:
    block_retention: 1h

storage:
  trace:
    backend: local
    wal:
      path: /var/tempo/wal
    local:
      path: /var/tempo/blocks
    pool:
      max_workers: 100
      queue_depth: 10000
```

#### Docker Compose with Tempo

```yaml
# docker-compose-monitoring.yml
version: '3.8'

services:
  tempo:
    image: grafana/tempo:2.3.0
    command: [ "-config.file=/etc/tempo.yaml" ]
    volumes:
      - ./tempo/tempo.yml:/etc/tempo.yaml
      - tempo-data:/var/tempo
    ports:
      - "3200:3200"   # Tempo HTTP
      - "4317:4317"   # OTLP gRPC receiver
      - "4318:4318"   # OTLP HTTP receiver
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:10.2.0
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
    ports:
      - "3000:3000"
    networks:
      - monitoring

  prometheus:
    image: prom/prometheus:v2.47.0
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - monitoring

volumes:
  tempo-data:
  grafana-data:
  prometheus-data:

networks:
  monitoring:
    driver: bridge
```

#### Grafana Datasource Configuration

```yaml
# grafana/datasources/datasources.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: false

  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    isDefault: true
    jsonData:
      httpMethod: GET
      serviceMap:
        datasourceUid: 'prometheus'
      search:
        hide: false
      nodeGraph:
        enabled: true
      tracesToLogs:
        datasourceUid: 'loki'
        tags: ['job', 'instance', 'pod', 'namespace']
        mappedTags: [
          { key: 'service.name', value: 'service' },
          { key: 'service.namespace', value: 'namespace' }
        ]
        mapTagNamesEnabled: true
        spanStartTimeShift: '-1h'
        spanEndTimeShift: '1h'
      tracesToMetrics:
        datasourceUid: 'prometheus'
        tags: [
          { key: 'service.name', value: 'service' },
          { key: 'service.namespace', value: 'namespace' }
        ]
        queries:
          - name: 'Sample query'
            query: 'sum(rate(traces_spanmetrics_latency_bucket{$$__tags}[5m]))'
```

#### Custom Tracing in Application Code

```java
// OpenTelemetry manual instrumentation
@Component
public class TracingService {
    
    private final Tracer tracer;
    
    public TracingService() {
        this.tracer = GlobalOpenTelemetry.getTracer("ecommerce-microservices");
    }
    
    public <T> T traceBusinessOperation(String operationName, 
                                       Map<String, String> attributes, 
                                       Supplier<T> operation) {
        
        Span span = tracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();
        
        // Add business attributes
        attributes.forEach(span::setAttribute);
        
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    public void addBusinessContext(String userId, String sessionId, String operation) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("business.user_id", userId);
            currentSpan.setAttribute("business.session_id", sessionId);
            currentSpan.setAttribute("business.operation", operation);
            currentSpan.setAttribute("service.version", getClass().getPackage().getImplementationVersion());
        }
    }
}

// Usage in service layer
@Service
public class OrderService {
    
    private final TracingService tracingService;
    
    @Transactional
    public Order createOrder(CheckoutRequest request) {
        Map<String, String> attributes = Map.of(
            "user.email", request.getUserEmail(),
            "order.type", "checkout",
            "payment.method", request.getPaymentMethod()
        );
        
        return tracingService.traceBusinessOperation("order.create", attributes, () -> {
            tracingService.addBusinessContext(
                request.getUserEmail(), 
                getCurrentSessionId(), 
                "CREATE_ORDER"
            );
            
            // Business logic
            return processOrder(request);
        });
    }
}
```

## Metrics Collection

### Spring Boot Actuator Configuration

```yaml
# application.yml - Metrics configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 30s
    distribution:
      percentiles-histogram:
        http.server.requests: true
        resilience4j.circuitbreaker.calls: true
      percentiles:
        http.server.requests: 0.5, 0.75, 0.9, 0.95, 0.99
      sla:
        http.server.requests: 100ms, 200ms, 500ms, 1s, 2s
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
      version: ${application.version:unknown}

# Custom metrics configuration
metrics:
  custom:
    business:
      enabled: true
      prefix: ecommerce
    jvm:
      enabled: true
      gc: true
      memory: true
      threads: true
```

### Custom Business Metrics

```java
// Custom metrics implementation
@Component
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    private final Counter userRegistrationCounter;
    private final Timer checkoutTimer;
    private final Gauge cartSizeGauge;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCounter = Counter.builder("ecommerce.orders.total")
            .description("Total number of orders placed")
            .tag("status", "completed")
            .register(meterRegistry);
            
        this.userRegistrationCounter = Counter.builder("ecommerce.users.registrations")
            .description("Total number of user registrations")
            .register(meterRegistry);
            
        this.checkoutTimer = Timer.builder("ecommerce.checkout.duration")
            .description("Time taken for checkout process")
            .register(meterRegistry);
            
        this.cartSizeGauge = Gauge.builder("ecommerce.cart.items.current")
            .description("Current number of items in all carts")
            .register(meterRegistry, this, BusinessMetrics::getCurrentCartItemsCount);
    }
    
    public void incrementOrderCount(String status, BigDecimal amount) {
        orderCounter.increment(
            Tags.of(
                Tag.of("status", status),
                Tag.of("amount_range", getAmountRange(amount))
            )
        );
    }
    
    public void recordCheckoutTime(Duration duration, String paymentMethod) {
        checkoutTimer.record(duration, Tags.of(Tag.of("payment_method", paymentMethod)));
    }
    
    public void recordUserRegistration(String source) {
        userRegistrationCounter.increment(Tags.of(Tag.of("source", source)));
    }
    
    private String getAmountRange(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(50)) < 0) return "0-50";
        if (amount.compareTo(BigDecimal.valueOf(100)) < 0) return "50-100";
        if (amount.compareTo(BigDecimal.valueOf(500)) < 0) return "100-500";
        return "500+";
    }
    
    private double getCurrentCartItemsCount() {
        // Implementation to get current cart items count
        return cartService.getTotalItemsCount();
    }
}
```

### Prometheus Configuration

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    cluster: 'ecommerce-prod'
    region: 'us-west-2'

rule_files:
  - "alerts/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
        - ecommerce-prod
        - ecommerce-staging
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: true
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
    - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
      action: replace
      regex: ([^:]+)(?::\d+)?;(\d+)
      replacement: $1:$2
      target_label: __address__

  - job_name: 'user-service'
    static_configs:
    - targets: ['user-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'product-service'
    static_configs:
    - targets: ['product-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'mongodb'
    static_configs:
    - targets: ['mongodb-exporter:9216']
    scrape_interval: 30s

  - job_name: 'nginx'
    static_configs:
    - targets: ['nginx-exporter:9113']
    scrape_interval: 30s
```

## Logging Strategy

### Structured Logging Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="!local">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp>
                        <timeZone>UTC</timeZone>
                    </timestamp>
                    <version/>
                    <logLevel/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                    <pattern>
                        <pattern>
                            {
                                "service": "${spring.application.name:-unknown}",
                                "version": "${application.version:-unknown}",
                                "environment": "${spring.profiles.active:-unknown}",
                                "pod": "${HOSTNAME:-unknown}",
                                "node": "${NODE_NAME:-unknown}"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
        </appender>
        
        <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
            <appender-ref ref="JSON"/>
            <queueSize>1000</queueSize>
            <discardingThreshold>0</discardingThreshold>
            <includeCallerData>false</includeCallerData>
        </appender>
    </springProfile>
    
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
    </springProfile>

    <!-- Separate appenders for different log types -->
    <appender name="AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <message/>
                <mdc/>
            </providers>
        </encoder>
    </appender>

    <!-- Logger configurations -->
    <logger name="com.ecommerce" level="INFO"/>
    <logger name="com.ecommerce.audit" level="INFO" additivity="false">
        <appender-ref ref="AUDIT"/>
    </logger>
    
    <!-- Third-party loggers -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.springframework.security" level="DEBUG"/>
    <logger name="org.mongodb.driver" level="WARN"/>
    
    <root level="INFO">
        <springProfile name="!local">
            <appender-ref ref="ASYNC"/>
        </springProfile>
        <springProfile name="local">
            <appender-ref ref="CONSOLE"/>
        </springProfile>
    </root>
</configuration>
```

### Audit Logging Implementation

```java
// Audit logging aspect
@Aspect
@Component
public class AuditLoggingAspect {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("com.ecommerce.audit");
    
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        // Get current user context
        String userId = getCurrentUserId();
        String sessionId = getCurrentSessionId();
        
        MDC.put("audit.action", auditable.action());
        MDC.put("audit.resource", auditable.resource());
        MDC.put("audit.userId", userId);
        MDC.put("audit.sessionId", sessionId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            auditLogger.info("Audit: action={}, resource={}, userId={}, duration={}ms, status=SUCCESS",
                auditable.action(), auditable.resource(), userId, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            auditLogger.error("Audit: action={}, resource={}, userId={}, duration={}ms, status=ERROR, error={}",
                auditable.action(), auditable.resource(), userId, executionTime, e.getMessage());
            
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
    
    private String getCurrentSessionId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
            return request.getSession().getId();
        }
        return "unknown";
    }
}

// Auditable annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
    String resource();
}

// Usage example
@RestController
public class UserController {
    
    @PostMapping("/register")
    @Auditable(action = "CREATE", resource = "USER")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        // Implementation
    }
    
    @PostMapping("/login")
    @Auditable(action = "LOGIN", resource = "USER")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Implementation
    }
}
```

### ELK Stack Configuration

```yaml
# elasticsearch/elasticsearch.yml
cluster.name: ecommerce-logs
node.name: ${HOSTNAME}
network.host: 0.0.0.0
discovery.type: single-node
xpack.security.enabled: false
xpack.ml.enabled: false

# Index lifecycle management
xpack.ilm.enabled: true
```

```yaml
# logstash/pipeline/logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  if [kubernetes][container][name] {
    mutate {
      add_field => { "service_name" => "%{[kubernetes][container][name]}" }
    }
  }
  
  # Parse JSON logs
  if [message] =~ /^\{.*\}$/ {
    json {
      source => "message"
    }
  }
  
  # Parse application logs
  grok {
    match => { 
      "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} \[%{DATA:trace_id},%{DATA:span_id}\] %{DATA:logger} - %{GREEDYDATA:log_message}"
    }
  }
  
  # Date parsing
  date {
    match => [ "timestamp", "ISO8601" ]
  }
  
  # Add environment metadata
  mutate {
    add_field => { 
      "environment" => "${ENVIRONMENT:unknown}"
      "cluster" => "${CLUSTER:unknown}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "ecommerce-logs-%{+YYYY.MM.dd}"
    template_name => "ecommerce-logs"
    template => "/usr/share/logstash/templates/ecommerce-logs.json"
    template_overwrite => true
  }
}
```

## Distributed Tracing

### Spring Cloud Sleuth Configuration

```yaml
# application.yml - Tracing configuration
spring:
  sleuth:
    sampler:
      probability: 0.1  # Sample 10% of traces in production
    zipkin:
      base-url: http://zipkin:9411
    web:
      client:
        enabled: true
      servlet:
        enabled: true
    async:
      enabled: true
    scheduled:
      enabled: true
  
  # OpenTracing configuration
  jaeger:
    service-name: ${spring.application.name}
    sampler:
      type: probabilistic
      param: 0.1
    sender:
      endpoint: http://jaeger-collector:14268/api/traces
```

### Custom Tracing

```java
// Custom tracing for business operations
@Component
public class TracingService {
    
    private final Tracer tracer;
    
    public TracingService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public <T> T traceBusinessOperation(String operationName, Supplier<T> operation) {
        Span span = tracer.nextSpan()
            .name(operationName)
            .tag("component", "business-logic")
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return operation.get();
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    public void addBusinessTags(String userId, String operation, String resource) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan
                .tag("business.user_id", userId)
                .tag("business.operation", operation)
                .tag("business.resource", resource);
        }
    }
}

// Usage in service layer
@Service
public class OrderService {
    
    private final TracingService tracingService;
    
    @Transactional
    public Order createOrder(CheckoutRequest request) {
        return tracingService.traceBusinessOperation("create-order", () -> {
            tracingService.addBusinessTags(request.getUserEmail(), "CREATE", "ORDER");
            
            // Validate cart
            List<CartItem> cartItems = validateCart(request.getUserEmail());
            
            // Calculate total
            BigDecimal total = calculateTotal(cartItems);
            
            // Create order
            Order order = buildOrder(request, cartItems, total);
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            // Clear cart
            clearCart(request.getUserEmail());
            
            return savedOrder;
        });
    }
}
```

## Alerting Strategy

### Prometheus Alert Rules

```yaml
# alerts/application.yml
groups:
- name: ecommerce.application
  rules:
  
  # High error rate
  - alert: HighErrorRate
    expr: rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m]) > 0.05
    for: 2m
    labels:
      severity: critical
      team: platform
    annotations:
      summary: "High error rate detected"
      description: "Service {{ $labels.service }} has error rate of {{ $value | humanizePercentage }} for the last 5 minutes"
      runbook_url: "https://runbooks.ecommerce.com/high-error-rate"

  # High response time
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m])) > 2
    for: 5m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "High response time detected"
      description: "Service {{ $labels.service }} 95th percentile response time is {{ $value }}s"

  # Low throughput
  - alert: LowThroughput
    expr: rate(http_server_requests_total[5m]) < 1
    for: 10m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "Low traffic detected"
      description: "Service {{ $labels.service }} receiving less than 1 request per second"

  # Database connection pool exhaustion
  - alert: DatabaseConnectionPoolExhaustion
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 2m
    labels:
      severity: critical
      team: platform
    annotations:
      summary: "Database connection pool nearly exhausted"
      description: "Service {{ $labels.service }} using {{ $value | humanizePercentage }} of database connections"

- name: ecommerce.business
  rules:
  
  # Order creation failure rate
  - alert: OrderCreationFailureRate
    expr: rate(ecommerce_orders_total{status="failed"}[5m]) / rate(ecommerce_orders_total[5m]) > 0.1
    for: 3m
    labels:
      severity: critical
      team: business
    annotations:
      summary: "High order creation failure rate"
      description: "Order failure rate is {{ $value | humanizePercentage }} over the last 5 minutes"

  # Payment processing issues
  - alert: PaymentProcessingFailure
    expr: rate(ecommerce_payments_total{status="failed"}[5m]) > 5
    for: 1m
    labels:
      severity: critical
      team: payments
    annotations:
      summary: "Payment processing failures detected"
      description: "{{ $value }} payment failures per second detected"

  # Inventory alerts
  - alert: LowInventory
    expr: ecommerce_inventory_items{quantity="low"} > 10
    for: 0m
    labels:
      severity: warning
      team: inventory
    annotations:
      summary: "Low inventory detected"
      description: "{{ $value }} products have low inventory levels"

- name: ecommerce.infrastructure
  rules:
  
  # Pod restarts
  - alert: PodCrashLooping
    expr: rate(kube_pod_container_status_restarts_total[15m]) > 0
    for: 5m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "Pod is crash looping"
      description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} is restarting frequently"

  # Memory usage
  - alert: HighMemoryUsage
    expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
    for: 5m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "High memory usage"
      description: "Container {{ $labels.container }} using {{ $value | humanizePercentage }} of memory limit"

  # CPU usage
  - alert: HighCPUUsage
    expr: rate(container_cpu_usage_seconds_total[5m]) / container_spec_cpu_quota * container_spec_cpu_period > 0.8
    for: 10m
    labels:
      severity: warning
      team: platform
    annotations:
      summary: "High CPU usage"
      description: "Container {{ $labels.container }} using {{ $value | humanizePercentage }} of CPU limit"
```

### AlertManager Configuration

```yaml
# alertmanager/alertmanager.yml
global:
  smtp_smarthost: 'smtp.company.com:587'
  smtp_from: 'alerts@ecommerce.com'
  slack_api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'

route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
    continue: true
  - match:
      team: business
    receiver: 'business-team'
  - match:
      team: platform
    receiver: 'platform-team'

receivers:
- name: 'default'
  slack_configs:
  - channel: '#alerts'
    title: 'Alert: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

- name: 'critical-alerts'
  email_configs:
  - to: 'oncall@ecommerce.com'
    subject: 'CRITICAL: {{ .GroupLabels.alertname }}'
    body: |
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      Runbook: {{ .Annotations.runbook_url }}
      {{ end }}
  slack_configs:
  - channel: '#critical-alerts'
    title: 'CRITICAL: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
    color: 'danger'

- name: 'business-team'
  slack_configs:
  - channel: '#business-alerts'
    title: 'Business Alert: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

- name: 'platform-team'
  slack_configs:
  - channel: '#platform-alerts'
    title: 'Platform Alert: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

## Performance Monitoring

### Application Performance Monitoring (APM)

```java
// Performance monitoring interceptor
@Component
public class PerformanceMonitoringInterceptor implements HandlerInterceptor {
    
    private final MeterRegistry meterRegistry;
    private final Timer.Sample sample = Timer.start(meterRegistry);
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                              HttpServletResponse response, 
                              Object handler, 
                              Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String status = String.valueOf(response.getStatus());
        
        Timer.builder("http.server.requests")
            .tag("method", method)
            .tag("uri", uri)
            .tag("status", status)
            .tag("exception", ex != null ? ex.getClass().getSimpleName() : "none")
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### Database Performance Monitoring

```java
// Database performance monitoring
@Configuration
public class DatabaseMonitoringConfig {
    
    @Bean
    public ProxyDataSource dataSource(@Qualifier("actualDataSource") DataSource dataSource,
                                    MeterRegistry meterRegistry) {
        return ProxyDataSourceBuilder
            .create(dataSource)
            .name("ecommerce-db")
            .listener(new SystemOutQueryLoggingListener())
            .listener(new MicrometerQueryExecutionListener(meterRegistry))
            .countQuery()
            .logQueryBySlf4j(SLF4JLogLevel.DEBUG)
            .multiline()
            .build();
    }
}

// Custom query performance metrics
@Component
public class QueryPerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public QueryPerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        Timer.builder("database.query.slow")
            .tag("query_type", event.getQueryType())
            .tag("table", event.getTableName())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
            
        // Log slow queries
        if (event.getDuration() > Duration.ofSeconds(1)) {
            log.warn("Slow query detected: {} took {}ms", 
                event.getQuery(), event.getDuration().toMillis());
        }
    }
}
```

## Grafana Dashboards

### Application Dashboard

```json
{
  "dashboard": {
    "title": "E-commerce Application Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "type": "stat",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_total{status=~\"5..\"}[5m])) by (service) / sum(rate(http_server_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "{{service}} - {{area}}"
          }
        ]
      },
      {
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active - {{service}}"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle - {{service}}"
          }
        ]
      }
    ]
  }
}
```

### Business Metrics Dashboard

```json
{
  "dashboard": {
    "title": "E-commerce Business Metrics",
    "panels": [
      {
        "title": "Orders per Hour",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(ecommerce_orders_total[1h]))",
            "legendFormat": "Orders"
          }
        ]
      },
      {
        "title": "Revenue per Hour",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(ecommerce_revenue_total[1h]))",
            "legendFormat": "Revenue ($)"
          }
        ]
      },
      {
        "title": "User Registrations",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(ecommerce_users_registrations[5m])",
            "legendFormat": "Registrations/sec"
          }
        ]
      },
      {
        "title": "Cart Abandonment Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "(sum(ecommerce_cart_created_total) - sum(ecommerce_orders_total)) / sum(ecommerce_cart_created_total)",
            "legendFormat": "Abandonment Rate"
          }
        ]
      }
    ]
  }
}
```

## Performance Optimization Strategy

### JVM Tuning

```bash
# Production JVM settings
JAVA_OPTS="
-Xms1g
-Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-Djava.security.egd=file:/dev/./urandom
-Dspring.backgroundpreinitializer.ignore=true
"
```

### Database Performance Tuning

```yaml
# MongoDB performance configuration
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 4
      directoryForIndexes: true
    indexConfig:
      prefixCompression: true
    collectionConfig:
      blockCompressor: snappy

# Connection pool optimization
spring:
  data:
    mongodb:
      uri: mongodb://admin:password@mongodb:27017/ecommerce?authSource=admin&maxPoolSize=20&minPoolSize=5&maxIdleTimeMS=300000
```

### Caching Strategy

```java
// Redis caching configuration
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}

// Cacheable service methods
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(String id) {
        return productRepository.findById(id);
    }
    
    @Cacheable(value = "product-search", key = "#searchTerm")
    public List<Product> searchProducts(String searchTerm) {
        return productRepository.findByNameContaining(searchTerm);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
```

This comprehensive monitoring strategy ensures observability, performance optimization, and proactive issue detection for the e-commerce microservices application.