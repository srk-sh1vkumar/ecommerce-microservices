# Security and Compliance Strategy

## Overview

This document outlines comprehensive security measures, compliance frameworks, and regulatory adherence strategies for the e-commerce microservices application, ensuring protection against vulnerabilities, data breaches, and regulatory violations.

## Security Architecture

### Defense in Depth Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                        Security Layers                          │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               Perimeter Security                        │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │     WAF     │ │     DDoS    │ │      Rate Limiting   │ │   │
│  │  │ CloudFlare/ │ │ Protection  │ │                     │ │   │
│  │  │   AWS WAF   │ │             │ │                     │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Network Security                           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │   TLS/SSL   │ │  Network    │ │    Service Mesh     │ │   │
│  │  │ Termination │ │ Policies    │ │       (mTLS)        │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            Application Security                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │   JWT Auth  │ │   Input     │ │   RBAC/ABAC         │ │   │
│  │  │             │ │ Validation  │ │                     │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Data Security                              │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │   │
│  │  │ Encryption  │ │ Data Masking│ │   Access Controls   │ │   │
│  │  │ at Rest     │ │             │ │                     │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Authentication and Authorization

### JWT Security Implementation

```java
// Enhanced JWT utility with security best practices
@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:3600}")
    private int jwtExpiration;
    
    @Value("${jwt.refresh.expiration:86400}")
    private int refreshTokenExpiration;
    
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogger auditLogger;
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        claims.put("tokenType", "access");
        claims.put("jti", UUID.randomUUID().toString()); // JWT ID for tracking
        
        return createToken(claims, userDetails.getUsername());
    }
    
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                auditLogger.logSecurityEvent("TOKEN_BLACKLISTED", userDetails.getUsername(), token);
                return false;
            }
            
            final String username = getUsernameFromToken(token);
            final String tokenType = getClaimFromToken(token, claims -> claims.get("tokenType", String.class));
            
            boolean isValid = username.equals(userDetails.getUsername()) 
                && !isTokenExpired(token)
                && "access".equals(tokenType);
                
            if (!isValid) {
                auditLogger.logSecurityEvent("TOKEN_VALIDATION_FAILED", username, token);
            }
            
            return isValid;
            
        } catch (ExpiredJwtException e) {
            auditLogger.logSecurityEvent("TOKEN_EXPIRED", userDetails.getUsername(), token);
            return false;
        } catch (Exception e) {
            auditLogger.logSecurityEvent("TOKEN_INVALID", userDetails.getUsername(), token, e.getMessage());
            return false;
        }
    }
    
    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            String jti = claims.get("jti", String.class);
            Date expiration = claims.getExpiration();
            
            if (jti != null && expiration != null) {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set("blacklist:" + jti, "true", ttl, TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token", e);
        }
    }
    
    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            String jti = claims.get("jti", String.class);
            return jti != null && redisTemplate.hasKey("blacklist:" + jti);
        } catch (Exception e) {
            return true; // Consider invalid tokens as blacklisted
        }
    }
}
```

### Role-Based Access Control (RBAC)

```java
// Security configuration with RBAC
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Higher cost factor for production
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // User endpoints
                .requestMatchers("/api/cart/**").hasRole("USER")
                .requestMatchers("/api/orders/**").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("USER")
                
                // Admin endpoints
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
                .and()
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.ecommerce.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### Multi-Factor Authentication (MFA)

```java
// MFA implementation
@Service
public class MfaService {
    
    private static final int TOKEN_LENGTH = 6;
    private static final int TOKEN_VALIDITY_MINUTES = 5;
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService smsService;
    private final EmailService emailService;
    
    public void sendMfaToken(String userId, String phoneNumber, String email) {
        String token = generateMfaToken();
        String key = "mfa:" + userId;
        
        // Store token with expiration
        redisTemplate.opsForValue().set(key, token, TOKEN_VALIDITY_MINUTES, TimeUnit.MINUTES);
        
        // Send via SMS and email
        smsService.sendSms(phoneNumber, "Your verification code: " + token);
        emailService.sendEmail(email, "Verification Code", 
            "Your verification code is: " + token + ". Valid for " + TOKEN_VALIDITY_MINUTES + " minutes.");
        
        auditLogger.logSecurityEvent("MFA_TOKEN_SENT", userId, "SMS and Email");
    }
    
