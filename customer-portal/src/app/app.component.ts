import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Subject, takeUntil, filter } from 'rxjs';

import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { LoadingComponent } from './shared/components/loading/loading.component';
import { NotificationComponent } from './shared/components/notification/notification.component';

import { AuthService } from './core/services/auth.service';
import { LoadingService } from './core/services/loading.service';
import { NotificationService } from './core/services/notification.service';
import { TelemetryService } from './core/services/telemetry.service';
import { SeoService } from './core/services/seo.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    HeaderComponent,
    FooterComponent,
    LoadingComponent,
    NotificationComponent
  ],
  template: `
    <div class="app-container">
      <!-- Loading Overlay -->
      <app-loading *ngIf="isLoading$ | async"></app-loading>
      
      <!-- Header -->
      <app-header></app-header>
      
      <!-- Main Content -->
      <main class="main-content" role="main">
        <router-outlet></router-outlet>
      </main>
      
      <!-- Footer -->
      <app-footer></app-footer>
      
      <!-- Notifications -->
      <app-notification></app-notification>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background-color: #fafafa;
    }
    
    .main-content {
      flex: 1;
      padding-top: 64px; /* Account for fixed header */
      min-height: calc(100vh - 64px - 200px); /* Header and footer height */
    }
    
    @media (max-width: 768px) {
      .main-content {
        padding-top: 56px; /* Smaller header on mobile */
        min-height: calc(100vh - 56px - 150px);
      }
    }
    
    /* Accessibility improvements */
    :host {
      display: block;
    }
    
    /* Skip to main content link for screen readers */
    .skip-link {
      position: absolute;
      top: -40px;
      left: 6px;
      background: #1976d2;
      color: white;
      padding: 8px;
      text-decoration: none;
      z-index: 1000;
      border-radius: 0 0 4px 4px;
    }
    
    .skip-link:focus {
      top: 0;
    }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Customer Portal - Ecommerce Platform';
  
  private destroy$ = new Subject<void>();
  
  constructor(
    public loadingService: LoadingService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private telemetryService: TelemetryService,
    private seoService: SeoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}
  
  get isLoading$() {
    return this.loadingService.isLoading$;
  }
  
  ngOnInit(): void {
    this.initializeApp();
    this.setupRouterEvents();
    this.setupErrorHandling();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  private initializeApp(): void {
    // Initialize authentication state
    this.authService.initializeAuth();
    
    // Set default SEO tags
    this.seoService.updateMetaTags({
      title: 'Ecommerce Platform - Shop Online',
      description: 'Enterprise ecommerce platform with secure checkout and fast delivery',
      keywords: 'ecommerce, shopping, online store, products',
      ogTitle: 'Ecommerce Platform',
      ogDescription: 'Shop the latest products with secure checkout',
      ogImage: '/assets/images/og-image.png',
      twitterCard: 'summary_large_image'
    });
    
    // Track application start
    this.telemetryService.trackEvent('app_started', {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language
    });
    
    // Check for updates (PWA)
    this.checkForAppUpdates();
  }
  
  private setupRouterEvents(): void {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        // Track page views
        this.telemetryService.trackPageView(event.urlAfterRedirects);
        
        // Update SEO for specific routes
        this.updateSeoForRoute(event.urlAfterRedirects);
        
        // Scroll to top on route change
        window.scrollTo(0, 0);
        
        // Mark loading as complete
        setTimeout(() => {
          this.loadingService.setLoading(false);
        }, 100);
      });
  }
  
  private setupErrorHandling(): void {
    // Global error handler
    window.addEventListener('error', (event) => {
      this.telemetryService.trackError(event.error || new Error(event.message), {
        source: 'global_error_handler',
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno
      });
    });
    
    // Unhandled promise rejection handler
    window.addEventListener('unhandledrejection', (event) => {
      this.telemetryService.trackError(new Error(event.reason), {
        source: 'unhandled_promise_rejection',
        reason: event.reason
      });
    });
  }
  
  private updateSeoForRoute(url: string): void {
    const routeSegments = url.split('/').filter(segment => segment);
    const primaryRoute = routeSegments[0] || 'home';
    
    switch (primaryRoute) {
      case 'products':
        this.seoService.updateMetaTags({
          title: 'Products - Ecommerce Platform',
          description: 'Browse our extensive product catalog with fast search and filtering',
          keywords: 'products, catalog, search, shopping'
        });
        break;
      
      case 'cart':
        this.seoService.updateMetaTags({
          title: 'Shopping Cart - Ecommerce Platform',
          description: 'Review your items and proceed to secure checkout',
          keywords: 'cart, checkout, shopping'
        });
        break;
      
      case 'orders':
        this.seoService.updateMetaTags({
          title: 'Order History - Ecommerce Platform',
          description: 'Track your orders and view purchase history',
          keywords: 'orders, history, tracking'
        });
        break;
      
      case 'login':
        this.seoService.updateMetaTags({
          title: 'Login - Ecommerce Platform',
          description: 'Sign in to your account to access your orders and saved items',
          keywords: 'login, signin, account'
        });
        break;
      
      case 'register':
        this.seoService.updateMetaTags({
          title: 'Create Account - Ecommerce Platform',
          description: 'Join our platform to start shopping and track your orders',
          keywords: 'register, signup, account, join'
        });
        break;
      
      default:
        this.seoService.updateMetaTags({
          title: 'Ecommerce Platform - Shop Online',
          description: 'Enterprise ecommerce platform with secure checkout and fast delivery',
          keywords: 'ecommerce, shopping, online store, products'
        });
    }
  }
  
  private checkForAppUpdates(): void {
    // Check for service worker updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        this.notificationService.showInfo(
          'App Updated', 
          'A new version is available. Refresh to use the latest features.'
        );
      });
    }
  }
}