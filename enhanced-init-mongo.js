// Enhanced MongoDB Initialization Script for Multi-Portal Ecommerce Platform
// Supports Customer Portal, Admin Dashboard, Vendor Portal, and Mobile App

// Switch to the ecommerce database
db = db.getSiblingDB('ecommerce');

// ============================================================================
// EXISTING COLLECTIONS (PRESERVE)
// ============================================================================

// Users collection with enhanced fields
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "role": 1 });
db.users.createIndex({ "createdAt": 1 });
db.users.createIndex({ "lastLoginAt": 1 });

// Products collection with enhanced indexing
db.products.createIndex({ "name": "text", "description": "text", "category": "text" });
db.products.createIndex({ "category": 1 });
db.products.createIndex({ "price": 1 });
db.products.createIndex({ "stockQuantity": 1 });
db.products.createIndex({ "vendorId": 1 });
db.products.createIndex({ "createdAt": 1 });
db.products.createIndex({ "featured": 1 });

// Cart items collection
db.cart_items.createIndex({ "userEmail": 1 });
db.cart_items.createIndex({ "productId": 1 });

// Orders collection with enhanced indexing
db.orders.createIndex({ "userEmail": 1 });
db.orders.createIndex({ "status": 1 });
db.orders.createIndex({ "orderDate": 1 });
db.orders.createIndex({ "vendorId": 1 });

// ============================================================================
// NEW MONITORING AND ANALYTICS COLLECTIONS
// ============================================================================

// Intelligent Monitoring Events
db.intelligent_monitoring_events.createIndex({ "timestamp": 1 });
db.intelligent_monitoring_events.createIndex({ "service": 1 });
db.intelligent_monitoring_events.createIndex({ "eventType": 1 });
db.intelligent_monitoring_events.createIndex({ "severity": 1 });
db.intelligent_monitoring_events.createIndex({ "correlationId": 1 });

// Error Patterns and Automated Fixes
db.error_patterns.createIndex({ "pattern": 1 });
db.error_patterns.createIndex({ "services": 1 });
db.error_patterns.createIndex({ "severity": 1 });
db.error_patterns.createIndex({ "firstSeen": 1 });
db.error_patterns.createIndex({ "lastSeen": 1 });
db.error_patterns.createIndex({ "autoFixed": 1 });

// Automated Fixes Applied
db.automated_fixes.createIndex({ "errorPatternId": 1 });
db.automated_fixes.createIndex({ "service": 1 });
db.automated_fixes.createIndex({ "appliedAt": 1 });
db.automated_fixes.createIndex({ "status": 1 });
db.automated_fixes.createIndex({ "rollbackAt": 1 });

// Cross-Platform Trace Correlation
db.cross_platform_traces.createIndex({ "traceId": 1 }, { unique: true });
db.cross_platform_traces.createIndex({ "sessionId": 1 });
db.cross_platform_traces.createIndex({ "userId": 1 });
db.cross_platform_traces.createIndex({ "platform": 1 });
db.cross_platform_traces.createIndex({ "startTime": 1 });

// Frontend Analytics
db.frontend_analytics.createIndex({ "sessionId": 1 });
db.frontend_analytics.createIndex({ "userId": 1 });
db.frontend_analytics.createIndex({ "portal": 1 });
db.frontend_analytics.createIndex({ "page": 1 });
db.frontend_analytics.createIndex({ "timestamp": 1 });
db.frontend_analytics.createIndex({ "event": 1 });

// Vendor Analytics
db.vendor_analytics.createIndex({ "vendorId": 1 });
db.vendor_analytics.createIndex({ "metricType": 1 });
db.vendor_analytics.createIndex({ "timestamp": 1 });
db.vendor_analytics.createIndex({ "period": 1 });

// Mobile App Metrics
db.mobile_app_metrics.createIndex({ "deviceId": 1 });
db.mobile_app_metrics.createIndex({ "userId": 1 });
db.mobile_app_metrics.createIndex({ "appVersion": 1 });
db.mobile_app_metrics.createIndex({ "platform": 1 });
db.mobile_app_metrics.createIndex({ "timestamp": 1 });
db.mobile_app_metrics.createIndex({ "metricType": 1 });

