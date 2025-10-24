# ADR-002: Use Eureka for Service Discovery

**Status**: Accepted
**Date**: 2024-01-20
**Decision Makers**: Architecture Team
**Technical Story**: Service-to-service communication design

---

## Context and Problem Statement

In a microservices architecture, services need to discover and communicate with each other dynamically. Hard-coding service locations creates inflexibility and operational overhead when services scale or move.

## Decision Drivers

- Dynamic service registration and discovery
- Load balancing across service instances
- Health checking and automatic deregistration of unhealthy instances
- Integration with Spring Cloud ecosystem
- Production-proven solution
- Active community and support

## Considered Options

- **Option 1**: Consul
- **Option 2**: Eureka
- **Option 3**: Kubernetes Service Discovery

## Decision Outcome

**Chosen option**: "Eureka"

**Justification**: Eureka provides seamless integration with Spring Cloud, is battle-tested at Netflix scale, and offers a simple yet powerful service registry without external dependencies.

### Positive Consequences

- Automatic service registration and deregistration
- Client-side load balancing with Ribbon
- Simple dashboard for monitoring registered services
- No single point of failure (Eureka cluster support)
- Works well with Spring Boot Actuator health checks

### Negative Consequences

- Additional service to maintain
- Eventual consistency model (may have stale data briefly)
- Not suitable for non-JVM services (though HTTP API available)

## Implementation

- **Eureka Server**: Port 8761
- **Registration**: All microservices register on startup
- **Health Checks**: Every 30 seconds
- **Lease Renewal**: 90 seconds before deregistration

## Links

- [Eureka GitHub](https://github.com/Netflix/eureka)
- [Spring Cloud Netflix Documentation](https://spring.io/projects/spring-cloud-netflix)
