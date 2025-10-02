# Spring Cloud Contract Testing

**Implementation Date:** 2025-10-01
**Status:** ✅ Implemented for Product Service
**Framework:** Spring Cloud Contract 4.1.0

---

## Overview

Consumer-Driven Contract (CDC) testing ensures that microservices can communicate correctly without breaking integration when services are updated independently.

### Benefits:
- ✅ Catch integration breaks early
- ✅ Test services in isolation
- ✅ Automatic stub generation
- ✅ API versioning safety
- ✅ Consumer-driven design

---

## Architecture

```
┌─────────────────────┐           ┌─────────────────────┐
│  Product Service    │           │   Cart Service      │
│    (Producer)       │           │   (Consumer)        │
├─────────────────────┤           ├─────────────────────┤
│                     │           │                     │
│ 1. Defines          │           │ 4. Uses stubs       │
│    Contracts        │           │    in tests         │
│                     │           │                     │
│ 2. Verifies         │◄──────────┤ 5. Validates        │
│    against          │  Stubs    │    expectations     │
│    contracts        │           │                     │
│                     │           │                     │
│ 3. Generates        │           │                     │
│    stubs            │           │                     │
└─────────────────────┘           └─────────────────────┘
```

---

## Producer Side (Product Service)

### 1. Contracts Definition

Location: `product-service/src/test/resources/contracts/`

**Example Contract:**
```groovy
// shouldReturnProductById.groovy
package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return product by ID when product exists"

    request {
        method GET()
        url '/api/products/test-product-123'
        headers {
            contentType(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 'test-product-123',
            name: 'Test Wireless Mouse',
            description: 'A high-quality wireless mouse',
            price: 29.99,
            category: 'Electronics',
            stockQuantity: 150,
            imageUrl: 'https://example.com/images/mouse.jpg'
        ])
        bodyMatchers {
            jsonPath('$.id', byRegex('[a-zA-Z0-9-]+'))
            jsonPath('$.price', byRegex('[0-9]+\\.[0-9]{2}'))
            jsonPath('$.stockQuantity', byRegex('[0-9]+'))
        }
    }
}
```

### 2. Base Test Class

Location: `product-service/src/test/java/com/ecommerce/product/contract/ContractVerifierBase.java`

Purpose: Sets up the test context and provides mock data for contract validation.

```java
@WebMvcTest(ProductController.class)
public abstract class ContractVerifierBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Setup mock data matching contracts
        Product testProduct = createTestProduct(...);
        when(productService.getProductById("test-product-123"))
            .thenReturn(Optional.of(testProduct));
    }
}
```

### 3. Maven Plugin Configuration

```xml
<plugin>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-contract-maven-plugin</artifactId>
    <version>4.1.0</version>
    <extensions>true</extensions>
    <configuration>
        <testFramework>JUNIT5</testFramework>
        <baseClassForTests>
            com.ecommerce.product.contract.ContractVerifierBase
        </baseClassForTests>
        <contractsDirectory>
            ${project.basedir}/src/test/resources/contracts
        </contractsDirectory>
    </configuration>
</plugin>
```

### 4. Generate Stubs

```bash
cd product-service
mvn clean install
```

This generates:
- Contract verification tests
- Stub JAR with mappings
- Stubs in local Maven repository

---

## Consumer Side (Cart Service)

### 1. Stub Runner Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. Consumer Contract Test

Location: `cart-service/src/test/java/com/ecommerce/cart/contract/ProductServiceContractTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.ecommerce:product-service:+:stubs:8082",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class ProductServiceContractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void shouldGetProductById() {
        // Given
        String url = "http://localhost:8082/api/products/test-product-123";

        // When
        ResponseEntity<ProductDTO> response =
            restTemplate.getForEntity(url, ProductDTO.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getId())
            .isEqualTo("test-product-123");
    }
}
```

### 3. Run Consumer Tests

```bash
cd cart-service
mvn test
```

The Stub Runner will:
1. Download stubs from local Maven repo
2. Start WireMock server on port 8082
3. Run tests against the stub
4. Verify consumer expectations match producer contract

---

## Implemented Contracts

### Product Service Contracts:

