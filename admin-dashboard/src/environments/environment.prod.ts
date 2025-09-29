export const environment = {
  production: true,
  
  // API Service URLs (Production)
  apiGatewayUrl: 'https://api.ecommerce.com',
  intelligentMonitoringServiceUrl: 'https://monitoring.ecommerce.com',
  
  // Microservice URLs (production internal network)
  userServiceUrl: 'http://user-service:8081',
  productServiceUrl: 'http://product-service:8082',
  cartServiceUrl: 'http://cart-service:8083',
  orderServiceUrl: 'http://order-service:8084',
  
  // External Service URLs
  appDynamicsUrl: 'https://your-account.saas.appdynamics.com',
  grafanaUrl: 'https://grafana.ecommerce.com',
  prometheusUrl: 'https://prometheus.ecommerce.com',
  
  // WebSocket Configuration
  webSocketUrl: 'wss://monitoring.ecommerce.com/ws',
  
  // Authentication
  authEnabled: true,
  tokenStorageKey: 'ecommerce_admin_token',
  
  // Feature Flags
  features: {
    humanReview: true,
    appDynamicsIntegration: true,
    automatedFixes: true,
    crossPlatformCorrelation: true,
    realTimeMonitoring: true
  },
  
  // Monitoring Configuration
  monitoring: {
    refreshInterval: 30000, // 30 seconds
    alertThresholds: {
      errorRate: 5.0, // percentage
      responseTime: 2000, // milliseconds
      cpuUsage: 80.0, // percentage
      memoryUsage: 85.0 // percentage
    }
  }
};