# E-commerce Microservices Architecture Documentation

## Overview

This document provides comprehensive architectural documentation for the distributed e-commerce microservices application built with Spring Boot, Spring Cloud, MongoDB, and Docker.

## System Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │  Mobile Client  │    │  Third-party    │
│                 │    │                 │    │   Integrations  │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
          ┌─────────────────────┬┴┬─────────────────────┐
          │                     │ │                     │
┌─────────▼─────────┐    ┌──────▼─▼──────┐    ┌─────────▼─────────┐
│   Load Balancer   │    │  API Gateway   │    │   CDN/Static      │
│   (Production)    │    │   (Port 8080)  │    │   Assets          │
└─────────┬─────────┘    └──────┬─┬──────┘    └───────────────────┘
          │                     │ │
          └─────────────────────┘ │
                                  │
                    ┌─────────────▼─────────────┐
                    │   Service Discovery       │
                    │   Eureka Server          │
                    │   (Port 8761)            │
                    └─────────────┬─────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
    ┌─────────▼─────────┐ ┌───────▼───────┐ ┌─────────▼─────────┐
    │   User Service    │ │ Product Service│ │   Cart Service    │
    │   (Port 8081)     │ │ (Port 8082)    │ │   (Port 8083)     │
    └─────────┬─────────┘ └───────┬───────┘ └─────────┬─────────┘
              │                   │                   │
              └───────────────────┼───────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │   Order Service   │
                        │   (Port 8084)     │
                        └─────────┬─────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │       MongoDB            │
                    │     (Port 27017)         │
                    │                          │
                    │  Collections:            │
                    │  - users                 │
                    │  - products              │
                    │  - cart_items            │
                    │  - orders                │
                    └──────────────────────────┘
```

## Service Design Patterns

### 1. Database per Service Pattern
Each microservice owns its data and database schema:
- **User Service**: Manages user authentication, profiles, and JWT tokens
- **Product Service**: Handles product catalog, inventory, and search
- **Cart Service**: Manages shopping cart state and operations
- **Order Service**: Processes checkout and order history

### 2. API Gateway Pattern
- Single entry point for all client requests
- Cross-cutting concerns: authentication, rate limiting, logging
- Route requests to appropriate microservices
- Protocol translation and request/response transformation

### 3. Service Discovery Pattern
- Eureka server for dynamic service registration and discovery
- Services register themselves on startup
- Client-side load balancing
- Health checking and fault tolerance

### 4. Circuit Breaker Pattern (Recommended Implementation)
```java
// Example implementation with Resilience4j
@CircuitBreaker(name = "product-service", fallbackMethod = "fallbackGetProduct")
@Retry(name = "product-service")
@TimeLimiter(name = "product-service")
public CompletableFuture<ProductDTO> getProduct(String productId) {
    return CompletableFuture.supplyAsync(() -> productServiceClient.getProduct(productId));
}

public CompletableFuture<ProductDTO> fallbackGetProduct(String productId, Exception ex) {
    return CompletableFuture.completedFuture(ProductDTO.builder()
        .id(productId)
        .name("Product Unavailable")
        .build());
}
```

## Data Architecture

### Database Strategy
- **Database Type**: MongoDB (Document-based NoSQL)
- **Consistency Model**: Eventual consistency across services
- **Transaction Handling**: Local transactions within services
- **Data Synchronization**: Event-driven architecture with message queues (recommended)

### Data Models

#### User Service Schema
```javascript
{
  _id: ObjectId,
  email: String (unique, indexed),
  password: String (BCrypt hashed),
  firstName: String,
  lastName: String,
  createdAt: Date,
  updatedAt: Date,
  roles: [String], // ["USER", "ADMIN"]
  isActive: Boolean,
  lastLoginAt: Date
}
```

#### Product Service Schema
```javascript
{
  _id: ObjectId,
  name: String (indexed),
  description: String,
  price: Decimal128,
  category: String (indexed),
  stockQuantity: Number,
  imageUrl: String,
  tags: [String],
  createdAt: Date,
  updatedAt: Date,
  isActive: Boolean,
  searchKeywords: [String] // For search optimization
}
```

#### Cart Service Schema
```javascript
{
  _id: ObjectId,
  userEmail: String (indexed),
  productId: String,
  quantity: Number,
  addedAt: Date,
  updatedAt: Date
}
```

#### Order Service Schema
```javascript
{
  _id: ObjectId,
  orderNumber: String (unique, indexed),
  userEmail: String (indexed),
  items: [{
    productId: String,
    productName: String,
    price: Decimal128,
    quantity: Number
  }],
  totalAmount: Decimal128,
  shippingAddress: String,
  status: String, // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
  createdAt: Date,
  updatedAt: Date,
  paymentId: String,
  trackingNumber: String
}
```

## Security Architecture

### Authentication & Authorization
- **JWT Token-based Authentication**
- **BCrypt Password Hashing**
- **Role-based Access Control (RBAC)**
- **API Gateway Security Filtering**

### Security Layers
1. **Transport Security**: HTTPS/TLS encryption
2. **API Security**: JWT validation, rate limiting
3. **Data Security**: Encrypted sensitive data, secure password storage
4. **Network Security**: Internal service communication encryption

## Communication Patterns

### Synchronous Communication
- **REST APIs**: HTTP/HTTPS between services
- **Service-to-Service**: Feign clients with load balancing
- **Client-to-Service**: Through API Gateway

### Asynchronous Communication (Recommended)
```yaml
# Message Queue Configuration (RabbitMQ/Kafka)
events:
  order-created:
    publisher: order-service
    subscribers: [inventory-service, notification-service]
  
  payment-processed:
    publisher: payment-service
    subscribers: [order-service, fulfillment-service]
  
  user-registered:
    publisher: user-service
    subscribers: [email-service, analytics-service]
