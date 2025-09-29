package com.ecommerce.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Provides interactive API documentation at /swagger-ui.html
 *
 * Features:
 * - Auto-generated API documentation
 * - Interactive API testing
 * - JWT authentication support
 * - Multiple environment servers
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecommerceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(securityRequirement())
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("E-commerce Microservices API")
                .description("""
                        Comprehensive e-commerce platform built with microservices architecture.

                        ## Features
                        - User registration and authentication
                        - Product catalog management
                        - Shopping cart operations
                        - Order processing
                        - JWT-based security

                        ## Getting Started
                        1. Register a new user at `/api/users/register`
                        2. Login at `/api/users/login` to get JWT token
                        3. Use the token in Authorization header: `Bearer <token>`
                        4. Access protected endpoints

                        ## Authentication
                        Most endpoints require JWT authentication. Obtain a token by logging in.
                        """)
                .version("2.0.0")
                .contact(apiContact())
                .license(apiLicense());
    }

    private Contact apiContact() {
        return new Contact()
                .name("E-commerce Development Team")
                .email("support@ecommerce.com")
                .url("https://github.com/srk-sh1vkumar/ecommerce-microservices");
    }

    private License apiLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> serverList() {
        Server localServer = new Server()
                .url("http://localhost:8081")
                .description("Local Development Server (API Gateway)");

        Server devServer = new Server()
                .url("http://dev.ecommerce.com")
                .description("Development Environment");

        Server prodServer = new Server()
                .url("https://api.ecommerce.com")
                .description("Production Environment");

        return List.of(localServer, devServer, prodServer);
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearer-jwt");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("bearer-jwt")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from login endpoint");
    }
}