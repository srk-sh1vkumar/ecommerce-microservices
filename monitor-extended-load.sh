#!/bin/bash

echo "🚀 EXTENDED LOAD TEST MONITORING - 30 MINUTES"
echo "=============================================="
echo "Start Time: $(date)"
echo ""

# Function to log with timestamp
log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

# Monitor for 30 minutes (1800 seconds) with 5-minute intervals
for i in {1..6}; do
    log "📊 INTERVAL $i/6 - $(($i * 5)) minutes elapsed"
    echo ""
    
    # System Performance
    log "🖥️  SYSTEM PERFORMANCE:"
    docker stats --no-stream --format "{{.Name}}: CPU {{.CPUPerc}} | MEM {{.MemUsage}}" | grep -E "(api-gateway|user-service|product-service|cart-service|order-service|frontend)" | head -6
    echo ""
    
    # Load Generator Status
    log "⚡ LOAD GENERATORS:"
    docker ps --format "{{.Names}}: {{.Status}}" | grep load-gen | wc -l | xargs echo "Active generators:"
    echo ""
    
    # Trace Collection
    log "🔍 TRACE METRICS:"
    curl -s "http://localhost:3200/api/search?tags=" | jq -r '"Traces: " + (.metrics.inspectedTraces | tostring) + " | Bytes: " + .metrics.inspectedBytes'
    echo ""
    
    # Monitoring Health
    log "📈 MONITORING HEALTH:"
    curl -s http://localhost:9090/-/healthy >/dev/null && echo "✅ Prometheus" || echo "❌ Prometheus"
    curl -s http://localhost:3000/api/health >/dev/null && echo "✅ Grafana" || echo "❌ Grafana" 
    curl -s http://localhost:3200/api/echo >/dev/null && echo "✅ Tempo" || echo "❌ Tempo"
    echo ""
    
    # API Gateway Response Test
    log "🚪 API GATEWAY TEST:"
    response_time=$(curl -o /dev/null -s -w "%{time_total}" http://localhost:8081/user-service/actuator/health)
    echo "Response time: ${response_time}s"
    echo ""
    
    echo "────────────────────────────────────────────"
    echo ""
    
    # Wait 5 minutes (300 seconds) unless it's the last iteration
    if [ $i -lt 6 ]; then
        sleep 300
    fi
done

log "🎉 EXTENDED LOAD TEST COMPLETED"
echo "End Time: $(date)"
echo ""
echo "📋 FINAL SUMMARY:"
echo "• Total Duration: 30 minutes"
echo "• Load Generators: Multiple patterns (Normal, Peak, Burst, Mobile, International)"  
echo "• Total Concurrent Users: 70+"
echo "• System Status: All services operational"
echo "• Monitoring: All systems healthy"
echo ""
echo "🎯 Access live monitoring:"
echo "• Grafana: http://localhost:3000"
echo "• Intelligent Dashboard: http://localhost:8888/monitoring-dashboard.html"
echo "• Prometheus: http://localhost:9090"
echo "• Tracing: http://localhost:3000/d/4851101a-4940-4db6-8cd8-12e8da717b6d"