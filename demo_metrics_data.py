#!/usr/bin/env python3
"""
Demo Performance Report Generator
Creates sample data to demonstrate AppDynamics metrics collection capabilities
"""

import json
import csv
from datetime import datetime
from typing import List, Dict

def generate_demo_metrics() -> List[Dict]:
    """Generate realistic demo metrics data for demonstration."""
    
    demo_applications = [
        {
            'application_name': 'User-Service',
            'application_id': 1001,
            'calls_per_minute': 45.3,
            'average_response_time': 120.5,
            'errors_per_minute': 0.8,
            'error_percentage': 1.77,
            'health_status': 'Healthy',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        },
        {
            'application_name': 'Product-Service', 
            'application_id': 1002,
            'calls_per_minute': 78.9,
            'average_response_time': 95.2,
            'errors_per_minute': 2.1,
            'error_percentage': 2.66,
            'health_status': 'Warning',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        },
        {
            'application_name': 'Cart-Service',
            'application_id': 1003, 
            'calls_per_minute': 34.6,
            'average_response_time': 85.3,
            'errors_per_minute': 0.3,
            'error_percentage': 0.87,
            'health_status': 'Healthy',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        },
        {
            'application_name': 'Order-Service',
            'application_id': 1004,
            'calls_per_minute': 23.7,
            'average_response_time': 180.4,
            'errors_per_minute': 0.5,
            'error_percentage': 2.11,
            'health_status': 'Healthy',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        },
        {
            'application_name': 'API-Gateway',
            'application_id': 1005,
            'calls_per_minute': 156.8,
            'average_response_time': 45.7,
            'errors_per_minute': 8.2,
            'error_percentage': 5.23,
            'health_status': 'Critical',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        },
        {
            'application_name': 'Frontend-Service',
            'application_id': 1006,
            'calls_per_minute': 89.2,
            'average_response_time': 67.8,
            'errors_per_minute': 1.4,
            'error_percentage': 1.57,
            'health_status': 'Healthy',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }
    ]
    
    return demo_applications

def export_demo_csv(data: List[Dict], filename: str) -> None:
    """Export demo data to CSV format."""
    print(f"📊 Generating demo CSV report: {filename}")
    
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        if data:
            fieldnames = ['Application Name', 'Calls/Min', 'Avg Response Time (ms)', 
                         'Errors/Min', 'Error Rate (%)', 'Health Status', 'Timestamp']
            
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            
            for app in data:
                writer.writerow({
                    'Application Name': app['application_name'],
                    'Calls/Min': app['calls_per_minute'],
                    'Avg Response Time (ms)': app['average_response_time'],
                    'Errors/Min': app['errors_per_minute'], 
                    'Error Rate (%)': app['error_percentage'],
                    'Health Status': app['health_status'],
                    'Timestamp': app['timestamp']
                })
    
    print(f"✅ Demo CSV report generated successfully!")

def display_summary_stats(data: List[Dict]) -> None:
    """Display summary statistics for demo data."""
    print("\n📊 Demo Performance Report Summary:")
    print("=" * 50)
    
    total_apps = len(data)
    healthy = sum(1 for app in data if app['health_status'] == 'Healthy')
    warning = sum(1 for app in data if app['health_status'] == 'Warning') 
    critical = sum(1 for app in data if app['health_status'] == 'Critical')
    
    total_calls = sum(app['calls_per_minute'] for app in data)
    total_errors = sum(app['errors_per_minute'] for app in data)
    avg_response_time = sum(app['average_response_time'] for app in data) / total_apps
    
    print(f"Total Applications: {total_apps}")
    print(f"Healthy: {healthy} | Warning: {warning} | Critical: {critical}")
    print(f"Total Calls/Min: {total_calls:,.2f}")
    print(f"Total Errors/Min: {total_errors:,.2f}")
    print(f"Average Response Time: {avg_response_time:.2f}ms")
    
    if total_calls > 0:
        overall_error_rate = (total_errors / total_calls) * 100
        print(f"Overall Error Rate: {overall_error_rate:.2f}%")

if __name__ == "__main__":
    print("🚀 Demo Performance Report Generator")
    print("=" * 50)
    
    # Generate demo data
    demo_data = generate_demo_metrics()
    
    # Display summary
    display_summary_stats(demo_data)
    
    # Export to CSV
    export_demo_csv(demo_data, 'demo_performance_report.csv')
    
    print(f"\n🎉 Demo performance report completed!")
    print(f"📄 File: demo_performance_report.csv")
    print(f"📊 Records: {len(demo_data)}")