```

## Scalability Design

### Horizontal Scaling
- **Stateless Services**: All services designed to be stateless
- **Load Balancing**: Multiple instances behind load balancers
- **Database Sharding**: Partition data across multiple MongoDB instances

### Performance Optimization
- **Caching Strategy**: Redis for frequently accessed data
- **Database Indexing**: Optimized queries with proper indexes
- **Connection Pooling**: Efficient database connection management
- **Async Processing**: Non-blocking I/O operations

## Fault Tolerance

### Resilience Patterns
1. **Circuit Breaker**: Prevent cascade failures
2. **Retry with Backoff**: Handle transient failures
3. **Timeout**: Prevent hanging requests
4. **Bulkhead**: Isolate critical resources

### Health Checks
```yaml
# Spring Boot Actuator Health Endpoints
management:
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## Configuration Management

### Environment-Specific Configuration
```yaml
# application-{profile}.yml structure
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  
  config:
    import: 
      - configserver:http://config-server:8888
      - consul:http://consul:8500
```

### External Configuration
- **Spring Cloud Config Server** for centralized configuration
- **Environment Variables** for sensitive data
- **Kubernetes ConfigMaps/Secrets** for container orchestration

## Design Decisions and Rationale

### Technology Choices

1. **Spring Boot**: Rapid development, production-ready features
2. **MongoDB**: Flexible schema, horizontal scaling capabilities
3. **Eureka**: Service discovery with Netflix OSS ecosystem
4. **Docker**: Containerization for consistent deployments
5. **Maven**: Dependency management and build automation

### Architectural Trade-offs

#### Advantages
- **Scalability**: Independent scaling of services
- **Technology Diversity**: Different technologies per service
- **Fault Isolation**: Failure in one service doesn't affect others
- **Team Independence**: Teams can work independently on services

#### Challenges
- **Complexity**: Distributed system complexity
- **Data Consistency**: Eventual consistency challenges
- **Network Latency**: Inter-service communication overhead
- **Testing**: Integration testing complexity

## Future Architecture Considerations

### Recommended Enhancements

1. **Event Sourcing**: Implement for audit trails and temporal queries
2. **CQRS**: Separate read and write models for better performance
3. **Saga Pattern**: Distributed transaction management
4. **API Versioning**: Backward compatibility for API evolution
5. **Service Mesh**: Istio/Linkerd for advanced service communication
6. **Observability**: Distributed tracing with Jaeger/Zipkin

### Migration Strategies

#### Monolith to Microservices (If Applicable)
1. **Strangler Fig Pattern**: Gradually replace monolith components
2. **Database Decomposition**: Split shared databases
3. **API Extraction**: Extract APIs from monolith
4. **Data Migration**: Migrate data to service-specific databases

## Documentation Maintenance

### Architecture Decision Records (ADRs)
Maintain ADRs for significant architectural decisions:

```markdown
# ADR-001: Choice of MongoDB for Data Persistence

## Status
Accepted

## Context
Need to choose a database technology for microservices architecture.

## Decision
Use MongoDB as the primary database for all services.

## Consequences
- Flexible schema evolution
- Horizontal scaling capabilities
- JSON-native data storage
- Learning curve for SQL-experienced developers
```

This architecture documentation should be updated whenever significant changes are made to the system design or technology stack.