# Accessibility and Internationalization Strategy

## Overview

This document outlines comprehensive strategies for making the e-commerce platform accessible to users with disabilities and supporting multiple languages, currencies, and regional preferences for global markets.

## Web Accessibility Implementation

### WCAG 2.1 AA Compliance

#### Frontend Accessibility Framework

```typescript
// React accessibility components
import React from 'react';
import { useTranslation } from 'react-i18next';
import { useA11y } from '@/hooks/useA11y';

// Accessible button component
interface AccessibleButtonProps {
  children: React.ReactNode;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
  ariaLabel?: string;
  ariaDescribedBy?: string;
  type?: 'button' | 'submit' | 'reset';
}

export const AccessibleButton: React.FC<AccessibleButtonProps> = ({
  children,
  onClick,
  variant = 'primary',
  disabled = false,
  ariaLabel,
  ariaDescribedBy,
  type = 'button'
}) => {
  const { t } = useTranslation();
  const { announceToScreenReader } = useA11y();
  
  const handleClick = () => {
    if (!disabled) {
      onClick();
      // Announce action to screen readers
      announceToScreenReader(t('actions.buttonPressed', { action: ariaLabel || children }));
    }
  };
  
  return (
    <button
      type={type}
      className={`btn btn-${variant} ${disabled ? 'btn-disabled' : ''}`}
      onClick={handleClick}
      disabled={disabled}
      aria-label={ariaLabel}
      aria-describedby={ariaDescribedBy}
      role="button"
      tabIndex={disabled ? -1 : 0}
    >
      {children}
    </button>
  );
};

// Accessible form input component
interface AccessibleInputProps {
  id: string;
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  required?: boolean;
  placeholder?: string;
  description?: string;
}

export const AccessibleInput: React.FC<AccessibleInputProps> = ({
  id,
  label,
  type = 'text',
  value,
  onChange,
  error,
  required = false,
  placeholder,
  description
}) => {
  const { t } = useTranslation();
  const errorId = error ? `${id}-error` : undefined;
  const descriptionId = description ? `${id}-description` : undefined;
  const ariaDescribedBy = [errorId, descriptionId].filter(Boolean).join(' ');
  
  return (
    <div className="form-group">
      <label htmlFor={id} className="form-label">
        {label}
        {required && <span className="required-indicator" aria-label={t('forms.required')}>*</span>}
      </label>
      
      {description && (
        <div id={descriptionId} className="form-description">
          {description}
        </div>
      )}
      
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        aria-invalid={!!error}
        aria-describedby={ariaDescribedBy || undefined}
        className={`form-input ${error ? 'form-input-error' : ''}`}
      />
      
      {error && (
        <div id={errorId} className="form-error" role="alert">
          {error}
        </div>
      )}
    </div>
  );
};
```

#### Screen Reader Support

```typescript
// Screen reader announcement hook
export const useA11y = () => {
  const announceToScreenReader = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', priority);
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    
    document.body.appendChild(announcement);
    
    // Remove after announcement
    setTimeout(() => {
      document.body.removeChild(announcement);
    }, 1000);
  };
  
  const setPageTitle = (title: string) => {
    document.title = title;
    announceToScreenReader(`Page loaded: ${title}`);
  };
  
  const announceRouteChange = (routeName: string) => {
    announceToScreenReader(`Navigated to ${routeName}`, 'assertive');
  };
  
  return {
    announceToScreenReader,
    setPageTitle,
    announceRouteChange
  };
};

// Focus management for SPA navigation
export const useFocusManagement = () => {
  const focusOnPageLoad = (elementSelector: string = 'h1') => {
    useEffect(() => {
      const element = document.querySelector(elementSelector) as HTMLElement;
      if (element) {
        element.focus();
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, []);
  };
  
  const trapFocus = (containerRef: React.RefObject<HTMLElement>) => {
    useEffect(() => {
      const container = containerRef.current;
      if (!container) return;
      
      const focusableElements = container.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      
      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;
      
      const handleTabKey = (e: KeyboardEvent) => {
        if (e.key === 'Tab') {
          if (e.shiftKey) {
            if (document.activeElement === firstElement) {
              lastElement.focus();
              e.preventDefault();
            }
          } else {
            if (document.activeElement === lastElement) {
              firstElement.focus();
              e.preventDefault();
            }
          }
        }
        
        if (e.key === 'Escape') {
          container.focus();
        }
      };
      
      container.addEventListener('keydown', handleTabKey);
      return () => container.removeEventListener('keydown', handleTabKey);
    }, [containerRef]);
  };
  
  return { focusOnPageLoad, trapFocus };
};
```

