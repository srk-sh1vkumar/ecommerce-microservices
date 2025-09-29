# Testing Strategy for E-commerce Microservices

## Overview

This document outlines a comprehensive testing strategy for the e-commerce microservices application, covering unit testing, integration testing, end-to-end testing, performance testing, and security testing.

## Testing Pyramid

```
                    /\
                   /  \
                  / E2E \
                 /______\
                /        \
               /Integration\
              /__________\
             /            \
            /     Unit     \
           /______________\
```

### Testing Levels

1. **Unit Tests (70%)**: Fast, isolated, developer-focused
2. **Integration Tests (20%)**: Service interaction validation
3. **End-to-End Tests (10%)**: Full user journey validation

## Unit Testing Strategy

### Framework and Tools
- **JUnit 5**: Primary testing framework
- **Mockito**: Mocking framework
- **TestContainers**: Database testing with real MongoDB
- **WireMock**: HTTP service mocking
- **AssertJ**: Fluent assertions

### Unit Test Structure

```java
// Example: User Service Unit Test
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("Should create user successfully with valid data")
    void shouldCreateUserSuccessfully() {
        // Given
        User user = User.builder()
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();
        
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // When
        User createdUser = userService.createUser(user, "YOUR_SECURE_PASSWORD");
        
        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user already exists")
    void shouldThrowExceptionWhenUserExists() {
        // Given
        User user = User.builder()
            .email("existing@example.com")
            .build();
        
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.createUser(user, "YOUR_SECURE_PASSWORD"))
            .isInstanceOf(UserServiceException.class)
            .hasMessage("User already exists with email: existing@example.com");
    }
}
```

### Test Configuration

```java
// TestConfiguration for common test setup
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4); // Faster encoding for tests
    }
}
```

### Database Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = User.builder()
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();
        
        userRepository.save(user);
        
        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFirstName()).isEqualTo("John");
    }
}
```

## Integration Testing Strategy

### Service Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CartServiceIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");
    
    @Container
    static WireMockContainer productServiceMock = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("product-service", CartServiceIntegrationTest.class, "product-service-stubs.json");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldAddItemToCart() {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
            .userEmail("test@example.com")
            .productId("product-123")
            .quantity(2)
            .build();
        
        // Mock product service response
        productServiceMock.stubFor(get(urlEqualTo("/products/product-123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"product-123\",\"name\":\"Test Product\",\"price\":99.99}")));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/cart/add", request, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### Contract Testing with Pact

```java
// Consumer contract test (Cart Service)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product-service")
class CartServiceContractTest {
    