// Load Test Correlations
db.load_test_correlations.createIndex({ "testId": 1 });
db.load_test_correlations.createIndex({ "timestamp": 1 });
db.load_test_correlations.createIndex({ "portal": 1 });
db.load_test_correlations.createIndex({ "scenario": 1 });

// ============================================================================
// SECURITY AND AUDIT COLLECTIONS
// ============================================================================

// Security Events
db.security_events.createIndex({ "userId": 1 });
db.security_events.createIndex({ "eventType": 1 });
db.security_events.createIndex({ "severity": 1 });
db.security_events.createIndex({ "timestamp": 1 });
db.security_events.createIndex({ "source": 1 });
db.security_events.createIndex({ "deviceFingerprint": 1 });

// Audit Logs
db.audit_logs.createIndex({ "userId": 1 });
db.audit_logs.createIndex({ "action": 1 });
db.audit_logs.createIndex({ "resource": 1 });
db.audit_logs.createIndex({ "timestamp": 1 });
db.audit_logs.createIndex({ "portal": 1 });
db.audit_logs.createIndex({ "severity": 1 });

// Session Management
db.user_sessions.createIndex({ "userId": 1 });
db.user_sessions.createIndex({ "sessionId": 1 }, { unique: true });
db.user_sessions.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });
db.user_sessions.createIndex({ "deviceFingerprint": 1 });
db.user_sessions.createIndex({ "portal": 1 });

// Failed Login Attempts
db.failed_login_attempts.createIndex({ "email": 1 });
db.failed_login_attempts.createIndex({ "ipAddress": 1 });
db.failed_login_attempts.createIndex({ "timestamp": 1 });
db.failed_login_attempts.createIndex({ "deviceFingerprint": 1 });

// ============================================================================
// BUSINESS INTELLIGENCE COLLECTIONS
// ============================================================================

// User Behavior Analytics
db.user_behavior.createIndex({ "userId": 1 });
db.user_behavior.createIndex({ "sessionId": 1 });
db.user_behavior.createIndex({ "portal": 1 });
db.user_behavior.createIndex({ "action": 1 });
db.user_behavior.createIndex({ "timestamp": 1 });
db.user_behavior.createIndex({ "conversionFunnel": 1 });

// Business Metrics
db.business_metrics.createIndex({ "metricType": 1 });
db.business_metrics.createIndex({ "portal": 1 });
db.business_metrics.createIndex({ "period": 1 });
db.business_metrics.createIndex({ "timestamp": 1 });

// Revenue Analytics
db.revenue_analytics.createIndex({ "vendorId": 1 });
db.revenue_analytics.createIndex({ "portal": 1 });
db.revenue_analytics.createIndex({ "period": 1 });
db.revenue_analytics.createIndex({ "timestamp": 1 });
db.revenue_analytics.createIndex({ "category": 1 });

// Customer Journey
db.customer_journey.createIndex({ "userId": 1 });
db.customer_journey.createIndex({ "sessionId": 1 });
db.customer_journey.createIndex({ "touchpoints": 1 });
db.customer_journey.createIndex({ "startTimestamp": 1 });
db.customer_journey.createIndex({ "conversionStatus": 1 });

// ============================================================================
// SAMPLE DATA FOR MULTI-PORTAL TESTING
// ============================================================================

// Enhanced Admin User
db.users.insertOne({
    email: "admin@example.com",
    password: "$2b$10$rN8J0bQyH1.9EV1L1k1QHe4nQZDv1aUKZ3D2F5F6G7H8I9J0K1L2M3",
    firstName: "System",
    lastName: "Administrator",
    role: "admin",
    permissions: ["MANAGE_USERS", "MANAGE_PRODUCTS", "MANAGE_ORDERS", "VIEW_ANALYTICS", "SYSTEM_ADMIN"],
    portal: "admin",
    createdAt: new Date(),
    lastLoginAt: null,
    isActive: true,
    securityLevel: "high",
    twoFactorEnabled: true
});