#### High Contrast and Color Accessibility

```css
/* High contrast mode support */
@media (prefers-contrast: high) {
  :root {
    --primary-color: #000000;
    --secondary-color: #ffffff;
    --text-color: #000000;
    --background-color: #ffffff;
    --border-color: #000000;
    --error-color: #ff0000;
    --success-color: #008000;
    --warning-color: #ff8c00;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* Color-blind friendly palette */
:root {
  /* Use colorbrewer-safe colors */
  --color-primary: #1f77b4;    /* Blue */
  --color-secondary: #ff7f0e;  /* Orange */
  --color-success: #2ca02c;    /* Green */
  --color-warning: #d62728;    /* Red */
  --color-info: #9467bd;       /* Purple */
  
  /* High contrast ratios (4.5:1 minimum) */
  --text-on-light: #212529;    /* Contrast ratio: 16.73:1 */
  --text-on-dark: #ffffff;     /* Contrast ratio: 21:1 */
  --link-color: #0066cc;       /* Contrast ratio: 7.03:1 */
}

/* Focus indicators */
.focus-visible {
  outline: 3px solid var(--color-primary);
  outline-offset: 2px;
}

/* Screen reader only content */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Skip navigation links */
.skip-link {
  position: absolute;
  top: -40px;
  left: 6px;
  background: var(--color-primary);
  color: var(--text-on-dark);
  padding: 8px;
  text-decoration: none;
  z-index: 9999;
}

.skip-link:focus {
  top: 6px;
}
```

### Backend Accessibility Support

```java
// Accessibility metadata service
@Service
public class AccessibilityService {
    
    public AccessibilityMetadata generateProductMetadata(Product product) {
        return AccessibilityMetadata.builder()
            .altText(generateAltText(product))
            .longDescription(generateLongDescription(product))
            .structuredData(generateStructuredData(product))
            .readingLevel(calculateReadingLevel(product.getDescription()))
            .build();
    }
    
    private String generateAltText(Product product) {
        // Generate descriptive alt text for product images
        StringBuilder altText = new StringBuilder();
        altText.append(product.getName());
        
        if (product.getColor() != null) {
            altText.append(" in ").append(product.getColor());
        }
        
        if (product.getCategory() != null) {
            altText.append(", ").append(product.getCategory());
        }
        
        return altText.toString();
    }
    
    private String generateLongDescription(Product product) {
        // Create detailed description for screen readers
        return String.format(
            "Product: %s. Category: %s. Price: %s. Description: %s. %s",
            product.getName(),
            product.getCategory(),
            formatCurrency(product.getPrice()),
            product.getDescription(),
            product.isInStock() ? "In stock" : "Out of stock"
        );
    }
    
    private ReadingLevel calculateReadingLevel(String text) {
        // Implement Flesch-Kincaid reading level calculation
        int sentences = countSentences(text);
        int words = countWords(text);
        int syllables = countSyllables(text);
        
        double score = 206.835 - (1.015 * (words / sentences)) - (84.6 * (syllables / words));
        
        if (score >= 90) return ReadingLevel.VERY_EASY;
        if (score >= 80) return ReadingLevel.EASY;
        if (score >= 70) return ReadingLevel.FAIRLY_EASY;
        if (score >= 60) return ReadingLevel.STANDARD;
        if (score >= 50) return ReadingLevel.FAIRLY_DIFFICULT;
        if (score >= 30) return ReadingLevel.DIFFICULT;
        return ReadingLevel.VERY_DIFFICULT;
    }
}

// Content simplification service
@Service
public class ContentSimplificationService {
    
    public SimplifiedContent simplifyForAccessibility(String content, ReadingLevel targetLevel) {
        // Use NLP libraries to simplify complex text
        String simplifiedText = simplifyText(content, targetLevel);
        List<String> keyPoints = extractKeyPoints(content);
        String summary = generateSummary(content);
        
        return SimplifiedContent.builder()
            .originalContent(content)
            .simplifiedContent(simplifiedText)
            .keyPoints(keyPoints)
            .summary(summary)
            .readingLevel(calculateReadingLevel(simplifiedText))
            .build();
    }
    
    public AudioDescription generateAudioDescription(Product product) {
        StringBuilder audioScript = new StringBuilder();
        
        audioScript.append("Product name: ").append(product.getName()).append(". ");
        audioScript.append("Price: ").append(formatCurrencyForAudio(product.getPrice())).append(". ");
        
        if (product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            audioScript.append("On sale. ").append(product.getDiscount()).append(" percent off. ");
        }
        
        audioScript.append("Description: ").append(product.getDescription()).append(". ");
        audioScript.append(product.isInStock() ? "Available for purchase." : "Currently out of stock.");
        
        return AudioDescription.builder()
            .script(audioScript.toString())
            .duration(estimateAudioDuration(audioScript.toString()))
            .language(getCurrentLanguage())
            .build();
    }
}
```

