# Contributing to E-commerce Microservices Platform

Thank you for your interest in contributing to the E-commerce Microservices Platform! This document provides guidelines and instructions for contributing to this project.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Documentation](#documentation)
- [Getting Help](#getting-help)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors. We expect everyone to:

- Be respectful and professional
- Accept constructive criticism gracefully
- Focus on what is best for the project and community
- Show empathy towards other contributors

### Unacceptable Behavior

- Harassment, discrimination, or offensive comments
- Trolling, insulting, or derogatory remarks
- Publishing others' private information
- Any conduct that would be inappropriate in a professional setting

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/downloads)

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/ecommerce-microservices.git
   cd ecommerce-microservices
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/srk-sh1vkumar/ecommerce-microservices.git
   ```

### Initial Setup

1. **Build all services**:
   ```bash
   ./build-all.sh
   ```

2. **Create `.env` file** (NEVER commit this!):
   ```bash
   # MongoDB Configuration
   MONGO_ROOT_PASSWORD=your_secure_password_here

   # AppDynamics Configuration (optional)
   APPDYNAMICS_CONTROLLER_HOST_NAME=your_controller.saas.appdynamics.com
   APPDYNAMICS_AGENT_ACCOUNT_NAME=your_account_name
   APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=your_access_key
   ```

3. **Start the complete stack**:
   ```bash
   docker-compose up -d
   ```

4. **Verify services are running**:
   ```bash
   docker-compose ps
   ```

See [Project Setup Guide](docs/development/PROJECT_SETUP_GUIDE.md) for detailed instructions.

---

## Development Workflow

### Branch Strategy

We follow a **feature branch workflow**:

1. **Main Branch** (`main`) - Production-ready code
2. **Feature Branches** - All development work
   - `feature/feature-name` - New features
   - `fix/bug-description` - Bug fixes
   - `chore/task-description` - Maintenance tasks
   - `docs/documentation-update` - Documentation changes

### Creating a Feature Branch

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create and switch to a feature branch
git checkout -b feature/your-feature-name
```

### Branch Naming Conventions

- Use lowercase and hyphens
- Be descriptive but concise
- Include type prefix

**Examples**:
```
feature/user-registration
feature/redis-caching
fix/jwt-token-expiration
fix/mongodb-connection-leak
chore/update-dependencies
chore/refactor-service-layer
docs/api-documentation
docs/update-readme
```

---

## Coding Standards

### Java Code Style

We follow standard Java conventions with some project-specific rules:

#### General Rules

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Encoding**: UTF-8
- **Naming**:
  - Classes: `PascalCase` (e.g., `UserService`, `ProductController`)
  - Methods/Variables: `camelCase` (e.g., `getUserById`, `productList`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT`)
  - Packages: lowercase (e.g., `com.ecommerce.user.service`)

#### Spring Boot Best Practices

1. **Controller Layer**:
   ```java
   @RestController
   @RequestMapping("/api/users")
   public class UserController {

       private final UserService userService;

       // Constructor injection (preferred over @Autowired)
       public UserController(UserService userService) {
           this.userService = userService;
       }

       @GetMapping("/{id}")
       public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
           return ResponseEntity.ok(userService.getUserById(id));
       }
   }
   ```

2. **Service Layer**:
   ```java
   @Service
   public class UserService {

       private final UserRepository userRepository;

       public UserService(UserRepository userRepository) {
           this.userRepository = userRepository;
       }

       public UserDTO getUserById(Long id) {
           return userRepository.findById(id)
               .map(this::convertToDTO)
               .orElseThrow(() -> new UserNotFoundException(id));
       }
   }
   ```

3. **Exception Handling**:
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {

       @ExceptionHandler(UserNotFoundException.class)
       public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
           ErrorResponse error = new ErrorResponse(
               HttpStatus.NOT_FOUND.value(),
               ex.getMessage(),
               LocalDateTime.now()
           );
           return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
       }
   }
   ```

#### Code Quality

- **No unused imports** - Clean up imports regularly
- **No magic numbers** - Use named constants
- **Proper exception handling** - Don't swallow exceptions
- **Logging** - Use SLF4J with appropriate levels:
  ```java
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  log.debug("Fetching user with ID: {}", id);
  log.info("User created successfully: {}", user.getEmail());
  log.warn("Retry attempt {} for user: {}", retryCount, userId);
  log.error("Failed to process order: {}", orderId, exception);
  ```

### Configuration Files

- **application.yml** - Use YAML format (preferred over .properties)
- **Environment Variables** - Use `${ENV_VAR:default}` syntax
- **Profiles** - Separate configs for dev, test, prod

Example:
```yaml
spring:
  application:
    name: user-service
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/ecommerce}

server:
  port: ${USER_SERVICE_PORT:8082}

logging:
  level:
    com.ecommerce.user: ${LOG_LEVEL:INFO}
```

---

## Testing Requirements

All contributions must include appropriate tests. We follow a multi-layered testing strategy.

### Test Coverage Requirements

- **Minimum Coverage**: 70% (enforced by CI)
- **Target Coverage**: 80%+
- **Critical Paths**: 100% coverage for security and payment logic

### Test Types

#### 1. Unit Tests

Test individual components in isolation using mocks.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        User user = new User(userId, "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(UserNotFoundException.class,
            () -> userService.getUserById(userId));
    }
}
```

#### 2. Integration Tests

Test component interactions with real dependencies.

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        String userJson = """
            {
                "email": "test@example.com",
                "password": "SecurePass123!",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.firstName").value("John"));
    }
}
```

