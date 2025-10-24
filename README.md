# 🛒 E-commerce Microservices Platform - Performance Optimized

A production-ready, distributed e-commerce application built with Spring Boot microservices featuring **enterprise-grade performance optimizations**, comprehensive resilience patterns, and advanced SRE analytics.

## ⚡ **Performance Improvements Summary**

### **Critical Issues Fixed**
Based on comprehensive load testing revealing **48-103% error rates** across services:

| Service | Error Rate Before | Target | Status After Fixes |
|---------|-------------------|--------|------------------|
| API Gateway | 0.179% (79% over) | <0.1% | 🔧 Circuit Breaker + Caching |
| User Service | 0.203% (103% over) | <0.1% | 🔧 Resilience Patterns |
| Product Service | 0.165% (65% over) | <0.1% | ✅ **Enhanced with CB + Redis** |
| Cart Service | 0.160% (60% over) | <0.1% | 🔧 In Progress |
| Order Service | 0.173% (73% over) | <0.1% | 🔧 In Progress |
| Frontend | 0.148% (48% over) | <0.1% | 🔧 In Progress |

### **Implemented Solutions** ✅
- **Circuit Breaker Pattern**: Resilience4j with intelligent fallbacks
- **Redis Caching Layer**: Multi-tier caching (1hr, 30min, 2hr TTL)
- **Retry Mechanisms**: Exponential backoff with 3 attempts
- **Performance Monitoring**: Real-time SRE analytics with AI insights
- **Database Resilience**: Connection pooling and query optimization

## 🏗️ Architecture Overview

### Core Components
- **🌐 Multi-Portal Frontend**: Customer, Admin, and Vendor portals
- **🚪 API Gateway**: Spring Cloud Gateway (Port 8081)
- **🔍 Service Discovery**: Eureka Server (Port 8761)
- **💾 Database**: MongoDB with replica support (Port 27017)
- **📊 Monitoring Stack**: Grafana, Prometheus, Tempo, OpenTelemetry
- **⚡ Load Testing**: Integrated performance testing tools

### Microservices
1. **👤 User Service** (Port 8082) - Authentication, user management, JWT tokens
2. **📦 Product Service** (Port 8083) - Product catalog, search, inventory
3. **🛒 Cart Service** (Port 8084) - Shopping cart, session management
4. **📋 Order Service** (Port 8085) - Order processing, checkout, history
5. **🔍 Intelligent Monitoring Service** (Port 8090) - Advanced monitoring and analytics

### Frontend Portals
- **🛒 Customer Portal** (Port 80) - Angular 17+ shopping interface
- **⚙️ Admin Dashboard** - Administrative management portal
- **📊 Vendor Portal** - Vendor management and analytics
- **📱 Mobile PWA** - Progressive web application

## 🔒 **Security Setup (REQUIRED)**

**⚠️ NEVER commit credentials to Git! ⚠️**

1. **Create `.env` file** (ignored by Git):
```bash
# MongoDB Configuration
MONGO_ROOT_PASSWORD=your_secure_password_here

# AppDynamics Configuration
APPDYNAMICS_CONTROLLER_HOST_NAME=your_controller.saas.appdynamics.com
APPDYNAMICS_AGENT_ACCOUNT_NAME=your_account_name
APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=your_access_key
```

2. **Generate strong passwords**:
```bash
# Generate secure MongoDB password
openssl rand -base64 32

# Update your .env file with generated password
```

## 🚀 Quick Start & Complete Startup Guide

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for building)
- Maven 3.6+ (for building)

### Option 1: Docker Complete Stack (Recommended)

```bash
# Clone and navigate to project
git clone <repository-url>
cd ecommerce-microservices

# Build all services
./build-all.sh

# Start complete stack with monitoring
docker-compose up -d --build

# Wait for services to start (30-60 seconds)
echo "⏳ Waiting for all services to start..."
sleep 45

# Verify services are running
docker-compose ps

# View logs for specific service
docker-compose logs -f user-service

# View all logs
docker-compose logs -f
```

### Startup Verification Commands
```bash
# Check service health
curl http://localhost:8081/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Product Service
curl http://localhost:8084/actuator/health  # Cart Service
curl http://localhost:8085/actuator/health  # Order Service

# Check Eureka Service Registry
curl http://localhost:8761/

# Check Grafana Health
curl http://localhost:3000/api/health

# Check Prometheus Targets
curl http://localhost:9090/api/v1/targets
```

### Stop/Restart Commands
```bash
# Stop all services
docker-compose down

# Stop and remove all data (clean slate)
docker-compose down -v

# Restart specific service
docker-compose restart user-service

# Rebuild and restart specific service
docker-compose up -d --build user-service

# View service status
docker-compose ps
```

### Option 2: Production Deployment