## Internationalization (i18n)

### Multi-language Support

```java
// Internationalization configuration
@Configuration
@EnableConfigurationProperties(InternationalizationProperties.class)
public class InternationalizationConfig {
    
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.US);
        return localeResolver;
    }
    
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
    
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

// Localization service
@Service
public class LocalizationService {
    
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;
    
    public String getMessage(String key, Object[] args, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        return messageSource.getMessage(key, args, locale);
    }
    
    public String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }
    
    public LocalizedContent getLocalizedContent(String contentKey, Locale locale) {
        String title = getMessage(contentKey + ".title", null, locale);
        String description = getMessage(contentKey + ".description", null, locale);
        String keywords = getMessage(contentKey + ".keywords", null, locale);
        
        return LocalizedContent.builder()
            .title(title)
            .description(description)
            .keywords(keywords)
            .locale(locale)
            .build();
    }
    
    public List<Locale> getSupportedLocales() {
        return Arrays.asList(
            Locale.US,                    // English (US)
            Locale.UK,                    // English (UK)
            new Locale("es", "ES"),       // Spanish (Spain)
            new Locale("es", "MX"),       // Spanish (Mexico)
            new Locale("fr", "FR"),       // French (France)
            new Locale("de", "DE"),       // German (Germany)
            new Locale("it", "IT"),       // Italian (Italy)
            new Locale("pt", "BR"),       // Portuguese (Brazil)
            new Locale("ja", "JP"),       // Japanese (Japan)
            new Locale("ko", "KR"),       // Korean (South Korea)
            new Locale("zh", "CN"),       // Chinese (Simplified)
            new Locale("zh", "TW"),       // Chinese (Traditional)
            new Locale("ar", "SA"),       // Arabic (Saudi Arabia)
            new Locale("hi", "IN"),       // Hindi (India)
            new Locale("ru", "RU"),       // Russian (Russia)
            new Locale("th", "TH"),       // Thai (Thailand)
            new Locale("vi", "VN")        // Vietnamese (Vietnam)
        );
    }
}
```

### Database Content Localization

```java
// Localized content entity
@Entity
@Table(name = "localized_content")
public class LocalizedContent {
    
    @Id
    private String id;
    
    @Column(name = "content_key")
    private String contentKey;
    
    @Column(name = "locale")
    private String locale;
    
    @Column(name = "title")
    private String title;
    
    @Lob
    @Column(name = "content")
    private String content;
    
    @Column(name = "meta_description")
    private String metaDescription;
    
    @Column(name = "meta_keywords")
    private String metaKeywords;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    // getters and setters
}

// Localized product service
@Service
public class LocalizedProductService {
    
    private final ProductRepository productRepository;
    private final LocalizedContentRepository localizedContentRepository;
    
    public LocalizedProductDTO getLocalizedProduct(String productId, Locale locale) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        
        LocalizedContent localizedContent = localizedContentRepository
            .findByContentKeyAndLocale("product." + productId, locale.toString())
            .orElse(getDefaultLocalizedContent(product));
        
        return LocalizedProductDTO.builder()
            .id(product.getId())
            .name(localizedContent.getTitle())
            .description(localizedContent.getContent())
            .metaDescription(localizedContent.getMetaDescription())
            .metaKeywords(localizedContent.getMetaKeywords())
            .price(product.getPrice())
            .currency(getCurrencyForLocale(locale))
            .imageUrls(product.getImageUrls())
            .category(getLocalizedCategory(product.getCategory(), locale))
            .specifications(getLocalizedSpecifications(product.getSpecifications(), locale))
            .locale(locale.toString())
            .build();
    }
    
    public void saveLocalizedContent(String productId, Locale locale, LocalizedProductRequest request) {
        LocalizedContent content = LocalizedContent.builder()
            .contentKey("product." + productId)
            .locale(locale.toString())
            .title(request.getName())
            .content(request.getDescription())
            .metaDescription(request.getMetaDescription())
            .metaKeywords(request.getMetaKeywords())
            .build();
        
        localizedContentRepository.save(content);
    }
}
```