### Running Tests

```bash
# Run all tests for a service
cd user-service
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#getUserById_ShouldReturnUser_WhenUserExists
```

### Test Best Practices

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **One assertion per test** (when practical)
3. **Use descriptive test names**: `methodName_ShouldExpectedBehavior_WhenCondition`
4. **Clean up test data** in `@BeforeEach` or `@AfterEach`
5. **Don't test framework code** - Focus on your business logic
6. **Use test fixtures** for common test data
7. **Mock external dependencies** - No real database/API calls in unit tests

---

## Commit Guidelines

We follow the **Conventional Commits** specification for clear and structured commit history.

### Commit Message Format

```
<type>(optional scope): <short summary>

[optional body]

[optional footer]
```

### Commit Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, no logic change)
- **refactor**: Code refactoring (no feature change or bug fix)
- **test**: Adding or updating tests
- **chore**: Maintenance tasks, dependency updates

### Commit Scope

The scope specifies which service or component is affected:

- `user` - User service
- `product` - Product service
- `cart` - Cart service
- `order` - Order service
- `gateway` - API Gateway
- `common` - Common library
- `docs` - Documentation
- `ci` - CI/CD changes

### Commit Examples

**Good commits**:
```bash
feat(user): add email verification for new registrations

fix(product): resolve race condition in inventory updates

docs(api): update authentication endpoints documentation

refactor(order): simplify checkout workflow logic

test(cart): add integration tests for cart operations

chore(deps): upgrade Spring Boot to 3.2.1
```

**Bad commits** (avoid these):
```bash
# Too vague
fix: bug fix

# No type prefix
Added new feature

# Not descriptive
WIP

# Multiple changes in one commit
feat: add user service, update docs, fix bug
```

### Commit Body Guidelines

Include a body when:
- The change is complex
- Context is needed
- Breaking changes are introduced

Example:
```
feat(auth): implement JWT refresh token mechanism

Add refresh token support to extend user sessions without requiring
re-authentication. Tokens expire after 7 days and can be used to obtain
a new access token.

BREAKING CHANGE: Authentication endpoint now returns both access_token
and refresh_token. Clients must update to handle both tokens.

Closes #123
```

### Signing Commits

