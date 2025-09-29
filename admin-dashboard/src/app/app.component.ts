import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject, takeUntil, filter } from 'rxjs';

import { HeaderComponent } from './shared/components/header/header.component';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { LoadingComponent } from './shared/components/loading/loading.component';
import { NotificationComponent } from './shared/components/notification/notification.component';
import { AlertBarComponent } from './shared/components/alert-bar/alert-bar.component';

import { AuthService } from './core/services/auth.service';
import { LoadingService } from './core/services/loading.service';
import { NotificationService } from './core/services/notification.service';
import { MonitoringService } from './core/services/monitoring.service';
import { TelemetryService } from './core/services/telemetry.service';
import { WebSocketService } from './core/services/websocket.service';
import { SeoService } from './core/services/seo.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatBadgeModule,
    HeaderComponent,
    SidebarComponent,
    LoadingComponent,
    NotificationComponent,
    AlertBarComponent
  ],
  template: `
    <div class="admin-container" [class.dark-theme]="isDarkTheme">
      <!-- Loading Overlay -->
      <app-loading *ngIf="isLoading$ | async"></app-loading>
      
      <!-- System Alert Bar -->
      <app-alert-bar></app-alert-bar>
      
      <!-- Main Layout -->
      <mat-sidenav-container class="sidenav-container" hasBackdrop="false">
        <!-- Sidebar -->
        <mat-sidenav 
          #sidenav 
          mode="side" 
          opened="true"
          class="sidebar"
          [fixedInViewport]="true"
          [fixedTopGap]="0">
          <app-sidebar (menuToggle)="toggleSidenav()"></app-sidebar>
        </mat-sidenav>
        
        <!-- Main Content -->
        <mat-sidenav-content class="main-content">
          <!-- Header -->
          <app-header 
            (menuToggle)="toggleSidenav()"
            (themeToggle)="toggleTheme()">
          </app-header>
          
          <!-- Content Area -->
          <main class="content-area" role="main">
            <router-outlet></router-outlet>
          </main>
        </mat-sidenav-content>
      </mat-sidenav-container>
      
      <!-- Notifications -->
      <app-notification></app-notification>
    </div>
  `,
  styles: [`
    .admin-container {
      height: 100vh;
      overflow: hidden;
      background-color: #fafafa;
      transition: background-color 0.3s ease;
    }
    
    .admin-container.dark-theme {
      background-color: #121212;
    }
    
    .sidenav-container {
      height: 100vh;
    }
    
    .sidebar {
      width: 280px;
      border-right: 1px solid #e0e0e0;
      background-color: #ffffff;
      transition: all 0.3s ease;
    }
    
    .dark-theme .sidebar {
      border-right-color: #333;
      background-color: #1e1e1e;
    }
    
    .main-content {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    
    .content-area {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background-color: #fafafa;
      transition: background-color 0.3s ease;
    }
    
    .dark-theme .content-area {
      background-color: #121212;
    }
    
    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .sidebar {
        width: 240px;
      }
      
      .content-area {
        padding: 16px;
      }
    }
    
    /* Accessibility improvements */
    .content-area:focus {
      outline: 2px solid #1976d2;
      outline-offset: 2px;
    }
    
    /* Loading state */
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(255, 255, 255, 0.8);
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .dark-theme .loading-overlay {
      background-color: rgba(18, 18, 18, 0.8);
    }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;
  
  title = 'Admin Dashboard - Ecommerce Platform';
  isDarkTheme = false;
  
  private destroy$ = new Subject<void>();
  
  constructor(
    public loadingService: LoadingService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private monitoringService: MonitoringService,
    private telemetryService: TelemetryService,
    private webSocketService: WebSocketService,
    private seoService: SeoService,
    private router: Router
  ) {}
  
  get isLoading$() {
    return this.loadingService.isLoading$;
  }
  
  ngOnInit(): void {
    this.initializeApp();
    this.setupRouterEvents();
    this.setupMonitoring();
    this.setupWebSocket();
    this.loadUserPreferences();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.webSocketService.disconnect();
  }
  
  private initializeApp(): void {
    // Initialize authentication state
    this.authService.initializeAuth();
    
    // Set default SEO tags for admin dashboard
    this.seoService.updateMetaTags({
      title: 'Admin Dashboard - Ecommerce Platform',
      description: 'Comprehensive admin dashboard with real-time monitoring and analytics',
      keywords: 'admin, dashboard, monitoring, analytics, ecommerce management',
      robots: 'noindex, nofollow' // Prevent search engine indexing
    });
    
    // Track application start
    this.telemetryService.trackEvent('admin_app_started', {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      }
    });
    
    // Initialize monitoring
    this.monitoringService.initialize();
  }
  
  private setupRouterEvents(): void {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        // Track page views
        this.telemetryService.trackPageView(event.urlAfterRedirects, 'admin_dashboard');
        
        // Update page title based on route
        this.updatePageTitle(event.urlAfterRedirects);
        
        // Mark loading as complete
        setTimeout(() => {
          this.loadingService.setLoading(false);
        }, 100);
        
        // Close mobile sidenav on navigation
        if (window.innerWidth <= 768 && this.sidenav) {
          this.sidenav.close();
        }
      });
  }
  
  private setupMonitoring(): void {
    // Start system health monitoring
    this.monitoringService.startHealthMonitoring();
    
    // Subscribe to critical alerts
    this.monitoringService.getCriticalAlerts()
      .pipe(takeUntil(this.destroy$))
      .subscribe(alert => {
        this.notificationService.showError(
          'Critical System Alert',
          alert.message,
          { duration: 0 } // Don't auto-dismiss critical alerts
        );
      });
    
    // Subscribe to system metrics updates
    this.monitoringService.getSystemMetrics()
      .pipe(takeUntil(this.destroy$))
      .subscribe(metrics => {
        // Update global metrics display or store in state management
        this.handleSystemMetricsUpdate(metrics);
      });
  }
  
  private setupWebSocket(): void {
    // Connect to real-time monitoring updates
    this.webSocketService.connect();
    
    // Subscribe to real-time events
    this.webSocketService.on('system_alert')
      .pipe(takeUntil(this.destroy$))
      .subscribe(alert => {
        this.notificationService.showWarning(
          'System Alert',
          alert.message
        );
      });
    
    this.webSocketService.on('user_activity')
      .pipe(takeUntil(this.destroy$))
      .subscribe(activity => {
        // Handle real-time user activity updates
        this.handleUserActivity(activity);
      });
    
    this.webSocketService.on('performance_metric')
      .pipe(takeUntil(this.destroy$))
      .subscribe(metric => {
        // Handle real-time performance metric updates
        this.handlePerformanceMetric(metric);
      });
  }
  
  private loadUserPreferences(): void {
    const preferences = localStorage.getItem('admin_preferences');
    if (preferences) {
      const parsed = JSON.parse(preferences);
      this.isDarkTheme = parsed.darkTheme || false;
      
      // Apply theme
      this.applyTheme(this.isDarkTheme);
    }
  }
  
  toggleSidenav(): void {
    if (this.sidenav) {
      this.sidenav.toggle();
      
      // Track UI interaction
      this.telemetryService.trackEvent('sidenav_toggled', {
        opened: this.sidenav.opened,
        timestamp: new Date().toISOString()
      });
    }
  }
  
  toggleTheme(): void {
    this.isDarkTheme = !this.isDarkTheme;
    this.applyTheme(this.isDarkTheme);
    this.saveUserPreferences();
    
    // Track theme change
    this.telemetryService.trackEvent('theme_changed', {
      theme: this.isDarkTheme ? 'dark' : 'light',
      timestamp: new Date().toISOString()
    });
  }
  
  private applyTheme(isDark: boolean): void {
    const body = document.body;
    if (isDark) {
      body.classList.add('dark-theme');
    } else {
      body.classList.remove('dark-theme');
    }
  }
  
  private saveUserPreferences(): void {
    const preferences = {
      darkTheme: this.isDarkTheme
    };
    localStorage.setItem('admin_preferences', JSON.stringify(preferences));
  }
  
  private updatePageTitle(url: string): void {
    const routeSegments = url.split('/').filter(segment => segment);
    const primaryRoute = routeSegments[0] || 'dashboard';
    
    let title = 'Admin Dashboard';
    
    switch (primaryRoute) {
      case 'analytics':
        title = 'Analytics - Admin Dashboard';
        break;
      case 'monitoring':
        title = 'System Monitoring - Admin Dashboard';
        break;
      case 'orders':
        title = 'Order Management - Admin Dashboard';
        break;
      case 'products':
        title = 'Product Management - Admin Dashboard';
        break;
      case 'users':
        title = 'User Management - Admin Dashboard';
        break;
      case 'settings':
        title = 'Settings - Admin Dashboard';
        break;
      default:
        title = 'Dashboard - Admin Platform';
    }
    
    this.seoService.updateTitle(title);
  }
  
  private handleSystemMetricsUpdate(metrics: any): void {
    // Store metrics in state management or update UI components
    // This could trigger updates to dashboard widgets
  }
  
  private handleUserActivity(activity: any): void {
    // Handle real-time user activity updates
    // Update user activity widgets or notifications
  }
  
  private handlePerformanceMetric(metric: any): void {
    // Handle real-time performance metrics
    // Update performance dashboards and alerts
    
    // Check for performance thresholds
    if (metric.value > metric.threshold) {
      this.notificationService.showWarning(
        'Performance Alert',
        `${metric.name} is above threshold: ${metric.value}`
      );
    }
  }
}