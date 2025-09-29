export const environment = {
  production: true,
  apiUrl: '/api',
  apiTimeout: 30000, // 30 seconds
  fileUploadTimeout: 60000, // 60 seconds
  apiRetryAttempts: 3,
  
  // Features
  features: {
    enablePWA: true,
    enableServiceWorker: true,
    enableAnalytics: true,
    enableTelemetry: true,
    enableNotifications: true,
    enableOfflineMode: true
  },
  
  // OpenTelemetry Configuration
  telemetry: {
    serviceName: 'customer-portal',
    serviceVersion: '1.0.0',
    environment: 'production',
    endpoint: '/api/traces',
    enableConsoleSpanExporter: false,
    enableAutoInstrumentation: true,
    sampleRate: 0.1 // 10% sampling in production
  },
  
  // Monitoring endpoints
  monitoring: {
    healthCheckInterval: 60000, // 1 minute
    performanceMetricsInterval: 300000, // 5 minutes
    errorReportingEndpoint: '/api/errors',
    metricsEndpoint: '/api/metrics'
  },
  
  // UI Configuration
  ui: {
    defaultPageSize: 20,
    maxPageSize: 100,
    debounceTime: 300,
    animationDuration: 250,
    toastDuration: 5000,
    modalBackdropClose: true
  },
  
  // Security
  security: {
    tokenRefreshBuffer: 300, // 5 minutes before expiration
    maxLoginAttempts: 3,
    lockoutDuration: 1800000, // 30 minutes
    passwordMinLength: 8,
    sessionTimeout: 3600000 // 1 hour
  },
  
  // Performance
  performance: {
    lazyLoadingDelay: 200,
    imageOptimization: true,
    enableVirtualScrolling: true,
    enableTrackByFunctions: true,
    enableOnPush: true
  },
  
  // External integrations
  integrations: {
    googleAnalytics: {
      enabled: true,
      trackingId: 'GA_TRACKING_ID' // Replace with actual tracking ID
    },
    stripe: {
      enabled: true,
      publicKey: 'pk_live_...' // Replace with actual Stripe public key
    },
    googleMaps: {
      enabled: true,
      apiKey: 'GOOGLE_MAPS_API_KEY' // Replace with actual Google Maps API key
    }
  }
};