We encourage (but don't require) signing commits with GPG:

```bash
# Configure Git to sign commits
git config --global user.signingkey YOUR_GPG_KEY_ID
git config --global commit.gpgsign true

# Sign individual commit
git commit -S -m "feat(user): add new feature"
```

---

## Pull Request Process

### Before Creating a PR

1. **Ensure all tests pass**:
   ```bash
   mvn clean test
   ```

2. **Check code coverage**:
   ```bash
   mvn clean test jacoco:report
   ```

3. **Format your code**:
   ```bash
   # Auto-format (if formatter is configured)
   mvn spring-javaformat:apply
   ```

4. **Update documentation** if needed

5. **Rebase on latest main**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

### Creating a Pull Request

1. **Push your branch**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Open PR on GitHub**

3. **Fill out PR template** with:
   - Clear description of changes
   - Related issue numbers (e.g., "Closes #123")
   - Testing performed
   - Screenshots (if UI changes)

### PR Title Format

Follow the same convention as commit messages:

```
feat(user): add email verification feature
fix(product): resolve inventory race condition
docs(api): update Swagger documentation
```

### PR Description Template

```markdown
## Summary
Brief description of what this PR does.

## Changes
- List of specific changes made
- Another change
- Yet another change

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manually tested locally
- [ ] All existing tests pass

## Related Issues
Closes #123
Relates to #456

## Screenshots (if applicable)
[Add screenshots here]

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Tests added/updated with good coverage
- [ ] All CI checks pass
```

### PR Review Process

1. **Automated Checks** - CI runs automatically:
   - Build verification
   - Unit tests
   - Integration tests
   - Code coverage (min 70%)
   - Code quality analysis
   - Security scans
   - License compliance

2. **Code Review**:
   - At least 1 approval required
   - Address all review comments
   - Push additional commits (don't force-push during review)

3. **Merge**:
   - Squash and merge (preferred for feature branches)
   - Ensure commit message follows conventions
   - Delete feature branch after merge

### Review Guidelines

When reviewing code, focus on:

- **Correctness**: Does the code do what it's supposed to?
- **Testing**: Are there adequate tests?
- **Readability**: Is the code clear and maintainable?
- **Performance**: Are there any performance concerns?
- **Security**: Are there any security vulnerabilities?
- **Documentation**: Is the code and API properly documented?

### Addressing Review Comments

```bash
# Make requested changes
git add .
git commit -m "refactor(user): address review comments"

# Push updates
git push origin feature/your-feature-name
```

---

## Documentation

### When to Update Documentation

Update documentation when you:

- Add new features or APIs
- Change existing functionality
- Fix bugs that affect documented behavior
- Add configuration options
- Modify deployment procedures

### Documentation Types

1. **Code Documentation** (Javadoc):
   ```java
   /**
    * Retrieves a user by their unique identifier.
    *
    * @param id the unique identifier of the user
    * @return UserDTO containing user information
    * @throws UserNotFoundException if user with given ID doesn't exist
    */
   public UserDTO getUserById(Long id) {
       // Implementation
   }
   ```

2. **API Documentation** (Swagger/OpenAPI):
   ```java
   @Operation(summary = "Get user by ID", description = "Retrieves user details by unique identifier")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "User found"),
       @ApiResponse(responseCode = "404", description = "User not found")
   })
   @GetMapping("/{id}")
   public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
       // Implementation
   }
   ```

3. **README Updates** - Update README.md if:
   - New services added
   - Setup process changes
   - New dependencies required

4. **Architecture Decision Records** (ADRs):
   - Document significant architectural decisions
   - Use template in `docs/architecture/adr/000-adr-template.md`
   - See [ADR Guide](docs/architecture/adr/README.md)

### Documentation Style

- Write in clear, concise English
- Use present tense ("Returns user" not "Will return user")
- Include code examples where helpful
- Keep documentation up-to-date with code

---

## Getting Help

### Resources

- **Documentation**: Check [docs/](docs/) directory
- **Architecture**: See [Architecture Overview](docs/architecture/ARCHITECTURE.md)
- **ADRs**: Review [Architecture Decision Records](docs/architecture/adr/README.md)
- **Troubleshooting**: See [Troubleshooting Guide](docs/guides/TROUBLESHOOTING_GUIDE.md)
- **API Docs**: See [API Documentation Guide](docs/api/API_DOCUMENTATION_GUIDE.md)

### Communication Channels

- **GitHub Issues**: For bug reports and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Pull Request Comments**: For code-specific questions

### Reporting Bugs

Use the bug report template and include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Exact steps to reproduce the issue
3. **Expected Behavior**: What should happen
4. **Actual Behavior**: What actually happens
5. **Environment**: OS, Java version, Docker version, etc.
6. **Logs**: Relevant error logs or stack traces
7. **Screenshots**: If applicable

### Requesting Features

Use the feature request template and include:

1. **Problem**: What problem does this solve?
2. **Proposed Solution**: Your suggested implementation
3. **Alternatives**: Other solutions considered
4. **Use Case**: Example scenario where this is useful
5. **Priority**: Low/Medium/High (from your perspective)

---

## Recognition

Contributors will be recognized in:

- **README.md** - Contributors section
- **Release Notes** - Feature/fix credits
- **GitHub Insights** - Automatic contributor tracking

---

## License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project (see [LICENSE](LICENSE) file).

---

## Questions?

If you have questions not covered in this guide:

1. Check existing documentation
2. Search closed issues
3. Ask in GitHub Discussions
4. Create a new issue with the "question" label

Thank you for contributing to the E-commerce Microservices Platform!
