# ADR-004: JWT Token-Based Authentication

**Status**: Accepted
**Date**: 2024-01-25

## Context

Need stateless authentication mechanism for microservices without shared session storage.

## Decision

Use **JWT (JSON Web Tokens)** for authentication

**Rationale**:
- Stateless - no server-side session storage needed
- Self-contained - includes user information
- Scalable across multiple service instances
- Industry standard with broad library support

## Implementation

- **Algorithm**: HS256 (HMAC with SHA-256)
- **Expiration**: 24 hours
- **Claims**: userId, email, roles
- **Validation**: API Gateway and individual services

**Related**: [ADR-003: API Gateway](003-api-gateway-pattern.md)
