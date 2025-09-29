import { bootstrapApplication } from '@angular/platform-browser';
import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { authInterceptor } from './app/core/interceptors/auth.interceptor';
import { errorInterceptor } from './app/core/interceptors/error.interceptor';
import { loadingInterceptor } from './app/core/interceptors/loading.interceptor';
import { telemetryInterceptor } from './app/core/interceptors/telemetry.interceptor';
import { environment } from './environments/environment';

// Initialize OpenTelemetry Web SDK
import { initializeOpenTelemetry } from './app/core/telemetry/otel-config';

// Initialize telemetry
initializeOpenTelemetry();

bootstrapApplication(AppComponent, {
  providers: [
    // Router
    provideRouter(routes),
    
    // HTTP Client with Interceptors
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        errorInterceptor,
        loadingInterceptor,
        telemetryInterceptor
      ])
    ),
    
    // Angular Material
    importProvidersFrom(
      BrowserAnimationsModule,
      MatSnackBarModule,
      MatDialogModule
    ),
    
    // Service Worker
    provideServiceWorker('ngsw-worker.js', {
      enabled: environment.production,
      registrationStrategy: 'registerWhenStable:30000'
    })
  ]
}).catch(err => {
  console.error('Error starting Customer Portal:', err);
  
  // Send error to monitoring
  if (window.gtag) {
    window.gtag('event', 'exception', {
      description: `Bootstrap Error: ${err.message}`,
      fatal: true
    });
  }
  
  // Show user-friendly error message
  document.body.innerHTML = `
    <div style="text-align: center; padding: 50px; font-family: Arial, sans-serif;">
      <h1 style="color: #d32f2f;">Application Error</h1>
      <p>We're sorry, but there was an error loading the application.</p>
      <p>Please refresh the page or try again later.</p>
      <button onclick="window.location.reload()" style="padding: 10px 20px; background: #1976d2; color: white; border: none; border-radius: 4px; cursor: pointer;">
        Reload Page
      </button>
    </div>
  `;
});