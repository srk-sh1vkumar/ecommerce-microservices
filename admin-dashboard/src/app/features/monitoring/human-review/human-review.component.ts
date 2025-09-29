import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Subject, takeUntil, interval } from 'rxjs';

import { HumanReviewService } from '../../../core/services/human-review.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { TelemetryService } from '../../../core/services/telemetry.service';

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

@Component({
  selector: 'app-human-review',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FormsModule,
    ReactiveFormsModule
  ],
  template: `
    <div class="human-review-container">
      <!-- Header -->
      <div class="header">
        <h2>Human Review Workflow</h2>
        <div class="header-actions">
          <button mat-raised-button color="primary" (click)="refreshData()">
            <mat-icon>refresh</mat-icon>
            Refresh
          </button>
          <button mat-raised-button (click)="processTimeouts()">
            <mat-icon>schedule</mat-icon>
            Process Timeouts
          </button>
        </div>
      </div>

      <!-- Statistics Overview -->
      <div class="statistics-overview">
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ statistics?.currentPendingCount || 0 }}</div>
            <div class="stat-label">Pending Reviews</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-value">{{ statistics?.last7Days?.approvalRate | number:'1.0-1' }}%</div>
            <div class="stat-label">Approval Rate (7d)</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card critical" *ngIf="statistics?.pendingBySeverity?.CRITICAL">
          <mat-card-content>
            <div class="stat-value">{{ statistics.pendingBySeverity.CRITICAL }}</div>
            <div class="stat-label">Critical Pending</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card warning" *ngIf="statistics?.oldestPendingHours && statistics.oldestPendingHours > 24">
          <mat-card-content>
            <div class="stat-value">{{ statistics.oldestPendingHours }}h</div>
            <div class="stat-label">Oldest Pending</div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Review Tabs -->
      <mat-tab-group class="review-tabs">
        
        <!-- Pending Reviews Tab -->
        <mat-tab label="Pending Reviews" [badgeContent]="pendingReviews.length">
          <div class="tab-content">
            <mat-card *ngIf="pendingReviews.length === 0" class="no-data-card">
              <mat-card-content>
                <mat-icon>check_circle</mat-icon>
                <h3>No Pending Reviews</h3>
                <p>All automated fixes have been reviewed!</p>
              </mat-card-content>
            </mat-card>

            <div class="reviews-list" *ngIf="pendingReviews.length > 0">
              <mat-card class="review-card" *ngFor="let review of pendingReviews" [class]="getReviewCardClass(review)">
                <mat-card-header>
                  <mat-card-title class="review-title">
                    <mat-chip [class]="getSeverityClass(review.severity)">
                      {{ review.severity }}
                    </mat-chip>
                    <span class="error-signature">{{ review.errorEvent.errorSignature }}</span>
                  </mat-card-title>
                  <mat-card-subtitle>
                    Service: {{ review.errorEvent.service }} | 
                    Submitted: {{ review.submittedAt | date:'short' }} | 
                    ID: {{ review.reviewId }}
                  </mat-card-subtitle>
                </mat-card-header>

                <mat-card-content>
                  <div class="fix-summary">
                    <h4>Proposed Fix</h4>
                    <p>{{ review.proposedFix.description }}</p>
                    
                    <div class="fix-details">
                      <div class="detail-item">
                        <mat-icon>code</mat-icon>
                        <span>{{ getFileCount(review.proposedFix.filesModified) }} files modified</span>
                      </div>
                      <div class="detail-item">
                        <mat-icon>verified</mat-icon>
                        <span>{{ review.proposedFix.testCases?.length || 0 }} test cases</span>
                      </div>
                      <div class="detail-item">
                        <mat-icon>psychology</mat-icon>
                        <span>{{ review.proposedFix.confidenceScore }}% confidence</span>
                      </div>
                    </div>

                    <div class="approval-required" *ngIf="review.requiresApproval">
                      <mat-icon class="warning-icon">warning</mat-icon>
                      <span>Human approval required</span>
                    </div>
                  </div>
                </mat-card-content>

                <mat-card-actions>
                  <button mat-button (click)="viewReviewDetails(review)">
                    <mat-icon>visibility</mat-icon>
                    View Details
                  </button>
                  <button mat-raised-button color="primary" (click)="approveReview(review)">
                    <mat-icon>check</mat-icon>
                    Approve
                  </button>
                  <button mat-raised-button color="warn" (click)="rejectReview(review)">
                    <mat-icon>close</mat-icon>
                    Reject
                  </button>
                  <button mat-button (click)="requestModifications(review)">
                    <mat-icon>edit</mat-icon>
                    Request Changes
                  </button>
                </mat-card-actions>
              </mat-card>
            </div>
          </div>
        </mat-tab>

        <!-- Review History Tab -->
        <mat-tab label="Review History">
          <div class="tab-content">
            <div class="history-controls">
              <mat-form-field appearance="outline">
                <mat-label>Filter by Status</mat-label>
                <mat-select [(value)]="historyFilter" (selectionChange)="filterHistory()">
                  <mat-option value="all">All Reviews</mat-option>
                  <mat-option value="APPROVED">Approved</mat-option>
                  <mat-option value="REJECTED">Rejected</mat-option>
                  <mat-option value="MODIFICATIONS_REQUESTED">Modifications Requested</mat-option>
                </mat-select>
              </mat-form-field>
              
              <mat-form-field appearance="outline">
                <mat-label>Days</mat-label>
                <mat-select [(value)]="historyDays" (selectionChange)="loadHistory()">
                  <mat-option value="7">Last 7 days</mat-option>
                  <mat-option value="30">Last 30 days</mat-option>
                  <mat-option value="90">Last 90 days</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <table mat-table [dataSource]="reviewHistory" class="history-table">
              <!-- Review ID Column -->
              <ng-container matColumnDef="reviewId">
                <th mat-header-cell *matHeaderCellDef>Review ID</th>
                <td mat-cell *matCellDef="let review">
                  <code class="review-id">{{ review.reviewId | slice:7:15 }}</code>
                </td>
              </ng-container>

              <!-- Error Pattern Column -->
              <ng-container matColumnDef="errorPattern">
                <th mat-header-cell *matHeaderCellDef>Error Pattern</th>
                <td mat-cell *matCellDef="let review">
                  <div class="error-info">
                    <div class="error-signature">{{ review.errorEvent.errorSignature | slice:0:50 }}...</div>
                    <div class="service-name">{{ review.errorEvent.service }}</div>
                  </div>
                </td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Status</th>
                <td mat-cell *matCellDef="let review">
                  <mat-chip [class]="getStatusClass(review.status)">
                    {{ review.status | titlecase }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Reviewer Column -->
              <ng-container matColumnDef="reviewer">
                <th mat-header-cell *matHeaderCellDef>Reviewed By</th>
                <td mat-cell *matCellDef="let review">
                  <span *ngIf="review.reviewDecisions?.length > 0">
                    {{ review.reviewDecisions[review.reviewDecisions.length - 1].reviewedBy }}
                  </span>
                  <span *ngIf="!review.reviewDecisions?.length" class="not-reviewed">
                    Not reviewed
                  </span>
                </td>
              </ng-container>

              <!-- Timeline Column -->
              <ng-container matColumnDef="timeline">
                <th mat-header-cell *matHeaderCellDef>Timeline</th>
                <td mat-cell *matCellDef="let review">
                  <div class="timeline">
                    <div class="timeline-item">
                      <small>Submitted: {{ review.submittedAt | date:'short' }}</small>
                    </div>
                    <div class="timeline-item" *ngIf="review.reviewDecisions?.length > 0">
                      <small>Reviewed: {{ review.reviewDecisions[review.reviewDecisions.length - 1].reviewedAt | date:'short' }}</small>
                    </div>
                  </div>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let review">
                  <button mat-icon-button (click)="viewReviewDetails(review)" matTooltip="View Details">
                    <mat-icon>visibility</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="historyColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: historyColumns;"></tr>
            </table>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .human-review-container {
      padding: 20px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }

    .statistics-overview {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      text-align: center;
      min-height: 80px;
    }

    .stat-card.critical {
      border-left: 4px solid #f44336;
    }

    .stat-card.warning {
      border-left: 4px solid #ff9800;
    }

    .stat-value {
      font-size: 2rem;
      font-weight: 500;
      color: #1976d2;
    }

    .stat-card.critical .stat-value {
      color: #f44336;
    }

    .stat-card.warning .stat-value {
      color: #ff9800;
    }

    .stat-label {
      color: #666;
      font-size: 0.9rem;
      margin-top: 4px;
    }

    .tab-content {
      padding: 20px 0;
    }

    .no-data-card {
      text-align: center;
      padding: 40px;
    }

    .no-data-card mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #4caf50;
      margin-bottom: 16px;
    }

    .reviews-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .review-card {
      border-left: 4px solid #e0e0e0;
    }

    .review-card.critical {
      border-left-color: #f44336;
    }

    .review-card.high {
      border-left-color: #ff9800;
    }

    .review-card.medium {
      border-left-color: #ffc107;
    }

    .review-card.low {
      border-left-color: #4caf50;
    }

    .review-title {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .error-signature {
      font-family: 'Courier New', monospace;
      font-size: 0.9rem;
    }

    .fix-summary h4 {
      margin: 0 0 8px 0;
      color: #333;
    }

    .fix-details {
      display: flex;
      gap: 16px;
      margin: 12px 0;
    }

    .detail-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.9rem;
      color: #666;
    }

    .approval-required {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fff3cd;
      border: 1px solid #ffeaa7;
      border-radius: 4px;
      padding: 8px 12px;
      margin-top: 12px;
    }

    .warning-icon {
      color: #ff9800;
    }

    .history-controls {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
    }

    .history-table {
      width: 100%;
    }

    .review-id {
      background: #f5f5f5;
      padding: 2px 6px;
      border-radius: 3px;
      font-size: 0.8rem;
    }

    .error-info .error-signature {
      font-weight: 500;
    }

    .error-info .service-name {
      font-size: 0.8rem;
      color: #666;
    }

    .not-reviewed {
      color: #999;
      font-style: italic;
    }

    .timeline {
      font-size: 0.8rem;
    }

    .timeline-item {
      margin: 2px 0;
    }

    /* Severity classes */
    .severity-critical {
      background-color: #ffebee;
      color: #b71c1c;
    }

    .severity-high {
      background-color: #fff3e0;
      color: #ef6c00;
    }

    .severity-medium {
      background-color: #fffde7;
      color: #f57f17;
    }

    .severity-low {
      background-color: #e8f5e8;
      color: #2e7d32;
    }

    /* Status classes */
    .status-approved {
      background-color: #e8f5e8;
      color: #2e7d32;
    }

    .status-rejected {
      background-color: #ffebee;
      color: #c62828;
    }

    .status-modifications-requested {
      background-color: #fff3e0;
      color: #ef6c00;
    }

    .status-pending {
      background-color: #e3f2fd;
      color: #1565c0;
    }
  `]
})
export class HumanReviewComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Component state
  pendingReviews: PendingReview[] = [];
  reviewHistory: PendingReview[] = [];
  statistics: ReviewStatistics | null = null;
  
  // Filters
  historyFilter = 'all';
  historyDays = 7;
  
  // Table configuration
  historyColumns = ['reviewId', 'errorPattern', 'status', 'reviewer', 'timeline', 'actions'];

  constructor(
    private humanReviewService: HumanReviewService,
    private webSocketService: WebSocketService,
    private telemetryService: TelemetryService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
    this.setupRealTimeUpdates();
    this.setupAutoRefresh();
    this.trackAccess();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadInitialData(): void {
    this.refreshData();
    this.loadHistory();
    this.loadStatistics();
  }

  private setupRealTimeUpdates(): void {
    // Listen for real-time review notifications
    this.webSocketService.on('code_review_notification')
      .pipe(takeUntil(this.destroy$))
      .subscribe(notification => {
        this.handleReviewNotification(notification);
      });
  }

  private setupAutoRefresh(): void {
    interval(60000) // Refresh every minute
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.refreshData();
        this.loadStatistics();
      });
  }

  private trackAccess(): void {
    this.telemetryService.trackEvent('human_review_dashboard_accessed', {
      timestamp: new Date().toISOString()
    });
  }

  refreshData(): void {
    this.humanReviewService.getPendingReviews().subscribe({
      next: (response) => {
        if (response.success) {
          this.pendingReviews = response.reviews;
          this.cdr.detectChanges();
        }
      },
      error: (error) => {
        console.error('Error loading pending reviews:', error);
        this.showError('Failed to load pending reviews');
      }
    });
  }

  loadHistory(): void {
    this.humanReviewService.getReviewHistory(this.historyDays).subscribe({
      next: (response) => {
        if (response.success) {
          this.reviewHistory = response.reviews;
          this.filterHistory();
        }
      },
      error: (error) => {
        console.error('Error loading review history:', error);
        this.showError('Failed to load review history');
      }
    });
  }

  loadStatistics(): void {
    this.humanReviewService.getStatistics().subscribe({
      next: (response) => {
        if (response.success) {
          this.statistics = response.statistics;
          this.cdr.detectChanges();
        }
      },
      error: (error) => {
        console.error('Error loading statistics:', error);
      }
    });
  }

  filterHistory(): void {
    // Filter logic would be applied here based on historyFilter
    this.cdr.detectChanges();
  }

  processTimeouts(): void {
    this.humanReviewService.processTimeouts().subscribe({
      next: (response) => {
        if (response.success) {
          this.showSuccess('Timeout processing completed');
          this.refreshData();
        }
      },
      error: (error) => {
        console.error('Error processing timeouts:', error);
        this.showError('Failed to process timeouts');
      }
    });
  }

  viewReviewDetails(review: PendingReview): void {
    this.telemetryService.trackEvent('review_details_viewed', {
      reviewId: review.reviewId,
      severity: review.severity
    });
    
    // Open review details dialog
    // Implementation would go here
  }

  approveReview(review: PendingReview): void {
    const approvalData = {
      reviewedBy: 'current-user', // Get from auth service
      comments: '',
      modifications: {}
    };

    this.humanReviewService.approveReview(review.reviewId, approvalData).subscribe({
      next: (response) => {
        if (response.success) {
          this.showSuccess('Review approved successfully');
          this.refreshData();
          this.telemetryService.trackEvent('review_approved', {
            reviewId: review.reviewId,
            severity: review.severity
          });
        } else {
          this.showError(response.message);
        }
      },
      error: (error) => {
        console.error('Error approving review:', error);
        this.showError('Failed to approve review');
      }
    });
  }

  rejectReview(review: PendingReview): void {
    const rejectionData = {
      reviewedBy: 'current-user', // Get from auth service
      rejectionReason: 'Requires manual review',
      improvementSuggestions: []
    };

    this.humanReviewService.rejectReview(review.reviewId, rejectionData).subscribe({
      next: (response) => {
        if (response.success) {
          this.showSuccess('Review rejected');
          this.refreshData();
          this.telemetryService.trackEvent('review_rejected', {
            reviewId: review.reviewId,
            severity: review.severity
          });
        } else {
          this.showError(response.message);
        }
      },
      error: (error) => {
        console.error('Error rejecting review:', error);
        this.showError('Failed to reject review');
      }
    });
  }

  requestModifications(review: PendingReview): void {
    const modificationData = {
      reviewedBy: 'current-user', // Get from auth service
      modificationRequest: 'Please add additional test cases',
      suggestedChanges: {}
    };

    this.humanReviewService.requestModifications(review.reviewId, modificationData).subscribe({
      next: (response) => {
        if (response.success) {
          this.showSuccess('Modifications requested');
          this.refreshData();
          this.telemetryService.trackEvent('review_modifications_requested', {
            reviewId: review.reviewId,
            severity: review.severity
          });
        } else {
          this.showError(response.message);
        }
      },
      error: (error) => {
        console.error('Error requesting modifications:', error);
        this.showError('Failed to request modifications');
      }
    });
  }

  private handleReviewNotification(notification: any): void {
    if (notification.type === 'code_review_required') {
      this.refreshData();
      this.showInfo(`New code review required: ${notification.reviewId}`);
    }
  }

  // Utility methods
  getSeverityClass(severity: string): string {
    return `severity-${severity.toLowerCase()}`;
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  getReviewCardClass(review: PendingReview): string {
    return review.severity.toLowerCase();
  }

  getFileCount(filesModified: any): number {
    return filesModified ? Object.keys(filesModified).length : 0;
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  private showInfo(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: ['info-snackbar']
    });
  }
}