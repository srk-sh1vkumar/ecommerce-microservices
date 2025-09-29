#!/usr/bin/env python3
"""
Health check script for the load generator
"""

import os
import sys
import requests
from datetime import datetime


def check_target_availability():
    """Check if the target application is available"""
    target_url = os.getenv("TARGET_BASE_URL", "http://frontend")
    
    try:
        response = requests.get(f"{target_url}/health", timeout=5)
        if response.status_code == 200:
            print(f"✅ Target {target_url} is healthy")
            return True
        else:
            print(f"❌ Target {target_url} returned status {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ Cannot reach target {target_url}: {e}")
        return False


def check_otel_collector():
    """Check if OpenTelemetry collector is available"""
    otel_endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4317")
    
    try:
        # Simple connection test
        import socket
        host, port = otel_endpoint.replace("http://", "").split(":")
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((host, int(port)))
        sock.close()
        
        if result == 0:
            print(f"✅ OTEL Collector {otel_endpoint} is reachable")
            return True
        else:
            print(f"❌ OTEL Collector {otel_endpoint} is not reachable")
            return False
    except Exception as e:
        print(f"❌ Error checking OTEL Collector: {e}")
        return False


def main():
    """Run health checks"""
    print(f"🏥 Load Generator Health Check - {datetime.now()}")
    print("=" * 50)
    
    checks = [
        ("Target Application", check_target_availability),
        ("OpenTelemetry Collector", check_otel_collector)
    ]
    
    all_healthy = True
    
    for check_name, check_func in checks:
        print(f"Checking {check_name}...")
        if not check_func():
            all_healthy = False
        print()
    
    if all_healthy:
        print("✅ All health checks passed")
        sys.exit(0)
    else:
        print("❌ Some health checks failed")
        sys.exit(1)


if __name__ == "__main__":
    main()