// Sample Vendor User
db.users.insertOne({
    email: "vendor@ecommerce.com",
    password: "$2b$10$rN8J0bQyH1.9EV1L1k1QHe5nQZDv1aUKZ3D2F5F6G7H8I9J0K1L2M4",
    firstName: "John",
    lastName: "Vendor",
    role: "vendor",
    permissions: ["MANAGE_OWN_PRODUCTS", "VIEW_OWN_ORDERS", "VIEW_OWN_ANALYTICS"],
    portal: "vendor",
    createdAt: new Date(),
    lastLoginAt: null,
    isActive: true,
    vendorId: "vendor_001",
    businessName: "John's Electronics Store",
    businessAddress: "123 Business St, Commerce City, CC 12345",
    taxId: "12-3456789",
    securityLevel: "high"
});

// Sample Customer User
db.users.insertOne({
    email: "customer@ecommerce.com",
    password: "$2b$10$rN8J0bQyH1.9EV1L1k1QHe6nQZDv1aUKZ3D2F5F6G7H8I9J0K1L2M5",
    firstName: "Jane",
    lastName: "Customer",
    role: "customer",
    permissions: ["PLACE_ORDERS", "VIEW_OWN_ORDERS", "MANAGE_CART"],
    portal: "customer",
    createdAt: new Date(),
    lastLoginAt: null,
    isActive: true,
    preferences: {
        newsletter: true,
        smsNotifications: false,
        theme: "light",
        language: "en"
    },
    addresses: [{
        type: "shipping",
        firstName: "Jane",
        lastName: "Customer",
        street: "456 Customer Ave",
        city: "Customer City",
        state: "CC",
        zipCode: "54321",
        country: "USA",
        isDefault: true
    }]
});

