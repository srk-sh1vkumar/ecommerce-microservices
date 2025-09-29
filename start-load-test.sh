#!/bin/bash

# E-Commerce Load Test Generator Script
# Usage: ./start-load-test.sh [pattern] [duration] [users]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
DEFAULT_DURATION=1800  # 30 minutes
DEFAULT_NETWORK="ecommerce-microservices_ecommerce-network"
DEFAULT_IMAGE="ecommerce-microservices-load-generator"

# Function to print colored output
print_color() {
    echo -e "${1}${2}${NC}"
}

# Function to print usage
print_usage() {
    print_color $BLUE "🚀 E-Commerce Load Test Generator"
    echo "================================================"
    echo ""
    echo "Usage: $0 [PATTERN] [DURATION] [USERS]"
    echo ""
    print_color $YELLOW "Available Patterns:"
    echo "  light      - Light load (5 users, slow ramp)"
    echo "  normal     - Normal traffic (10 users, medium ramp)" 
    echo "  peak       - Peak traffic (20 users, fast ramp)"
    echo "  stress     - Stress test (50 users, very fast ramp)"
    echo "  burst      - Burst traffic (25 users, quick bursts)"
    echo "  mobile     - Mobile traffic pattern (8 users, frontend focus)"
    echo "  international - International traffic (12 users, slower)"
    echo "  black-friday - Black Friday simulation (100 users, intense)"
    echo "  custom     - Custom pattern (specify users and duration)"
    echo "  all        - Run all patterns simultaneously"
    echo ""
    print_color $YELLOW "Examples:"
    echo "  $0 normal                    # Normal load for 30 minutes"
    echo "  $0 peak 900                  # Peak load for 15 minutes" 
    echo "  $0 custom 600 15             # 15 users for 10 minutes"
    echo "  $0 all                       # All patterns for comprehensive test"
    echo ""
    print_color $YELLOW "Management Commands:"
    echo "  $0 stop                      # Stop all load generators"
    echo "  $0 status                    # Show status of all generators"
    echo "  $0 monitor                   # Start real-time monitoring"
    echo "  $0 cleanup                   # Clean up all load test containers"
}

# Function to check if Docker network exists
check_network() {
    if ! docker network ls | grep -q "$DEFAULT_NETWORK"; then
        print_color $RED "❌ Error: Docker network '$DEFAULT_NETWORK' not found"
        print_color $YELLOW "💡 Make sure your e-commerce services are running:"
        echo "   docker-compose up -d"
        exit 1
    fi
}

# Function to check if load generator image exists
check_image() {
    if ! docker images | grep -q "$DEFAULT_IMAGE"; then
        print_color $RED "❌ Error: Load generator image '$DEFAULT_IMAGE' not found"
        print_color $YELLOW "💡 Build the image first:"
        echo "   docker-compose build load-generator"
        exit 1
    fi
}

# Function to generate unique container name
generate_name() {
    local pattern=$1
    echo "load-gen-${pattern}-$(date +%s)"
}

# Function to start a load generator
start_generator() {
    local name=$1
    local users=$2
    local spawn_rate=$3
    local duration=$4
    local target=$5
    local service_name=$6
    
    print_color $GREEN "🚀 Starting load generator: $name"
    print_color $CYAN "   Users: $users | Spawn Rate: $spawn_rate/s | Duration: ${duration}s | Target: $target"
    
    docker run -d \
        --name "$name" \
        --network "$DEFAULT_NETWORK" \
        -e CONCURRENT_USERS="$users" \
        -e SPAWN_RATE="$spawn_rate" \
        -e DURATION="$duration" \
        -e TARGET_BASE_URL="$target" \
        -e OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317" \
        -e OTEL_SERVICE_NAME="$service_name" \
        "$DEFAULT_IMAGE" \
        >/dev/null
    
    echo "   ✅ Container ID: $(docker ps -q -f name=$name)"
}

# Function to start light load
start_light() {
    local duration=${1:-$DEFAULT_DURATION}
    local name=$(generate_name "light")
    start_generator "$name" 5 0.5 "$duration" "http://frontend:80" "load-generator-light"
}

# Function to start normal load  
start_normal() {
    local duration=${1:-$DEFAULT_DURATION}
    local name=$(generate_name "normal")
    start_generator "$name" 10 1 "$duration" "http://api-gateway:8080" "load-generator-normal"
}

# Function to start peak load
start_peak() {
    local duration=${1:-900}  # Default 15 minutes for peak
    local name=$(generate_name "peak")
    start_generator "$name" 20 3 "$duration" "http://api-gateway:8080" "load-generator-peak"
}

# Function to start stress test
start_stress() {
    local duration=${1:-600}  # Default 10 minutes for stress
    local name=$(generate_name "stress")
    start_generator "$name" 50 5 "$duration" "http://api-gateway:8080" "load-generator-stress"
}

# Function to start burst load
start_burst() {
    local duration=${1:-600}  # Default 10 minutes
    local name=$(generate_name "burst")
    start_generator "$name" 25 8 "$duration" "http://api-gateway:8080" "load-generator-burst"
}

# Function to start mobile pattern
start_mobile() {
    local duration=${1:-$DEFAULT_DURATION}
    local name=$(generate_name "mobile")
    start_generator "$name" 8 2 "$duration" "http://frontend:80" "load-generator-mobile"
}

# Function to start international pattern
start_international() {
    local duration=${1:-$DEFAULT_DURATION}
    local name=$(generate_name "international")
    start_generator "$name" 12 1.5 "$duration" "http://api-gateway:8080" "load-generator-international"
}