    @Pact(consumer = "cart-service")
    public RequestResponsePact getProduct(PactDslWithProvider builder) {
        return builder
            .given("product exists")
            .uponReceiving("a request for product details")
            .path("/products/123")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("id", "123")
                .stringType("name", "Test Product")
                .numberType("price", 99.99)
                .numberType("stockQuantity", 10))
            .toPact();
    }
    
    @Test
    @PactTestFor
    void testGetProduct(MockServer mockServer) {
        // Given
        ProductServiceClient client = new ProductServiceClient(mockServer.getUrl());
        
        // When
        ProductDTO product = client.getProduct("123");
        
        // Then
        assertThat(product.getId()).isEqualTo("123");
        assertThat(product.getName()).isEqualTo("Test Product");
    }
}
```

## End-to-End Testing Strategy

### Framework: Playwright/Selenium

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class E2EUserJourneyTest {
    
    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
            new File("docker-compose.test.yml"))
            .withExposedService("api-gateway", 8080)
            .withExposedService("mongodb", 27017);
    
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @Test
    @DisplayName("Complete user journey: Register → Login → Add to Cart → Checkout")
    void completeUserJourney() {
        // Given
        String baseUrl = "http://localhost:8080";
        String userEmail = "e2e-test@example.com";
        
        // Step 1: Register new user
        driver.get(baseUrl + "/register");
        driver.findElement(By.id("email")).sendKeys(userEmail);
        driver.findElement(By.id("password")).sendKeys("YOUR_SECURE_PASSWORD");
        driver.findElement(By.id("firstName")).sendKeys("John");
        driver.findElement(By.id("lastName")).sendKeys("Doe");
        driver.findElement(By.id("register-button")).click();
        
        // Step 2: Login
        driver.get(baseUrl + "/login");
        driver.findElement(By.id("email")).sendKeys(userEmail);
        driver.findElement(By.id("password")).sendKeys("YOUR_SECURE_PASSWORD");
        driver.findElement(By.id("login-button")).click();
        
        // Step 3: Browse products and add to cart
        driver.get(baseUrl + "/products");
        driver.findElement(By.cssSelector("[data-product-id='1'] .add-to-cart")).click();
        
        // Step 4: View cart
        driver.findElement(By.id("cart-icon")).click();
        WebElement cartTotal = driver.findElement(By.id("cart-total"));
        assertThat(cartTotal.getText()).isNotEmpty();
        
        // Step 5: Checkout
        driver.findElement(By.id("checkout-button")).click();
        driver.findElement(By.id("shipping-address")).sendKeys("123 Test Street");
        driver.findElement(By.id("place-order-button")).click();
        
        // Verify order confirmation
        WebElement orderConfirmation = driver.findElement(By.id("order-confirmation"));
        assertThat(orderConfirmation.getText()).contains("Order placed successfully");
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

## Performance Testing Strategy

### Load Testing with JMeter/Gatling

```scala
// Gatling load test scenario
class EcommerceLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
  
  val userRegistration = scenario("User Registration")
    .exec(http("Register User")
      .post("/api/users/register")
      .body(StringBody("""{"email":"user${userId}@test.com","password":"YOUR_SECURE_PASSWORD","firstName":"John","lastName":"Doe"}"""))
      .check(status.is(200)))
  
  val productBrowsing = scenario("Product Browsing")
    .exec(http("Get Products")
      .get("/api/products")
      .check(status.is(200)))
    .pause(1, 3)
    .exec(http("Search Products")
      .get("/api/products/search?name=iPhone")
      .check(status.is(200)))
  
  val checkoutFlow = scenario("Checkout Flow")
    .exec(http("Login")
      .post("/api/users/login")
      .body(StringBody("""{"email":"user@test.com","password":"YOUR_SECURE_PASSWORD"}"""))
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("authToken")))
    .exec(http("Add to Cart")
      .post("/api/cart/add")
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody("""{"userEmail":"user@test.com","productId":"1","quantity":1}"""))
      .check(status.is(200)))
    .exec(http("Checkout")
      .post("/api/orders/checkout")
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody("""{"userEmail":"user@test.com","shippingAddress":"123 Test St"}"""))
      .check(status.is(200)))
  
  setUp(
    userRegistration.inject(rampUsers(100) during (30 seconds)),
    productBrowsing.inject(constantUsers(50) during (60 seconds)),
    checkoutFlow.inject(rampUsers(20) during (60 seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(5000),
     global.responseTime.mean.lt(1000),
     global.successfulRequests.percent.gt(95)
   )
}
```

### Performance Benchmarks

| Operation | Target Response Time | Target Throughput | SLA |
|-----------|---------------------|-------------------|-----|
| User Login | < 500ms | 1000 RPS | 99.9% |
| Product Search | < 1000ms | 500 RPS | 99.5% |
| Add to Cart | < 300ms | 800 RPS | 99.9% |
| Checkout | < 2000ms | 100 RPS | 99.0% |

## Security Testing Strategy

### Security Test Categories

1. **Authentication & Authorization Tests**
2. **Input Validation Tests**
3. **SQL/NoSQL Injection Tests**
4. **Cross-Site Scripting (XSS) Tests**
5. **Cross-Site Request Forgery (CSRF) Tests**
6. **Security Headers Tests**

### OWASP ZAP Integration

```java
@Test
void securityScanWithZAP() {
    ClientApi zapClient = new ClientApi("localhost", 8090);
    
    try {
        // Spider the application
        zapClient.spider.scan("http://localhost:8080", null, null, null, null);
        
        // Active security scan
        zapClient.ascan.scan("http://localhost:8080", "True", "False", null, null, null);
        
        // Generate report
        String reportHtml = new String(zapClient.core.htmlreport());
        
        // Assert no high-risk vulnerabilities
        assertThat(reportHtml).doesNotContain("High (High)");
        
    } catch (ClientApiException e) {
        fail("Security scan failed: " + e.getMessage());
    }
}
```

### Security Unit Tests

```java
@Test
void shouldPreventSQLInjection() {
    // Given
    String maliciousEmail = "admin@test.com'; DROP TABLE users; --";
    
    // When & Then
    assertThatThrownBy(() -> userService.findByEmail(maliciousEmail))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void shouldValidateJWTToken() {
    // Given
    String invalidToken = "invalid.jwt.token";
    
    // When & Then
    assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
        .isInstanceOf(SecurityException.class);
}
```

## Test Data Management

### Test Data Strategy

```java
// Test data builder pattern
public class TestDataBuilder {
    
    public static User.UserBuilder defaultUser() {
        return User.builder()
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .password("hashedPassword")
            .createdAt(Instant.now())
            .isActive(true);
    }
    
    public static Product.ProductBuilder defaultProduct() {
        return Product.builder()
            .name("Test Product")
            .description("Test product description")
            .price(BigDecimal.valueOf(99.99))
            .category("Electronics")
            .stockQuantity(10)
            .imageUrl("http://example.com/image.jpg");
    }
}
```

### Database Cleanup Strategy

```java
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class
})
@DatabaseSetup("classpath:test-data/initial-dataset.xml")
@DatabaseTearDown(type = DatabaseOperation.DELETE_ALL, value = "classpath:test-data/cleanup-dataset.xml")
class DatabaseTest {
    // Test implementation
}
```

## Continuous Integration Testing

### GitHub Actions Workflow

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mongodb:
        image: mongo:7.0
        env:
          MONGO_INITDB_ROOT_USERNAME: admin
          MONGO_INITDB_ROOT_PASSWORD: YOUR_SECURE_PASSWORD
        ports:
          - 27017:27017
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run unit tests
      run: mvn test
    
    - name: Run integration tests
      run: mvn verify -P integration-tests
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: '**/target/surefire-reports/*.xml'
        reporter: java-junit
    
    - name: Code coverage
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
```

