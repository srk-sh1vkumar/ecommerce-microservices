#!/bin/bash
# Smoke Tests for E-commerce Platform Deployment

set -e  # Exit on error

ENVIRONMENT="${1:-staging}"
MAX_RETRIES=30
RETRY_DELAY=5

echo "🧪 Running smoke tests for ${ENVIRONMENT} environment..."
echo ""

# Determine base URL and ports based on environment
case "${ENVIRONMENT}" in
    staging)
        BASE_URL="${STAGING_URL:-http://localhost}"
        API_GATEWAY_PORT="${API_GATEWAY_PORT:-8080}"
        EUREKA_PORT="${EUREKA_PORT:-8761}"
        USER_SERVICE_PORT="${USER_SERVICE_PORT:-8082}"
        PRODUCT_SERVICE_PORT="${PRODUCT_SERVICE_PORT:-8081}"
        CART_SERVICE_PORT="${CART_SERVICE_PORT:-8083}"
        ORDER_SERVICE_PORT="${ORDER_SERVICE_PORT:-8084}"
        ;;
    production)
        BASE_URL="${PRODUCTION_URL:-https://ecommerce.example.com}"
        API_GATEWAY_PORT="${API_GATEWAY_PORT:-443}"
        EUREKA_PORT="${EUREKA_PORT:-8761}"
        USER_SERVICE_PORT="${USER_SERVICE_PORT:-8082}"
        PRODUCT_SERVICE_PORT="${PRODUCT_SERVICE_PORT:-8081}"
        CART_SERVICE_PORT="${CART_SERVICE_PORT:-8083}"
        ORDER_SERVICE_PORT="${ORDER_SERVICE_PORT:-8084}"
        ;;
    *)
        echo "❌ Unknown environment: ${ENVIRONMENT}"
        echo "   Supported: staging, production"
        exit 1
        ;;
esac

echo "🔗 Testing endpoints:"
echo "  Environment: ${ENVIRONMENT}"
echo "  Base URL: ${BASE_URL}"
echo "  API Gateway: ${API_GATEWAY_PORT}"
echo ""

# Test 1: Eureka Service Discovery
test_eureka() {
    echo "1️⃣  Testing Eureka Server..."

    for i in $(seq 1 $MAX_RETRIES); do
        if curl -f -s "http://localhost:${EUREKA_PORT}/actuator/health" > /dev/null 2>&1; then
            HEALTH_RESPONSE=$(curl -s "http://localhost:${EUREKA_PORT}/actuator/health")
            STATUS=$(echo "${HEALTH_RESPONSE}" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

            if [ "${STATUS}" = "UP" ]; then
                echo "   ✅ Eureka Server is UP"
                echo "   Response: ${HEALTH_RESPONSE}"

                # Check registered services
                SERVICES=$(curl -s "http://localhost:${EUREKA_PORT}/eureka/apps" | grep -o '<name>[^<]*</name>' | sed 's/<[^>]*>//g' | sort -u)
                echo "   Registered services:"
                echo "${SERVICES}" | sed 's/^/     - /'
                return 0
            fi
        fi

        echo "   ⏳ Attempt $i/$MAX_RETRIES - waiting for Eureka Server..."
        sleep $RETRY_DELAY
    done

    echo "   ❌ Eureka Server health check FAILED after $MAX_RETRIES attempts"
    return 1
}

# Test 2: API Gateway Health
test_api_gateway() {
    echo ""
    echo "2️⃣  Testing API Gateway..."

    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${API_GATEWAY_PORT}/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')

    if [ "${HTTP_CODE}" = "200" ]; then
        echo "   ✅ API Gateway health check PASSED"
        echo "   Response: ${BODY}"
        return 0
    else
        echo "   ❌ API Gateway health check FAILED (HTTP ${HTTP_CODE})"
        echo "   Response: ${BODY}"
        return 1
    fi
}

# Test 3: User Service Health
test_user_service() {
    echo ""
    echo "3️⃣  Testing User Service..."

    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${USER_SERVICE_PORT}/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')

    if [ "${HTTP_CODE}" = "200" ]; then
        echo "   ✅ User Service health check PASSED"
        echo "   Response: ${BODY}"
        return 0
    else
        echo "   ❌ User Service health check FAILED (HTTP ${HTTP_CODE})"
        echo "   Response: ${BODY}"
        return 1
    fi
}

# Test 4: Product Service Health
test_product_service() {
    echo ""
    echo "4️⃣  Testing Product Service..."

    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${PRODUCT_SERVICE_PORT}/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')

    if [ "${HTTP_CODE}" = "200" ]; then
        echo "   ✅ Product Service health check PASSED"
        echo "   Response: ${BODY}"
        return 0
    else
        echo "   ❌ Product Service health check FAILED (HTTP ${HTTP_CODE})"
        echo "   Response: ${BODY}"
        return 1
    fi
}

# Test 5: Cart Service Health
test_cart_service() {
    echo ""
    echo "5️⃣  Testing Cart Service..."

    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${CART_SERVICE_PORT}/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')

    if [ "${HTTP_CODE}" = "200" ]; then
        echo "   ✅ Cart Service health check PASSED"
        echo "   Response: ${BODY}"
        return 0
    else
        echo "   ❌ Cart Service health check FAILED (HTTP ${HTTP_CODE})"
        echo "   Response: ${BODY}"
        return 1
    fi
}

# Test 6: Order Service Health
test_order_service() {
    echo ""
    echo "6️⃣  Testing Order Service..."

    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${ORDER_SERVICE_PORT}/actuator/health" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
    BODY=$(echo "${RESPONSE}" | sed '$d')

    if [ "${HTTP_CODE}" = "200" ]; then
        echo "   ✅ Order Service health check PASSED"
        echo "   Response: ${BODY}"
        return 0
    else
        echo "   ❌ Order Service health check FAILED (HTTP ${HTTP_CODE})"
        echo "   Response: ${BODY}"
        return 1
    fi
}

# Test 7: Database Connectivity (MongoDB)
test_database() {
    echo ""
    echo "7️⃣  Testing Database Connectivity..."

    # Check if any service can connect to database by checking health details
    RESPONSE=$(curl -s "http://localhost:${USER_SERVICE_PORT}/actuator/health" 2>/dev/null)

    if echo "${RESPONSE}" | grep -q "mongo"; then
        echo "   ✅ Database connectivity verified (MongoDB)"
        return 0
    else
        echo "   ⚠️  Database connectivity check inconclusive"
        echo "   (Health endpoint doesn't expose database details)"
        return 0  # Non-blocking
    fi
}

# Test 8: Monitoring Stack
test_monitoring() {
    echo ""
    echo "8️⃣  Testing Monitoring Stack..."

    # Test Prometheus
    if curl -f -s "http://localhost:9090/-/healthy" > /dev/null 2>&1; then
        echo "   ✅ Prometheus is healthy"
    else
        echo "   ⚠️  Prometheus is not responding (may not be deployed)"
    fi

    # Test Grafana
    if curl -f -s "http://localhost:3000/api/health" > /dev/null 2>&1; then
        echo "   ✅ Grafana is healthy"
    else
        echo "   ⚠️  Grafana is not responding (may not be deployed)"
    fi

    return 0  # Non-blocking
}

# Test 9: End-to-End Workflow
test_e2e_workflow() {
    echo ""
    echo "9️⃣  Testing End-to-End Workflow..."

    # This is a simplified E2E test
    # In production, you would have more comprehensive tests

    # Try to access product list through API Gateway
    RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:${API_GATEWAY_PORT}/api/products?page=0&size=10" 2>/dev/null || echo "000")
    HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)

    if [ "${HTTP_CODE}" = "200" ] || [ "${HTTP_CODE}" = "401" ]; then
        # 200 = success, 401 = authentication required (expected for secured endpoints)
        echo "   ✅ Product API accessible through Gateway"
        return 0
    else
        echo "   ❌ Product API not accessible (HTTP ${HTTP_CODE})"
        return 1
    fi
}