# Function to start Black Friday simulation
start_black_friday() {
    local duration=${1:-1200}  # Default 20 minutes
    local name=$(generate_name "blackfriday")
    start_generator "$name" 100 10 "$duration" "http://api-gateway:8080" "load-generator-blackfriday"
    
    # Add additional burst generators
    sleep 5
    local name2=$(generate_name "blackfriday-burst1")
    start_generator "$name2" 50 15 "$duration" "http://frontend:80" "load-generator-blackfriday-frontend"
    
    sleep 5
    local name3=$(generate_name "blackfriday-burst2")
    start_generator "$name3" 30 8 "$duration" "http://api-gateway:8080" "load-generator-blackfriday-api"
}

# Function to start custom load
start_custom() {
    local duration=${1:-$DEFAULT_DURATION}
    local users=${2:-10}
    local spawn_rate=${3:-2}
    local target=${4:-"http://api-gateway:8080"}
    local name=$(generate_name "custom")
    start_generator "$name" "$users" "$spawn_rate" "$duration" "$target" "load-generator-custom"
}

# Function to start all patterns
start_all() {
    print_color $PURPLE "🎯 Starting comprehensive load test with all patterns..."
    echo ""
    
    start_light 1800
    sleep 3
    start_normal 1800  
    sleep 3
    start_peak 900
    sleep 3
    start_burst 600
    sleep 3
    start_mobile 1800
    sleep 3
    start_international 1800
    
    echo ""
    print_color $GREEN "✅ All load generators started!"
    show_status
}

# Function to stop all load generators
stop_all() {
    print_color $YELLOW "🛑 Stopping all load generators..."
    
    containers=$(docker ps -q -f name=load-gen)
    if [ -z "$containers" ]; then
        print_color $YELLOW "No load generators running"
        return
    fi
    
    echo "$containers" | xargs docker stop >/dev/null
    print_color $GREEN "✅ All load generators stopped"
}

# Function to show status
show_status() {
    print_color $BLUE "📊 Load Generator Status"
    echo "=========================="
    
    local generators=$(docker ps -f name=load-gen --format "table {{.Names}}\t{{.Status}}\t{{.CreatedAt}}")
    if [ "$(echo "$generators" | wc -l)" -eq 1 ]; then
        print_color $YELLOW "No load generators currently running"
        echo ""
        print_color $CYAN "💡 Start a load test:"
        echo "   $0 normal"
        return
    fi
    
    echo "$generators"
    echo ""
    
    # Show total resource usage
    print_color $CYAN "🔥 System Impact:"
    docker stats --no-stream --format "{{.Name}}: {{.CPUPerc}} CPU, {{.MemUsage}} RAM" | grep load-gen | head -5
    echo ""
    
    # Show trace collection
    print_color $CYAN "🔍 Trace Collection:"
    local traces=$(curl -s "http://localhost:3200/api/search?tags=" 2>/dev/null | jq -r '.metrics.inspectedTraces // "N/A"' 2>/dev/null || echo "N/A")
    echo "Total traces collected: $traces"
    echo ""
    
    print_color $CYAN "🎛️ Monitoring Access:"
    echo "• Grafana: http://localhost:3000"
    echo "• Intelligent Dashboard: http://localhost:8888/monitoring-dashboard.html"
    echo "• Tracing: http://localhost:3000/d/4851101a-4940-4db6-8cd8-12e8da717b6d"
}

# Function to cleanup
cleanup() {
    print_color $YELLOW "🧹 Cleaning up load test containers..."
    
    # Stop and remove all load generator containers
    containers=$(docker ps -aq -f name=load-gen)
    if [ ! -z "$containers" ]; then
        echo "$containers" | xargs docker rm -f >/dev/null
        print_color $GREEN "✅ Cleanup completed"
    else
        print_color $YELLOW "No load generator containers to clean up"
    fi
}

# Function to start monitoring
start_monitoring() {
    print_color $BLUE "📈 Starting real-time monitoring..."
    
    if [ -f "./monitor-extended-load.sh" ]; then
        ./monitor-extended-load.sh
    else
        print_color $YELLOW "Monitoring script not found, showing basic status..."
        while true; do
            clear
            show_status
            sleep 30
        done
    fi
}

# Main script logic
main() {
    local pattern=${1:-"help"}
    local duration=${2:-$DEFAULT_DURATION}
    local users=${3:-10}
    
    # Check prerequisites
    check_network
    check_image
    
    case $pattern in
        "help"|"-h"|"--help")
            print_usage
            ;;
        "light")
            start_light "$duration"
            ;;
        "normal")
            start_normal "$duration"
            ;;
        "peak")
            start_peak "$duration"
            ;;
        "stress")
            start_stress "$duration"
            ;;
        "burst")
            start_burst "$duration"
            ;;
        "mobile")
            start_mobile "$duration"
            ;;
        "international")
            start_international "$duration"
            ;;
        "black-friday")
            start_black_friday "$duration"
            ;;
        "custom")
            start_custom "$duration" "$users"
            ;;
        "all")
            start_all
            ;;
        "stop")
            stop_all
            ;;
        "status")
            show_status
            ;;
        "monitor")
            start_monitoring
            ;;
        "cleanup")
            cleanup
            ;;
        *)
            print_color $RED "❌ Unknown pattern: $pattern"
            echo ""
            print_usage
            exit 1
            ;;
    esac
}

# Handle Ctrl+C gracefully
trap 'echo ""; print_color $YELLOW "🛑 Interrupted by user"; exit 0' INT

# Run main function
main "$@"