import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { map, catchError, tap, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';

import { ApiService } from './api.service';
import { TelemetryService } from './telemetry.service';
import { NotificationService } from './notification.service';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  avatar?: string;
  preferences?: {
    theme: 'light' | 'dark';
    language: string;
    notifications: boolean;
  };
  addresses?: Address[];
  createdAt: string;
  lastLoginAt?: string;
}

export interface Address {
  id: string;
  type: 'shipping' | 'billing';
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'ecommerce_token';
  private readonly REFRESH_TOKEN_KEY = 'ecommerce_refresh_token';
  private readonly USER_KEY = 'ecommerce_user';
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  
  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public isLoading$ = this.isLoadingSubject.asObservable();

  constructor(
    private apiService: ApiService,
    private router: Router,
    private telemetryService: TelemetryService,
    private notificationService: NotificationService
  ) {}

  // Initialize authentication state
  initializeAuth(): void {
    const token = this.getToken();
    const user = this.getStoredUser();
    
    if (token && user) {
      this.currentUserSubject.next(user);
      this.isAuthenticatedSubject.next(true);
      
      // Validate token with backend
      this.validateToken().subscribe({
        next: (isValid) => {
          if (!isValid) {
            this.logout();
          }
        },
        error: () => {
          this.logout();
        }
      });
    }
  }

  // Login
  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.isLoadingSubject.next(true);
    
    return this.apiService.post<AuthResponse>('/auth/login', credentials).pipe(
      map(response => response.data),
      tap(authResponse => {
        this.handleSuccessfulAuth(authResponse);
        
        // Track login event
        this.telemetryService.trackEvent('user_login', {
          userId: authResponse.user.id,
          email: authResponse.user.email,
          rememberMe: credentials.rememberMe || false,
          timestamp: new Date().toISOString()
        });
        
        this.notificationService.showSuccess(
          'Welcome back!', 
          `Hello ${authResponse.user.firstName}, you're now logged in.`
        );
      }),
      catchError(error => {
        this.telemetryService.trackError(new Error('Login failed'), {
          email: credentials.email,
          errorMessage: error.message
        });
        
        this.notificationService.showError(
          'Login Failed',
          error.message || 'Invalid email or password. Please try again.'
        );
        
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoadingSubject.next(false);
      })
    );
  }

  // Register
  register(userData: RegisterRequest): Observable<AuthResponse> {
    this.isLoadingSubject.next(true);
    
    return this.apiService.post<AuthResponse>('/auth/register', userData).pipe(
      map(response => response.data),
      tap(authResponse => {
        this.handleSuccessfulAuth(authResponse);
        
        // Track registration event
        this.telemetryService.trackEvent('user_register', {
          userId: authResponse.user.id,
          email: authResponse.user.email,
          timestamp: new Date().toISOString()
        });
        
        this.notificationService.showSuccess(
          'Account Created!',
          `Welcome ${authResponse.user.firstName}! Your account has been created successfully.`
        );
      }),
      catchError(error => {
        this.telemetryService.trackError(new Error('Registration failed'), {
          email: userData.email,
          errorMessage: error.message
        });
        
        this.notificationService.showError(
          'Registration Failed',
          error.message || 'Failed to create account. Please try again.'
        );
        
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoadingSubject.next(false);
      })
    );
  }

  // Logout
  logout(showNotification: boolean = true): void {
    const currentUser = this.currentUserSubject.value;
    
    // Track logout event
    if (currentUser) {
      this.telemetryService.trackEvent('user_logout', {
        userId: currentUser.id,
        email: currentUser.email,
        timestamp: new Date().toISOString()
      });
    }
    
    // Clear local storage
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    
    // Update state
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    
    // Navigate to login
    this.router.navigate(['/login']);
    
    if (showNotification) {
      this.notificationService.showInfo(
        'Logged Out',
        'You have been successfully logged out.'
      );
    }
  }

  // Refresh token
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem(this.REFRESH_TOKEN_KEY);
    
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }
    
    return this.apiService.post<AuthResponse>('/auth/refresh', { refreshToken }).pipe(
      map(response => response.data),
      tap(authResponse => {
        this.handleSuccessfulAuth(authResponse);
      }),
      catchError(error => {
        this.logout(false);
        return throwError(() => error);
      })
    );
  }

  // Update user profile
  updateProfile(userData: Partial<User>): Observable<User> {
    this.isLoadingSubject.next(true);
    
    return this.apiService.put<User>('/auth/profile', userData).pipe(
      map(response => response.data),
      tap(user => {
        this.currentUserSubject.next(user);
        this.storeUser(user);
        
        this.telemetryService.trackEvent('user_profile_updated', {
          userId: user.id,
          timestamp: new Date().toISOString()
        });
        
        this.notificationService.showSuccess(
          'Profile Updated',
          'Your profile has been updated successfully.'
        );
      }),
      catchError(error => {
        this.notificationService.showError(
          'Update Failed',
          error.message || 'Failed to update profile. Please try again.'
        );
        
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoadingSubject.next(false);
      })
    );
  }

  // Change password
  changePassword(oldPassword: string, newPassword: string): Observable<void> {
    this.isLoadingSubject.next(true);
    
    return this.apiService.post<void>('/auth/change-password', {
      oldPassword,
      newPassword
    }).pipe(
      map(response => response.data),
      tap(() => {
        this.telemetryService.trackEvent('user_password_changed', {
          userId: this.currentUserSubject.value?.id,
          timestamp: new Date().toISOString()
        });
        
        this.notificationService.showSuccess(
          'Password Changed',
          'Your password has been updated successfully.'
        );
      }),
      catchError(error => {
        this.notificationService.showError(
          'Password Change Failed',
          error.message || 'Failed to change password. Please try again.'
        );
        
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoadingSubject.next(false);
      })
    );
  }

  // Reset password
  resetPassword(email: string): Observable<void> {
    return this.apiService.post<void>('/auth/reset-password', { email }).pipe(
      map(response => response.data),
      tap(() => {
        this.notificationService.showSuccess(
          'Reset Email Sent',
          'Password reset instructions have been sent to your email.'
        );
      }),
      catchError(error => {
        this.notificationService.showError(
          'Reset Failed',
          error.message || 'Failed to send reset email. Please try again.'
        );
        
        return throwError(() => error);
      })
    );
  }

  // Validate token
  private validateToken(): Observable<boolean> {
    return this.apiService.get<boolean>('/auth/validate').pipe(
      map(response => response.data),
      catchError(() => of(false))
    );
  }

  // Handle successful authentication
  private handleSuccessfulAuth(authResponse: AuthResponse): void {
    // Store tokens and user data
    localStorage.setItem(this.TOKEN_KEY, authResponse.token);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, authResponse.refreshToken);
    this.storeUser(authResponse.user);
    
    // Update state
    this.currentUserSubject.next(authResponse.user);
    this.isAuthenticatedSubject.next(true);
    
    // Set up token refresh timer
    this.setupTokenRefresh(authResponse.expiresIn);
  }

  // Set up automatic token refresh
  private setupTokenRefresh(expiresIn: number): void {
    // Refresh token 5 minutes before expiration
    const refreshTime = (expiresIn - 300) * 1000;
    
    setTimeout(() => {
      if (this.isAuthenticatedSubject.value) {
        this.refreshToken().subscribe();
      }
    }, refreshTime);
  }

  // Get stored token
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // Get stored user
  private getStoredUser(): User | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  // Store user data
  private storeUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  // Get current user
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }
}