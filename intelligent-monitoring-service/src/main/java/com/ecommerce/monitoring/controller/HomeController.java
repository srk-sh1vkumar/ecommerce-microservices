package com.ecommerce.monitoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("serviceName", "Intelligent Monitoring Service");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("endpoints", getEndpoints());
        return "index";
    }

    private List<Map<String, String>> getEndpoints() {
        List<Map<String, String>> endpoints = new ArrayList<>();

        // Actuator Endpoints
        addEndpoint(endpoints, "Health Check", "/actuator/health", "View service health status");
        addEndpoint(endpoints, "Metrics", "/actuator/metrics", "View application metrics");
        addEndpoint(endpoints, "Prometheus Metrics", "/actuator/prometheus", "Metrics in Prometheus format");
        addEndpoint(endpoints, "Environment Info", "/actuator/env", "View environment properties");
        addEndpoint(endpoints, "Service Info", "/actuator/info", "View service information");

        // Monitoring API Endpoints
        addEndpoint(endpoints, "Grafana Proxy", "/api/monitoring/proxy/grafana/**", "Proxy to Grafana");
        addEndpoint(endpoints, "Prometheus Proxy", "/api/monitoring/proxy/prometheus/**", "Proxy to Prometheus");
        addEndpoint(endpoints, "Tempo Proxy", "/api/monitoring/proxy/tempo/**", "Proxy to Tempo");
        addEndpoint(endpoints, "AlertManager Proxy", "/api/monitoring/proxy/alertmanager/**", "Proxy to AlertManager");
        addEndpoint(endpoints, "Elasticsearch Proxy", "/api/monitoring/proxy/elasticsearch/**", "Proxy to Elasticsearch");

        return endpoints;
    }

    private void addEndpoint(List<Map<String, String>> endpoints, String name, String path, String description) {
        Map<String, String> endpoint = new HashMap<>();
        endpoint.put("name", name);
        endpoint.put("path", path);
        endpoint.put("description", description);
        endpoints.add(endpoint);
    }
}
