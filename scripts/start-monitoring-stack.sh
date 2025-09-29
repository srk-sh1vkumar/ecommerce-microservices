#!/bin/bash
# Start the complete monitoring and observability stack

set -e

echo "🚀 Starting E-commerce Microservices with Full Observability Stack"
echo "=================================================================="

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "❌ .env file not found. Please create it with AppDynamics credentials."
    echo "Required variables:"
    echo "  APPDYNAMICS_CONTROLLER_HOST_NAME"
    echo "  APPDYNAMICS_AGENT_ACCOUNT_NAME" 
    echo "  APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY"
    exit 1
fi

echo "📋 Starting infrastructure services..."
docker-compose up -d mongodb eureka-server

echo "⏳ Waiting for infrastructure to be ready..."
sleep 30

echo "🏗️ Building and starting microservices..."
docker-compose up -d user-service product-service cart-service order-service api-gateway

echo "⏳ Waiting for microservices to be ready..."
sleep 45

echo "📊 Starting monitoring stack..."
docker-compose up -d tempo prometheus otel-collector

echo "⏳ Waiting for monitoring stack to be ready..."
sleep 30

echo "🌐 Starting frontend with OpenTelemetry..."
docker-compose up -d frontend

echo "📈 Starting Grafana..."
docker-compose up -d grafana

echo "⏳ Waiting for all services to be healthy..."
sleep 30

echo "✅ All services started! Here are the access points:"
echo ""
echo "🌐 Applications:"
echo "  Frontend:           http://localhost"
echo "  API Gateway:        http://localhost:8081"
echo "  Eureka Dashboard:   http://localhost:8761"
echo ""
echo "📊 Monitoring & Observability:"
echo "  Grafana:           http://localhost:3000 (admin/YOUR_ADMIN_PASSWORD)"
echo "  Prometheus:        http://localhost:9090"
echo "  Tempo:             http://localhost:3200"
echo "  OTEL Collector:    http://localhost:13133/health"
echo ""
echo "🔍 Service Health Checks:"
docker-compose ps

echo ""
echo "🧪 To start load testing (generates realistic user traffic):"
echo "  docker-compose --profile load-testing up -d load-generator"
echo ""
echo "📋 To view logs:"
echo "  docker-compose logs -f [service-name]"
echo ""
echo "🎯 Key Features Enabled:"
echo "  ✅ AppDynamics APM monitoring"
echo "  ✅ OpenTelemetry distributed tracing" 
echo "  ✅ Tempo trace storage"
echo "  ✅ Prometheus metrics collection"
echo "  ✅ Grafana visualization"
echo "  ✅ Apache HTTP monitoring"
echo "  ✅ Realistic load generator"
echo ""
echo "🎉 Your e-commerce platform is ready with full observability!"