For production deployment with Kubernetes, see [DEPLOYMENT_STRATEGY.md](DEPLOYMENT_STRATEGY.md)

## 📊 Monitoring & Observability Access

### 🔐 Access Points & Login Credentials

#### **📊 Grafana Dashboards**
- **URL**: http://localhost:3000
- **Login**: `admin` / `YOUR_ADMIN_PASSWORD`
- **Features**: Custom dashboards, Tempo tracing, Prometheus metrics

#### **📈 Prometheus Metrics**
- **URL**: http://localhost:9090
- **Authentication**: None required
- **Features**: Metrics collection, alerting rules, targets monitoring

#### **🔗 Distributed Tracing**
- **URL**: http://localhost:3000/explore
- **Login**: Use Grafana credentials (`admin` / `YOUR_ADMIN_PASSWORD`)
- **Datasource**: Select "Tempo" for distributed tracing
- **Features**: Trace search, service maps, performance analysis

#### **🗺️ Service Discovery (Eureka)**
- **URL**: http://localhost:8761
- **Authentication**: None required
- **Features**: Service registry, health status, load balancing info

#### **🎯 AppDynamics APM**
- **Environment**: Integrated with all services
- **Features**: Application performance monitoring, business flow analysis

### OpenTelemetry Tracing
- **OTLP Collector**: Port 4317 (gRPC), 4318 (HTTP)
- **Tempo Backend**: Port 3200
- **Trace Visualization**: Grafana with Tempo integration
- **Service Maps**: Real-time dependency visualization

### Pre-configured Dashboards
- E-commerce OpenTelemetry Tracing Dashboard
- Performance Metrics Overview
- Service Health Monitoring
- Business KPI Dashboard

## 🌐 API Gateway & Endpoints

All requests go through the API Gateway at `http://localhost:8081`

### 📖 Interactive API Documentation (Swagger UI)

Explore and test all APIs interactively:

| Service | Swagger UI | OpenAPI Spec |
|---------|-----------|--------------|
| **User Service** | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| **Product Service** | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| **Cart Service** | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| **Order Service** | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |

**📚 Complete Guide**: See [API Documentation Guide](docs/api/API_DOCUMENTATION_GUIDE.md) for comprehensive documentation including:
- Interactive testing with Swagger UI
- Authentication flows with JWT
- Complete endpoint reference
- Common use cases and examples
- Error handling guide
- Best practices

### User Service
```bash
# Register new user
POST /api/users/register
{
  "email": "user@example.com",
  "password": "your_password_here",
  "firstName": "John",
  "lastName": "Doe"
}

# User login
POST /api/users/login
{
  "email": "user@example.com",
  "password": "your_password_here"
}

# Get user profile
GET /api/users/{email}
```

### Product Service
```bash
# Get products (paginated)
GET /api/products?page=0&size=10

# Search products
GET /api/products/search?name=iPhone

# Get by category
GET /api/products/category/Electronics

# Create product (admin)
POST /api/products
{
  "name": "iPhone 15",
  "description": "Latest iPhone model",
  "price": 999.99,
  "category": "Electronics",
  "stockQuantity": 50,
  "imageUrl": "https://example.com/iphone15.jpg"
}
```

### Cart Service
```bash
# Get cart items
GET /api/cart/{userEmail}

# Add to cart
POST /api/cart/add
{
  "userEmail": "user@example.com",
  "productId": 1,
  "quantity": 2
}

# Update quantity
PUT /api/cart/{userEmail}/{productId}
{
  "quantity": 3
}

# Remove item
DELETE /api/cart/{userEmail}/{productId}

# Clear cart
DELETE /api/cart/{userEmail}
```

### Order Service
```bash
# Checkout cart
POST /api/orders/checkout
{
  "userEmail": "user@example.com",
  "shippingAddress": "123 Main St, City, State 12345"
}

# Get order history
GET /api/orders/history/{userEmail}

# Get specific order
GET /api/orders/{orderId}
```

## ⚡ Load Testing & Performance

### 🚀 Complete Load Generator Commands

#### **Basic Load Testing Patterns**
```bash
# Light load (5 users, slow ramp-up)
./start-load-test.sh light

# Normal traffic (10 users, medium ramp-up)
./start-load-test.sh normal

# Peak traffic (20 users, fast ramp-up)
./start-load-test.sh peak

# Stress test (50 users, very fast ramp-up)
./start-load-test.sh stress

# Burst traffic (25 users, quick bursts)
./start-load-test.sh burst

# Mobile traffic pattern (8 users, frontend focus)
./start-load-test.sh mobile

# International traffic (12 users, slower connections)
./start-load-test.sh international

# Black Friday simulation (100 users, intense load)
./start-load-test.sh black-friday
```

