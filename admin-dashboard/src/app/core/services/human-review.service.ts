import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface PendingReview {
  reviewId: string;
  errorEvent: {
    errorSignature: string;
    service: string;
    severity: string;
    message: string;
  };
  proposedFix: {
    description: string;
    filesModified: any;
    testCases: any[];
    confidenceScore: number;
  };
  submittedBy: string;
  submittedAt: Date;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'MODIFICATIONS_REQUESTED';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  requiresApproval: boolean;
  reviewDecisions: ReviewDecision[];
}

export interface ReviewDecision {
  reviewedBy: string;
  reviewedAt: Date;
  decision: 'APPROVED' | 'REJECTED' | 'MODIFICATIONS_REQUESTED';
  comments: string;
  modifications?: any;
  suggestedChanges?: any;
  improvementSuggestions?: string[];
}

export interface ReviewStatistics {
  currentPendingCount: number;
  pendingBySeverity: { [key: string]: number };
  last7Days: {
    approved: number;
    rejected: number;
    total: number;
    approvalRate: number;
  };
  oldestPendingHours?: number;
  oldestPendingReviewId?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
  [key: string]: any; // Allow additional properties
}

export interface ApprovalRequest {
  reviewedBy: string;
  comments?: string;
  modifications?: any;
}

export interface RejectionRequest {
  reviewedBy: string;
  rejectionReason: string;
  improvementSuggestions?: string[];
}

export interface ModificationRequest {
  reviewedBy: string;
  modificationRequest: string;
  suggestedChanges?: any;
}

@Injectable({
  providedIn: 'root'
})
export class HumanReviewService {
  private baseUrl = `${environment.intelligentMonitoringServiceUrl}/api/monitoring/human-review`;
  
  // State management
  private pendingReviewsSubject = new BehaviorSubject<PendingReview[]>([]);
  public pendingReviews$ = this.pendingReviewsSubject.asObservable();
  
  private statisticsSubject = new BehaviorSubject<ReviewStatistics | null>(null);
  public statistics$ = this.statisticsSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Get all pending reviews
   */
  getPendingReviews(): Observable<ApiResponse<PendingReview[]>> {
    return this.http.get<ApiResponse<PendingReview[]>>(`${this.baseUrl}/pending`)
      .pipe(
        tap(response => {
          if (response.success && response.reviews) {
            this.pendingReviewsSubject.next(response.reviews);
          }
        }),
        catchError(this.handleError<ApiResponse<PendingReview[]>>('getPendingReviews'))
      );
  }

  /**
   * Get review details by ID
   */
  getReviewDetails(reviewId: string): Observable<ApiResponse<PendingReview>> {
    return this.http.get<ApiResponse<PendingReview>>(`${this.baseUrl}/review/${reviewId}`)
      .pipe(
        catchError(this.handleError<ApiResponse<PendingReview>>('getReviewDetails'))
      );
  }

