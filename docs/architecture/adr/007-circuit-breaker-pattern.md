# ADR-007: Circuit Breaker Pattern with Resilience4j

**Status**: Accepted
**Date**: 2024-02-10

## Context

Service failures should not cascade. Need to fail fast and provide fallbacks when dependencies are unavailable.

## Decision

Implement **Circuit Breaker** pattern using **Resilience4j**

**Rationale**:
- Prevents cascading failures
- Fail fast when service is down
- Automatic recovery detection
- Lightweight and Spring Boot friendly
- Better than Netflix Hystrix (in maintenance mode)

## Configuration

- **Failure Rate Threshold**: 50%
- **Wait Duration in Open State**: 60 seconds
- **Sliding Window Size**: 100 requests
- **Fallback**: Return cached data or default response

**Implementation**: Product Service uses circuit breaker for external API calls

**Related**: [ADR-006: Redis Caching](006-redis-caching-strategy.md)
