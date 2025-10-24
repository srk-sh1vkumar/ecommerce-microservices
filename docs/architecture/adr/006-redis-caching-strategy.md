# ADR-006: Redis for Caching Layer

**Status**: Accepted
**Date**: 2024-02-05

## Context

Product catalog queries generate high database load. Need caching to improve response times and reduce database pressure.

## Decision

Use **Redis** as distributed cache

**Rationale**:
- In-memory performance (sub-millisecond latency)
- Distributed caching across service instances
- TTL support for cache expiration
- Rich data structures (strings, sets, sorted sets)
- Production-proven at scale

## Implementation

- **Product Service**: 1-hour TTL for product data
- **Cache Strategy**: Cache-aside pattern
- **Eviction Policy**: allkeys-lru
- **Persistence**: RDB snapshots for cache warmup

**Related**: [ADR-007: Circuit Breaker Pattern](007-circuit-breaker-pattern.md)
