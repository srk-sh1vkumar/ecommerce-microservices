package com.ecommerce.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
@EnableScheduling
@EnableAsync
public class IntelligentMonitoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelligentMonitoringApplication.class, args);
    }
}