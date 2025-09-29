export const environment = {
  production: false,
  apiUrl: '/api',
  apiTimeout: 30000, // 30 seconds
  fileUploadTimeout: 60000, // 60 seconds
  apiRetryAttempts: 2,
  
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
    environment: 'development',
    endpoint: '/api/traces',
    enableConsoleSpanExporter: true,
    enableAutoInstrumentation: true,
    sampleRate: 1.0 // 100% sampling in development
  },
  
  // Monitoring endpoints
  monitoring: {
    healthCheckInterval: 30000, // 30 seconds
    performanceMetricsInterval: 60000, // 1 minute
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
    maxLoginAttempts: 5,
    lockoutDuration: 900000, // 15 minutes
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
      enabled: false,
      trackingId: ''
    },
    stripe: {
      enabled: false,
      publicKey: ''
    },
    googleMaps: {
      enabled: false,
      apiKey: ''
    }
  }
};