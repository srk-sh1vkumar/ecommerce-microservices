# Multi-stage build with official AppDynamics Java Agent
FROM appdynamics/java-agent:25.7.0.37201 AS appdynamics-agent

FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

# Install curl and git for health checks and code operations
RUN apt-get update && apt-get install -y curl git maven && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy AppDynamics Java Agent from official image
COPY --from=appdynamics-agent /opt/appdynamics /opt/appdynamics

# Copy pre-built application JAR
COPY target/intelligent-monitoring-service-1.0.0.jar app.jar

# Copy OpenTelemetry configuration
COPY otel-config.properties /app/otel-config.properties

# Download OpenTelemetry Java agent
RUN curl -L -o /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Create directories for monitoring operations
RUN mkdir -p /app/logs /app/monitoring-workspace && \
    chown -R appuser:appgroup /app /opt/appdynamics

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Environment variables for AppDynamics
ENV APPDYNAMICS_AGENT_APPLICATION_NAME="ecommerce-microservices"
ENV APPDYNAMICS_AGENT_TIER_NAME="intelligent-monitoring-service"
ENV APPDYNAMICS_AGENT_NODE_NAME="intelligent-monitoring-${HOSTNAME}"
ENV APPDYNAMICS_CONTROLLER_PORT="443"
ENV APPDYNAMICS_CONTROLLER_SSL_ENABLED="true"
ENV APPDYNAMICS_JAVA_AGENT_REUSE_NODE_NAME="true"
ENV APPDYNAMICS_JAVA_AGENT_REUSE_NODE_NAME_PREFIX="intelligent-monitoring"

# OpenTelemetry integration with AppDynamics
ENV APPDYNAMICS_OPENTELEMETRY_ENABLED="true"
ENV APPDYNAMICS_OPENTELEMETRY_ENDPOINT="http://otel-collector:4317"
ENV APPDYNAMICS_OPENTELEMETRY_RESOURCE_ATTRIBUTES="service.name=intelligent-monitoring-service,service.version=1.0.0,deployment.environment=docker"

# Base JVM configuration with increased memory for code analysis
ENV JAVA_OPTS_BASE="-server -Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# AppDynamics agent JAR
ENV APPDYNAMICS_AGENT_JAR="-javaagent:/opt/appdynamics/javaagent.jar"

# AppDynamics system properties
ENV APPDYNAMICS_AGENT_PROPS="-Dappdynamics.agent.logs.dir=/app/logs -Dappdynamics.agent.runtime.dir=/opt/appdynamics/runtime"

# OpenTelemetry Java agent configuration
ENV OTEL_JAVAAGENT_JAR="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_JAVAAGENT_CONFIG="-Dotel.javaagent.configuration-file=/app/otel-config.properties"

EXPOSE 8090

# Start with both AppDynamics and OpenTelemetry agents
ENTRYPOINT ["sh", "-c", "echo 'Starting Intelligent Monitoring Service with AppDynamics + OpenTelemetry...'; java $JAVA_OPTS_BASE $APPDYNAMICS_AGENT_JAR $OTEL_JAVAAGENT_JAR $OTEL_JAVAAGENT_CONFIG -Dappdynamics.agent.applicationName=\"$APPDYNAMICS_AGENT_APPLICATION_NAME\" -Dappdynamics.agent.tierName=\"$APPDYNAMICS_AGENT_TIER_NAME\" -Dappdynamics.agent.nodeName=\"$APPDYNAMICS_AGENT_NODE_NAME\" -Dappdynamics.controller.hostName=\"$APPDYNAMICS_CONTROLLER_HOST_NAME\" -Dappdynamics.controller.port=\"$APPDYNAMICS_CONTROLLER_PORT\" -Dappdynamics.controller.ssl.enabled=\"$APPDYNAMICS_CONTROLLER_SSL_ENABLED\" -Dappdynamics.agent.accountName=\"$APPDYNAMICS_AGENT_ACCOUNT_NAME\" -Dappdynamics.agent.accountAccessKey=\"$APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY\" $APPDYNAMICS_AGENT_PROPS -jar app.jar"]