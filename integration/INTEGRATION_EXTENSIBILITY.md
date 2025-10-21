# Integration and Extensibility Guide

## Overview

This document provides comprehensive guidance for integrating the e-commerce microservices application with external systems and extending functionality for evolving business requirements.

## API Integration Patterns

### RESTful API Design

#### API Versioning Strategy

```java
// Version-aware controller design
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {
    
    @GetMapping
    public ResponseEntity<PagedResponse<ProductDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        // V1 implementation
    }
}

@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {
    
    @GetMapping
    public ResponseEntity<PagedResponse<ProductDTOV2>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags) {
        // V2 implementation with additional features
    }
}

// API versioning configuration
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(true)
            .parameterName("version")
            .favorPathExtension(false)
            .favorHeader(true)
            .headerName("API-Version")
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("v1", MediaType.APPLICATION_JSON)
            .mediaType("v2", MediaType.APPLICATION_JSON);
    }
}
```

#### API Documentation with OpenAPI 3.0

```java
// OpenAPI configuration
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "E-commerce Microservices API",
        version = "2.0.0",
        description = "Comprehensive e-commerce platform APIs",
        contact = @Contact(
            name = "API Support",
            url = "https://ecommerce.example.com/support",
            email = "api-support@ecommerce.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "https://api.ecommerce.com", description = "Production"),
        @Server(url = "https://staging-api.ecommerce.com", description = "Staging"),
        @Server(url = "http://localhost:8080", description = "Development")
    }
)
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSchemas("ErrorResponse", 
                    new Schema<ErrorResponse>()
                        .type("object")
                        .addProperties("timestamp", new DateTimeSchema())
                        .addProperties("status", new IntegerSchema())
                        .addProperties("error", new StringSchema())
                        .addProperties("message", new StringSchema())
                        .addProperties("path", new StringSchema())))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}

// Enhanced API documentation
@RestController
@Tag(name = "Product Management", description = "Product catalog operations")
public class ProductController {
    
    @Operation(
        summary = "Get products with filtering",
        description = "Retrieve paginated list of products with optional filtering by category, price range, and search terms",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Products retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PagedResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
                )
            )
        }
    )
    @GetMapping("/products")
    public ResponseEntity<PagedResponse<ProductDTO>> getProducts(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Product category filter", example = "Electronics")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Minimum price filter", example = "10.00")
            @RequestParam(required = false) BigDecimal minPrice,
            
            @Parameter(description = "Maximum price filter", example = "1000.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            
            @Parameter(description = "Search term for product name or description")
            @RequestParam(required = false) String search) {
        
        return ResponseEntity.ok(productService.getProducts(
            page, size, category, minPrice, maxPrice, search));
    }
}
```

### Event-Driven Integration

#### Domain Events with Spring Events

```java
// Domain event definitions
public abstract class DomainEvent {
    private final String eventId;
    private final Instant timestamp;
    private final String aggregateId;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.aggregateId = aggregateId;
    }
    
    // getters
}

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends DomainEvent {
    private final String userEmail;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final String shippingAddress;
    
    public OrderCreatedEvent(String orderId, String userEmail, 
                           List<OrderItem> items, BigDecimal totalAmount, 
                           String shippingAddress) {
        super(orderId);
        this.userEmail = userEmail;
        this.items = items;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends DomainEvent {
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String registrationSource;
    
    public UserRegisteredEvent(String userId, String email, String firstName, 
                             String lastName, String registrationSource) {
        super(userId);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registrationSource = registrationSource;
    }
}

// Event publishing service
@Service
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    private final EventOutboxService eventOutboxService;
    
    @Transactional
    public void publishEvent(DomainEvent event) {
        // Store in outbox for reliability
        eventOutboxService.saveEvent(event);
        
        // Publish locally
        eventPublisher.publishEvent(event);
    }
}

// Event listeners
@Component
public class OrderEventListener {
    
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Update inventory
        inventoryService.reserveItems(event.getItems());
        
        // Send confirmation email
        emailService.sendOrderConfirmation(event.getUserEmail(), event.getAggregateId());
        
        // Send notification
        notificationService.notifyOrderCreated(event);
    }
    
    @EventListener
    @Async
    public void handleUserRegistered(UserRegisteredEvent event) {
        // Send welcome email
        emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName());
        
        // Track registration analytics
        analyticsService.trackUserRegistration(event);
        
        // Initialize user preferences
        userPreferencesService.initializeDefaults(event.getAggregateId());
    }
}
```

#### Message Queue Integration

