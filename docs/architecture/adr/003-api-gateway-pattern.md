# ADR-003: Implement API Gateway Pattern with Spring Cloud Gateway

**Status**: Accepted
**Date**: 2024-01-22
**Decision Makers**: Architecture Team

---

## Context

Clients need a single entry point to access microservices without knowing internal service locations. Cross-cutting concerns like authentication, rate limiting, and routing need centralized handling.

## Decision

Use **Spring Cloud Gateway** as the API Gateway

**Rationale**:
- Non-blocking, reactive architecture
- Native Spring Cloud integration
- Built-in load balancing and circuit breaker support
- Flexible routing based on paths, headers, etc.
- Easy to extend with custom filters

## Consequences

**Positive**:
- Single entry point for all client requests
- Centralized authentication and authorization
- Rate limiting and throttling
- Protocol translation and routing
- Reduces client complexity

**Negative**:
- Potential single point of failure (mitigated by clustering)
- Additional network hop
- Gateway configuration complexity

## Implementation

- Port: 8080
- Routes: `/api/users/**`, `/api/products/**`, `/api/cart/**`, `/api/orders/**`
- Features: JWT validation, CORS, rate limiting, logging

**Related**: [ADR-004: JWT Authentication](004-jwt-authentication.md)
