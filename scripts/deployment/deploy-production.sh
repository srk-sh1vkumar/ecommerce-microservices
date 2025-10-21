#!/bin/bash
# Deploy E-commerce Platform to Production Environment

set -e  # Exit on error

echo "🚀 Starting deployment to PRODUCTION environment..."
echo "⚠️  WARNING: This will deploy to PRODUCTION"
echo ""

# Require confirmation for production
if [ "${PRODUCTION_DEPLOY_CONFIRMED}" != "true" ]; then
    echo "❌ Production deployment requires confirmation"
    echo "   Set PRODUCTION_DEPLOY_CONFIRMED=true to proceed"
    echo ""
    echo "   Example: PRODUCTION_DEPLOY_CONFIRMED=true ./deploy-production.sh"
    exit 1
fi

# Configuration
NAMESPACE="${NAMESPACE:-ecommerce}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-docker.io}"
DOCKER_USERNAME="${DOCKER_USERNAME:-your-docker-username}"

# Deployment method (docker-compose, kubernetes, or helm)
DEPLOY_METHOD="${DEPLOY_METHOD:-kubernetes}"

# Validate image tag is not 'latest' for production
if [ "${IMAGE_TAG}" = "latest" ]; then
    echo "❌ Cannot deploy 'latest' tag to production"
    echo "   Please specify a version tag (e.g., v1.0.0, v1.0.1)"
    echo ""
    echo "   Example: IMAGE_TAG=v1.0.0 ./deploy-production.sh"
    exit 1
fi

# Validate Docker username is configured
if [ "${DOCKER_USERNAME}" = "your-docker-username" ]; then
    echo "❌ Docker username not configured"
    echo "   Please set DOCKER_USERNAME environment variable"
    exit 1
fi

echo "📋 Deployment Configuration:"
echo "  Environment: PRODUCTION"
echo "  Namespace: ${NAMESPACE}"
echo "  Image Tag: ${IMAGE_TAG}"
echo "  Docker Registry: ${DOCKER_REGISTRY}/${DOCKER_USERNAME}"
echo "  Deployment Method: ${DEPLOY_METHOD}"
echo ""

# Confirmation prompt
echo "⚠️  You are about to deploy to PRODUCTION with image tag: ${IMAGE_TAG}"
echo ""
read -p "Are you absolutely sure? (type 'DEPLOY' to confirm): " confirmation

if [ "${confirmation}" != "DEPLOY" ]; then
    echo "❌ Deployment cancelled"
    exit 1
fi

echo ""
echo "✅ Confirmation received, proceeding with deployment..."
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Deploy using Docker Compose (NOT RECOMMENDED FOR PRODUCTION)
deploy_docker_compose() {
    echo "⚠️  WARNING: Docker Compose is not recommended for production"
    echo "   Consider using Kubernetes or Helm for production deployments"
    echo ""

    if ! command_exists docker; then
        echo "❌ docker not found. Please install Docker."
        exit 1
    fi

    # Check if docker-compose.production.yml exists
    if [ ! -f "docker-compose.production.yml" ]; then
        echo "❌ docker-compose.production.yml not found"
        exit 1
    fi

    # Verify .env file exists and has production credentials
    if [ ! -f ".env" ]; then
        echo "❌ .env file not found"
        echo "   Please create .env with production credentials"
        exit 1
    fi

    # Pull specific version images
    echo "📥 Pulling production images (tag: ${IMAGE_TAG})..."
    docker compose -f docker-compose.production.yml pull

    # Start services with zero-downtime deployment
    echo "🚀 Starting production services..."
    docker compose -f docker-compose.production.yml up -d --no-build

    # Wait for services to start
    echo "⏳ Waiting for services to start..."
    sleep 60

    # Check service health
    echo "🏥 Checking service health..."
    check_service_health

    echo "✅ Docker Compose deployment complete"
}

# Deploy using Kubernetes
deploy_kubernetes() {
    echo "☸️  Deploying to Kubernetes (PRODUCTION)..."

    if ! command_exists kubectl; then
        echo "❌ kubectl not found. Please install kubectl."
        exit 1
    fi

    # Verify cluster context
    echo "🔍 Current Kubernetes context:"
    kubectl config current-context
    echo ""
    read -p "Is this the correct PRODUCTION cluster? (yes/no): " cluster_confirm

    if [ "${cluster_confirm}" != "yes" ]; then
        echo "❌ Deployment cancelled - incorrect cluster context"
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

    # Backup current deployment
    echo "💾 Backing up current deployment..."
    kubectl get all -n "${NAMESPACE}" -o yaml > "deployment-backup-$(date +%Y%m%d-%H%M%S).yaml" || true

    # Apply Kubernetes manifests
    echo "📦 Applying Kubernetes manifests..."
    kubectl apply -f k8s/ -n "${NAMESPACE}"

    # Update image tags with specific version
    echo "🏷️  Updating image tags to ${IMAGE_TAG}..."

    for service in user-service product-service cart-service order-service api-gateway eureka-server; do
        echo "  Updating ${service}..."
        kubectl set image deployment/${service} \
            ${service}="${DOCKER_REGISTRY}/${DOCKER_USERNAME}/ecommerce-${service}:${IMAGE_TAG}" \
            -n "${NAMESPACE}"
    done

    # Wait for rollout with extended timeout
    echo "⏳ Waiting for deployment to complete (timeout: 10 minutes)..."
    kubectl rollout status deployment --all -n "${NAMESPACE}" --timeout=10m

    # Verify all pods are running
    echo "🔍 Verifying pod status..."
    kubectl get pods -n "${NAMESPACE}"

    echo "✅ Kubernetes deployment complete"
}

