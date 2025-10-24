# ADR-008: OpenTelemetry for Distributed Tracing

**Status**: Accepted
**Date**: 2024-03-01

## Context

Need to trace requests across multiple microservices for debugging and performance analysis.

## Decision

Adopt **OpenTelemetry** for distributed tracing

**Rationale**:
- Vendor-neutral standard
- Auto-instrumentation for Spring Boot
- Compatible with multiple backends (Tempo, Jaeger, Zipkin)
- Combines traces, metrics, and logs
- Active CNCF project with strong community

## Implementation

- **Collector**: OTLP Collector (ports 4317 gRPC, 4318 HTTP)
- **Backend**: Grafana Tempo
- **Visualization**: Grafana dashboards
- **Sampling**: 100% in development, 10% in production

**Stack**: Prometheus (metrics) + Tempo (traces) + Grafana (visualization)