    public boolean verifyMfaToken(String userId, String providedToken) {
        String key = "mfa:" + userId;
        String storedToken = redisTemplate.opsForValue().get(key);
        
        if (storedToken == null) {
            auditLogger.logSecurityEvent("MFA_TOKEN_EXPIRED", userId, providedToken);
            return false;
        }
        
        boolean isValid = storedToken.equals(providedToken);
        
        if (isValid) {
            redisTemplate.delete(key); // Token can only be used once
            auditLogger.logSecurityEvent("MFA_SUCCESS", userId, providedToken);
        } else {
            auditLogger.logSecurityEvent("MFA_FAILED", userId, providedToken);
        }
        
        return isValid;
    }
    
    private String generateMfaToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(random.nextInt(10));
        }
        return token.toString();
    }
}
```

## Data Protection and Encryption

### Encryption at Rest

```java
// Database field encryption
@Configuration
public class EncryptionConfig {
    
    @Bean
    public AESUtil aesUtil(@Value("${encryption.key}") String encryptionKey) {
        return new AESUtil(encryptionKey);
    }
}

@Component
public class AESUtil {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private final SecretKeySpec secretKey;
    
    public AESUtil(String key) {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }
    
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);
            
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            
            byte[] iv = new byte[12]; // GCM standard IV length
            byteBuffer.get(iv);
            
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] decryptedData = cipher.doFinal(cipherText);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

// Encrypted entity fields
@Entity
public class User {
    
    @Id
    private String id;
    
    private String email;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String socialSecurityNumber;
    
    private String hashedPassword;
    
    // getters and setters
}

@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private AESUtil aesUtil;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? aesUtil.encrypt(attribute) : null;
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? aesUtil.decrypt(dbData) : null;
    }
}
```

### Data Masking and Anonymization

```java
// Data masking for logging and APIs
@Component
public class DataMaskingUtil {
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "**@" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
    
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        
        return "*".repeat(phoneNumber.length() - 4) + 
               phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    public static String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        return "*".repeat(cardNumber.length() - 4) + 
               cardNumber.substring(cardNumber.length() - 4);
    }
}

// JSON serialization with masking
@JsonComponent
public class MaskingSerializer extends JsonSerializer<String> {
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        
        String fieldName = gen.getOutputContext().getCurrentName();
        String maskedValue = value;
        
        if ("email".equals(fieldName)) {
            maskedValue = DataMaskingUtil.maskEmail(value);
        } else if ("phoneNumber".equals(fieldName)) {
            maskedValue = DataMaskingUtil.maskPhoneNumber(value);
        } else if ("creditCardNumber".equals(fieldName)) {
            maskedValue = DataMaskingUtil.maskCreditCard(value);
        }
        
        gen.writeString(maskedValue);
    }
}
```

## Input Validation and Sanitization

### Comprehensive Input Validation

```java
// Custom validation annotations
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSqlInjectionValidator.class)
public @interface NoSqlInjection {
    String message() default "Input contains potential NoSQL injection patterns";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class NoSqlInjectionValidator implements ConstraintValidator<NoSqlInjection, String> {
    
    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile(".*\\$where.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\$regex.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\$ne.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\$in.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\$or.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\$and.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*javascript.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\beval\\b.*", Pattern.CASE_INSENSITIVE)
    };
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(value).matches()) {
                return false;
            }
        }
        
        return true;
    }
}

// Input sanitization
@Component
public class InputSanitizer {
    
    private final Policy htmlPolicy;
    
    public InputSanitizer() {
        this.htmlPolicy = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS);
    }
    
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return htmlPolicy.sanitize(input);
    }
    
    public String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes and control characters
        return input.replaceAll("\\x00", "")
                   .replaceAll("\\p{Cntrl}", "")
                   .trim();
    }
    
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.toLowerCase().trim();
    }
}