#### **Custom Load Testing**
```bash
# Custom pattern with specific duration and users
./start-load-test.sh custom 600 15    # 15 users for 10 minutes
./start-load-test.sh custom 1800 25   # 25 users for 30 minutes

# Run all patterns simultaneously (comprehensive test)
./start-load-test.sh all
```

#### **Load Test Management**
```bash
# Check status of all load generators
./start-load-test.sh status

# Stop all running load tests
./start-load-test.sh stop

# Start real-time monitoring
./start-load-test.sh monitor

# Clean up all load test containers
./start-load-test.sh cleanup
```

#### **Advanced Load Testing**
```bash
# Extended monitoring with metrics collection
./monitor-extended-load.sh

# Generate performance reports
python3 appdynamics_metrics.py

# Demo performance data generation
python3 demo_metrics_data.py
```

### 📊 Performance Monitoring
- **Real-time Metrics**: Grafana dashboards with live updates
- **APM Integration**: AppDynamics monitoring with business flow analysis
- **Custom Metrics**: Python-based performance reporting (`demo_performance_report.xlsx`)
- **Load Generator**: Locust-based realistic traffic simulation
- **Distributed Tracing**: OpenTelemetry traces for request flow analysis

## 🗄️ Database Configuration

### MongoDB Setup
**Docker Environment:**
- **Connection**: `mongodb://admin:YOUR_MONGODB_PASSWORD@localhost:27017/ecommerce?authSource=admin`
- **Admin UI**: MongoDB Compass support
- **Collections**: users, products, cart_items, orders

**Pre-loaded Data:**
- **Admin User**: admin@example.com / YOUR_ADMIN_PASSWORD
- **Sample Products**: 8 demo products across categories
- **Test Data**: Ready for immediate testing

## 🌐 Portal Access & Login Credentials

### **🛒 Customer Portal**
- **URL**: http://localhost:80
- **Features**: Product browsing, cart management, order placement
- **Registration**: Create new account or use test user
- **Test User**: Use any email/password you create via registration

### **⚙️ Admin Dashboard**
- **URL**: http://localhost:80/admin (or dedicated admin port if configured)
- **Login**: `admin@example.com` / `YOUR_ADMIN_PASSWORD`
- **Features**: User management, product catalog admin, order management
- **Permissions**: Full administrative access

### **📊 Vendor Portal**
- **URL**: http://localhost:80/vendor (or dedicated vendor port if configured)
- **Login**: Create vendor account or use admin credentials
- **Features**: Product management, inventory tracking, sales analytics

### **📱 Mobile PWA**
- **URL**: http://localhost:80/mobile (responsive mobile interface)
- **Login**: Same as customer portal
- **Features**: Mobile-optimized shopping experience

### Sample Database Initialization
```javascript
// Automatic initialization via init-mongo.js
// Creates sample products, admin user, and indexes
```

## 🔧 Development Setup

### Local Development
```bash
# Prerequisites: Java 17+, Maven 3.6+, MongoDB

# Start MongoDB
docker run -d -p 27017:27017 --name mongodb \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=YOUR_SECURE_PASSWORD \
  mongo:7.0

# Build services
mvn clean install -DskipTests

# Startup order (each in separate terminal)
cd eureka-server && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd cart-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

## 🛡️ Security Features

- **JWT Authentication**: Secure token-based authentication
- **BCrypt Passwords**: Encrypted password storage
- **CORS Configuration**: Proper cross-origin setup
- **API Gateway Security**: Centralized security policies
- **MongoDB Authentication**: Database access control

## 🔍 Troubleshooting

### Health Checks
```bash
# Service health
curl http://localhost:8081/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Product Service

# Eureka dashboard
open http://localhost:8761

# Monitoring stack
curl http://localhost:9090/api/v1/targets    # Prometheus
curl http://localhost:3000/api/health        # Grafana
```

### Common Issues

1. **Port Conflicts**: Ensure ports 27017, 3000, 4317, 8081-8085, 8761, 9090 are free
2. **MongoDB Connection**: Wait for MongoDB to fully initialize
3. **Build Failures**: Verify Java 17+ and Maven 3.6+
4. **Docker Issues**: Ensure Docker daemon is running

### Useful Commands
```bash
# View container logs
docker-compose logs -f user-service

# Restart specific service
docker-compose restart product-service

# Check running containers
docker ps

