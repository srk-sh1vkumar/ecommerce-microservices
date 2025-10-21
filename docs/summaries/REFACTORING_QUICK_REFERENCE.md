# 🚀 Refactoring Quick Reference Guide

Quick reference for developers working with the refactored codebase.

## 📦 Common Library Components

### Exception Handling

```java
// Import
import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.constants.ErrorCodes;

// Usage
throw ServiceException.badRequest("Invalid input");
throw ServiceException.notFound("Resource not found", ErrorCodes.USER_NOT_FOUND);
throw ServiceException.unauthorized("Access denied", ErrorCodes.INVALID_CREDENTIALS);
throw ServiceException.conflict("Already exists", ErrorCodes.USER_ALREADY_EXISTS);
throw ServiceException.internalError("Internal error");
throw ServiceException.serviceUnavailable("Service unavailable");
```

### Validation

```java
// Import
import com.ecommerce.common.util.ValidationUtils;

// Email validation
ValidationUtils.validateEmail(email);
String normalized = ValidationUtils.normalizeEmail(email);

// Password validation
ValidationUtils.validatePassword(password);

// Field validation
ValidationUtils.validateNotBlank(field, "Field Name");
ValidationUtils.validateId(id, "Entity Name");

// Number validation
ValidationUtils.validatePositive(quantity, "Quantity");
ValidationUtils.validatePrice(price);
ValidationUtils.validateQuantity(quantity);

// Stock validation
ValidationUtils.validateStock(requestedQty, availableQty);
```

### JWT Operations

```java
// Import
import com.ecommerce.common.util.JwtUtil;

// Inject
@Autowired
private JwtUtil jwtUtil;

// Generate token
String token = jwtUtil.generateToken(email);

// Validate token
boolean isValid = jwtUtil.validateToken(token);

// Extract email
String email = jwtUtil.getEmailFromToken(token);

// Check expiration
boolean expired = jwtUtil.isTokenExpired(token);
```

### API Response

```java
// Import
import com.ecommerce.common.dto.ApiResponse;

// Success with data
return ApiResponse.success(data);

// Success with message
return ApiResponse.success(data, "Operation successful");

// Error response
return ApiResponse.error("Operation failed");
```

## 🛡️ Resilience Patterns

### Circuit Breaker

```java
// Import
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

// Usage
@CircuitBreaker(name = "serviceName", fallbackMethod = "methodNameFallback")
public Result methodName() {
    // Your code
}

// Fallback method (must match parameters + Exception)
private Result methodNameFallback(Exception e) {
    logger.error("Operation failed", e);
    return defaultValue;
}
```

### Retry

```java
// Import
import io.github.resilience4j.retry.annotation.Retry;

// Usage
@Retry(name = "serviceName")
public Result methodName() {
    // Your code
}

// Combined with Circuit Breaker
@CircuitBreaker(name = "serviceName", fallbackMethod = "fallback")
@Retry(name = "serviceName")
public Result methodName() {
    // Your code
}
```

### Caching

```java
// Import
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

// Read with cache
@Cacheable(value = "cacheName", key = "#id")
public Entity findById(String id) {
    return repository.findById(id);
}

// Write with cache eviction
@CacheEvict(value = "cacheName", key = "#id")
public Entity update(String id, Entity data) {
    return repository.save(data);
}

// Clear all cache entries
@CacheEvict(value = "cacheName", allEntries = true)
public Entity create(Entity data) {
    return repository.save(data);
}
```

## 📝 Error Codes

### User Service (USR-1xxx)
- `USR-1001` - User not found
- `USR-1002` - User already exists
- `USR-1003` - Invalid credentials
- `USR-1004` - Weak password
- `USR-1005` - Invalid email

### Product Service (PRD-2xxx)
- `PRD-2001` - Product not found
- `PRD-2002` - Insufficient stock
- `PRD-2003` - Invalid price
- `PRD-2004` - Invalid category
- `PRD-2005` - Product out of stock

### Cart Service (CRT-3xxx)
- `CRT-3001` - Cart empty
- `CRT-3002` - Cart item not found
- `CRT-3003` - Invalid quantity
- `CRT-3004` - Cart limit exceeded

### Order Service (ORD-4xxx)
- `ORD-4001` - Order not found
- `ORD-4002` - Order already processed
- `ORD-4003` - Invalid shipping address
- `ORD-4004` - Payment failed
- `ORD-4005` - Order cannot be cancelled

### General (GEN-9xxx)
- `GEN-9001` - Validation error
- `GEN-9002` - Internal error
- `GEN-9003` - Service unavailable
- `GEN-9004` - Database error
- `GEN-9005` - External service error

## 🔧 Build Commands

```bash
# Build common library
cd ecommerce-microservices/common-lib
mvn clean install

# Build all services
cd ecommerce-microservices
./build-all.sh

# Build specific service
cd ecommerce-microservices/user-service
mvn clean package

# Run tests
mvn test

# Skip tests
mvn clean install -DskipTests
```

## 🐳 Docker Commands

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f user-service

# Restart service
docker-compose restart user-service

# Stop all
docker-compose down

# Clean slate
docker-compose down -v
```

## 📊 Service Endpoints

### Health Checks
```bash
curl http://localhost:8081/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Product Service
curl http://localhost:8084/actuator/health  # Cart Service
curl http://localhost:8085/actuator/health  # Order Service
```

### Monitoring
- **Grafana:** http://localhost:3000 (admin/YOUR_ADMIN_PASSWORD)
- **Prometheus:** http://localhost:9090
- **Eureka:** http://localhost:8761

## 💡 Common Patterns

### Service Method Pattern

```java
@Service
@Transactional
public class MyService {

    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    private static final String CIRCUIT_BREAKER_NAME = "myService";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @Transactional(readOnly = true)
    @Cacheable(value = "cache", key = "#id")
    public Entity getById(String id) {
        logger.debug("Fetching entity: {}", id);

        ValidationUtils.validateId(id, "Entity");

        return repository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(
                        "Entity not found",
                        ErrorCodes.ENTITY_NOT_FOUND
                ));
    }

    private Entity getFallback(String id, Exception e) {
        logger.error("Failed to fetch entity: {}", id, e);
        throw ServiceException.serviceUnavailable("Service temporarily unavailable");
    }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/entities")
public class MyController {

    private static final Logger logger = LoggerFactory.getLogger(MyController.class);
    private final MyService service;

    @Autowired
    public MyController(MyService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Entity>> getById(@PathVariable String id) {
        logger.info("GET request for entity: {}", id);
        Entity entity = service.getById(id);
        return ResponseEntity.ok(ApiResponse.success(entity));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Entity>> create(@Valid @RequestBody Entity entity) {
        logger.info("POST request to create entity");
        Entity created = service.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Entity created successfully"));
    }
}
```

## 🔍 Troubleshooting

### Common Library Not Found
```bash
cd ecommerce-microservices/common-lib
mvn clean install
```

### Compilation Errors
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/com/ecommerce/common-lib
mvn clean install
```

### Test Failures
```bash
# Skip tests temporarily
mvn clean package -DskipTests

# Run specific test
mvn test -Dtest=MyTest
```

### Circuit Breaker Not Working
- Verify `@EnableCircuitBreaker` in main application class
- Check resilience4j configuration in application.yml
- Ensure fallback method signature matches

## 📚 Additional Resources

- [Full Refactoring Summary](REFACTORING_SUMMARY.md)
- [Architecture Diagram](ARCHITECTURE_DIAGRAM.md)
- [Development Guide](DEVELOPMENT_GUIDE.md)
- [README](README.md)