  /**
   * Approve a review
   */
  approveReview(reviewId: string, approvalData: ApprovalRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/review/${reviewId}/approve`, approvalData)
      .pipe(
        tap(response => {
          if (response.success) {
            this.refreshPendingReviews();
          }
        }),
        catchError(this.handleError<ApiResponse<any>>('approveReview'))
      );
  }

  /**
   * Reject a review
   */
  rejectReview(reviewId: string, rejectionData: RejectionRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/review/${reviewId}/reject`, rejectionData)
      .pipe(
        tap(response => {
          if (response.success) {
            this.refreshPendingReviews();
          }
        }),
        catchError(this.handleError<ApiResponse<any>>('rejectReview'))
      );
  }

  /**
   * Request modifications to a review
   */
  requestModifications(reviewId: string, modificationData: ModificationRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/review/${reviewId}/request-modifications`, modificationData)
      .pipe(
        tap(response => {
          if (response.success) {
            this.refreshPendingReviews();
          }
        }),
        catchError(this.handleError<ApiResponse<any>>('requestModifications'))
      );
  }

  /**
   * Get review history
   */
  getReviewHistory(days: number = 30): Observable<ApiResponse<PendingReview[]>> {
    return this.http.get<ApiResponse<PendingReview[]>>(`${this.baseUrl}/history?days=${days}`)
      .pipe(
        catchError(this.handleError<ApiResponse<PendingReview[]>>('getReviewHistory'))
      );
  }

  /**
   * Get review statistics
   */
  getStatistics(): Observable<ApiResponse<ReviewStatistics>> {
    return this.http.get<ApiResponse<ReviewStatistics>>(`${this.baseUrl}/statistics`)
      .pipe(
        tap(response => {
          if (response.success && response.statistics) {
            this.statisticsSubject.next(response.statistics);
          }
        }),
        catchError(this.handleError<ApiResponse<ReviewStatistics>>('getStatistics'))
      );
  }

  /**
   * Process timed-out reviews
   */
  processTimeouts(): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/process-timeouts`, {})
      .pipe(
        tap(response => {
          if (response.success) {
            this.refreshPendingReviews();
          }
        }),
        catchError(this.handleError<ApiResponse<any>>('processTimeouts'))
      );
  }

  /**
   * Get current pending reviews count
   */
  getPendingCount(): number {
    return this.pendingReviewsSubject.value.length;
  }

  /**
   * Get critical pending reviews
   */
  getCriticalPendingReviews(): PendingReview[] {
    return this.pendingReviewsSubject.value.filter(review => 
      review.severity === 'CRITICAL' && review.status === 'PENDING'
    );
  }

  /**
   * Check if there are any reviews requiring immediate attention
   */
  hasUrgentReviews(): boolean {
    const currentReviews = this.pendingReviewsSubject.value;
    return currentReviews.some(review => 
      review.severity === 'CRITICAL' || 
      (review.requiresApproval && this.getReviewAgeHours(review) > 24)
    );
  }

  /**
   * Get reviews that have been pending for more than specified hours
   */
  getOverdueReviews(hours: number = 24): PendingReview[] {
    return this.pendingReviewsSubject.value.filter(review => 
      this.getReviewAgeHours(review) > hours
    );
  }

  /**
   * Refresh pending reviews data
   */
  private refreshPendingReviews(): void {
    this.getPendingReviews().subscribe();
  }

  /**
   * Calculate review age in hours
   */
  private getReviewAgeHours(review: PendingReview): number {
    const now = new Date();
    const submitted = new Date(review.submittedAt);
    return (now.getTime() - submitted.getTime()) / (1000 * 60 * 60);
  }

  /**
   * Handle HTTP errors
   */
  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      
      // Return a safe result
      const result = {
        success: false,
        message: error.message || `${operation} failed`,
        error: error
      } as any;
      
      return new Observable(observer => {
        observer.next(result);
        observer.complete();
      });
    };
  }

  /**
   * Build HTTP headers with authentication
   */
  private buildHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    // Add authentication token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  /**
   * Validate review decision data
   */
  private validateApprovalRequest(data: ApprovalRequest): boolean {
    return !!(data.reviewedBy && data.reviewedBy.trim());
  }

  private validateRejectionRequest(data: RejectionRequest): boolean {
    return !!(data.reviewedBy && data.reviewedBy.trim() && 
              data.rejectionReason && data.rejectionReason.trim());
  }

  private validateModificationRequest(data: ModificationRequest): boolean {
    return !!(data.reviewedBy && data.reviewedBy.trim() && 
              data.modificationRequest && data.modificationRequest.trim());
  }

  /**
   * Get review priority score (higher = more urgent)
   */
  getReviewPriorityScore(review: PendingReview): number {
    let score = 0;
    
    // Severity scoring
    const severityScores = {
      'CRITICAL': 100,
      'HIGH': 75,
      'MEDIUM': 50,
      'LOW': 25
    };
    score += severityScores[review.severity] || 0;
    
    // Age scoring (1 point per hour)
    score += this.getReviewAgeHours(review);
    
    // Approval requirement scoring
    if (review.requiresApproval) {
      score += 50;
    }
    
    // Service criticality (basic heuristic)
    if (review.errorEvent.service.includes('gateway') || 
        review.errorEvent.service.includes('auth')) {
      score += 25;
    }
    
    return score;
  }

  /**
   * Sort reviews by priority
   */
  sortReviewsByPriority(reviews: PendingReview[]): PendingReview[] {
    return reviews.sort((a, b) => 
      this.getReviewPriorityScore(b) - this.getReviewPriorityScore(a)
    );
  }
}