// Request validation filter
@Component
@Order(1)
public class RequestValidationFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestValidationFilter.class);
    private final InputSanitizer inputSanitizer;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // Check for suspicious patterns in headers
            if (containsSuspiciousHeaders(httpRequest)) {
                logger.warn("Suspicious headers detected from IP: {}", 
                    httpRequest.getRemoteAddr());
                ((HttpServletResponse) response).setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
            
            // Validate request size
            if (httpRequest.getContentLength() > 10 * 1024 * 1024) { // 10MB limit
                logger.warn("Request size too large from IP: {}", 
                    httpRequest.getRemoteAddr());
                ((HttpServletResponse) response).setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean containsSuspiciousHeaders(HttpServletRequest request) {
        // Check for common attack patterns in headers
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && containsAttackPattern(userAgent)) {
            return true;
        }
        
        String referer = request.getHeader("Referer");
        if (referer != null && containsAttackPattern(referer)) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsAttackPattern(String value) {
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("<script") || 
               lowerValue.contains("javascript:") ||
               lowerValue.contains("vbscript:") ||
               lowerValue.contains("onload=") ||
               lowerValue.contains("onerror=");
    }
}
```

## Security Monitoring and Incident Response

### Security Event Logging

```java
// Security audit logger
@Component
public class SecurityAuditLogger {
    
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private final ObjectMapper objectMapper;
    
    public void logSecurityEvent(String eventType, String userId, String details) {
        logSecurityEvent(eventType, userId, details, null);
    }
    
    public void logSecurityEvent(String eventType, String userId, String details, String additionalInfo) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                .timestamp(Instant.now())
                .eventType(eventType)
                .userId(userId)
                .sourceIp(getCurrentRequestIp())
                .userAgent(getCurrentUserAgent())
                .sessionId(getCurrentSessionId())
                .details(details)
                .additionalInfo(additionalInfo)
                .severity(determineSeverity(eventType))
                .build();
            
            securityLogger.info("SECURITY_EVENT: {}", objectMapper.writeValueAsString(event));
            
            // Send to SIEM if high severity
            if (event.getSeverity() == SecuritySeverity.HIGH || 
                event.getSeverity() == SecuritySeverity.CRITICAL) {
                sendToSiem(event);
            }
            
        } catch (Exception e) {
            securityLogger.error("Failed to log security event", e);
        }
    }
    
    private SecuritySeverity determineSeverity(String eventType) {
        switch (eventType) {
            case "LOGIN_FAILED_MULTIPLE":
            case "TOKEN_BLACKLISTED":
            case "SUSPICIOUS_ACTIVITY":
                return SecuritySeverity.HIGH;
            case "UNAUTHORIZED_ACCESS":
            case "DATA_BREACH_ATTEMPT":
                return SecuritySeverity.CRITICAL;
            case "LOGIN_SUCCESS":
            case "LOGOUT":
                return SecuritySeverity.LOW;
            default:
                return SecuritySeverity.MEDIUM;
        }
    }
}

// Fraud detection service
@Service
public class FraudDetectionService {
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditLogger auditLogger;
    
    public boolean isAccountLocked(String userId) {
        String key = "failed_attempts:" + userId;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null && Integer.parseInt(attempts) >= MAX_FAILED_ATTEMPTS;
    }
    
    public void recordFailedLogin(String userId, String sourceIp) {
        String key = "failed_attempts:" + userId;
        String currentAttempts = redisTemplate.opsForValue().get(key);
        
        int attempts = currentAttempts != null ? Integer.parseInt(currentAttempts) + 1 : 1;
        
        redisTemplate.opsForValue().set(key, String.valueOf(attempts), 
            LOCKOUT_DURATION.toSeconds(), TimeUnit.SECONDS);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            auditLogger.logSecurityEvent("LOGIN_FAILED_MULTIPLE", userId, 
                "Account locked after " + attempts + " failed attempts", sourceIp);
        }
        
