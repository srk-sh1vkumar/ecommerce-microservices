#!/bin/bash

# Setup script for comprehensive monitoring stack
# Combines AppDynamics APM with Grafana Tempo tracing

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check Docker Compose
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not available. Please install Docker Compose."
        exit 1
    fi
    
    print_status "Docker and Docker Compose are available"
}

# Setup directory structure
setup_directories() {
    print_header "Setting Up Directory Structure"
    
    mkdir -p {prometheus/alerts,grafana/{dashboards,dashboard-configs},alertmanager,logstash/{pipeline,config},filebeat}
    
    print_status "Directory structure created"
}

# Generate configuration files
generate_configs() {
    print_header "Generating Configuration Files"
    
    # Prometheus configuration
    cat > prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    cluster: 'ecommerce-local'
    environment: 'development'

rule_files:
  - "alerts/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # Self monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node Exporter
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  # cAdvisor
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']

  # Application services (when running)
  - job_name: 'user-service'
    static_configs:
      - targets: ['host.docker.internal:8081']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'product-service'
    static_configs:
      - targets: ['host.docker.internal:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'cart-service'
    static_configs:
      - targets: ['host.docker.internal:8083']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'order-service'
    static_configs:
      - targets: ['host.docker.internal:8084']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'api-gateway'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s

  - job_name: 'eureka-server'
    static_configs:
      - targets: ['host.docker.internal:8761']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
EOF

    # AlertManager configuration
    cat > alertmanager/alertmanager.yml << 'EOF'
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@ecommerce.local'

route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
  - match:
      severity: warning
    receiver: 'warning-alerts'

receivers:
- name: 'default'
  webhook_configs:
  - url: 'http://localhost:5001/alerts'
    send_resolved: true

- name: 'critical-alerts'
  webhook_configs:
  - url: 'http://localhost:5001/critical-alerts'
    send_resolved: true

- name: 'warning-alerts'
  webhook_configs:
  - url: 'http://localhost:5001/warning-alerts'
    send_resolved: true
EOF

    # Filebeat configuration
    cat > filebeat/filebeat.yml << 'EOF'
filebeat.inputs:
- type: container
  paths:
    - /var/lib/docker/containers/*/*.log
  processors:
  - add_docker_metadata:
      host: "unix:///var/run/docker.sock"
  - decode_json_fields:
      fields: ["message"]
      target: ""
      overwrite_keys: true

output.logstash:
  hosts: ["logstash:5044"]

logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644
EOF

    # Logstash pipeline
    cat > logstash/pipeline/logstash.conf << 'EOF'
input {
  beats {
    port => 5044
  }
}

filter {
  # Parse container logs
  if [container][name] {
    mutate {
      add_field => { "service_name" => "%{[container][name]}" }
    }
  }
  
  # Parse JSON logs from applications
  if [message] =~ /^\{.*\}$/ {
    json {
      source => "message"
    }
  }
  
  # Parse Spring Boot logs
  grok {
    match => { 
      "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} \[%{DATA:trace_id},%{DATA:span_id}\] %{DATA:logger} - %{GREEDYDATA:log_message}"
    }
    tag_on_failure => ["_grokparsefailure"]
  }
  
  # Date parsing
  if [timestamp] {
    date {
      match => [ "timestamp", "ISO8601" ]
    }
  }
  
  # Add environment metadata
  mutate {
    add_field => { 
      "environment" => "development"
      "cluster" => "local"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "ecommerce-logs-%{+YYYY.MM.dd}"
  }
  
  # Debug output (remove in production)
  stdout {
    codec => rubydebug
  }
}
EOF

    # Dashboard provisioning
    cat > grafana/dashboards/dashboards.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'E-commerce Dashboards'
    orgId: 1
    folder: 'E-commerce'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF

    print_status "Configuration files generated"
}

# Start monitoring stack
start_monitoring() {
    print_header "Starting Monitoring Stack"
    
    # Start the monitoring stack
    docker compose -f docker-compose-monitoring.yml up -d
    
    print_status "Monitoring stack started"
    print_status "Services starting up... This may take a few minutes."
    
    # Wait for services to be ready
    sleep 30
    
    # Check service health
    check_services
}

# Check service health
check_services() {
    print_header "Checking Service Health"
    
    services=(
        "Grafana:http://localhost:3000"
        "Prometheus:http://localhost:9090"
        "Tempo:http://localhost:3200/ready"
        "AlertManager:http://localhost:9093"
        "Elasticsearch:http://localhost:9200"
        "Kibana:http://localhost:5601"
    )
    
    for service in "${services[@]}"; do
        name=$(echo $service | cut -d: -f1)
        url=$(echo $service | cut -d: -f2-)
        
        if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|302"; then
            print_status "$name is healthy"
        else
            print_warning "$name is not ready yet (this is normal during startup)"
        fi
    done
}

# Display access information
display_access_info() {
    print_header "Access Information"
    
    echo -e "${GREEN}Monitoring Services:${NC}"
    echo "  📊 Grafana:       http://localhost:3000 (admin/admin)"
    echo "  📈 Prometheus:    http://localhost:9090"
    echo "  🔍 Tempo:         http://localhost:3200"
    echo "  🚨 AlertManager:  http://localhost:9093"
    echo "  📋 Elasticsearch: http://localhost:9200"
    echo "  📊 Kibana:        http://localhost:5601"
    echo ""
    echo -e "${GREEN}Exporters:${NC}"
    echo "  🖥️  Node Exporter: http://localhost:9100"
    echo "  📦 cAdvisor:      http://localhost:8080"
    echo ""
    echo -e "${YELLOW}AppDynamics Configuration:${NC}"
    echo "  Set these environment variables for your services:"
    echo "  - APPDYNAMICS_CONTROLLER_HOST_NAME=<your-controller>"
    echo "  - APPDYNAMICS_AGENT_ACCOUNT_NAME=<your-account>"
    echo "  - APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=<your-access-key>"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "  1. Start your e-commerce application services"
    echo "  2. Configure AppDynamics credentials"
    echo "  3. Import Grafana dashboards"
    echo "  4. Set up alerting rules"
}

# Show help
show_help() {
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  setup     - Complete setup and start monitoring stack"
    echo "  start     - Start monitoring services"
    echo "  stop      - Stop monitoring services"
    echo "  restart   - Restart monitoring services"
    echo "  status    - Check service status"
    echo "  logs      - Show logs from all services"
    echo "  clean     - Stop services and remove volumes"
    echo "  help      - Show this help message"
}

# Main execution
case "${1:-setup}" in
    setup)
        check_prerequisites
        setup_directories
        generate_configs
        start_monitoring
        display_access_info
        ;;
    start)
        docker compose -f docker-compose-monitoring.yml up -d
        display_access_info
        ;;
    stop)
        docker compose -f docker-compose-monitoring.yml down
        print_status "Monitoring stack stopped"
        ;;
    restart)
        docker compose -f docker-compose-monitoring.yml restart
        print_status "Monitoring stack restarted"
        ;;
    status)
        docker compose -f docker-compose-monitoring.yml ps
        check_services
        ;;
    logs)
        docker compose -f docker-compose-monitoring.yml logs -f
        ;;
    clean)
        docker compose -f docker-compose-monitoring.yml down -v
        print_status "Monitoring stack stopped and volumes removed"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac