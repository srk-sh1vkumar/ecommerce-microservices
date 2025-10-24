# ADR-005: MongoDB for Data Persistence

**Status**: Accepted
**Date**: 2024-01-28

## Context

Need flexible schema for e-commerce data with varying product attributes and user profiles.

## Decision

Use **MongoDB** as the primary database

**Rationale**:
- Schema flexibility for product catalog variations
- Horizontal scalability with sharding
- JSON-like documents match application DTOs
- Rich query capabilities
- Built-in replication for high availability

## Consequences

**Positive**:
- Easy schema evolution
- Fast reads for product catalog
- Native JSON support
- Horizontal scalability

**Negative**:
- Eventual consistency in distributed setup
- No ACID transactions across collections (before MongoDB 4.0)
- Requires careful index design

## Implementation

- **Version**: MongoDB 5.0+
- **Connection**: One database per service
- **Replica Set**: For production high availability