## Test Metrics and Reporting

### Coverage Requirements
- **Unit Test Coverage**: Minimum 80%
- **Integration Test Coverage**: Minimum 60%
- **Critical Path Coverage**: 100%

### Quality Gates
```xml
<!-- Maven Surefire Plugin Configuration -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Testing Best Practices

### General Guidelines

1. **Fast Feedback**: Unit tests should run in < 10 seconds
2. **Independent Tests**: No test dependencies or shared state
3. **Descriptive Names**: Test names should describe behavior
4. **AAA Pattern**: Arrange, Act, Assert structure
5. **Single Responsibility**: One assertion per test method

### Test Naming Convention

```java
// Pattern: should[ExpectedBehavior]When[StateUnderTest]
@Test
void shouldReturnUserWhenValidEmailProvided() { }

@Test
void shouldThrowExceptionWhenUserNotFound() { }

@Test
void shouldUpdateInventoryWhenOrderCompleted() { }
```

### Mock Usage Guidelines

```java
// Good: Mock external dependencies
@Mock
private ProductServiceClient productServiceClient;

// Good: Verify important interactions
verify(userRepository).save(any(User.class));

// Avoid: Over-mocking internal logic
// Don't mock value objects or simple data structures
```

This comprehensive testing strategy ensures reliability, security, and maintainability of the e-commerce microservices application.