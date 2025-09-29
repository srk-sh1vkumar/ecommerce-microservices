import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil, filter, interval } from 'rxjs';

import { HeaderComponent } from './shared/components/header/header.component';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { LoadingComponent } from './shared/components/loading/loading.component';
import { NotificationComponent } from './shared/components/notification/notification.component';
import { SecurityAlertComponent } from './shared/components/security-alert/security-alert.component';
import { SessionTimeoutComponent } from './shared/components/session-timeout/session-timeout.component';

import { AuthService } from './core/services/auth.service';
import { LoadingService } from './core/services/loading.service';
import { NotificationService } from './core/services/notification.service';
import { SecurityService } from './core/services/security.service';
import { SessionService } from './core/services/session.service';
import { AuditService } from './core/services/audit.service';
import { TelemetryService } from './core/services/telemetry.service';
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
    MatSnackBarModule,
    HeaderComponent,
    SidebarComponent,
    LoadingComponent,
    NotificationComponent,
    SecurityAlertComponent,
    SessionTimeoutComponent
  ],
  template: `
    <div class="vendor-container" [class.high-security]="isHighSecurityMode">
      <!-- Loading Overlay -->
      <app-loading *ngIf="isLoading$ | async"></app-loading>
      
      <!-- Security Alert Component -->
      <app-security-alert 
        *ngIf="securityAlerts.length > 0"
        [alerts]="securityAlerts"
        (alertDismissed)="dismissSecurityAlert($event)">
      </app-security-alert>
      
      <!-- Session Timeout Warning -->
      <app-session-timeout
        *ngIf="showSessionWarning"
        [timeRemaining]="sessionTimeRemaining"
        (extendSession)="extendSession()"
        (logout)="handleSessionTimeout()">
      </app-session-timeout>
      
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
          <app-sidebar 
            (menuToggle)="toggleSidenav()"
            [userRole]="currentUserRole"
            [securityLevel]="securityLevel">
          </app-sidebar>
        </mat-sidenav>
        
        <!-- Main Content -->
        <mat-sidenav-content class="main-content">
          <!-- Header -->
          <app-header 
            (menuToggle)="toggleSidenav()"
            (securityToggle)="toggleSecurityMode()"
            [isHighSecurity]="isHighSecurityMode"
            [notifications]="notificationCount">
          </app-header>
          
          <!-- Security Banner (if in high security mode) -->
          <div class="security-banner" *ngIf="isHighSecurityMode">
            <mat-icon>security</mat-icon>
            <span>High Security Mode Active - Enhanced monitoring and audit logging enabled</span>
          </div>
          
          <!-- Breadcrumb Navigation -->
          <nav class="breadcrumb-nav" aria-label="Breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item">
                <a routerLink="/" [attr.aria-current]="currentRoute === '/' ? 'page' : null">
                  Dashboard
                </a>
              </li>
              <li class="breadcrumb-item active" *ngIf="currentRoute !== '/'">
                {{ getCurrentPageTitle() }}
              </li>
            </ol>
          </nav>
          
          <!-- Content Area -->
          <main class="content-area" role="main" [attr.aria-label]="getCurrentPageTitle()">
            <router-outlet></router-outlet>
          </main>
          
          <!-- Footer with Security Information -->
          <footer class="security-footer">
            <div class="security-info">
              <span class="last-login">Last login: {{ lastLoginTime | date:'medium' }}</span>
              <span class="session-info">Session expires: {{ sessionExpiryTime | date:'medium' }}</span>
              <span class="security-level">Security Level: {{ securityLevel | titlecase }}</span>
            </div>
          </footer>
        </mat-sidenav-content>
      </mat-sidenav-container>
      
      <!-- Notifications -->
      <app-notification></app-notification>
    </div>
  `,
  styles: [`
    .vendor-container {
      height: 100vh;
      overflow: hidden;
      background-color: #fafafa;
      transition: all 0.3s ease;
    }
    
    .vendor-container.high-security {
      background-color: #f5f5f5;
      border: 2px solid #ff9800;
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
    
    .high-security .sidebar {
      border-right-color: #ff9800;
      background-color: #fffbf2;
    }
    
    .main-content {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    
    .security-banner {
      background-color: #ff9800;
      color: white;
      padding: 8px 16px;
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      font-size: 0.9rem;
    }
    
    .breadcrumb-nav {
      padding: 8px 20px;
      background-color: #f8f9fa;
      border-bottom: 1px solid #e0e0e0;
    }
    
    .breadcrumb {
      margin: 0;
      padding: 0;
      list-style: none;
      display: flex;
      align-items: center;
    }
    
    .breadcrumb-item {
      font-size: 0.9rem;
    }
    
    .breadcrumb-item + .breadcrumb-item::before {
      content: ">";
      margin: 0 8px;
      color: #666;
    }
    
    .breadcrumb-item a {
      color: #1976d2;
      text-decoration: none;
    }
    
    .breadcrumb-item a:hover {
      text-decoration: underline;
    }
    
    .breadcrumb-item.active {
      color: #666;
    }
    
    .content-area {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background-color: #fafafa;
      transition: background-color 0.3s ease;
    }
    
    .high-security .content-area {
      background-color: #f5f5f5;
    }
    
    .security-footer {
      background-color: #f8f9fa;
      border-top: 1px solid #e0e0e0;
      padding: 12px 20px;
      font-size: 0.8rem;
      color: #666;
    }
    
    .security-info {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 16px;
    }
    
    .security-info span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .sidebar {
        width: 240px;
      }
      
      .content-area {
        padding: 16px;
      }
      
      .security-info {
        flex-direction: column;
        gap: 8px;
      }
      
      .breadcrumb-nav {
        padding: 8px 16px;
      }
    }
    
    /* Accessibility improvements */
    .content-area:focus {
      outline: 2px solid #1976d2;
      outline-offset: 2px;
    }
    
    /* Security mode indicators */
    .high-security .mat-toolbar {
      background-color: #fff3e0 !important;
    }
    
    /* Focus indicators for security */
    .high-security input:focus,
    .high-security textarea:focus,
    .high-security select:focus {
      outline: 2px solid #ff9800;
      outline-offset: 2px;
    }
    
    /* Loading state */
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(255, 255, 255, 0.9);
      z-index: 9999;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .high-security .loading-overlay {
      background-color: rgba(255, 251, 242, 0.95);
    }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;
  
  title = 'Vendor Portal - Ecommerce Platform';
  currentRoute = '/';
  currentUserRole = '';
  
  // Security State
  isHighSecurityMode = false;
  securityLevel = 'standard';
  securityAlerts: any[] = [];
  
  // Session Management
  showSessionWarning = false;
  sessionTimeRemaining = 0;
  lastLoginTime = new Date();
  sessionExpiryTime = new Date();
  
  // Notifications
  notificationCount = 0;
  
  private destroy$ = new Subject<void>();
  private sessionCheckInterval = 60000; // 1 minute
  
  constructor(
    public loadingService: LoadingService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private securityService: SecurityService,
    private sessionService: SessionService,
    private auditService: AuditService,
    private telemetryService: TelemetryService,
    private seoService: SeoService,
    private router: Router
  ) {}
  
  get isLoading$() {
    return this.loadingService.isLoading$;
  }
  
  ngOnInit(): void {
    this.initializeApp();
    this.setupRouterEvents();
    this.setupSecurityMonitoring();
    this.setupSessionManagement();
    this.loadUserPreferences();
    this.startSecurityChecks();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  private initializeApp(): void {
    // Initialize authentication state
    this.authService.initializeAuth();
    
    // Set vendor-specific SEO tags
    this.seoService.updateMetaTags({
      title: 'Vendor Portal - Ecommerce Platform',
      description: 'Secure vendor portal for product management and business analytics',
      keywords: 'vendor, seller, product management, analytics, business',
      robots: 'noindex, nofollow' // Prevent search engine indexing
    });
    
    // Track application start with enhanced security context
    this.telemetryService.trackEvent('vendor_app_started', {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language,
      securityLevel: this.securityLevel,
      screenResolution: `${screen.width}x${screen.height}`,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    });
    
    // Initialize security services
    this.securityService.initialize();
    this.auditService.logEvent('app_initialization', {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent
    });
  }
  
  private setupRouterEvents(): void {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.urlAfterRedirects;
        
        // Track page views with security context
        this.telemetryService.trackPageView(event.urlAfterRedirects, 'vendor_portal');
        
        // Audit log navigation
        this.auditService.logEvent('page_navigation', {
          from: this.currentRoute,
          to: event.urlAfterRedirects,
          timestamp: new Date().toISOString(),
          securityLevel: this.securityLevel
        });
        
        // Update page title and SEO
        this.updatePageTitle(event.urlAfterRedirects);
        
        // Security check for sensitive pages
        this.performPageSecurityCheck(event.urlAfterRedirects);
        
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
  
  private setupSecurityMonitoring(): void {
    // Subscribe to security alerts
    this.securityService.getSecurityAlerts()
      .pipe(takeUntil(this.destroy$))
      .subscribe(alerts => {
        this.securityAlerts = alerts;
        
        // Show critical alerts immediately
        const criticalAlerts = alerts.filter(alert => alert.severity === 'critical');
        criticalAlerts.forEach(alert => {
          this.notificationService.showError(
            'Security Alert',
            alert.message,
            { duration: 0 } // Don't auto-dismiss critical alerts
          );
        });
      });
    
    // Monitor for suspicious activity
    this.securityService.getSuspiciousActivity()
      .pipe(takeUntil(this.destroy$))
      .subscribe(activity => {
        this.handleSuspiciousActivity(activity);
      });
  }
  
  private setupSessionManagement(): void {
    // Monitor session status
    this.sessionService.getSessionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.sessionExpiryTime = new Date(status.expiryTime);
        this.lastLoginTime = new Date(status.lastLoginTime);
        
        // Calculate time remaining
        const now = new Date().getTime();
        const expiry = this.sessionExpiryTime.getTime();
        this.sessionTimeRemaining = Math.max(0, expiry - now);
        
        // Show warning if session expires in less than 5 minutes
        this.showSessionWarning = this.sessionTimeRemaining > 0 && this.sessionTimeRemaining < 300000;
      });
    
    // Periodic session check
    interval(this.sessionCheckInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.sessionService.checkSession();
      });
  }
  
  private startSecurityChecks(): void {
    // Periodic security monitoring
    interval(30000) // 30 seconds
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.securityService.performSecurityCheck();
      });
    
    // Monitor for multiple failed login attempts
    this.securityService.monitorLoginAttempts();
    
    // Check for outdated browser security features
    this.securityService.checkBrowserSecurity();
  }
  
  private loadUserPreferences(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUserRole = user.role || 'vendor';
      
      // Load security preferences
      const securityPrefs = localStorage.getItem('vendor_security_preferences');
      if (securityPrefs) {
        const parsed = JSON.parse(securityPrefs);
        this.isHighSecurityMode = parsed.highSecurityMode || false;
        this.securityLevel = parsed.securityLevel || 'standard';
      }
      
      // Apply security level
      this.applySecurityLevel();
    }
  }
  
  toggleSidenav(): void {
    if (this.sidenav) {
      this.sidenav.toggle();
      
      // Track UI interaction with security context
      this.telemetryService.trackEvent('sidenav_toggled', {
        opened: this.sidenav.opened,
        timestamp: new Date().toISOString(),
        securityLevel: this.securityLevel
      });
      
      // Audit log UI interaction
      this.auditService.logEvent('ui_interaction', {
        action: 'sidenav_toggle',
        opened: this.sidenav.opened,
        timestamp: new Date().toISOString()
      });
    }
  }
  
  toggleSecurityMode(): void {
    this.isHighSecurityMode = !this.isHighSecurityMode;
    this.securityLevel = this.isHighSecurityMode ? 'high' : 'standard';
    
    this.applySecurityLevel();
    this.saveSecurityPreferences();
    
    // Track security mode change
    this.telemetryService.trackEvent('security_mode_changed', {
      newSecurityLevel: this.securityLevel,
      timestamp: new Date().toISOString()
    });
    
    // Audit log security change
    this.auditService.logEvent('security_mode_change', {
      oldLevel: this.isHighSecurityMode ? 'standard' : 'high',
      newLevel: this.securityLevel,
      timestamp: new Date().toISOString()
    });
    
    // Notify user
    this.notificationService.showInfo(
      'Security Mode Updated',
      `Security level changed to ${this.securityLevel}`
    );
  }
  
  private applySecurityLevel(): void {
    this.securityService.setSecurityLevel(this.securityLevel);
    
    if (this.isHighSecurityMode) {
      // Enhanced security measures
      this.sessionCheckInterval = 30000; // More frequent session checks
      document.body.classList.add('high-security-mode');
    } else {
      this.sessionCheckInterval = 60000;
      document.body.classList.remove('high-security-mode');
    }
  }
  
  private saveSecurityPreferences(): void {
    const preferences = {
      highSecurityMode: this.isHighSecurityMode,
      securityLevel: this.securityLevel
    };
    localStorage.setItem('vendor_security_preferences', JSON.stringify(preferences));
  }
  
  extendSession(): void {
    this.sessionService.extendSession().subscribe({
      next: () => {
        this.showSessionWarning = false;
        this.notificationService.showSuccess(
          'Session Extended',
          'Your session has been extended for another hour'
        );
        
        // Audit log session extension
        this.auditService.logEvent('session_extended', {
          timestamp: new Date().toISOString(),
          extensionDuration: 3600000 // 1 hour
        });
      },
      error: (error) => {
        this.notificationService.showError(
          'Session Extension Failed',
          'Unable to extend session. Please login again.'
        );
        this.handleSessionTimeout();
      }
    });
  }
  
  handleSessionTimeout(): void {
    this.auditService.logEvent('session_timeout', {
      timestamp: new Date().toISOString(),
      forced: true
    });
    
    this.authService.logout();
  }
  
  dismissSecurityAlert(alertId: string): void {
    this.securityAlerts = this.securityAlerts.filter(alert => alert.id !== alertId);
    
    this.auditService.logEvent('security_alert_dismissed', {
      alertId,
      timestamp: new Date().toISOString()
    });
  }
  
  private updatePageTitle(url: string): void {
    const routeSegments = url.split('/').filter(segment => segment);
    const primaryRoute = routeSegments[0] || 'dashboard';
    
    let title = 'Vendor Portal';
    
    switch (primaryRoute) {
      case 'products':
        title = 'Product Management - Vendor Portal';
        break;
      case 'orders':
        title = 'Order Management - Vendor Portal';
        break;
      case 'analytics':
        title = 'Sales Analytics - Vendor Portal';
        break;
      case 'inventory':
        title = 'Inventory Management - Vendor Portal';
        break;
      case 'profile':
        title = 'Vendor Profile - Vendor Portal';
        break;
      case 'settings':
        title = 'Settings - Vendor Portal';
        break;
      default:
        title = 'Dashboard - Vendor Portal';
    }
    
    this.seoService.updateTitle(title);
  }
  
  getCurrentPageTitle(): string {
    const routeSegments = this.currentRoute.split('/').filter(segment => segment);
    const primaryRoute = routeSegments[0] || 'dashboard';
    
    const titles = {
      'dashboard': 'Dashboard',
      'products': 'Product Management',
      'orders': 'Order Management',
      'analytics': 'Sales Analytics',
      'inventory': 'Inventory Management',
      'profile': 'Vendor Profile',
      'settings': 'Settings'
    };
    
    return titles[primaryRoute as keyof typeof titles] || 'Dashboard';
  }
  
  private performPageSecurityCheck(url: string): void {
    // Enhanced security for sensitive pages
    const sensitivePages = ['/settings', '/profile', '/analytics'];
    
    if (sensitivePages.some(page => url.includes(page))) {
      this.securityService.performEnhancedSecurityCheck();
      
      // Require re-authentication for critical pages if in high security mode
      if (this.isHighSecurityMode && url.includes('/settings')) {
        this.securityService.requireReAuthentication();
      }
    }
  }
  
  private handleSuspiciousActivity(activity: any): void {
    // Log suspicious activity
    this.auditService.logEvent('suspicious_activity_detected', {
      activity: activity.type,
      details: activity.details,
      timestamp: new Date().toISOString(),
      severity: activity.severity
    });
    
    // Take appropriate action based on severity
    switch (activity.severity) {
      case 'critical':
        this.authService.logout();
        this.notificationService.showError(
          'Security Breach Detected',
          'Session terminated for security reasons'
        );
        break;
      case 'high':
        this.toggleSecurityMode();
        this.notificationService.showWarning(
          'Suspicious Activity',
          'Enhanced security mode activated'
        );
        break;
      case 'medium':
        this.notificationService.showWarning(
          'Security Notice',
          activity.message
        );
        break;
    }
  }
}