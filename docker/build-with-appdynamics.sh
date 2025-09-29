#!/bin/bash

# Build script for all microservices with AppDynamics Java Agent 25.6
# Usage: ./build-with-appdynamics.sh [service-name] [service-port]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if service name and port are provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <service-name> <service-port>"
    echo "Available services:"
    echo "  user-service 8081"
    echo "  product-service 8082"
    echo "  cart-service 8083"
    echo "  order-service 8084"
    echo "  api-gateway 8080"
    echo "  eureka-server 8761"
    echo ""
    echo "Example: $0 user-service 8081"
    exit 1
fi

SERVICE_NAME="$1"
SERVICE_PORT="$2"
DOCKER_TAG="ecommerce/${SERVICE_NAME}:latest"
APPDYNAMICS_VERSION="25.6.0"

# Validate service directory exists
if [ ! -d "../${SERVICE_NAME}" ]; then
    print_error "Service directory '../${SERVICE_NAME}' not found!"
    exit 1
fi

print_status "Building ${SERVICE_NAME} with AppDynamics Java Agent ${APPDYNAMICS_VERSION}..."

# Check if AppDynamics credentials are set
if [ -z "$APPDYNAMICS_CONTROLLER_HOST_NAME" ] || [ -z "$APPDYNAMICS_AGENT_ACCOUNT_NAME" ] || [ -z "$APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY" ]; then
    print_warning "AppDynamics credentials not set in environment variables."
    print_warning "Please set the following environment variables:"
    print_warning "  - APPDYNAMICS_CONTROLLER_HOST_NAME"
    print_warning "  - APPDYNAMICS_AGENT_ACCOUNT_NAME"
    print_warning "  - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY"
    print_warning "The image will be built but AppDynamics monitoring will not work until these are configured."
fi

# Build the Docker image
print_status "Building Docker image for ${SERVICE_NAME}..."

docker build \
    --build-arg SERVICE_NAME="${SERVICE_NAME}" \
    --build-arg SERVICE_PORT="${SERVICE_PORT}" \
    --build-arg APPDYNAMICS_AGENT_VERSION="${APPDYNAMICS_VERSION}" \
    -f Dockerfile.template \
    -t "${DOCKER_TAG}" \
    "../${SERVICE_NAME}" || {
    print_error "Failed to build Docker image for ${SERVICE_NAME}"
    exit 1
}

print_status "Successfully built Docker image: ${DOCKER_TAG}"

# Optionally run the container for testing
read -p "Do you want to run the container for testing? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_status "Starting container for ${SERVICE_NAME}..."
    
    docker run -d \
        --name "${SERVICE_NAME}-test" \
        -p "${SERVICE_PORT}:${SERVICE_PORT}" \
        -e APPDYNAMICS_CONTROLLER_HOST_NAME="${APPDYNAMICS_CONTROLLER_HOST_NAME:-}" \
        -e APPDYNAMICS_AGENT_ACCOUNT_NAME="${APPDYNAMICS_AGENT_ACCOUNT_NAME:-}" \
        -e APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY="${APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY:-}" \
        -e SPRING_PROFILES_ACTIVE="docker" \
        "${DOCKER_TAG}" || {
        print_error "Failed to start container for ${SERVICE_NAME}"
        exit 1
    }
    
    print_status "Container started successfully!"
    print_status "Service URL: http://localhost:${SERVICE_PORT}"
    print_status "Health Check: http://localhost:${SERVICE_PORT}/actuator/health"
    print_status "Metrics: http://localhost:${SERVICE_PORT}/actuator/prometheus"
    
    # Wait for the service to start
    print_status "Waiting for service to start..."
    sleep 30
    
    # Check health
    if curl -f "http://localhost:${SERVICE_PORT}/actuator/health" > /dev/null 2>&1; then
        print_status "Service is healthy!"
    else
        print_warning "Service health check failed. Check container logs:"
        print_warning "docker logs ${SERVICE_NAME}-test"
    fi
    
    print_status "To stop the test container: docker stop ${SERVICE_NAME}-test && docker rm ${SERVICE_NAME}-test"
fi

print_status "Build completed for ${SERVICE_NAME}"