        // Check for distributed brute force attack
        checkDistributedBruteForce(sourceIp);
    }
    
    public void recordSuccessfulLogin(String userId) {
        String key = "failed_attempts:" + userId;
        redisTemplate.delete(key);
    }
    
    private void checkDistributedBruteForce(String sourceIp) {
        String key = "ip_failures:" + sourceIp;
        String currentFailures = redisTemplate.opsForValue().get(key);
        
        int failures = currentFailures != null ? Integer.parseInt(currentFailures) + 1 : 1;
        
        redisTemplate.opsForValue().set(key, String.valueOf(failures), 
            Duration.ofHours(1).toSeconds(), TimeUnit.SECONDS);
        
        if (failures > 50) { // Threshold for distributed attack
            auditLogger.logSecurityEvent("DISTRIBUTED_BRUTE_FORCE", "SYSTEM", 
                "IP " + sourceIp + " exceeded failure threshold", String.valueOf(failures));
        }
    }
}
```

## Compliance Framework

### GDPR Compliance

```java
// GDPR data handling service
@Service
public class GdprComplianceService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AuditLogger auditLogger;
    
    public UserDataExport exportUserData(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        UserDataExport export = UserDataExport.builder()
            .personalData(extractPersonalData(user))
            .orderHistory(orderRepository.findByUserEmail(user.getEmail()))
            .loginHistory(getLoginHistory(userId))
            .consentHistory(getConsentHistory(userId))
            .exportDate(Instant.now())
            .build();
        
        auditLogger.logDataEvent("DATA_EXPORT_REQUESTED", userId, "Full user data export");
        
        return export;
    }
    
    public void deleteUserData(String userId, DataDeletionRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Validate deletion request
        if (!request.isConfirmed() || !request.getReasonProvided()) {
            throw new IllegalArgumentException("Deletion request not properly confirmed");
        }
        
        // Check for legal hold
        if (hasLegalHold(userId)) {
            throw new DataDeletionException("Cannot delete data under legal hold");
        }
        
        // Anonymize instead of delete for order history (business requirement)
        anonymizeOrderHistory(user.getEmail());
        
        // Delete personal data
        deletePersonalData(userId);
        
        auditLogger.logDataEvent("DATA_DELETED", userId, 
            "User data deleted per GDPR request. Reason: " + request.getReason());
    }
    
    public ConsentRecord recordConsent(String userId, ConsentType consentType, boolean granted) {
        ConsentRecord record = ConsentRecord.builder()
            .userId(userId)
            .consentType(consentType)
            .granted(granted)
            .timestamp(Instant.now())
            .ipAddress(getCurrentRequestIp())
            .userAgent(getCurrentUserAgent())
            .build();
        
        consentRepository.save(record);
        
        auditLogger.logDataEvent("CONSENT_RECORDED", userId, 
            "Consent " + (granted ? "granted" : "withdrawn") + " for " + consentType);
        
        return record;
    }
    
    private void anonymizeOrderHistory(String userEmail) {
        List<Order> orders = orderRepository.findByUserEmail(userEmail);
        for (Order order : orders) {
            order.setUserEmail("anonymized-user-" + UUID.randomUUID().toString());
            order.setShippingAddress("ANONYMIZED");
            orderRepository.save(order);
        }
    }
}

// Data retention policy
@Component
public class DataRetentionService {
    
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void enforceDataRetention() {
        // Delete old audit logs (7 years retention)
        Instant auditCutoff = Instant.now().minus(7 * 365, ChronoUnit.DAYS);
        auditRepository.deleteByTimestampBefore(auditCutoff);
        
        // Delete old session data (30 days retention)
        Instant sessionCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        sessionRepository.deleteByLastAccessBefore(sessionCutoff);
        
        // Archive old orders (10 years retention, then anonymize)
        Instant orderCutoff = Instant.now().minus(10 * 365, ChronoUnit.DAYS);
        List<Order> oldOrders = orderRepository.findByCreatedAtBefore(orderCutoff);
        for (Order order : oldOrders) {
            archiveAndAnonymizeOrder(order);
        }
    }
}
```

### PCI DSS Compliance

```java
// PCI DSS compliant payment handling
@Service
public class PaymentSecurityService {
    
    private final PaymentTokenizer tokenizer;
    private final EncryptionService encryptionService;
    
