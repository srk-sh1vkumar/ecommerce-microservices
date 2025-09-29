#!/bin/bash

echo "Building all microservices..."

# Build parent project first
echo "Building parent project..."
mvn clean install -DskipTests

# Build each service
services=("eureka-server" "user-service" "product-service" "cart-service" "order-service" "api-gateway")

for service in "${services[@]}"; do
    echo "Building $service..."
    cd $service
    mvn clean package -DskipTests
    cd ..
done

echo "All services built successfully!"
echo "You can now run: docker-compose up --build"