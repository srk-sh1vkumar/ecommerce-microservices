# E-commerce Platform - API Documentation Guide

**Version**: 2.0.0
**Last Updated**: 2025-10-21
**Target Audience**: Developers, QA Engineers, Integration Partners

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Accessing Swagger UI](#accessing-swagger-ui)
4. [API Structure](#api-structure)
5. [Authentication](#authentication)
6. [Testing APIs](#testing-apis)
7. [Service-Specific Documentation](#service-specific-documentation)
8. [Common Use Cases](#common-use-cases)
9. [Error Handling](#error-handling)
10. [Best Practices](#best-practices)

---

## Overview

The E-commerce Platform provides comprehensive OpenAPI 3.0 (Swagger) documentation for all microservices. Each service exposes interactive API documentation through Swagger UI, allowing you to explore, test, and integrate with our APIs.

### Key Features

✅ **Interactive Documentation** - Test APIs directly from your browser
✅ **JWT Authentication** - Secure API access with token-based auth
✅ **Request/Response Examples** - Real-world examples for every endpoint
✅ **Schema Definitions** - Complete data model documentation
✅ **Multi-Environment Support** - Local, dev, and production configurations

---

## Quick Start

### Prerequisites

- Running e-commerce platform (see [Deployment Guide](../deployment/DEPLOYMENT_AUTOMATION_GUIDE.md))
- Web browser (Chrome, Firefox, Safari, Edge)
- JWT token (obtained from login endpoint)

### 5-Minute Tutorial

1. **Start the platform**:
   ```bash
   docker-compose up -d
   ```

2. **Access Swagger UI** (choose any service):
   - User Service: http://localhost:8082/swagger-ui.html
   - Product Service: http://localhost:8081/swagger-ui.html
   - Cart Service: http://localhost:8083/swagger-ui.html
   - Order Service: http://localhost:8084/swagger-ui.html

3. **Register a user**:
   - Navigate to User Service Swagger UI
   - Find `POST /api/users/register` endpoint
   - Click "Try it out"
   - Fill in the request body
   - Click "Execute"

4. **Login to get JWT token**:
   - Find `POST /api/users/login` endpoint
   - Provide credentials from registration
   - Copy the JWT token from the response

5. **Authorize future requests**:
   - Click the "Authorize" button (🔓) at the top of Swagger UI
   - Enter: `Bearer <your-jwt-token>`
   - Click "Authorize"
   - All subsequent requests will include the token

6. **Test protected endpoints**:
   - Try any protected endpoint (marked with 🔒 icon)
   - The JWT token will be automatically included

---

## Accessing Swagger UI

### Local Development

When running via Docker Compose:

| Service | Swagger UI URL | API Base URL |
|---------|---------------|--------------|
| **API Gateway** | http://localhost:8080/swagger-ui.html | http://localhost:8080/api |
| **User Service** | http://localhost:8082/swagger-ui.html | http://localhost:8082/api |
| **Product Service** | http://localhost:8081/swagger-ui.html | http://localhost:8081/api |
| **Cart Service** | http://localhost:8083/swagger-ui.html | http://localhost:8083/api |
| **Order Service** | http://localhost:8084/swagger-ui.html | http://localhost:8084/api |

### OpenAPI JSON Specification

Each service also exposes raw OpenAPI JSON:

| Service | OpenAPI Spec URL |
|---------|-----------------|
| User Service | http://localhost:8082/v3/api-docs |
| Product Service | http://localhost:8081/v3/api-docs |
| Cart Service | http://localhost:8083/v3/api-docs |
| Order Service | http://localhost:8084/v3/api-docs |

**Use Case**: Import into Postman, Insomnia, or code generators.

---

## API Structure

### Common Patterns

All services follow consistent API patterns:

```
/api/{resource}
├── GET      /{resource}           # List all (paginated)
├── GET      /{resource}/{id}      # Get by ID
├── POST     /{resource}           # Create new
├── PUT      /{resource}/{id}      # Update existing
└── DELETE   /{resource}/{id}      # Delete
```

### Pagination

List endpoints support pagination:

```http
GET /api/products?page=0&size=20&sort=createdAt,desc
```

**Parameters**:
- `page`: Page number (0-indexed, default: 0)
- `size`: Items per page (max: 100, default: 20)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Response** (Spring Data Page):
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

### Filtering and Search

Services support various filtering options:

```http
# Search products by name
GET /api/products/search?name=laptop

# Filter by category
GET /api/products/category/electronics

# Get available products only
GET /api/products/available
```

---

## Authentication

### Overview

The platform uses **JWT (JSON Web Token)** based authentication:

1. Register a user or login to obtain a JWT token
2. Include the token in the `Authorization` header for all protected endpoints
3. Tokens expire after 24 hours (configurable)

### Registration

**Endpoint**: `POST /api/users/register`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response**:
```json
{
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Login

**Endpoint**: `POST /api/users/login`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaXNzIjoiZWNvbW1lcmNlLXBsYXRmb3JtIiwiaWF0IjoxNjk4MjQwMDAwLCJleHAiOjE2OTgzMjY0MDB9.signature",
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com"
}
```

### Using JWT in Requests

**Method 1: Swagger UI** (Recommended for testing):
1. Click "Authorize" button (🔓)
2. Enter: `Bearer <token>`
3. Click "Authorize"
4. All requests will now include the token

**Method 2: Manual Header**:
```http
GET /api/products HTTP/1.1
Host: localhost:8081
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Method 3: cURL**:
```bash
curl -X GET "http://localhost:8081/api/products" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Testing APIs

### Using Swagger UI (Recommended)

Swagger UI provides the easiest way to test APIs interactively:

1. **Navigate to endpoint** - Expand the endpoint you want to test
2. **Click "Try it out"** - Activates the request form
3. **Fill parameters** - Enter required/optional parameters
4. **Execute** - Click "Execute" to send the request
5. **View response** - See response body, headers, and status code

### Using cURL

**Example: Get all products**:
```bash
curl -X GET "http://localhost:8081/api/products?page=0&size=10" \
  -H "accept: application/json"
```

**Example: Create product (authenticated)**:
```bash
curl -X POST "http://localhost:8081/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "category": "Electronics",
    "stock": 50
  }'
```

### Using Postman

1. **Import OpenAPI Spec**:
   - File → Import → Link
   - Enter: http://localhost:8081/v3/api-docs
   - Postman will create a collection with all endpoints

2. **Configure Authorization**:
   - Collection → Authorization → Bearer Token
   - Enter your JWT token
   - All requests will inherit the token

3. **Send Requests**:
   - Select endpoint from collection
   - Modify parameters/body as needed
   - Click "Send"

---

## Service-Specific Documentation

### User Service (Port 8082)

**Base URL**: http://localhost:8082/api/users
**Swagger UI**: http://localhost:8082/swagger-ui.html

**Key Endpoints**:
- `POST /api/users/register` - Register new user (public)
- `POST /api/users/login` - Login and get JWT token (public)
- `GET /api/users/profile` - Get current user profile (authenticated)
- `PUT /api/users/profile` - Update user profile (authenticated)
- `GET /api/users/{id}` - Get user by ID (admin only)

**Special Features**:
- Email validation
- Password strength requirements
- Rate limiting on login attempts
- JWT token generation

---

### Product Service (Port 8081)

**Base URL**: http://localhost:8081/api/products
**Swagger UI**: http://localhost:8081/swagger-ui.html

**Key Endpoints**:
- `GET /api/products` - Get all products (public, paginated)
- `GET /api/products/{id}` - Get product by ID (public)
- `GET /api/products/category/{category}` - Get products by category (public)
- `GET /api/products/search?name={name}` - Search products (public)
- `POST /api/products` - Create product (admin only)
- `PUT /api/products/{id}` - Update product (admin only)
- `DELETE /api/products/{id}` - Delete product (admin only)

**Special Features**:
- Redis caching (1-hour TTL)
- Circuit breaker protection (Resilience4j)
- Full-text search
- Category-based filtering
- Availability filtering

---

### Cart Service (Port 8083)

**Base URL**: http://localhost:8083/api/cart
**Swagger UI**: http://localhost:8083/swagger-ui.html

**Key Endpoints**:
- `GET /api/cart` - Get current user's cart (authenticated)
- `POST /api/cart/items` - Add item to cart (authenticated)
- `PUT /api/cart/items/{itemId}` - Update item quantity (authenticated)
- `DELETE /api/cart/items/{itemId}` - Remove item from cart (authenticated)
- `DELETE /api/cart` - Clear cart (authenticated)
- `GET /api/cart/total` - Get cart total (authenticated)

**Special Features**:
- User-specific cart isolation
- Automatic total calculation
- Stock validation
- Session management

---

### Order Service (Port 8084)

**Base URL**: http://localhost:8084/api/orders
**Swagger UI**: http://localhost:8084/swagger-ui.html

**Key Endpoints**:
- `GET /api/orders` - Get user's orders (authenticated)
- `GET /api/orders/{id}` - Get order details (authenticated)
- `POST /api/orders` - Create order from cart (authenticated)
- `PUT /api/orders/{id}/status` - Update order status (admin only)
- `POST /api/orders/{id}/payment` - Process payment (authenticated)

**Special Features**:
- Stripe payment integration
- Order status tracking
- Inventory management
- Email notifications

---

## Common Use Cases

### Use Case 1: Complete Shopping Flow

**Step 1: Register**:
```bash
curl -X POST "http://localhost:8082/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "shopper@example.com",
    "password": "SecureP@ss123",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

**Step 2: Login**:
```bash
TOKEN=$(curl -X POST "http://localhost:8082/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"shopper@example.com","password":"SecureP@ss123"}' \
  | jq -r '.token')
```

**Step 3: Browse Products**:
```bash
curl -X GET "http://localhost:8081/api/products?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Step 4: Add to Cart**:
```bash
curl -X POST "http://localhost:8083/api/cart/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": "507f1f77bcf86cd799439011",
    "quantity": 2
  }'
```

**Step 5: View Cart**:
```bash
curl -X GET "http://localhost:8083/api/cart" \
  -H "Authorization: Bearer $TOKEN"
```

**Step 6: Checkout**:
```bash
curl -X POST "http://localhost:8084/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "shippingAddress": "123 Main St, City, 12345",
    "paymentMethod": "stripe",
    "paymentToken": "tok_visa"
  }'
```

---

### Use Case 2: Admin Product Management

**Step 1: Login as Admin**:
```bash
ADMIN_TOKEN=$(curl -X POST "http://localhost:8082/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce.com","password":"AdminP@ss"}' \
  | jq -r '.token')
```

**Step 2: Create Product**:
```bash
curl -X POST "http://localhost:8081/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4090",
    "price": 2499.99,
    "category": "Electronics",
    "stock": 25,
    "imageUrl": "https://example.com/laptop.jpg"
  }'
```

**Step 3: Update Product**:
```bash
curl -X PUT "http://localhost:8081/api/products/507f1f77bcf86cd799439011" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Gaming Laptop Pro",
    "price": 2299.99,
    "stock": 30
  }'
```

---

### Use Case 3: Integration Testing

**Automated Test Script**:
```bash
#!/bin/bash
# test-api-flow.sh

API_URL="http://localhost:8082"

echo "1. Registering user..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123","firstName":"Test","lastName":"User"}')

echo "2. Logging in..."
TOKEN=$(curl -s -X POST "$API_URL/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123"}' \
  | jq -r '.token')

echo "3. Fetching profile..."
PROFILE=$(curl -s -X GET "$API_URL/api/users/profile" \
  -H "Authorization: Bearer $TOKEN")

echo "Profile: $PROFILE"
echo "Test completed successfully!"
```

---

## Error Handling

### Standard Error Response Format

All services return consistent error responses:

```json
{
  "timestamp": "2025-10-21T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid product data: price cannot be negative",
  "path": "/api/products"
}
```

### Common HTTP Status Codes

| Status Code | Meaning | Example |
|------------|---------|---------|
| `200 OK` | Success | Product retrieved successfully |
| `201 Created` | Resource created | User registered successfully |
| `204 No Content` | Success, no response body | Item removed from cart |
| `400 Bad Request` | Invalid input | Missing required field |
| `401 Unauthorized` | Missing or invalid token | JWT token expired |
| `403 Forbidden` | Insufficient permissions | Admin endpoint accessed by regular user |
| `404 Not Found` | Resource not found | Product ID does not exist |
| `409 Conflict` | Resource conflict | Email already registered |
| `500 Internal Server Error` | Server error | Database connection failure |
| `503 Service Unavailable` | Circuit breaker open | Product service unavailable |

### Validation Errors

Validation errors include detailed field-level information:

```json
{
  "timestamp": "2025-10-21T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Multiple validation errors",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address"
    },
    {
      "field": "password",
      "message": "must be at least 8 characters"
    }
  ]
}
```

---

## Best Practices

### Security

✅ **Always use HTTPS in production** - Never send JWT tokens over HTTP
✅ **Store tokens securely** - Use secure storage (not localStorage)
✅ **Handle token expiration** - Implement token refresh logic
✅ **Validate input** - Never trust client-side data
✅ **Use strong passwords** - Minimum 8 characters, mixed case, numbers, symbols

### Performance

✅ **Use pagination** - Always paginate large datasets
✅ **Cache responses** - Product Service uses Redis caching
✅ **Minimize payload size** - Only request fields you need
✅ **Use compression** - Enable gzip compression
✅ **Implement retry logic** - Handle transient failures gracefully

### API Integration

✅ **Version your integrations** - API version is in the path (`/api/v2/...`)
✅ **Handle errors gracefully** - Check status codes and error messages
✅ **Implement circuit breakers** - Prevent cascading failures
✅ **Log requests/responses** - For debugging and monitoring
✅ **Use idempotency keys** - For payment and order operations

### Testing

✅ **Test all endpoints** - Use Swagger UI for exploratory testing
✅ **Automate tests** - Create test scripts for CI/CD
✅ **Test error cases** - Don't just test happy paths
✅ **Use test data** - Never use production data for testing
✅ **Clean up after tests** - Delete test resources

---

## Additional Resources

### Documentation

- [Architecture Overview](../architecture/ARCHITECTURE.md)
- [Deployment Guide](../deployment/DEPLOYMENT_AUTOMATION_GUIDE.md)
- [Development Guide](../development/DEVELOPMENT_GUIDE.md)
- [Testing Strategy](../../testing/TESTING_STRATEGY.md)

### External Tools

- **Swagger Editor**: https://editor.swagger.io/
- **Postman**: https://www.postman.com/downloads/
- **Insomnia**: https://insomnia.rest/download
- **curl Documentation**: https://curl.se/docs/

### OpenAPI Specification

- **OpenAPI 3.0 Spec**: https://spec.openapis.org/oas/v3.0.0
- **Springdoc OpenAPI**: https://springdoc.org/
- **Swagger Annotations**: https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations

---

## Support

For questions or issues:

1. Check the Swagger UI interactive documentation
2. Review this guide and related documentation
3. Check service health endpoints: `http://localhost:{port}/actuator/health`
4. Review service logs: `docker-compose logs -f <service-name>`
5. Create an issue: https://github.com/srk-sh1vkumar/ecommerce-microservices/issues

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-21
**Maintained By**: E-commerce Development Team