    public TokenizedPayment tokenizePaymentMethod(PaymentMethod paymentMethod) {
        // Never store actual credit card numbers
        String token = tokenizer.tokenize(paymentMethod.getCardNumber());
        
        // Store only last 4 digits for display
        String maskedNumber = "**** **** **** " + 
            paymentMethod.getCardNumber().substring(paymentMethod.getCardNumber().length() - 4);
        
        // Encrypt and store other necessary data
        String encryptedExpiry = encryptionService.encrypt(paymentMethod.getExpiryDate());
        
        TokenizedPayment tokenized = TokenizedPayment.builder()
            .token(token)
            .maskedNumber(maskedNumber)
            .encryptedExpiry(encryptedExpiry)
            .cardType(determineCardType(paymentMethod.getCardNumber()))
            .build();
        
        auditLogger.logSecurityEvent("PAYMENT_TOKENIZED", getCurrentUserId(), token);
        
        return tokenized;
    }
    
    public void processPayment(String paymentToken, BigDecimal amount) {
        // Detokenize for payment processing
        String actualCardNumber = tokenizer.detokenize(paymentToken);
        
        try {
            // Process payment with external provider
            PaymentResult result = paymentGateway.processPayment(actualCardNumber, amount);
            
            auditLogger.logSecurityEvent("PAYMENT_PROCESSED", getCurrentUserId(), 
                "Amount: " + amount + ", Status: " + result.getStatus());
            
        } finally {
            // Ensure card number is not kept in memory
            actualCardNumber = null;
            System.gc(); // Suggest garbage collection
        }
    }
}
```

### SOX Compliance

```java
// Financial controls for SOX compliance
@Service
public class FinancialControlsService {
    
    @Transactional
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    public void processRefund(String orderId, BigDecimal amount, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        // Four-eyes principle: require approval for large refunds
        if (amount.compareTo(BigDecimal.valueOf(1000)) > 0) {
            requireApproval(orderId, amount, reason);
        }
        
        // Create audit trail
        FinancialTransaction transaction = FinancialTransaction.builder()
            .orderId(orderId)
            .type(TransactionType.REFUND)
            .amount(amount)
            .reason(reason)
            .processedBy(getCurrentUserId())
            .timestamp(Instant.now())
            .approvalRequired(amount.compareTo(BigDecimal.valueOf(1000)) > 0)
            .build();
        
        financialTransactionRepository.save(transaction);
        
        auditLogger.logFinancialEvent("REFUND_PROCESSED", getCurrentUserId(), 
            "Order: " + orderId + ", Amount: " + amount + ", Reason: " + reason);
    }
    
    @Scheduled(cron = "0 0 1 1 * ?") // Monthly on 1st day
    public void generateFinancialReport() {
        Instant startOfMonth = Instant.now().with(TemporalAdjusters.firstDayOfMonth())
            .truncatedTo(ChronoUnit.DAYS);
        Instant endOfMonth = Instant.now().with(TemporalAdjusters.lastDayOfMonth())
            .truncatedTo(ChronoUnit.DAYS);
        
        FinancialReport report = FinancialReport.builder()
            .reportPeriod(startOfMonth + " to " + endOfMonth)
            .totalRevenue(calculateTotalRevenue(startOfMonth, endOfMonth))
            .totalRefunds(calculateTotalRefunds(startOfMonth, endOfMonth))
            .totalFees(calculateTotalFees(startOfMonth, endOfMonth))
            .generatedBy("SYSTEM")
            .generatedAt(Instant.now())
            .build();
        
        financialReportRepository.save(report);
        
        auditLogger.logFinancialEvent("MONTHLY_REPORT_GENERATED", "SYSTEM", 
            "Period: " + report.getReportPeriod());
    }
}
```

## Security Testing and Vulnerability Management

### Automated Security Scanning

```yaml
# Security scanning in CI/CD pipeline
name: Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Run OWASP Dependency Check
      uses: dependency-check/Dependency-Check_Action@main
      with:
        project: 'ecommerce-microservices'
        path: '.'
        format: 'ALL'
        
    - name: Upload dependency check results
      uses: actions/upload-artifact@v3
      with:
        name: dependency-check-report
        path: reports/
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'
    
    - name: Run Snyk security scan
      uses: snyk/actions/maven@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --severity-threshold=high
    
    - name: SonarQube Security Scan
      uses: sonarqube-quality-gate-action@master
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

This comprehensive security and compliance strategy ensures the e-commerce application meets industry standards and regulatory requirements while protecting against security threats.