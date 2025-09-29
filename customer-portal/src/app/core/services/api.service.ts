import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, timeout } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { TelemetryService } from './telemetry.service';

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
  errors?: string[];
  meta?: {
    page?: number;
    limit?: number;
    total?: number;
    totalPages?: number;
  };
}

export interface ApiError {
  status: number;
  statusText: string;
  message: string;
  errors?: string[];
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiUrl;
  private readonly defaultHeaders = new HttpHeaders({
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  });

  constructor(
    private http: HttpClient,
    private telemetryService: TelemetryService
  ) {}

  // GET request
  get<T>(endpoint: string, params?: { [key: string]: any }): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const httpParams = this.buildHttpParams(params);
    
    const startTime = performance.now();
    
    return this.http.get<ApiResponse<T>>(url, {
      headers: this.defaultHeaders,
      params: httpParams
    }).pipe(
      timeout(environment.apiTimeout),
      retry(environment.apiRetryAttempts),
      catchError(error => this.handleError(error, 'GET', endpoint, startTime))
    );
  }

  // POST request
  post<T>(endpoint: string, data: any, options?: { headers?: HttpHeaders }): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const headers = options?.headers || this.defaultHeaders;
    
    const startTime = performance.now();
    
    return this.http.post<ApiResponse<T>>(url, data, {
      headers
    }).pipe(
      timeout(environment.apiTimeout),
      catchError(error => this.handleError(error, 'POST', endpoint, startTime))
    );
  }

  // PUT request
  put<T>(endpoint: string, data: any): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const startTime = performance.now();
    
    return this.http.put<ApiResponse<T>>(url, data, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(environment.apiTimeout),
      catchError(error => this.handleError(error, 'PUT', endpoint, startTime))
    );
  }

  // DELETE request
  delete<T>(endpoint: string): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const startTime = performance.now();
    
    return this.http.delete<ApiResponse<T>>(url, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(environment.apiTimeout),
      catchError(error => this.handleError(error, 'DELETE', endpoint, startTime))
    );
  }

  // PATCH request
  patch<T>(endpoint: string, data: any): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const startTime = performance.now();
    
    return this.http.patch<ApiResponse<T>>(url, data, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(environment.apiTimeout),
      catchError(error => this.handleError(error, 'PATCH', endpoint, startTime))
    );
  }

  // File upload
  uploadFile<T>(endpoint: string, file: File, additionalData?: { [key: string]: any }): Observable<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const formData = new FormData();
    
    formData.append('file', file);
    
    if (additionalData) {
      Object.keys(additionalData).forEach(key => {
        formData.append(key, additionalData[key]);
      });
    }
    
    const startTime = performance.now();
    
    return this.http.post<ApiResponse<T>>(url, formData).pipe(
      timeout(environment.fileUploadTimeout),
      catchError(error => this.handleError(error, 'UPLOAD', endpoint, startTime))
    );
  }

  // Health check
  healthCheck(): Observable<any> {
    return this.http.get(`${this.baseUrl}/health`, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(5000),
      catchError(error => {
        console.error('Health check failed:', error);
        return throwError(() => error);
      })
    );
  }

  // Build HTTP params from object
  private buildHttpParams(params?: { [key: string]: any }): HttpParams {
    let httpParams = new HttpParams();
    
    if (params) {
      Object.keys(params).forEach(key => {
        const value = params[key];
        if (value !== null && value !== undefined) {
          if (Array.isArray(value)) {
            value.forEach(item => {
              httpParams = httpParams.append(key, item.toString());
            });
          } else {
            httpParams = httpParams.set(key, value.toString());
          }
        }
      });
    }
    
    return httpParams;
  }

  // Error handler
  private handleError(error: any, method: string, endpoint: string, startTime: number): Observable<never> {
    const duration = performance.now() - startTime;
    
    const apiError: ApiError = {
      status: error.status || 0,
      statusText: error.statusText || 'Unknown Error',
      message: error.error?.message || error.message || 'An unexpected error occurred',
      errors: error.error?.errors || [],
      timestamp: new Date().toISOString()
    };

    // Track API error for monitoring
    this.telemetryService.trackError(new Error(`API ${method} ${endpoint} failed`), {
      apiEndpoint: endpoint,
      httpMethod: method,
      httpStatus: apiError.status,
      duration: duration,
      errorMessage: apiError.message,
      userAgent: navigator.userAgent
    });

    // Track API performance metrics
    this.telemetryService.trackEvent('api_request_completed', {
      endpoint,
      method,
      status: apiError.status,
      duration: duration,
      success: false
    });

    console.error(`API ${method} Error:`, {
      endpoint,
      duration: `${duration.toFixed(2)}ms`,
      error: apiError
    });

    return throwError(() => apiError);
  }

  // Track successful API calls
  trackApiSuccess(method: string, endpoint: string, startTime: number, responseSize?: number): void {
    const duration = performance.now() - startTime;
    
    this.telemetryService.trackEvent('api_request_completed', {
      endpoint,
      method,
      duration: duration,
      success: true,
      responseSize: responseSize || 0
    });
  }
}