### Multi-Currency Support

```java
// Currency configuration
@Configuration
public class CurrencyConfig {
    
    @Bean
    public CurrencyService currencyService() {
        return new CurrencyService();
    }
    
    @Bean
    public ExchangeRateProvider exchangeRateProvider() {
        return new ExchangeRateProvider();
    }
}

// Currency service
@Service
public class CurrencyService {
    
    private final ExchangeRateProvider exchangeRateProvider;
    private final CurrencyRepository currencyRepository;
    
    public List<Currency> getSupportedCurrencies() {
        return Arrays.asList(
            Currency.getInstance("USD"), // US Dollar
            Currency.getInstance("EUR"), // Euro
            Currency.getInstance("GBP"), // British Pound
            Currency.getInstance("JPY"), // Japanese Yen
            Currency.getInstance("CAD"), // Canadian Dollar
            Currency.getInstance("AUD"), // Australian Dollar
            Currency.getInstance("CHF"), // Swiss Franc
            Currency.getInstance("CNY"), // Chinese Yuan
            Currency.getInstance("INR"), // Indian Rupee
            Currency.getInstance("BRL"), // Brazilian Real
            Currency.getInstance("MXN"), // Mexican Peso
            Currency.getInstance("KRW"), // South Korean Won
            Currency.getInstance("SGD"), // Singapore Dollar
            Currency.getInstance("HKD"), // Hong Kong Dollar
            Currency.getInstance("NOK"), // Norwegian Krone
            Currency.getInstance("SEK"), // Swedish Krona
            Currency.getInstance("DKK"), // Danish Krone
            Currency.getInstance("PLN"), // Polish Zloty
            Currency.getInstance("CZK"), // Czech Koruna
            Currency.getInstance("HUF")  // Hungarian Forint
        );
    }
    
    public BigDecimal convertPrice(BigDecimal amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        ExchangeRate rate = exchangeRateProvider.getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
    }
    
    public String formatCurrency(BigDecimal amount, Currency currency, Locale locale) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        currencyFormatter.setCurrency(currency);
        return currencyFormatter.format(amount);
    }
    
    public Currency getCurrencyForLocale(Locale locale) {
        // Map locales to preferred currencies
        Map<String, String> localeToCurrency = Map.of(
            "US", "USD",
            "GB", "GBP",
            "FR", "EUR",
            "DE", "EUR",
            "IT", "EUR",
            "ES", "EUR",
            "JP", "JPY",
            "CN", "CNY",
            "IN", "INR",
            "BR", "BRL",
            "CA", "CAD",
            "AU", "AUD",
            "MX", "MXN",
            "KR", "KRW"
        );
        
        String currencyCode = localeToCurrency.getOrDefault(locale.getCountry(), "USD");
        return Currency.getInstance(currencyCode);
    }
}

// Exchange rate provider
@Service
public class ExchangeRateProvider {
    
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "exchange-rates", key = "#fromCurrency.currencyCode + '-' + #toCurrency.currencyCode")
    public ExchangeRate getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        String cacheKey = "exchange-rate:" + fromCurrency.getCurrencyCode() + "-" + toCurrency.getCurrencyCode();
        
        // Try cache first
        ExchangeRate cachedRate = (ExchangeRate) redisTemplate.opsForValue().get(cacheKey);
        if (cachedRate != null && !isRateExpired(cachedRate)) {
            return cachedRate;
        }
        
        // Fetch from external API
        try {
            String url = String.format("https://api.exchangerate-api.com/v4/latest/%s", 
                fromCurrency.getCurrencyCode());
            
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);
            BigDecimal rate = response.getRates().get(toCurrency.getCurrencyCode());
            
            ExchangeRate exchangeRate = ExchangeRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .timestamp(Instant.now())
                .source("exchangerate-api.com")
                .build();
            
            // Cache for 1 hour
            redisTemplate.opsForValue().set(cacheKey, exchangeRate, Duration.ofHours(1));
            
            return exchangeRate;
            
        } catch (Exception e) {
            log.error("Failed to fetch exchange rate for {} to {}", 
                fromCurrency.getCurrencyCode(), toCurrency.getCurrencyCode(), e);
            
            // Return fallback rate or throw exception
            throw new ExchangeRateException("Unable to fetch exchange rate", e);
        }
    }
}
```