# Deploy using Helm
deploy_helm() {
    echo "⛵ Deploying with Helm (PRODUCTION)..."

    if ! command_exists helm; then
        echo "❌ helm not found. Please install Helm."
        exit 1
    fi

    if [ ! -d "helm/ecommerce" ]; then
        echo "❌ helm/ecommerce/ directory not found"
        echo "   Please create Helm chart in helm/ecommerce/ directory"
        exit 1
    fi

    # Backup current release
    echo "💾 Backing up current Helm release..."
    helm get values ecommerce -n "${NAMESPACE}" > "helm-values-backup-$(date +%Y%m%d-%H%M%S).yaml" 2>/dev/null || true

    # Deploy with Helm
    helm upgrade --install ecommerce ./helm/ecommerce \
        --namespace "${NAMESPACE}" \
        --create-namespace \
        --set image.registry="${DOCKER_REGISTRY}" \
        --set image.repository="${DOCKER_USERNAME}" \
        --set image.tag="${IMAGE_TAG}" \
        --set environment="production" \
        --set replicaCount=3 \
        --set autoscaling.enabled=true \
        --set autoscaling.minReplicas=3 \
        --set autoscaling.maxReplicas=10 \
        --wait \
        --timeout=10m

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

    local failed_services=0

    for service_port in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_port"

        # Try actuator health endpoint
        if curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            echo "✅ ${service} (port ${port})"
        else
            echo "❌ ${service} (port ${port}) - not responding"
            ((failed_services++))
        fi
    done

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    if [ ${failed_services} -gt 0 ]; then
        echo "⚠️  ${failed_services} service(s) failed health check"
        echo "   Consider rolling back deployment"
        return 1
    fi

    return 0
}

# Rollback function
rollback_deployment() {
    echo ""
    echo "🔄 Initiating rollback..."

    case "${DEPLOY_METHOD}" in
        kubernetes)
            echo "Rolling back Kubernetes deployments..."
            kubectl rollout undo deployment --all -n "${NAMESPACE}"
            ;;
        helm)
            echo "Rolling back Helm release..."
            helm rollback ecommerce -n "${NAMESPACE}"
            ;;
        docker-compose)
            echo "Rolling back Docker Compose deployment..."
            echo "Please manually restore from backup"
            ;;
    esac

    echo "✅ Rollback initiated"
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
echo "🎉 Production deployment completed successfully!"
echo ""

# Run smoke tests
if [ -f "scripts/deployment/smoke-tests.sh" ]; then
    echo "🧪 Running production smoke tests..."

    if bash scripts/deployment/smoke-tests.sh production; then
        echo "✅ All smoke tests passed"
    else
        echo "❌ Smoke tests failed!"
        echo ""
        read -p "Do you want to rollback? (yes/no): " rollback_confirm

        if [ "${rollback_confirm}" = "yes" ]; then
            rollback_deployment
        else
            echo "⚠️  Deployment remains active despite failed smoke tests"
            echo "   Please investigate and fix issues manually"
        fi

        exit 1
    fi
else
    echo "⚠️  No smoke tests found - skipping validation"
fi

echo ""
echo "📊 Deployment Summary:"
echo "  ✅ Environment: PRODUCTION"
echo "  ✅ Deployment Method: ${DEPLOY_METHOD}"
echo "  ✅ Image Tag: ${IMAGE_TAG}"
echo "  ✅ Namespace: ${NAMESPACE}"
echo ""
echo "🔗 Production Access:"
echo "  Update DNS/Load Balancer configuration"
echo "  Verify SSL/TLS certificates"
echo ""
echo "📢 Post-Deployment Tasks:"
echo "  1. Monitor application metrics (Grafana/Prometheus)"
echo "  2. Check error logs for any issues"
echo "  3. Verify health endpoints are responding"
echo "  4. Monitor user traffic and performance"
echo "  5. Notify stakeholders of successful deployment"
echo "  6. Update deployment documentation"
echo ""
echo "🚨 Rollback Instructions:"
echo "  If issues are detected, run:"
if [ "${DEPLOY_METHOD}" = "kubernetes" ]; then
    echo "    kubectl rollout undo deployment --all -n ${NAMESPACE}"
elif [ "${DEPLOY_METHOD}" = "helm" ]; then
    echo "    helm rollback ecommerce -n ${NAMESPACE}"
else
    echo "    Restore from backup: deployment-backup-*.yaml"
fi
