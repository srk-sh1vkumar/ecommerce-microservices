import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, timer, interval } from 'rxjs';
import { map, catchError, tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import * as CryptoJS from 'crypto-js';
import * as zxcvbn from 'zxcvbn';

import { ApiService } from './api.service';
import { AuditService } from './audit.service';
import { TelemetryService } from './telemetry.service';
import { NotificationService } from './notification.service';

export interface SecurityAlert {
  id: string;
  type: 'suspicious_login' | 'multiple_failures' | 'unusual_activity' | 'security_breach' | 'policy_violation';
  severity: 'low' | 'medium' | 'high' | 'critical';
  message: string;
  timestamp: Date;
  source: string;
  details: any;
  dismissed: boolean;
}

export interface SuspiciousActivity {
  type: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  message: string;
  details: any;
  timestamp: Date;
}

export interface SecurityCheck {
  id: string;
  type: string;
  passed: boolean;
  message: string;
  timestamp: Date;
  recommendations?: string[];
}

export interface PasswordStrength {
  score: number; // 0-4
  feedback: {
    warning: string;
    suggestions: string[];
  };
  crackTime: string;
}

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  private readonly SECURITY_KEY = 'vendor_security_context';
  private readonly MAX_LOGIN_ATTEMPTS = 3;
  private readonly LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes
  
  private securityAlertsSubject = new BehaviorSubject<SecurityAlert[]>([]);
  private suspiciousActivitySubject = new BehaviorSubject<SuspiciousActivity[]>([]);
  private securityLevel = 'standard';
  private deviceFingerprint = '';
  private loginAttempts = 0;
  private lastActivityTime = Date.now();
  private locationHistory: any[] = [];
  private isInitialized = false;

  constructor(
    private apiService: ApiService,
    private auditService: AuditService,
    private telemetryService: TelemetryService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  initialize(): void {
    if (this.isInitialized) return;
    
    this.generateDeviceFingerprint();
    this.setupSecurityMonitoring();
    this.checkBrowserSecurity();
    this.initializeCSP();
    this.monitorUserActivity();
    
    this.isInitialized = true;
    
    this.auditService.logEvent('security_service_initialized', {
      deviceFingerprint: this.deviceFingerprint,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString()
    });
  }

  // Device Fingerprinting
  private generateDeviceFingerprint(): void {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.textBaseline = 'top';
      ctx.font = '14px Arial';
      ctx.fillText('Device fingerprint test', 2, 2);
    }
    
    const fingerprint = CryptoJS.SHA256([
      navigator.userAgent,
      navigator.language,
      navigator.platform,
      screen.width + 'x' + screen.height,
      screen.colorDepth,
      new Date().getTimezoneOffset(),
      canvas.toDataURL(),
      navigator.hardwareConcurrency || 'unknown',
      navigator.deviceMemory || 'unknown'
    ].join('|')).toString();
    
    this.deviceFingerprint = fingerprint;
  }

  getDeviceFingerprint(): string {
    return this.deviceFingerprint;
  }

  // Security Level Management
  setSecurityLevel(level: 'standard' | 'high' | 'maximum'): void {
    const previousLevel = this.securityLevel;
    this.securityLevel = level;
    
    this.auditService.logEvent('security_level_changed', {
      previousLevel,
      newLevel: level,
      timestamp: new Date().toISOString()
    });
    
    // Apply security level specific configurations
    this.applySecurityLevelConfiguration(level);
  }

  private applySecurityLevelConfiguration(level: string): void {
    switch (level) {
      case 'maximum':
        this.enableEnhancedCSP();
        this.enableStrictSessionManagement();
        this.enableAdvancedThreatDetection();
        break;
      case 'high':
        this.enableStandardCSP();
        this.enableStrictSessionManagement();
        break;
      default:
        this.enableBasicCSP();
    }
  }

  // Content Security Policy Management
  private initializeCSP(): void {
    const cspMeta = document.createElement('meta');
    cspMeta.httpEquiv = 'Content-Security-Policy';
    cspMeta.content = this.getCSPPolicy();
    document.head.appendChild(cspMeta);
  }

  private getCSPPolicy(): string {
    const basePolicy = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // Angular requires unsafe-inline/eval
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      "img-src 'self' data: https:",
      "font-src 'self' data: https://fonts.gstatic.com",
      "connect-src 'self' /api/",
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'"
    ];

    if (this.securityLevel === 'maximum') {
      return basePolicy.concat([
        "upgrade-insecure-requests",
        "block-all-mixed-content"
      ]).join('; ');
    }

    return basePolicy.join('; ');
  }

  private enableBasicCSP(): void {
    this.updateCSP(this.getCSPPolicy());
  }

  private enableStandardCSP(): void {
    this.updateCSP(this.getCSPPolicy());
  }

  private enableEnhancedCSP(): void {
    this.updateCSP(this.getCSPPolicy());
  }

  private updateCSP(policy: string): void {
    const existingMeta = document.querySelector('meta[http-equiv="Content-Security-Policy"]');
    if (existingMeta) {
      existingMeta.setAttribute('content', policy);
    }
  }

  // Password Security
  checkPasswordStrength(password: string): PasswordStrength {
    const result = zxcvbn(password);
    
    return {
      score: result.score,
      feedback: {
        warning: result.feedback.warning || '',
        suggestions: result.feedback.suggestions || []
      },
      crackTime: result.crack_times_display.offline_slow_hashing_1e4_per_second || 'unknown'
    };
  }

  generateSecurePassword(length: number = 16): string {
    const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?';
    let password = '';
    
    for (let i = 0; i < length; i++) {
      const randomIndex = Math.floor(Math.random() * charset.length);
      password += charset[randomIndex];
    }
    
    return password;
  }

  // Login Attempt Monitoring
  monitorLoginAttempts(): void {
    const storedAttempts = localStorage.getItem('login_attempts');
    const storedTimestamp = localStorage.getItem('last_attempt_time');
    
    if (storedAttempts && storedTimestamp) {
      const lastAttemptTime = parseInt(storedTimestamp, 10);
      const timeSinceLastAttempt = Date.now() - lastAttemptTime;
      
      if (timeSinceLastAttempt < this.LOCKOUT_DURATION) {
        this.loginAttempts = parseInt(storedAttempts, 10);
        
        if (this.loginAttempts >= this.MAX_LOGIN_ATTEMPTS) {
          this.triggerAccountLockout();
        }
      } else {
        this.resetLoginAttempts();
      }
    }
  }

  recordFailedLoginAttempt(): void {
    this.loginAttempts++;
    localStorage.setItem('login_attempts', this.loginAttempts.toString());
    localStorage.setItem('last_attempt_time', Date.now().toString());
    
    this.auditService.logEvent('failed_login_attempt', {
      attempts: this.loginAttempts,
      deviceFingerprint: this.deviceFingerprint,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      ipAddress: 'client-side' // Would be filled by server
    });
    
    if (this.loginAttempts >= this.MAX_LOGIN_ATTEMPTS) {
      this.triggerAccountLockout();
    } else {
      this.addSecurityAlert({
        id: `failed-login-${Date.now()}`,
        type: 'multiple_failures',
        severity: 'medium',
        message: `${this.loginAttempts} failed login attempts detected`,
        timestamp: new Date(),
        source: 'login_monitor',
        details: { attempts: this.loginAttempts },
        dismissed: false
      });
    }
  }

  private triggerAccountLockout(): void {
    this.addSecurityAlert({
      id: `account-lockout-${Date.now()}`,
      type: 'multiple_failures',
      severity: 'high',
      message: 'Account temporarily locked due to multiple failed login attempts',
      timestamp: new Date(),
      source: 'login_monitor',
      details: { lockoutDuration: this.LOCKOUT_DURATION },
      dismissed: false
    });
    
    this.auditService.logEvent('account_lockout', {
      attempts: this.loginAttempts,
      lockoutDuration: this.LOCKOUT_DURATION,
      timestamp: new Date().toISOString()
    });
  }

  resetLoginAttempts(): void {
    this.loginAttempts = 0;
    localStorage.removeItem('login_attempts');
    localStorage.removeItem('last_attempt_time');
  }

  isAccountLocked(): boolean {
    const storedAttempts = localStorage.getItem('login_attempts');
    const storedTimestamp = localStorage.getItem('last_attempt_time');
    
    if (storedAttempts && storedTimestamp) {
      const attempts = parseInt(storedAttempts, 10);
      const lastAttemptTime = parseInt(storedTimestamp, 10);
      const timeSinceLastAttempt = Date.now() - lastAttemptTime;
      
      return attempts >= this.MAX_LOGIN_ATTEMPTS && timeSinceLastAttempt < this.LOCKOUT_DURATION;
    }
    
    return false;
  }

  // Activity Monitoring
  private monitorUserActivity(): void {
    // Track mouse movements
    document.addEventListener('mousemove', () => {
      this.updateLastActivity();
    });
    
    // Track keyboard activity
    document.addEventListener('keypress', () => {
      this.updateLastActivity();
    });
    
    // Track clicks
    document.addEventListener('click', () => {
      this.updateLastActivity();
    });
    
    // Monitor for rapid-fire actions (potential bot behavior)
    this.monitorRapidActions();
  }

  private updateLastActivity(): void {
    this.lastActivityTime = Date.now();
  }

  private monitorRapidActions(): void {
    let actionCount = 0;
    const timeWindow = 5000; // 5 seconds
    
    const resetCounter = () => {
      actionCount = 0;
    };
    
    document.addEventListener('click', () => {
      actionCount++;
      
      if (actionCount > 20) { // More than 20 clicks in 5 seconds
        this.addSuspiciousActivity({
          type: 'rapid_clicking',
          severity: 'medium',
          message: 'Unusual rapid clicking pattern detected',
          details: { clickCount: actionCount, timeWindow },
          timestamp: new Date()
        });
      }
      
      setTimeout(resetCounter, timeWindow);
    });
  }

  // Browser Security Checks
  checkBrowserSecurity(): Observable<SecurityCheck[]> {
    const checks: SecurityCheck[] = [];
    
    // Check for HTTPS
    checks.push({
      id: 'https-check',
      type: 'connection_security',
      passed: location.protocol === 'https:',
      message: location.protocol === 'https:' ? 'Connection is secure (HTTPS)' : 'Connection is not secure (HTTP)',
      timestamp: new Date(),
      recommendations: location.protocol !== 'https:' ? ['Use HTTPS for secure communication'] : undefined
    });
    
    // Check for modern browser features
    const hasWebCrypto = typeof window.crypto !== 'undefined' && typeof window.crypto.subtle !== 'undefined';
    checks.push({
      id: 'webcrypto-check',
      type: 'browser_capability',
      passed: hasWebCrypto,
      message: hasWebCrypto ? 'Web Crypto API available' : 'Web Crypto API not available',
      timestamp: new Date(),
      recommendations: !hasWebCrypto ? ['Update to a modern browser with Web Crypto API support'] : undefined
    });
    
    // Check for secure context
    const isSecureContext = window.isSecureContext;
    checks.push({
      id: 'secure-context-check',
      type: 'secure_context',
      passed: isSecureContext,
      message: isSecureContext ? 'Running in secure context' : 'Not running in secure context',
      timestamp: new Date(),
      recommendations: !isSecureContext ? ['Ensure application runs in a secure context (HTTPS)'] : undefined
    });
    
    return of(checks);
  }

  // Threat Detection
  private enableAdvancedThreatDetection(): void {
    // Monitor for suspicious patterns
    this.detectUnusualBehavior();
    this.monitorNetworkRequests();
    this.detectPotentialXSS();
  }

  private detectUnusualBehavior(): void {
    // Monitor for unusual navigation patterns
    let pageViews = 0;
    const timeWindow = 60000; // 1 minute
    
    setInterval(() => {
      if (pageViews > 50) { // More than 50 page views per minute
        this.addSuspiciousActivity({
          type: 'unusual_navigation',
          severity: 'high',
          message: 'Unusual navigation pattern detected',
          details: { pageViewsPerMinute: pageViews },
          timestamp: new Date()
        });
      }
      pageViews = 0;
    }, timeWindow);
    
    // Track page views
    this.router.events.subscribe(() => {
      pageViews++;
    });
  }

  private monitorNetworkRequests(): void {
    // Override fetch to monitor outgoing requests
    const originalFetch = window.fetch;
    
    window.fetch = function(...args: any[]) {
      const url = args[0];
      
      // Check for suspicious external requests
      if (typeof url === 'string' && !url.startsWith('/') && !url.includes(window.location.hostname)) {
        console.warn('External request detected:', url);
        // Could log this as suspicious activity
      }
      
      return originalFetch.apply(this, args);
    };
  }

  private detectPotentialXSS(): void {
    // Monitor for script injections
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === 'childList') {
          mutation.addedNodes.forEach((node) => {
            if (node.nodeName === 'SCRIPT') {
              this.addSuspiciousActivity({
                type: 'script_injection',
                severity: 'critical',
                message: 'Potential script injection detected',
                details: { scriptContent: (node as HTMLScriptElement).textContent },
                timestamp: new Date()
              });
            }
          });
        }
      });
    });
    
    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
  }

  // Security Alerts Management
  getSecurityAlerts(): Observable<SecurityAlert[]> {
    return this.securityAlertsSubject.asObservable();
  }

  addSecurityAlert(alert: SecurityAlert): void {
    const currentAlerts = this.securityAlertsSubject.value;
    this.securityAlertsSubject.next([...currentAlerts, alert]);
    
    this.auditService.logEvent('security_alert_created', {
      alertId: alert.id,
      type: alert.type,
      severity: alert.severity,
      timestamp: alert.timestamp.toISOString()
    });
  }

  dismissSecurityAlert(alertId: string): void {
    const currentAlerts = this.securityAlertsSubject.value;
    const updatedAlerts = currentAlerts.map(alert => 
      alert.id === alertId ? { ...alert, dismissed: true } : alert
    );
    this.securityAlertsSubject.next(updatedAlerts);
  }

  // Suspicious Activity Management
  getSuspiciousActivity(): Observable<SuspiciousActivity[]> {
    return this.suspiciousActivitySubject.asObservable();
  }

  addSuspiciousActivity(activity: SuspiciousActivity): void {
    const currentActivities = this.suspiciousActivitySubject.value;
    this.suspiciousActivitySubject.next([...currentActivities, activity]);
    
    this.auditService.logEvent('suspicious_activity_detected', {
      type: activity.type,
      severity: activity.severity,
      details: activity.details,
      timestamp: activity.timestamp.toISOString()
    });
  }

  // Enhanced Security Operations
  performSecurityCheck(): Observable<SecurityCheck[]> {
    return this.checkBrowserSecurity().pipe(
      tap(checks => {
        const failedChecks = checks.filter(check => !check.passed);
        if (failedChecks.length > 0) {
          this.addSecurityAlert({
            id: `security-check-${Date.now()}`,
            type: 'policy_violation',
            severity: 'medium',
            message: `${failedChecks.length} security checks failed`,
            timestamp: new Date(),
            source: 'security_audit',
            details: { failedChecks },
            dismissed: false
          });
        }
      })
    );
  }

  performEnhancedSecurityCheck(): void {
    // Additional security checks for sensitive operations
    this.verifyDeviceIntegrity();
    this.checkForSuspiciousPatterns();
    this.validateSessionIntegrity();
  }

  private verifyDeviceIntegrity(): void {
    const currentFingerprint = this.generateDeviceFingerprint();
    if (currentFingerprint !== this.deviceFingerprint) {
      this.addSuspiciousActivity({
        type: 'device_change',
        severity: 'high',
        message: 'Device fingerprint has changed',
        details: { 
          originalFingerprint: this.deviceFingerprint,
          currentFingerprint 
        },
        timestamp: new Date()
      });
    }
  }

  private checkForSuspiciousPatterns(): void {
    // Check for rapid requests
    // Check for unusual user agent changes
    // Check for time-based anomalies
  }

  private validateSessionIntegrity(): void {
    // Validate session tokens
    // Check for session hijacking indicators
    // Verify request patterns
  }

  requireReAuthentication(): void {
    this.auditService.logEvent('re_authentication_required', {
      reason: 'high_security_operation',
      timestamp: new Date().toISOString()
    });
    
    // Redirect to re-authentication flow
    this.router.navigate(['/re-authenticate']);
  }

  // Session Management
  private enableStrictSessionManagement(): void {
    // Implement stricter session controls
    this.monitorSessionAnomalies();
  }

  private monitorSessionAnomalies(): void {
    // Monitor for session anomalies like:
    // - Multiple concurrent sessions
    // - Geolocation changes
    // - User agent changes
    // - Timing anomalies
  }

  // Data Encryption/Decryption
  encryptSensitiveData(data: string, key?: string): string {
    const encryptionKey = key || this.getEncryptionKey();
    return CryptoJS.AES.encrypt(data, encryptionKey).toString();
  }

  decryptSensitiveData(encryptedData: string, key?: string): string {
    const encryptionKey = key || this.getEncryptionKey();
    const bytes = CryptoJS.AES.decrypt(encryptedData, encryptionKey);
    return bytes.toString(CryptoJS.enc.Utf8);
  }

  private getEncryptionKey(): string {
    // In production, this should come from a secure key management system
    return this.deviceFingerprint + '-' + new Date().getFullYear();
  }

  // Setup Security Monitoring
  private setupSecurityMonitoring(): void {
    // Monitor for console manipulation
    this.preventConsoleManipulation();
    
    // Monitor for developer tools
    this.detectDeveloperTools();
    
    // Monitor for frame manipulation
    this.preventFrameManipulation();
  }

  private preventConsoleManipulation(): void {
    // Disable right-click in production
    if (this.securityLevel === 'maximum') {
      document.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        return false;
      });
      
      // Disable F12, Ctrl+Shift+I, etc.
      document.addEventListener('keydown', (e) => {
        if (e.key === 'F12' || 
            (e.ctrlKey && e.shiftKey && e.key === 'I') ||
            (e.ctrlKey && e.shiftKey && e.key === 'C') ||
            (e.ctrlKey && e.key === 'U')) {
          e.preventDefault();
          return false;
        }
      });
    }
  }

  private detectDeveloperTools(): void {
    // Basic developer tools detection
    let devtools = false;
    
    setInterval(() => {
      if (window.outerHeight - window.innerHeight > 200 || 
          window.outerWidth - window.innerWidth > 200) {
        if (!devtools) {
          devtools = true;
          this.addSuspiciousActivity({
            type: 'developer_tools_open',
            severity: 'medium',
            message: 'Developer tools may be open',
            details: { 
              windowDimensions: {
                outer: { width: window.outerWidth, height: window.outerHeight },
                inner: { width: window.innerWidth, height: window.innerHeight }
              }
            },
            timestamp: new Date()
          });
        }
      } else {
        devtools = false;
      }
    }, 1000);
  }

  private preventFrameManipulation(): void {
    // Prevent the page from being embedded in frames
    if (window.top !== window.self) {
      this.addSuspiciousActivity({
        type: 'frame_embedding',
        severity: 'high',
        message: 'Application is being embedded in a frame',
        details: { 
          topLocation: window.top?.location.href,
          selfLocation: window.self.location.href
        },
        timestamp: new Date()
      });
      
      // Break out of frame
      window.top!.location = window.self.location;
    }
  }

  // Cleanup
  cleanup(): void {
    this.securityAlertsSubject.complete();
    this.suspiciousActivitySubject.complete();
  }
}