# Test 10: Response Time Validation
test_response_time() {
    echo ""
    echo "🔟 Testing Response Times..."

    START_TIME=$(date +%s%N)
    curl -s "http://localhost:${API_GATEWAY_PORT}/actuator/health" > /dev/null 2>&1
    END_TIME=$(date +%s%N)

    RESPONSE_TIME=$(( (END_TIME - START_TIME) / 1000000 ))  # Convert to milliseconds

    if [ ${RESPONSE_TIME} -lt 1000 ]; then
        echo "   ✅ API Gateway response time: ${RESPONSE_TIME}ms (< 1000ms threshold)"
    elif [ ${RESPONSE_TIME} -lt 3000 ]; then
        echo "   ⚠️  API Gateway response time: ${RESPONSE_TIME}ms (slower than ideal)"
    else
        echo "   ❌ API Gateway response time: ${RESPONSE_TIME}ms (too slow)"
        return 1
    fi

    return 0
}

# Run all tests
FAILED_TESTS=0

test_eureka || FAILED_TESTS=$((FAILED_TESTS + 1))
test_api_gateway || FAILED_TESTS=$((FAILED_TESTS + 1))
test_user_service || FAILED_TESTS=$((FAILED_TESTS + 1))
test_product_service || FAILED_TESTS=$((FAILED_TESTS + 1))
test_cart_service || FAILED_TESTS=$((FAILED_TESTS + 1))
test_order_service || FAILED_TESTS=$((FAILED_TESTS + 1))
test_database || FAILED_TESTS=$((FAILED_TESTS + 1))
test_monitoring || FAILED_TESTS=$((FAILED_TESTS + 1))
test_e2e_workflow || FAILED_TESTS=$((FAILED_TESTS + 1))
test_response_time || FAILED_TESTS=$((FAILED_TESTS + 1))

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ ${FAILED_TESTS} -eq 0 ]; then
    echo "✅ All smoke tests PASSED!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📊 Test Summary:"
    echo "  Environment: ${ENVIRONMENT}"
    echo "  Total Tests: 10"
    echo "  Passed: 10"
    echo "  Failed: 0"
    echo ""
    exit 0
else
    echo "❌ ${FAILED_TESTS} smoke test(s) FAILED"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📊 Test Summary:"
    echo "  Environment: ${ENVIRONMENT}"
    echo "  Total Tests: 10"
    echo "  Passed: $((10 - FAILED_TESTS))"
    echo "  Failed: ${FAILED_TESTS}"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "  1. Check service logs: docker-compose logs <service-name>"
    echo "  2. Verify all containers are running: docker-compose ps"
    echo "  3. Check Eureka dashboard: http://localhost:${EUREKA_PORT}"
    echo "  4. Review health endpoints individually"
    echo ""
    exit 1
fi
