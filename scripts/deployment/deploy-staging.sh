#!/bin/bash
# Deploy E-commerce Platform to Staging Environment

set -e  # Exit on error

echo "🚀 Starting deployment to STAGING environment..."
echo ""

# Configuration
NAMESPACE="${NAMESPACE:-ecommerce-staging}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-docker.io}"
DOCKER_USERNAME="${DOCKER_USERNAME:-your-docker-username}"

# Deployment method (docker-compose, kubernetes, or helm)
DEPLOY_METHOD="${DEPLOY_METHOD:-docker-compose}"

echo "📋 Deployment Configuration:"
echo "  Environment: STAGING"
echo "  Namespace: ${NAMESPACE}"
echo "  Image Tag: ${IMAGE_TAG}"
echo "  Docker Registry: ${DOCKER_REGISTRY}"
echo "  Deployment Method: ${DEPLOY_METHOD}"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Deploy using Docker Compose
deploy_docker_compose() {
    echo "🐳 Deploying with Docker Compose..."

    if ! command_exists docker; then
        echo "❌ docker not found. Please install Docker."
        exit 1
    fi

    # Check if docker-compose.yml exists
    if [ ! -f "docker-compose.yml" ]; then
        echo "❌ docker-compose.yml not found"
        exit 1
    fi

    # Create .env file if it doesn't exist
    if [ ! -f ".env" ]; then
        echo "⚠️  .env file not found, creating template..."
        cat > .env << EOF
# MongoDB Configuration
MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=staging_password_change_me

# Redis Configuration
REDIS_PASSWORD=redis_staging_password

# JWT Configuration
JWT_SECRET=staging_jwt_secret_change_me

# AppDynamics Configuration (optional)
APPDYNAMICS_CONTROLLER_HOST_NAME=
APPDYNAMICS_AGENT_ACCOUNT_NAME=
APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=

# Email Configuration (optional)
GMAIL_USERNAME=
GMAIL_PASSWORD=

# Payment Configuration (optional)
STRIPE_API_KEY=
EOF
        echo "⚠️  Please update .env file with actual credentials before deploying!"
        echo "   Edit .env and run this script again."
        exit 1
    fi

    # Build services first
    echo "🔨 Building all services..."
    if [ -f "build-all.sh" ]; then
        ./build-all.sh
    else
        echo "⚠️  build-all.sh not found, building with Maven..."
        mvn clean install -DskipTests
    fi

    # Pull latest images (if using pre-built images)
    echo "📥 Pulling latest images..."
    docker compose pull || echo "⚠️  Could not pull images (expected if using local builds)"

    # Start services
    echo "🚀 Starting services..."
    docker compose up -d --build

    # Wait for services to start
    echo "⏳ Waiting for services to start..."
    sleep 45

    # Check service health
    echo "🏥 Checking service health..."
    check_service_health

    echo "✅ Docker Compose deployment complete"
}

# Deploy using Kubernetes
deploy_kubernetes() {
    echo "☸️  Deploying to Kubernetes..."

    if ! command_exists kubectl; then
        echo "❌ kubectl not found. Please install kubectl."
        exit 1
    fi

    # Create namespace if it doesn't exist
    kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

    # Check if k8s directory exists
    if [ ! -d "k8s" ]; then
        echo "❌ k8s/ directory not found"
        echo "   Please create Kubernetes manifests in k8s/ directory"
        exit 1
    fi

    # Apply Kubernetes manifests
    echo "📦 Applying Kubernetes manifests..."
    kubectl apply -f k8s/ -n "${NAMESPACE}"

    # Update image tags if using custom images
    if [ "${DOCKER_USERNAME}" != "your-docker-username" ]; then
        echo "🏷️  Updating image tags..."

        for service in user-service product-service cart-service order-service api-gateway eureka-server; do
            kubectl set image deployment/${service} \
                ${service}="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/ecommerce-${service}:${IMAGE_TAG}" \
                -n "${NAMESPACE}" 2>/dev/null || echo "⚠️  Could not update ${service}"
        done
    fi

    # Wait for rollout
    echo "⏳ Waiting for deployment to complete..."
    kubectl rollout status deployment --all -n "${NAMESPACE}" --timeout=5m

    echo "✅ Kubernetes deployment complete"
}

# Deploy using Helm
deploy_helm() {
    echo "⛵ Deploying with Helm..."

    if ! command_exists helm; then
        echo "❌ helm not found. Please install Helm."
        exit 1
    fi

    if [ ! -d "helm/ecommerce" ]; then
        echo "❌ helm/ecommerce/ directory not found"
        echo "   Please create Helm chart in helm/ecommerce/ directory"
        exit 1
    fi

    helm upgrade --install ecommerce ./helm/ecommerce \
        --namespace "${NAMESPACE}" \
        --create-namespace \
        --set image.registry="${DOCKER_REGISTRY}" \
        --set image.repository="${DOCKER_USERNAME}" \
        --set image.tag="${IMAGE_TAG}" \
        --set environment="staging" \
        --wait \
        --timeout=5m

    echo "✅ Helm deployment complete"
}

# Check service health
check_service_health() {
    local services=(
        "eureka-server:8761"
        "api-gateway:8080"
        "user-service:8082"
        "product-service:8081"
        "cart-service:8083"
        "order-service:8084"
    )

    echo ""
    echo "🏥 Health Check Results:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    for service_port in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_port"

        # Try actuator health endpoint
        if curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            echo "✅ ${service} (port ${port})"
        else
            echo "❌ ${service} (port ${port}) - not responding"
        fi
    done

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Main deployment logic
case "${DEPLOY_METHOD}" in
    docker-compose)
        deploy_docker_compose
        ;;
    kubernetes)
        deploy_kubernetes
        ;;
    helm)
        deploy_helm
        ;;
    *)
        echo "❌ Unknown deployment method: ${DEPLOY_METHOD}"
        echo "   Supported: docker-compose, kubernetes, helm"
        exit 1
        ;;
esac

echo ""
echo "🎉 Staging deployment completed successfully!"
echo ""

# Run smoke tests
if [ -f "scripts/deployment/smoke-tests.sh" ]; then
    echo "🧪 Running smoke tests..."
    bash scripts/deployment/smoke-tests.sh staging
else
    echo "⚠️  No smoke tests found - skipping"
fi

echo ""
echo "📊 Deployment Summary:"
echo "  ✅ Environment: STAGING"
echo "  ✅ Deployment Method: ${DEPLOY_METHOD}"
echo "  ✅ Image Tag: ${IMAGE_TAG}"
echo ""

if [ "${DEPLOY_METHOD}" == "docker-compose" ]; then
    echo "🔗 Access Points:"
    echo "  Customer Portal: http://localhost:80"
    echo "  API Gateway: http://localhost:8080"
    echo "  Eureka Dashboard: http://localhost:8761"
    echo "  Grafana: http://localhost:3000"
    echo "  Prometheus: http://localhost:9090"
else
    echo "🔗 Access staging at:"
    echo "  Update kubectl context to access services"
    echo "  kubectl get svc -n ${NAMESPACE}"
fi

echo ""
echo "📝 Next Steps:"
echo "  1. Verify all services are running"
echo "  2. Run integration tests"
echo "  3. Monitor application metrics"
echo "  4. Review logs for any errors"