### Regional Content and Preferences

```java
// Regional configuration service
@Service
public class RegionalConfigurationService {
    
    public RegionalSettings getRegionalSettings(Locale locale) {
        RegionalSettings.Builder settings = RegionalSettings.builder()
            .locale(locale)
            .currency(getCurrencyForLocale(locale))
            .dateFormat(getDateFormatForLocale(locale))
            .timeFormat(getTimeFormatForLocale(locale))
            .numberFormat(getNumberFormatForLocale(locale))
            .firstDayOfWeek(getFirstDayOfWeekForLocale(locale))
            .rtlLanguage(isRtlLanguage(locale));
        
        // Shipping settings
        settings.shippingSettings(getShippingSettingsForRegion(locale));
        
        // Tax settings
        settings.taxSettings(getTaxSettingsForRegion(locale));
        
        // Payment methods
        settings.paymentMethods(getPaymentMethodsForRegion(locale));
        
        // Legal requirements
        settings.legalRequirements(getLegalRequirementsForRegion(locale));
        
        return settings.build();
    }
    
    private ShippingSettings getShippingSettingsForRegion(Locale locale) {
        String country = locale.getCountry();
        
        return switch (country) {
            case "US" -> ShippingSettings.builder()
                .weightUnit(WeightUnit.POUNDS)
                .dimensionUnit(DimensionUnit.INCHES)
                .providers(List.of("USPS", "UPS", "FedEx"))
                .freeShippingThreshold(new BigDecimal("35.00"))
                .build();
                
            case "GB" -> ShippingSettings.builder()
                .weightUnit(WeightUnit.KILOGRAMS)
                .dimensionUnit(DimensionUnit.CENTIMETERS)
                .providers(List.of("Royal Mail", "DPD", "Hermes"))
                .freeShippingThreshold(new BigDecimal("25.00"))
                .build();
                
            case "DE", "FR", "IT", "ES" -> ShippingSettings.builder()
                .weightUnit(WeightUnit.KILOGRAMS)
                .dimensionUnit(DimensionUnit.CENTIMETERS)
                .providers(List.of("DHL", "DPD", "UPS"))
                .freeShippingThreshold(new BigDecimal("29.00"))
                .build();
                
            default -> getDefaultShippingSettings();
        };
    }
    
    private PaymentMethodSettings getPaymentMethodsForRegion(Locale locale) {
        String country = locale.getCountry();
        
        List<PaymentMethod> methods = new ArrayList<>();
        methods.add(PaymentMethod.CREDIT_CARD);
        methods.add(PaymentMethod.DEBIT_CARD);
        
        switch (country) {
            case "US":
                methods.add(PaymentMethod.PAYPAL);
                methods.add(PaymentMethod.APPLE_PAY);
                methods.add(PaymentMethod.GOOGLE_PAY);
                break;
            case "DE":
                methods.add(PaymentMethod.PAYPAL);
                methods.add(PaymentMethod.SOFORT);
                methods.add(PaymentMethod.SEPA_DIRECT_DEBIT);
                break;
            case "NL":
                methods.add(PaymentMethod.IDEAL);
                methods.add(PaymentMethod.PAYPAL);
                break;
            case "CN":
                methods.add(PaymentMethod.ALIPAY);
                methods.add(PaymentMethod.WECHAT_PAY);
                break;
            case "IN":
                methods.add(PaymentMethod.UPI);
                methods.add(PaymentMethod.PAYTM);
                methods.add(PaymentMethod.RAZORPAY);
                break;
        }
        
        return PaymentMethodSettings.builder()
            .supportedMethods(methods)
            .defaultMethod(PaymentMethod.CREDIT_CARD)
            .build();
    }
    
    private boolean isRtlLanguage(Locale locale) {
        String language = locale.getLanguage();
        return Set.of("ar", "he", "fa", "ur").contains(language);
    }
}

// Legal compliance service
@Service
public class LegalComplianceService {
    
    public LegalRequirements getLegalRequirementsForRegion(Locale locale) {
        String country = locale.getCountry();
        
        return switch (country) {
            case "US" -> LegalRequirements.builder()
                .requiresAgeVerification(false)
                .cookieConsentRequired(false)
                .gdprApplicable(false)
                .ccpaApplicable(true)
                .taxDisplayRequirement(TaxDisplayRequirement.EXCLUDING_TAX)
                .returnPolicyRequired(true)
                .warrantyRequired(false)
                .build();
                
            case "GB", "DE", "FR", "IT", "ES", "NL" -> LegalRequirements.builder()
                .requiresAgeVerification(false)
                .cookieConsentRequired(true)
                .gdprApplicable(true)
                .ccpaApplicable(false)
                .taxDisplayRequirement(TaxDisplayRequirement.INCLUDING_TAX)
                .returnPolicyRequired(true)
                .warrantyRequired(true)
                .coolingOffPeriod(Duration.ofDays(14))
                .build();
                
            default -> getDefaultLegalRequirements();
        };
    }
    
    public CookieConsentSettings getCookieConsentSettings(Locale locale) {
        boolean isEuCountry = isEuCountry(locale.getCountry());
        
        return CookieConsentSettings.builder()
            .required(isEuCountry)
            .categories(getCookieCategories(isEuCountry))
            .bannerText(getLocalizedCookieBanner(locale))
            .policyUrl(getLocalizedPrivacyPolicyUrl(locale))
            .build();
    }
}
```