| Contract | Method | Endpoint | Purpose |
|----------|--------|----------|---------|
| shouldReturnProductById | GET | `/api/products/{id}` | Get product details |
| shouldReturnProductsByCategory | GET | `/api/products/category/{category}` | List products by category |
| shouldUpdateStockSuccessfully | PUT | `/api/products/{id}/stock?quantity={qty}` | Update product stock |

---

## Workflow

### Producer (Product Service):

1. **Define Contract**
   ```bash
   vi product-service/src/test/resources/contracts/newContract.groovy
   ```

2. **Update Base Test Class**
   - Add mock data for new contract
   - Ensure controller behavior matches contract

3. **Verify Contracts**
   ```bash
   mvn clean verify
   ```
   This auto-generates and runs verification tests

4. **Publish Stubs**
   ```bash
   mvn clean install
   ```
   Installs stubs to local Maven repo

### Consumer (Cart Service):

1. **Use Latest Stubs**
   ```bash
   mvn clean test
   ```
   Automatically downloads latest stubs

2. **Write Consumer Tests**
   - Test expected behavior
   - Validate response structure
   - Check error scenarios

3. **CI/CD Integration**
   - Tests fail if producer breaks contract
   - Early warning before deployment

---

## Running Tests

### Full Producer Verification:
```bash
cd product-service
mvn clean verify
```

### Full Consumer Validation:
```bash
cd cart-service
mvn clean test -Dtest=ProductServiceContractTest
```

### Generate Contract Tests Only:
```bash
cd product-service
mvn spring-cloud-contract:generateTests
```

---

## Best Practices

### 1. Contract Organization:
```
contracts/
├── getProductById.groovy
├── getProductsByCategory.groovy
├── updateStock.groovy
└── bulkUpdateStock.groovy
```

### 2. Use Descriptive Names:
- ✅ `shouldReturnProductWhenExists.groovy`
- ❌ `test1.groovy`

### 3. Include Matchers:
```groovy
bodyMatchers {
    jsonPath('$.id', byRegex('[a-zA-Z0-9-]+'))
    jsonPath('$.price', byRegex('[0-9]+\\.[0-9]{2}'))
    jsonPath('$.email', byRegex('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}'))
}
```

### 4. Version Contracts:
- Keep backward compatibility
- Deprecate old contracts gradually
- Document breaking changes

### 5. CI/CD Integration:
```yaml
# .github/workflows/ci.yml
- name: Verify Producer Contracts
  run: mvn verify -pl product-service

- name: Validate Consumer Contracts
  run: mvn test -pl cart-service,order-service,wishlist-service
```

---

## Troubleshooting

### Issue: "Contract verification failed"
**Solution:** Check that mock data in `ContractVerifierBase` matches contract response

### Issue: "Stubs not found"
**Solution:**
```bash
cd product-service
mvn clean install  # Regenerate and install stubs
```

### Issue: "Port already in use"
**Solution:** Change stub runner port:
```java
@AutoConfigureStubRunner(
    ids = "com.ecommerce:product-service:+:stubs:8082",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    stubsPerConsumer = true
)
```

---

## Future Enhancements

### 1. Add More Services:
- [ ] Order Service contracts
- [ ] User Service contracts
- [ ] Wishlist Service contracts

### 2. Remote Stub Repository:
```xml
<configuration>
    <contractsRepositoryUrl>
        https://nexus.company.com/repository/contracts
    </contractsRepositoryUrl>
</configuration>
```

### 3. Contract Testing in CI:
- Run on every PR
- Block merge if contracts break
- Publish stub artifacts to Nexus/Artifactory

### 4. Messaging Contracts:
```groovy
Contract.make {
    label 'order_created'
    input {
        triggeredBy('createOrder()')
    }
    outputMessage {
        sentTo('order.created')
        body([
            orderId: '123',
            userId: 'user@example.com',
            amount: 99.99
        ])
    }
}
```

---

## Resources

- [Spring Cloud Contract Docs](https://spring.io/projects/spring-cloud-contract)
- [Contract DSL Reference](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#contract-dsl)
- [Consumer-Driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html)

---

**Last Updated:** 2025-10-01
**Next Steps:** Extend contracts to Order Service and Wishlist Service
