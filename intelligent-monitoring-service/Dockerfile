# Multi-stage build with official AppDynamics Java Agent
FROM appdynamics/java-agent:25.7.0.37201 AS appdynamics-agent

FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user (Debian syntax)
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

# Install curl for health checks (Debian syntax)
RUN apt-get update && apt-get install -y curl && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy AppDynamics Java Agent from official image
COPY --from=appdynamics-agent /opt/appdynamics /opt/appdynamics

# Copy pre-built application JAR
COPY target/intelligent-monitoring-service-1.0.0.jar app.jar

# Download OpenTelemetry Java agent
RUN curl -L -o /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Create logs directory for AppDynamics
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app /opt/appdynamics

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Environment variables for AppDynamics (defaults, can be overridden)
ENV APPDYNAMICS_AGENT_APPLICATION_NAME="ecommerce-microservices"
ENV APPDYNAMICS_AGENT_TIER_NAME="intelligent-monitoring-service"
ENV APPDYNAMICS_AGENT_NODE_NAME="intelligent-monitoring-service-${HOSTNAME}"
ENV APPDYNAMICS_CONTROLLER_PORT="443"
ENV APPDYNAMICS_CONTROLLER_SSL_ENABLED="true"
ENV APPDYNAMICS_JAVA_AGENT_REUSE_NODE_NAME="true"
ENV APPDYNAMICS_JAVA_AGENT_REUSE_NODE_NAME_PREFIX="intelligent-monitoring-service"

# OpenTelemetry integration with AppDynamics
ENV APPDYNAMICS_OPENTELEMETRY_ENABLED="true"
ENV APPDYNAMICS_OPENTELEMETRY_ENDPOINT="http://otel-collector:4317"
ENV APPDYNAMICS_OPENTELEMETRY_RESOURCE_ATTRIBUTES="service.name=intelligent-monitoring-service,service.version=1.0.0,deployment.environment=docker"

# Base JVM configuration
ENV JAVA_OPTS_BASE="-server -Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# AppDynamics agent JAR
ENV APPDYNAMICS_AGENT_JAR="-javaagent:/opt/appdynamics/javaagent.jar"

# OpenTelemetry agent JAR
ENV OTEL_JAVAAGENT_JAR="-javaagent:/app/opentelemetry-javaagent.jar"

# AppDynamics system properties (runtime evaluation) with performance optimization
ENV APPDYNAMICS_AGENT_PROPS="-Dappdynamics.agent.logs.dir=/app/logs -Dappdynamics.agent.runtime.dir=/opt/appdynamics/runtime -Dappdynamics.opentelemetry.enabled=true -Dappdynamics.opentelemetry.endpoint=http://otel-collector:4317 -Dappdynamics.opentelemetry.resource.attributes=service.name=intelligent-monitoring-service,service.version=1.0.0,deployment.environment=docker -Dappagent.start.timeout=3000 -Dappdynamics.agent.maxMetrics=100 -Dappdynamics.controller.connect.retry.limit=3"

# OpenTelemetry configuration with exporters enabled
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="otlp"
ENV OTEL_EXPORTER_OTLP_TIMEOUT="5000"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"

EXPOSE 8090

# Start with both AppDynamics and OpenTelemetry agents (optimized)
ENTRYPOINT ["sh", "-c", "echo 'Starting with optimized AppDynamics + OpenTelemetry agents...'; java $JAVA_OPTS_BASE $OTEL_JAVAAGENT_JAR $APPDYNAMICS_AGENT_JAR -Dappdynamics.agent.applicationName=\"$APPDYNAMICS_AGENT_APPLICATION_NAME\" -Dappdynamics.agent.tierName=\"$APPDYNAMICS_AGENT_TIER_NAME\" -Dappdynamics.agent.nodeName=\"$APPDYNAMICS_AGENT_NODE_NAME\" -Dappdynamics.controller.hostName=\"$APPDYNAMICS_CONTROLLER_HOST_NAME\" -Dappdynamics.controller.port=\"$APPDYNAMICS_CONTROLLER_PORT\" -Dappdynamics.controller.ssl.enabled=\"$APPDYNAMICS_CONTROLLER_SSL_ENABLED\" -Dappdynamics.agent.accountName=\"$APPDYNAMICS_AGENT_ACCOUNT_NAME\" -Dappdynamics.agent.accountAccessKey=\"$APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY\" $APPDYNAMICS_AGENT_PROPS -jar app.jar"]