export const environment = {
  production: false,
  
  // API Service URLs
  apiGatewayUrl: 'http://localhost:8080',
  intelligentMonitoringServiceUrl: 'http://localhost:8090',
  
  // Microservice URLs (direct access for admin dashboard)
  userServiceUrl: 'http://localhost:8081',
  productServiceUrl: 'http://localhost:8082',
  cartServiceUrl: 'http://localhost:8083',
  orderServiceUrl: 'http://localhost:8084',
  
  // External Service URLs
  appDynamicsUrl: 'https://your-account.saas.appdynamics.com',
  grafanaUrl: 'http://localhost:3000',
  prometheusUrl: 'http://localhost:9090',
  
  // WebSocket Configuration
  webSocketUrl: 'ws://localhost:8090/ws',
  
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