// Enhanced Sample Products with Vendor Information
const sampleProducts = [
    {
        name: "iPhone 15 Pro Max",
        description: "Latest Apple smartphone with advanced camera system and A17 Pro chip",
        price: 1199.99,
        category: "Electronics",
        stockQuantity: 50,
        imageUrl: "https://example.com/iphone15.jpg",
        vendorId: "vendor_001",
        featured: true,
        specifications: {
            brand: "Apple",
            model: "iPhone 15 Pro Max",
            storage: "256GB",
            color: "Natural Titanium"
        },
        seoData: {
            metaTitle: "iPhone 15 Pro Max - Latest Apple Smartphone",
            metaDescription: "Get the newest iPhone 15 Pro Max with advanced features",
            keywords: ["iPhone", "Apple", "smartphone", "mobile phone"]
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Samsung Galaxy S24 Ultra",
        description: "Premium Android smartphone with S Pen and exceptional camera capabilities",
        price: 1299.99,
        category: "Electronics",
        stockQuantity: 30,
        imageUrl: "https://example.com/galaxy-s24.jpg",
        vendorId: "vendor_001",
        featured: true,
        specifications: {
            brand: "Samsung",
            model: "Galaxy S24 Ultra",
            storage: "512GB",
            color: "Titanium Black"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "MacBook Pro 16-inch M3",
        description: "Professional laptop with M3 chip for ultimate performance",
        price: 2499.99,
        category: "Computers",
        stockQuantity: 20,
        imageUrl: "https://example.com/macbook-pro.jpg",
        vendorId: "vendor_001",
        featured: true,
        specifications: {
            brand: "Apple",
            model: "MacBook Pro 16-inch",
            processor: "M3 Max",
            memory: "32GB",
            storage: "1TB SSD"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Sony WH-1000XM5 Headphones",
        description: "Industry-leading noise canceling wireless headphones",
        price: 399.99,
        category: "Audio",
        stockQuantity: 75,
        imageUrl: "https://example.com/sony-headphones.jpg",
        vendorId: "vendor_001",
        featured: false,
        specifications: {
            brand: "Sony",
            model: "WH-1000XM5",
            type: "Over-ear",
            wireless: true,
            noiseCanceling: true
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: "Nike Air Max 270",
        description: "Comfortable running shoes with Max Air cushioning",
        price: 149.99,
        category: "Footwear",
        stockQuantity: 100,
        imageUrl: "https://example.com/nike-airmax.jpg",
        vendorId: "vendor_001",
        featured: false,
        specifications: {
            brand: "Nike",
            model: "Air Max 270",
            type: "Running Shoes",
            material: "Mesh and synthetic"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

db.products.insertMany(sampleProducts);

// ============================================================================
// SAMPLE MONITORING DATA
// ============================================================================

// Sample Error Patterns
db.error_patterns.insertMany([
    {
        id: "pattern_001",
        pattern: "NullPointerException in ProductService.getProduct()",
        services: ["product-service"],
        count: 15,
        severity: "medium",
        firstSeen: new Date(Date.now() - 86400000), // 1 day ago
        lastSeen: new Date(),
        autoFixed: true,
        fixApplied: "Added null check and Optional wrapper",
        fixDetails: {
            filePath: "src/main/java/com/ecommerce/product/service/ProductService.java",
            lineNumber: 45,
            fixType: "null_check_addition"
        }
    },
    {
        id: "pattern_002",
        pattern: "Connection timeout to MongoDB",
        services: ["user-service", "product-service", "order-service"],
        count: 8,
        severity: "high",
        firstSeen: new Date(Date.now() - 43200000), // 12 hours ago
        lastSeen: new Date(Date.now() - 3600000), // 1 hour ago
        autoFixed: true,
        fixApplied: "Increased connection timeout and added retry logic",
        fixDetails: {
            configFile: "application.yml",
            fixType: "configuration_update"
        }
    }
]);

// Sample Cross-Platform Traces
db.cross_platform_traces.insertMany([
    {
        traceId: "trace_001",
        sessionId: "session_001",
        userId: "customer@ecommerce.com",
        platform: "customer-portal",
        startTime: new Date(Date.now() - 300000), // 5 minutes ago
        endTime: new Date(Date.now() - 60000), // 1 minute ago
        duration: 240000,
        spans: [
            {
                serviceName: "customer-portal",
                operationName: "page_load",
                duration: 1200,
                tags: { page: "/products", userAgent: "Chrome/120.0" }
            },
            {
                serviceName: "api-gateway",
                operationName: "route_request",
                duration: 15,
                tags: { route: "/api/products" }
            },
            {
                serviceName: "product-service",
                operationName: "get_products",
                duration: 85,
                tags: { query: "category=Electronics" }
            }
        ],
        businessTransaction: "product_browse",
        conversionEvent: null,
        errors: []
    }
]);

// Sample Frontend Analytics
db.frontend_analytics.insertMany([
    {
        sessionId: "session_001",
        userId: "customer@ecommerce.com",
        portal: "customer-portal",
        page: "/products",
        event: "page_view",
        timestamp: new Date(),
        metadata: {
            userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            viewport: { width: 1920, height: 1080 },
            loadTime: 1200,
            renderTime: 800
        }
    },
    {
        sessionId: "admin_session_001",
        userId: "admin@example.com",
        portal: "admin-dashboard",
        page: "/monitoring",
        event: "dashboard_view",
        timestamp: new Date(),
        metadata: {
            userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
            viewport: { width: 2560, height: 1440 },
            loadTime: 950,
            renderTime: 600
        }
    }
]);

// ============================================================================
// TTL INDEXES FOR DATA RETENTION
// ============================================================================

// Set TTL for monitoring events (30 days)
db.intelligent_monitoring_events.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 2592000 });

// Set TTL for frontend analytics (90 days)
db.frontend_analytics.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 7776000 });

// Set TTL for audit logs (1 year)
db.audit_logs.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 31536000 });

// Set TTL for security events (6 months)
db.security_events.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 15552000 });

// Set TTL for cross-platform traces (7 days)
db.cross_platform_traces.createIndex({ "startTime": 1 }, { expireAfterSeconds: 604800 });

print("Enhanced MongoDB initialization completed successfully!");
print("Created collections for multi-portal ecommerce platform:");
print("- Core business collections with enhanced indexing");
print("- Monitoring and analytics collections");
print("- Security and audit collections");
print("- Business intelligence collections");
print("- Sample data for testing");
print("- TTL indexes for data retention");
print("");
print("Portal-specific users created:");
print("- admin@example.com (Admin Dashboard)");
print("- vendor@ecommerce.com (Vendor Portal)");
print("- customer@ecommerce.com (Customer Portal)");
print("");
print("Sample products and monitoring data inserted for comprehensive testing.");