# System cleanup
docker-compose down -v
docker system prune -a
```

## 📁 Project Structure

```
ecommerce-microservices/
├── api-gateway/              # Spring Cloud Gateway
├── user-service/             # User management microservice
├── product-service/          # Product catalog microservice
├── cart-service/             # Shopping cart microservice
├── order-service/            # Order processing microservice
├── eureka-server/            # Service discovery
├── intelligent-monitoring-service/  # Advanced monitoring
├── frontend/                 # Customer portal (Angular)
├── admin-dashboard/          # Admin portal
├── customer-portal/          # Customer portal (Angular)
├── vendor-portal/            # Vendor management portal
├── monitoring/               # Observability stack configs
├── docs/                     # Comprehensive documentation
├── scripts/                  # Utility scripts
├── docker-compose.yml        # Complete stack orchestration
├── ARCHITECTURE_DIAGRAM.md   # Detailed architecture
├── DEPLOYMENT_STRATEGY.md    # Production deployment guide
└── TRACING_ACCESS_GUIDE.md   # OpenTelemetry setup
```

## 📚 Documentation

Comprehensive documentation is organized in the [`docs/`](docs/) directory:

### Quick Links
- **Getting Started**: [Project Setup](docs/development/PROJECT_SETUP_GUIDE.md) | [Development Guide](docs/development/DEVELOPMENT_GUIDE.md) | [Local Setup](docs/development/README.local.md)
- **Architecture**: [Overview](docs/architecture/ARCHITECTURE.md) | [Diagram](docs/architecture/ARCHITECTURE_DIAGRAM.md) | [Flow Summary](docs/architecture/EXECUTIVE_FLOW_SUMMARY.md)
- **Deployment**: [Strategy](docs/DEPLOYMENT_STRATEGY.md) | [Success Summary](docs/deployment/DEPLOYMENT_SUCCESS_SUMMARY.md) | [Test Status](docs/deployment/DEPLOYMENT_TEST_STATUS.md)
- **Monitoring**: [Strategy](monitoring/MONITORING_STRATEGY.md) | [Portal](monitoring/UNIFIED_MONITORING_PORTAL.md) | [Tracing](docs/guides/TRACING_ACCESS_GUIDE.md)
- **Testing**: [Strategy](testing/TESTING_STRATEGY.md) | [Contract Testing](docs/development/CONTRACT_TESTING.md)
- **Setup Guides**: [AppDynamics](docs/guides/APPDYNAMICS_SETUP.md) | [Tracing Status](docs/guides/TRACING_STATUS_REPORT.md)

### Documentation Categories
- **🏗️ [Architecture](docs/architecture/)** - System design, diagrams, and executive summaries
- **🚀 [Deployment](docs/deployment/)** - Deployment strategies, success reports, and test status
- **💻 [Development](docs/development/)** - Development guides, setup instructions, and contract testing
- **📖 [Guides](docs/guides/)** - Setup guides for AppDynamics, tracing, and monitoring
- **📊 [Monitoring](monitoring/)** - Monitoring strategies and unified portal documentation
- **🔗 [Integration](integration/)** - Integration patterns and SRE Analytics integration
- **✅ [Testing](testing/)** - Testing strategies and best practices
- **🔒 [Compliance](compliance/)** - Security, accessibility, and business strategy
- **📈 [Improvements](docs/improvements/)** - Roadmaps, analysis, and refactoring documentation
- **📝 [Summaries](docs/summaries/)** - Session summaries and quick references

See **[docs/README.md](docs/README.md)** for complete documentation index and navigation.

## 🎯 Production Considerations

- **Security**: Replace default MongoDB credentials
- **Scaling**: Kubernetes deployment ready
- **Monitoring**: Comprehensive observability stack included
- **Configuration**: Externalized configuration support
- **SSL/TLS**: HTTPS termination ready
- **High Availability**: Load balancing and replica support

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🚀 Quick Reference

### **Essential Startup Commands**
```bash
# Complete stack startup
./build-all.sh && docker-compose up -d --build

# Verify all services
docker-compose ps && curl http://localhost:8081/actuator/health

# Start load testing
./start-load-test.sh normal

# Check monitoring
open http://localhost:3000  # Grafana (admin/YOUR_ADMIN_PASSWORD)
open http://localhost:8761  # Eureka Dashboard
```

### **Key Access Points**
| Service | URL | Credentials |
|---------|-----|-------------|
| **Customer Portal** | http://localhost:80 | Register new account |
| **Admin Dashboard** | http://localhost:80/admin | admin@example.com / YOUR_ADMIN_PASSWORD |
| **API Gateway** | http://localhost:8081 | None required |
| **Grafana** | http://localhost:3000 | admin / YOUR_ADMIN_PASSWORD |
| **Prometheus** | http://localhost:9090 | None required |
| **Eureka** | http://localhost:8761 | None required |
| **MongoDB** | localhost:27017 | admin / YOUR_SECURE_PASSWORD |

### **Load Testing Quick Commands**
```bash
./start-load-test.sh light    # Light load
./start-load-test.sh peak     # Peak traffic
./start-load-test.sh status   # Check status
./start-load-test.sh stop     # Stop all tests
```

---

**🚀 Ready for production deployment with comprehensive monitoring and observability!**