## Frontend Internationalization

### React i18n Implementation

```typescript
// i18n configuration
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    debug: process.env.NODE_ENV === 'development',
    
    detection: {
      order: ['querystring', 'cookie', 'localStorage', 'navigator', 'htmlTag'],
      caches: ['localStorage', 'cookie'],
    },
    
    backend: {
      loadPath: '/api/locales/{{lng}}/{{ns}}',
    },
    
    interpolation: {
      escapeValue: false,
    },
    
    react: {
      useSuspense: false,
    },
  });

export default i18n;

// Localized number and currency formatting
export const useNumberFormat = () => {
  const { i18n } = useTranslation();
  
  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat(i18n.language, {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };
  
  const formatNumber = (number: number) => {
    return new Intl.NumberFormat(i18n.language).format(number);
  };
  
  const formatPercentage = (value: number) => {
    return new Intl.NumberFormat(i18n.language, {
      style: 'percent',
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).format(value);
  };
  
  return { formatCurrency, formatNumber, formatPercentage };
};

// Localized date formatting
export const useDateFormat = () => {
  const { i18n } = useTranslation();
  
  const formatDate = (date: Date, options?: Intl.DateTimeFormatOptions) => {
    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    };
    
    return new Intl.DateTimeFormat(i18n.language, { ...defaultOptions, ...options }).format(date);
  };
  
  const formatTime = (date: Date) => {
    return new Intl.DateTimeFormat(i18n.language, {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  };
  
  const formatDateTime = (date: Date) => {
    return new Intl.DateTimeFormat(i18n.language, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  };
  
  const formatRelativeTime = (date: Date) => {
    const rtf = new Intl.RelativeTimeFormat(i18n.language, { numeric: 'auto' });
    const diffInSeconds = (date.getTime() - Date.now()) / 1000;
    
    if (Math.abs(diffInSeconds) < 60) {
      return rtf.format(Math.round(diffInSeconds), 'second');
    }
    
    const diffInMinutes = diffInSeconds / 60;
    if (Math.abs(diffInMinutes) < 60) {
      return rtf.format(Math.round(diffInMinutes), 'minute');
    }
    
    const diffInHours = diffInMinutes / 60;
    if (Math.abs(diffInHours) < 24) {
      return rtf.format(Math.round(diffInHours), 'hour');
    }
    
    const diffInDays = diffInHours / 24;
    return rtf.format(Math.round(diffInDays), 'day');
  };
  
  return { formatDate, formatTime, formatDateTime, formatRelativeTime };
};
```

This comprehensive accessibility and internationalization strategy ensures the e-commerce platform is inclusive, accessible to users with disabilities, and ready for global markets with proper localization support.