```java
// RabbitMQ configuration
@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String INVENTORY_UPDATE_QUEUE = "inventory.update.queue";
    public static final String EMAIL_NOTIFICATION_QUEUE = "email.notification.queue";
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
            .build();
    }
    
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
            .bind(orderCreatedQueue())
            .to(orderExchange())
            .with("order.created.*");
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMandatory(true);
        template.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                if (!ack) {
                    log.error("Message not delivered: {}", cause);
                }
            }
        });
        return template;
    }
}

// Message publishing
@Service
public class MessagePublishingService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        String routingKey = "order.created." + event.getUserEmail();
        
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(event.getEventId());
        properties.setTimestamp(Date.from(event.getTimestamp()));
        properties.setCorrelationId(event.getAggregateId());
        
        Message message = new Message(
            JsonUtils.toJson(event).getBytes(StandardCharsets.UTF_8),
            properties
        );
        
        rabbitTemplate.send(RabbitMQConfig.ORDER_EXCHANGE, routingKey, message);
    }
}

// Message consuming
@RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
@Component
public class OrderMessageConsumer {
    
    private final OrderFulfillmentService fulfillmentService;
    
    @RabbitHandler
    public void handleOrderCreated(@Payload OrderCreatedEvent event,
                                  @Header Map<String, Object> headers) {
        try {
            fulfillmentService.processOrder(event);
        } catch (Exception e) {
            log.error("Failed to process order: {}", event.getAggregateId(), e);
            throw new AmqpRejectAndDontRequeueException("Processing failed", e);
        }
    }
}
```

## External System Integration

### Payment Gateway Integration

```java
// Payment gateway abstraction
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
    RefundResult processRefund(RefundRequest request);
    PaymentStatus getPaymentStatus(String transactionId);
}

// Stripe implementation
@Service
@ConditionalOnProperty(name = "payment.gateway", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {
    
    private final Stripe stripeClient;
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency("usd")
                .setPaymentMethod(request.getPaymentMethodId())
                .setConfirm(true)
                .setReturnUrl(request.getReturnUrl())
                .build();
                
            PaymentIntent intent = PaymentIntent.create(params);
            
            return PaymentResult.builder()
                .transactionId(intent.getId())
                .status(mapStripeStatus(intent.getStatus()))
                .amount(request.getAmount())
                .gatewayResponse(intent.toJson())
                .build();
                
        } catch (StripeException e) {
            return PaymentResult.builder()
                .status(PaymentStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}

// PayPal implementation
@Service
@ConditionalOnProperty(name = "payment.gateway", havingValue = "paypal")
public class PayPalPaymentGateway implements PaymentGateway {
    
    private final PayPalHttpClient payPalClient;
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        purchaseUnits.add(new PurchaseUnitRequest()
            .amountWithBreakdown(new AmountWithBreakdown()
                .currencyCode("USD")
                .value(request.getAmount().toString())));
        orderRequest.purchaseUnits(purchaseUnits);
        
        OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest();
        ordersCreateRequest.requestBody(orderRequest);
        
        try {
            HttpResponse<Order> response = payPalClient.execute(ordersCreateRequest);
            Order order = response.result();
            
            return PaymentResult.builder()
                .transactionId(order.id())
                .status(mapPayPalStatus(order.status()))
                .amount(request.getAmount())
                .gatewayResponse(order.toString())
                .build();
                
        } catch (IOException e) {
            return PaymentResult.builder()
                .status(PaymentStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}

// Payment service with strategy pattern
@Service
public class PaymentService {
    
    private final Map<String, PaymentGateway> paymentGateways;
    
    public PaymentService(List<PaymentGateway> gateways) {
        this.paymentGateways = gateways.stream()
            .collect(Collectors.toMap(
                gateway -> gateway.getClass().getSimpleName().toLowerCase(),
                Function.identity()
            ));
    }
    
    public PaymentResult processPayment(PaymentRequest request) {
        PaymentGateway gateway = paymentGateways.get(request.getGateway().toLowerCase());
        if (gateway == null) {
            throw new UnsupportedPaymentGatewayException("Gateway not supported: " + request.getGateway());
        }
        
        return gateway.processPayment(request);
    }
}
```

### Third-Party Service Integration

