#!/bin/bash
# Script to update all microservices with OpenTelemetry integration

set -e

echo "🔄 Updating microservices with OpenTelemetry integration..."

# Services to update
services=("cart-service" "order-service" "api-gateway" "eureka-server")

for service in "${services[@]}"; do
    echo "📝 Updating ${service}..."
    
    dockerfile_path="${service}/Dockerfile"
    
    if [ -f "$dockerfile_path" ]; then
        # Backup original
        cp "$dockerfile_path" "${dockerfile_path}.backup"
        
        # Add OpenTelemetry environment variables after AppDynamics section
        sed -i.tmp '/ENV APPDYNAMICS_JAVA_AGENT_REUSE_NODE_NAME_PREFIX=.*$/a\
\
# OpenTelemetry integration with AppDynamics\
ENV APPDYNAMICS_OPENTELEMETRY_ENABLED="true"\
ENV APPDYNAMICS_OPENTELEMETRY_ENDPOINT="http://otel-collector:4317"\
ENV APPDYNAMICS_OPENTELEMETRY_RESOURCE_ATTRIBUTES="service.name='$service',service.version=1.0.0,deployment.environment=docker"' "$dockerfile_path"
        
        # Update APPDYNAMICS_AGENT_PROPS to include OpenTelemetry
        sed -i.tmp 's|ENV APPDYNAMICS_AGENT_PROPS="-Dappdynamics.agent.logs.dir=/app/logs -Dappdynamics.agent.runtime.dir=/opt/appdynamics/runtime"|ENV APPDYNAMICS_AGENT_PROPS="-Dappdynamics.agent.logs.dir=/app/logs -Dappdynamics.agent.runtime.dir=/opt/appdynamics/runtime -Dappdynamics.opentelemetry.enabled=true -Dappdynamics.opentelemetry.endpoint=http://otel-collector:4317 -Dappdynamics.opentelemetry.resource.attributes=service.name='$service',service.version=1.0.0,deployment.environment=docker"|' "$dockerfile_path"
        
        # Clean up temp files
        rm -f "${dockerfile_path}.tmp"
        
        echo "✅ Updated ${service}"
    else
        echo "⚠️  Dockerfile not found for ${service}"
    fi
done

echo "🎉 All services updated with OpenTelemetry integration!"