```java
// Shipping provider integration
public interface ShippingProvider {
    ShippingRate calculateRate(ShippingRateRequest request);
    ShippingLabel createLabel(ShippingLabelRequest request);
    TrackingInfo getTrackingInfo(String trackingNumber);
}

@Service
public class FedExShippingProvider implements ShippingProvider {
    
    private final FedExApiClient fedexClient;
    
    @Override
    public ShippingRate calculateRate(ShippingRateRequest request) {
        // FedEx API implementation
        FedExRateRequest fedexRequest = FedExRateRequest.builder()
            .origin(request.getOrigin())
            .destination(request.getDestination())
            .weight(request.getWeight())
            .dimensions(request.getDimensions())
            .serviceType(request.getServiceType())
            .build();
            
        FedExRateResponse response = fedexClient.getRates(fedexRequest);
        
        return ShippingRate.builder()
            .provider("FedEx")
            .serviceType(response.getServiceType())
            .cost(response.getTotalCost())
            .estimatedDelivery(response.getEstimatedDelivery())
            .build();
    }
}

// Email service integration
@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    public void sendOrderConfirmation(String to, String orderId) {
        Context context = new Context();
        context.setVariable("orderId", orderId);
        context.setVariable("customerName", getCustomerName(to));
        
        String htmlContent = templateEngine.process("order-confirmation", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Order Confirmation - " + orderId);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email", e);
        }
    }
}

// SMS service integration
@Service
public class SmsService {
    
    private final TwilioClient twilioClient;
    
    public void sendOrderUpdate(String phoneNumber, String message) {
        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioProperties.getFromNumber()),
                message
            ).create(twilioClient);
            
            log.info("SMS sent successfully: {}", twilioMessage.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phoneNumber, e);
        }
    }
}
```

## Plugin Architecture

### Plugin Framework

```java
// Plugin interface
public interface EcommercePlugin {
    String getName();
    String getVersion();
    String getDescription();
    PluginType getType();
    void initialize(PluginContext context);
    void shutdown();
}

// Plugin types
public enum PluginType {
    PAYMENT_PROCESSOR,
    SHIPPING_PROVIDER,
    TAX_CALCULATOR,
    INVENTORY_SYNC,
    ANALYTICS,
    NOTIFICATION,
    PROMOTION_ENGINE
}

// Plugin context
public class PluginContext {
    private final ApplicationContext applicationContext;
    private final Map<String, Object> configuration;
    private final EventPublisher eventPublisher;
    
    // Plugin access to core services
    public <T> T getService(Class<T> serviceClass) {
        return applicationContext.getBean(serviceClass);
    }
    
    public String getConfigValue(String key) {
        return (String) configuration.get(key);
    }
    
    public void publishEvent(Object event) {
        eventPublisher.publishEvent(event);
    }
}

// Plugin manager
@Service
public class PluginManager {
    
    private final Map<String, EcommercePlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final PluginLoader pluginLoader;
    private final PluginConfiguration pluginConfiguration;
    
    @PostConstruct
    public void loadPlugins() {
        List<PluginDefinition> definitions = pluginConfiguration.getPluginDefinitions();
        
        for (PluginDefinition definition : definitions) {
            if (definition.isEnabled()) {
                try {
                    EcommercePlugin plugin = pluginLoader.loadPlugin(definition);
                    PluginContext context = createPluginContext(definition);
                    plugin.initialize(context);
                    loadedPlugins.put(plugin.getName(), plugin);
                    log.info("Loaded plugin: {} v{}", plugin.getName(), plugin.getVersion());
                } catch (Exception e) {
                    log.error("Failed to load plugin: {}", definition.getName(), e);
                }
            }
        }
    }
    
    public <T extends EcommercePlugin> List<T> getPluginsByType(PluginType type) {
        return loadedPlugins.values().stream()
            .filter(plugin -> plugin.getType() == type)
            .map(plugin -> (T) plugin)
            .collect(Collectors.toList());
    }
}

// Example tax calculation plugin
@Component
public class TaxJarTaxPlugin implements EcommercePlugin {
    
    private TaxJarClient taxJarClient;
    private PluginContext context;
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        String apiKey = context.getConfigValue("taxjar.api.key");
        this.taxJarClient = new TaxJarClient(apiKey);
    }
    
    @Override
    public String getName() {
        return "TaxJar Tax Calculator";
    }
    
    @Override
    public PluginType getType() {
        return PluginType.TAX_CALCULATOR;
    }
    
    public TaxCalculation calculateTax(TaxRequest request) {
        try {
            TaxJarRequest taxJarRequest = TaxJarRequest.builder()
                .amount(request.getSubtotal())
                .shipping(request.getShipping())
                .toCountry(request.getToAddress().getCountry())
                .toState(request.getToAddress().getState())
                .toZip(request.getToAddress().getZipCode())
                .fromCountry(request.getFromAddress().getCountry())
                .fromState(request.getFromAddress().getState())
                .fromZip(request.getFromAddress().getZipCode())
                .build();
                
            TaxJarResponse response = taxJarClient.calculateTax(taxJarRequest);
            
            return TaxCalculation.builder()
                .taxAmount(response.getTaxAmount())
                .taxRate(response.getTaxRate())
                .jurisdiction(response.getJurisdiction())
                .breakdown(response.getBreakdown())
                .build();
                
        } catch (Exception e) {
            log.error("Tax calculation failed", e);
            throw new TaxCalculationException("TaxJar calculation failed", e);
        }
    }
}
```

## Data Integration Patterns

### ETL/ELT Pipelines

```java
// Data pipeline framework
@Component
public class DataPipelineManager {
    
    private final Map<String, DataPipeline> pipelines;
    private final TaskScheduler taskScheduler;
    
    @PostConstruct
    public void initializePipelines() {
        // Schedule regular data synchronization
        pipelines.values().forEach(pipeline -> {
            if (pipeline.getSchedule() != null) {
                taskScheduler.scheduleWithFixedDelay(
                    pipeline::execute,
                    Duration.parse(pipeline.getSchedule())
                );
            }
        });
    }
}

// Customer data sync pipeline
@Component
public class CustomerDataSyncPipeline implements DataPipeline {
    
    private final CustomerRepository customerRepository;
    private final CrmClient crmClient;
    private final DataTransformationService transformationService;
    
    @Override
    public void execute() {
        try {
            // Extract
            List<Customer> customers = customerRepository.findModifiedSince(getLastSyncTime());
            
            // Transform
            List<CrmContact> crmContacts = customers.stream()
                .map(transformationService::transformCustomerToCrmContact)
                .collect(Collectors.toList());
            
            // Load
            crmClient.syncContacts(crmContacts);
            
            updateLastSyncTime(Instant.now());
            
        } catch (Exception e) {
            log.error("Customer data sync failed", e);
            notificationService.alertDataSyncFailure("Customer", e.getMessage());
        }
    }
    
    @Override
    public String getSchedule() {
        return "PT1H"; // Every hour
    }
}

// Analytics data pipeline
@Component
public class AnalyticsDataPipeline implements DataPipeline {
    
    private final OrderRepository orderRepository;
    private final AnalyticsService analyticsService;
    
    @Override
    public void execute() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Daily sales metrics
        DailySalesMetrics metrics = calculateDailySalesMetrics(yesterday);
        analyticsService.recordDailySales(metrics);
        
        // Customer behavior analytics
        List<CustomerBehaviorEvent> events = extractCustomerBehaviorEvents(yesterday);
        analyticsService.recordCustomerBehavior(events);
        
        // Product performance metrics
        ProductPerformanceMetrics productMetrics = calculateProductPerformance(yesterday);
        analyticsService.recordProductPerformance(productMetrics);
    }
    
    @Override
    public String getSchedule() {
        return "0 2 * * *"; // Daily at 2 AM
    }
}
```

### Webhook Integration

```java
// Webhook framework
@RestController
@RequestMapping("/webhooks")
public class WebhookController {
    
    private final WebhookProcessor webhookProcessor;
    private final WebhookValidator webhookValidator;
    
    @PostMapping("/{provider}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request) {
        
        try {
            // Validate webhook signature
            if (!webhookValidator.validateSignature(provider, payload, headers)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
            }
            
            // Process webhook
            WebhookEvent event = WebhookEvent.builder()
                .provider(provider)
                .payload(payload)
                .headers(headers)
                .timestamp(Instant.now())
                .sourceIp(getClientIpAddress(request))
                .build();
                
            webhookProcessor.processWebhook(event);
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Webhook processing failed for provider: {}", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Webhook processing failed");
        }
    }
}

// Stripe webhook handler
@Component
public class StripeWebhookHandler implements WebhookHandler {
    
    @Override
    public String getProvider() {
        return "stripe";
    }
    
    @Override
    public void handleWebhook(WebhookEvent event) {
        Event stripeEvent = Event.GSON.fromJson(event.getPayload(), Event.class);
        
        switch (stripeEvent.getType()) {
            case "payment_intent.succeeded":
                handlePaymentSuccess(stripeEvent);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailure(stripeEvent);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePayment(stripeEvent);
                break;
            default:
                log.info("Unhandled Stripe event type: {}", stripeEvent.getType());
        }
    }
}
```

This comprehensive integration and extensibility guide provides the foundation for connecting the e-commerce platform with external systems and extending functionality through plugins and custom integrations.