import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

import { Subject, takeUntil, interval, combineLatest } from 'rxjs';

import { MonitoringService } from '../../../core/services/monitoring.service';
import { AppDynamicsService } from '../../../core/services/appdynamics.service';
import { OpenTelemetryService } from '../../../core/services/opentelemetry.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { TelemetryService } from '../../../core/services/telemetry.service';
import { HumanReviewService } from '../../../core/services/human-review.service';

export interface SystemMetric {
  name: string;
  value: number;
  unit: string;
  timestamp: Date;
  status: 'healthy' | 'warning' | 'critical';
  threshold: number;
}

export interface ServiceHealth {
  serviceName: string;
  status: 'up' | 'down' | 'degraded';
  responseTime: number;
  errorRate: number;
  throughput: number;
  lastChecked: Date;
  dependencies: string[];
}

export interface ErrorPattern {
  id: string;
  pattern: string;
  count: number;
  services: string[];
  firstSeen: Date;
  lastSeen: Date;
  severity: 'low' | 'medium' | 'high' | 'critical';
  autoFixed: boolean;
  fixApplied?: string;
}

@Component({
  selector: 'app-monitoring-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    MatBadgeModule,
    FormsModule,
    ReactiveFormsModule,
    BaseChartDirective
  ],
  template: `
    <div class="monitoring-dashboard">
      <!-- Header Section -->
      <div class="dashboard-header">
        <h1>System Monitoring & Analytics</h1>
        <div class="dashboard-controls">
          <mat-form-field appearance="outline">
            <mat-label>Time Range</mat-label>
            <mat-select [(value)]="selectedTimeRange" (selectionChange)="onTimeRangeChange()">
              <mat-option value="5m">Last 5 minutes</mat-option>
              <mat-option value="15m">Last 15 minutes</mat-option>
              <mat-option value="1h">Last hour</mat-option>
              <mat-option value="6h">Last 6 hours</mat-option>
              <mat-option value="24h">Last 24 hours</mat-option>
              <mat-option value="7d">Last 7 days</mat-option>
            </mat-select>
          </mat-form-field>
          
          <mat-slide-toggle 
            [(ngModel)]="autoRefresh" 
            (change)="toggleAutoRefresh()"
            matTooltip="Auto refresh every 30 seconds">
            Auto Refresh
          </mat-slide-toggle>
          
          <button mat-raised-button color="primary" (click)="refreshData()">
            <mat-icon>refresh</mat-icon>
            Refresh
          </button>
        </div>
      </div>

      <!-- System Overview Cards -->
      <div class="overview-cards">
        <mat-card class="metric-card">
          <mat-card-header>
            <mat-card-title>System Health</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="metric-value" [class]="getHealthStatusClass(systemHealth.status)">
              {{ systemHealth.status | titlecase }}
            </div>
            <div class="metric-subtitle">
              {{ systemHealth.healthyServices }}/{{ systemHealth.totalServices }} services healthy
            </div>
            <mat-progress-bar 
              mode="determinate" 
              [value]="(systemHealth.healthyServices / systemHealth.totalServices) * 100">
            </mat-progress-bar>
          </mat-card-content>
        </mat-card>

        <mat-card class="metric-card">
          <mat-card-header>
            <mat-card-title>Response Time</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="metric-value">{{ averageResponseTime | number:'1.0-2' }}ms</div>
            <div class="metric-subtitle">Average across all services</div>
            <div class="metric-trend" [class]="responseTrendClass">
              <mat-icon>{{ responseTrendIcon }}</mat-icon>
              {{ responseTrendText }}
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="metric-card">
          <mat-card-header>
            <mat-card-title>Error Rate</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="metric-value" [class]="getErrorRateClass(errorRate)">
              {{ errorRate | number:'1.0-2' }}%
            </div>
            <div class="metric-subtitle">Errors in the last hour</div>
            <div class="metric-details">
              {{ totalErrors }} total errors
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="metric-card">
          <mat-card-header>
            <mat-card-title>Auto-Fixes Applied</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="metric-value text-success">{{ autoFixesApplied }}</div>
            <div class="metric-subtitle">In the last 24 hours</div>
            <div class="metric-details">
              {{ pendingFixes }} pending fixes
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Tabs for Different Monitoring Views -->
      <mat-tab-group [(selectedIndex)]="selectedTab" class="monitoring-tabs">
        
        <!-- Real-time Metrics Tab -->
        <mat-tab label="Real-time Metrics">
          <div class="tab-content">
            <div class="charts-grid">
              <!-- Response Time Chart -->
              <mat-card class="chart-card">
                <mat-card-header>
                  <mat-card-title>Response Time Trends</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <canvas 
                    baseChart
                    [data]="responseTimeChartData"
                    [options]="chartOptions"
                    [type]="chartType">
                  </canvas>
                </mat-card-content>
              </mat-card>

              <!-- Throughput Chart -->
              <mat-card class="chart-card">
                <mat-card-header>
                  <mat-card-title>Request Throughput</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <canvas 
                    baseChart
                    [data]="throughputChartData"
                    [options]="chartOptions"
                    [type]="chartType">
                  </canvas>
                </mat-card-content>
              </mat-card>

              <!-- Error Rate Chart -->
              <mat-card class="chart-card">
                <mat-card-header>
                  <mat-card-title>Error Rate Over Time</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <canvas 
                    baseChart
                    [data]="errorRateChartData"
                    [options]="chartOptions"
                    [type]="chartType">
                  </canvas>
                </mat-card-content>
              </mat-card>

              <!-- System Resources Chart -->
              <mat-card class="chart-card">
                <mat-card-header>
                  <mat-card-title>System Resources</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <canvas 
                    baseChart
                    [data]="systemResourcesChartData"
                    [options]="chartOptions"
                    [type]="'line'">
                  </canvas>
                </mat-card-content>
              </mat-card>
            </div>
          </div>
        </mat-tab>

        <!-- Service Health Tab -->
        <mat-tab label="Service Health">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Service Status Overview</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <table mat-table [dataSource]="serviceHealthData" class="full-width-table">
                  
                  <!-- Service Name Column -->
                  <ng-container matColumnDef="serviceName">
                    <th mat-header-cell *matHeaderCellDef>Service</th>
                    <td mat-cell *matCellDef="let service">
                      <div class="service-name">
                        <mat-icon [class]="getServiceStatusIconClass(service.status)">
                          {{ getServiceStatusIcon(service.status) }}
                        </mat-icon>
                        {{ service.serviceName }}
                      </div>
                    </td>
                  </ng-container>

                  <!-- Status Column -->
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let service">
                      <mat-chip [class]="getServiceStatusClass(service.status)">
                        {{ service.status | titlecase }}
                      </mat-chip>
                    </td>
                  </ng-container>

                  <!-- Response Time Column -->
                  <ng-container matColumnDef="responseTime">
                    <th mat-header-cell *matHeaderCellDef>Response Time</th>
                    <td mat-cell *matCellDef="let service">
                      {{ service.responseTime | number:'1.0-0' }}ms
                    </td>
                  </ng-container>

                  <!-- Error Rate Column -->
                  <ng-container matColumnDef="errorRate">
                    <th mat-header-cell *matHeaderCellDef>Error Rate</th>
                    <td mat-cell *matCellDef="let service">
                      <span [class]="getErrorRateClass(service.errorRate)">
                        {{ service.errorRate | number:'1.0-2' }}%
                      </span>
                    </td>
                  </ng-container>

                  <!-- Throughput Column -->
                  <ng-container matColumnDef="throughput">
                    <th mat-header-cell *matHeaderCellDef>Throughput</th>
                    <td mat-cell *matCellDef="let service">
                      {{ service.throughput | number:'1.0-0' }} req/min
                    </td>
                  </ng-container>

                  <!-- Last Checked Column -->
                  <ng-container matColumnDef="lastChecked">
                    <th mat-header-cell *matHeaderCellDef>Last Checked</th>
                    <td mat-cell *matCellDef="let service">
                      {{ service.lastChecked | date:'short' }}
                    </td>
                  </ng-container>

                  <!-- Actions Column -->
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let service">
                      <button mat-icon-button (click)="viewServiceDetails(service)" matTooltip="View Details">
                        <mat-icon>visibility</mat-icon>
                      </button>
                      <button mat-icon-button (click)="restartService(service)" matTooltip="Restart Service">
                        <mat-icon>restart_alt</mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="serviceHealthColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: serviceHealthColumns;"></tr>
                </table>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>

        <!-- Error Patterns & Auto-fixes Tab -->
        <mat-tab label="Error Patterns & Auto-fixes">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Error Patterns & Automated Fixes</mat-card-title>
                <mat-card-subtitle>
                  Intelligent error detection and automated code fixing
                </mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <table mat-table [dataSource]="errorPatternsData" class="full-width-table">
                  
                  <!-- Pattern Column -->
                  <ng-container matColumnDef="pattern">
                    <th mat-header-cell *matHeaderCellDef>Error Pattern</th>
                    <td mat-cell *matCellDef="let error">
                      <div class="error-pattern">
                        <code>{{ error.pattern }}</code>
                        <mat-chip-listbox>
                          <mat-chip *ngFor="let service of error.services" 
                                   [class]="'service-chip'">
                            {{ service }}
                          </mat-chip>
                        </mat-chip-listbox>
                      </div>
                    </td>
                  </ng-container>

                  <!-- Count Column -->
                  <ng-container matColumnDef="count">
                    <th mat-header-cell *matHeaderCellDef>Occurrences</th>
                    <td mat-cell *matCellDef="let error">
                      <span class="error-count">{{ error.count }}</span>
                    </td>
                  </ng-container>

                  <!-- Severity Column -->
                  <ng-container matColumnDef="severity">
                    <th mat-header-cell *matHeaderCellDef>Severity</th>
                    <td mat-cell *matCellDef="let error">
                      <mat-chip [class]="getSeverityClass(error.severity)">
                        {{ error.severity | titlecase }}
                      </mat-chip>
                    </td>
                  </ng-container>

                  <!-- Auto-fixed Column -->
                  <ng-container matColumnDef="autoFixed">
                    <th mat-header-cell *matHeaderCellDef>Auto-fixed</th>
                    <td mat-cell *matCellDef="let error">
                      <mat-icon [class]="error.autoFixed ? 'text-success' : 'text-warning'">
                        {{ error.autoFixed ? 'check_circle' : 'pending' }}
                      </mat-icon>
                      <span *ngIf="error.fixApplied" class="fix-applied">
                        {{ error.fixApplied }}
                      </span>
                    </td>
                  </ng-container>

                  <!-- Timeline Column -->
                  <ng-container matColumnDef="timeline">
                    <th mat-header-cell *matHeaderCellDef>Timeline</th>
                    <td mat-cell *matCellDef="let error">
                      <div class="timeline">
                        <div class="timeline-item">
                          <small>First: {{ error.firstSeen | date:'short' }}</small>
                        </div>
                        <div class="timeline-item">
                          <small>Last: {{ error.lastSeen | date:'short' }}</small>
                        </div>
                      </div>
                    </td>
                  </ng-container>

                  <!-- Actions Column -->
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let error">
                      <button mat-icon-button (click)="viewErrorDetails(error)" matTooltip="View Details">
                        <mat-icon>info</mat-icon>
                      </button>
                      <button mat-icon-button 
                              (click)="applyFix(error)" 
                              [disabled]="error.autoFixed"
                              matTooltip="Apply Fix">
                        <mat-icon>build</mat-icon>
                      </button>
                      <button mat-icon-button (click)="suppressPattern(error)" matTooltip="Suppress Pattern">
                        <mat-icon>visibility_off</mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="errorPatternsColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: errorPatternsColumns;"></tr>
                </table>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>

        <!-- Human Review Tab -->
        <mat-tab>
          <ng-template mat-tab-label>
            <span [matBadge]="pendingReviewsCount" 
                  [matBadgeHidden]="pendingReviewsCount === 0"
                  matBadgeColor="warn"
                  matBadgeSize="small">
              Human Review
            </span>
          </ng-template>
          <div class="tab-content">
            <div class="human-review-section">
              <!-- Quick Actions -->
              <div class="quick-actions" *ngIf="pendingReviewsCount > 0">
                <mat-card class="action-card urgent" *ngIf="criticalReviewsCount > 0">
                  <mat-card-content>
                    <div class="action-content">
                      <mat-icon class="warning-icon">warning</mat-icon>
                      <div class="action-text">
                        <strong>{{ criticalReviewsCount }} Critical Review{{ criticalReviewsCount > 1 ? 's' : '' }}</strong>
                        <div class="action-subtitle">Require immediate attention</div>
                      </div>
                      <button mat-raised-button color="warn" (click)="viewCriticalReviews()">
                        Review Now
                      </button>
                    </div>
                  </mat-card-content>
                </mat-card>

                <mat-card class="action-card">
                  <mat-card-content>
                    <div class="action-content">
                      <mat-icon class="info-icon">schedule</mat-icon>
                      <div class="action-text">
                        <strong>{{ pendingReviewsCount }} Pending Review{{ pendingReviewsCount > 1 ? 's' : '' }}</strong>
                        <div class="action-subtitle">Automated fixes awaiting approval</div>
                      </div>
                      <button mat-raised-button color="primary" (click)="viewAllReviews()">
                        View All
                      </button>
                    </div>
                  </mat-card-content>
                </mat-card>
              </div>

              <!-- No Reviews State -->
              <mat-card *ngIf="pendingReviewsCount === 0" class="no-reviews-card">
                <mat-card-content>
                  <mat-icon class="success-icon">check_circle</mat-icon>
                  <h3>All Reviews Complete</h3>
                  <p>No automated fixes require human review at this time.</p>
                  <div class="stats-summary">
                    <span class="stat-item">
                      <strong>{{ reviewStats?.last7Days?.approved || 0 }}</strong> approved this week
                    </span>
                    <span class="stat-divider">•</span>
                    <span class="stat-item">
                      <strong>{{ reviewStats?.last7Days?.approvalRate || 0 | number:'1.0-1' }}%</strong> approval rate
                    </span>
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Recent Activity -->
              <mat-card class="recent-activity-card" *ngIf="recentReviewActivity.length > 0">
                <mat-card-header>
                  <mat-card-title>Recent Review Activity</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="activity-list">
                    <div class="activity-item" *ngFor="let activity of recentReviewActivity.slice(0, 5)">
                      <mat-icon [class]="getActivityIconClass(activity.decision)">
                        {{ getActivityIcon(activity.decision) }}
                      </mat-icon>
                      <div class="activity-details">
                        <div class="activity-text">
                          <strong>{{ activity.reviewedBy }}</strong> 
                          {{ activity.decision | lowercase }} review 
                          <code>{{ activity.reviewId.substring(7, 15) }}</code>
                        </div>
                        <div class="activity-time">
                          {{ activity.reviewedAt | date:'short' }}
                        </div>
                      </div>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Review Statistics -->
              <div class="review-stats-grid">
                <mat-card class="stat-card">
                  <mat-card-content>
                    <div class="stat-value">{{ reviewStats?.last7Days?.total || 0 }}</div>
                    <div class="stat-label">Reviews (7 days)</div>
                  </mat-card-content>
                </mat-card>

                <mat-card class="stat-card">
                  <mat-card-content>
                    <div class="stat-value text-success">{{ reviewStats?.last7Days?.approved || 0 }}</div>
                    <div class="stat-label">Approved</div>
                  </mat-card-content>
                </mat-card>

                <mat-card class="stat-card">
                  <mat-card-content>
                    <div class="stat-value text-error">{{ reviewStats?.last7Days?.rejected || 0 }}</div>
                    <div class="stat-label">Rejected</div>
                  </mat-card-content>
                </mat-card>

                <mat-card class="stat-card">
                  <mat-card-content>
                    <div class="stat-value">{{ reviewStats?.last7Days?.approvalRate || 0 | number:'1.0-1' }}%</div>
                    <div class="stat-label">Approval Rate</div>
                  </mat-card-content>
                </mat-card>
              </div>
            </div>
          </div>
        </mat-tab>

        <!-- AppDynamics Integration Tab -->
        <mat-tab label="AppDynamics Integration">
          <div class="tab-content">
            <div class="appdynamics-integration">
              <!-- AppDynamics Status -->
              <mat-card>
                <mat-card-header>
                  <mat-card-title>AppDynamics Integration Status</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="integration-status">
                    <mat-icon [class]="appDynamicsConnected ? 'text-success' : 'text-error'">
                      {{ appDynamicsConnected ? 'check_circle' : 'error' }}
                    </mat-icon>
                    <span>{{ appDynamicsConnected ? 'Connected' : 'Disconnected' }}</span>
                    <button mat-button color="primary" (click)="reconnectAppDynamics()" 
                            [disabled]="appDynamicsConnected">
                      Reconnect
                    </button>
                  </div>
                </mat-card-content>
              </mat-card>

              <!-- Business Transactions -->
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Business Transactions</mat-card-title>
                  <mat-card-subtitle>Top business transactions by response time and throughput</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <!-- Business transactions data would be displayed here -->
                  <div class="business-transactions">
                    <p *ngIf="!appDynamicsConnected" class="no-data">
                      AppDynamics integration required to view business transactions
                    </p>
                    <!-- Actual business transactions table would go here -->
                  </div>
                </mat-card-content>
              </mat-card>
            </div>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .monitoring-dashboard {
      padding: 20px;
    }
    
    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }
    
    .dashboard-controls {
      display: flex;
      gap: 16px;
      align-items: center;
    }
    
    .overview-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    
    .metric-card {
      text-align: center;
    }
    
    .metric-value {
      font-size: 2.5rem;
      font-weight: 500;
      margin: 8px 0;
    }
    
    .metric-subtitle {
      color: #666;
      font-size: 0.9rem;
    }
    
    .metric-details {
      color: #888;
      font-size: 0.8rem;
      margin-top: 4px;
    }
    
    .metric-trend {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      margin-top: 8px;
      font-size: 0.9rem;
    }
    
    .charts-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
      gap: 16px;
    }
    
    .chart-card {
      height: 300px;
    }
    
    .full-width-table {
      width: 100%;
    }
    
    .service-name {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .error-pattern {
      max-width: 300px;
    }
    
    .error-pattern code {
      display: block;
      background: #f5f5f5;
      padding: 4px 8px;
      border-radius: 4px;
      margin-bottom: 8px;
      font-size: 0.85rem;
    }
    
    .service-chip {
      margin: 2px;
    }
    
    .timeline {
      font-size: 0.8rem;
    }
    
    .timeline-item {
      margin: 2px 0;
    }
    
    .integration-status {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .no-data {
      text-align: center;
      color: #666;
      font-style: italic;
      margin: 20px 0;
    }
    
    /* Status classes */
    .status-healthy { color: #4caf50; }
    .status-warning { color: #ff9800; }
    .status-critical { color: #f44336; }
    
    .text-success { color: #4caf50; }
    .text-warning { color: #ff9800; }
    .text-error { color: #f44336; }
    
    .trend-up { color: #4caf50; }
    .trend-down { color: #f44336; }
    .trend-stable { color: #666; }
    
    /* Chip styles */
    .service-chip { background-color: #e3f2fd; }
    .severity-low { background-color: #e8f5e8; color: #2e7d32; }
    .severity-medium { background-color: #fff3e0; color: #ef6c00; }
    .severity-high { background-color: #ffebee; color: #c62828; }
    .severity-critical { background-color: #ffebee; color: #b71c1c; }
    
    /* Human Review Styles */
    .human-review-section {
      padding: 20px 0;
    }
    
    .quick-actions {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
    }
    
    .action-card {
      flex: 1;
      min-width: 300px;
    }
    
    .action-card.urgent {
      border-left: 4px solid #f44336;
    }
    
    .action-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    
    .action-text {
      flex: 1;
    }
    
    .action-subtitle {
      color: #666;
      font-size: 0.9rem;
      margin-top: 4px;
    }
    
    .warning-icon {
      color: #f44336;
      font-size: 2rem;
    }
    
    .info-icon {
      color: #1976d2;
      font-size: 2rem;
    }
    
    .no-reviews-card {
      text-align: center;
      padding: 40px;
    }
    
    .success-icon {
      color: #4caf50;
      font-size: 3rem;
      margin-bottom: 16px;
    }
    
    .stats-summary {
      margin-top: 16px;
      color: #666;
    }
    
    .stat-item {
      margin: 0 8px;
    }
    
    .stat-divider {
      color: #ccc;
    }
    
    .recent-activity-card {
      margin-bottom: 24px;
    }
    
    .activity-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .activity-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }
    
    .activity-item:last-child {
      border-bottom: none;
    }
    
    .activity-details {
      flex: 1;
    }
    
    .activity-text {
      font-size: 0.9rem;
    }
    
    .activity-text code {
      background: #f5f5f5;
      padding: 2px 4px;
      border-radius: 2px;
      font-size: 0.8rem;
    }
    
    .activity-time {
      font-size: 0.8rem;
      color: #999;
    }
    
    .review-stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }
    
    .text-info {
      color: #1976d2;
    }
  `]
})
export class MonitoringDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // UI State
  selectedTab = 0;
  selectedTimeRange = '1h';
  autoRefresh = true;
  
  // System Health Overview
  systemHealth = {
    status: 'healthy' as 'healthy' | 'warning' | 'critical',
    healthyServices: 6,
    totalServices: 7
  };
  
  averageResponseTime = 145.2;
  errorRate = 0.23;
  totalErrors = 12;
  autoFixesApplied = 8;
  pendingFixes = 3;
  
  responseTrendClass = 'trend-down';
  responseTrendIcon = 'trending_down';
  responseTrendText = '12% improvement';
  
  // AppDynamics Status
  appDynamicsConnected = true;
  
  // Human Review Data
  pendingReviewsCount = 0;
  criticalReviewsCount = 0;
  reviewStats: any = null;
  recentReviewActivity: any[] = [];
  
  // Chart Configuration
  chartType: ChartType = 'line';
  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'minute'
        }
      },
      y: {
        beginAtZero: true
      }
    }
  };
  
  // Chart Data
  responseTimeChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Response Time (ms)',
        data: [],
        borderColor: '#1976d2',
        backgroundColor: 'rgba(25, 118, 210, 0.1)',
        tension: 0.4
      }
    ]
  };
  
  throughputChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Requests/min',
        data: [],
        borderColor: '#4caf50',
        backgroundColor: 'rgba(76, 175, 80, 0.1)',
        tension: 0.4
      }
    ]
  };
  
  errorRateChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Error Rate (%)',
        data: [],
        borderColor: '#f44336',
        backgroundColor: 'rgba(244, 67, 54, 0.1)',
        tension: 0.4
      }
    ]
  };
  
  systemResourcesChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'CPU Usage (%)',
        data: [],
        borderColor: '#ff9800',
        backgroundColor: 'rgba(255, 152, 0, 0.1)',
        tension: 0.4
      },
      {
        label: 'Memory Usage (%)',
        data: [],
        borderColor: '#9c27b0',
        backgroundColor: 'rgba(156, 39, 176, 0.1)',
        tension: 0.4
      }
    ]
  };
  
  // Table Data
  serviceHealthColumns = ['serviceName', 'status', 'responseTime', 'errorRate', 'throughput', 'lastChecked', 'actions'];
  serviceHealthData: ServiceHealth[] = [];
  
  errorPatternsColumns = ['pattern', 'count', 'severity', 'autoFixed', 'timeline', 'actions'];
  errorPatternsData: ErrorPattern[] = [];
  
  constructor(
    private monitoringService: MonitoringService,
    private appDynamicsService: AppDynamicsService,
    private openTelemetryService: OpenTelemetryService,
    private webSocketService: WebSocketService,
    private telemetryService: TelemetryService,
    private humanReviewService: HumanReviewService,
    private cdr: ChangeDetectorRef
  ) {}
  
  ngOnInit(): void {
    this.initializeMonitoring();
    this.loadInitialData();
    this.setupRealTimeUpdates();
    this.setupAutoRefresh();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  private initializeMonitoring(): void {
    // Track dashboard access
    this.telemetryService.trackEvent('monitoring_dashboard_viewed', {
      timestamp: new Date().toISOString(),
      initialTab: this.selectedTab,
      timeRange: this.selectedTimeRange
    });
  }
  
  private loadInitialData(): void {
    this.refreshData();
    this.loadHumanReviewData();
  }
  
  private setupRealTimeUpdates(): void {
    // Subscribe to real-time metric updates
    this.webSocketService.on('system_metrics')
      .pipe(takeUntil(this.destroy$))
      .subscribe(metrics => {
        this.updateMetrics(metrics);
      });
    
    this.webSocketService.on('service_health')
      .pipe(takeUntil(this.destroy$))
      .subscribe(health => {
        this.updateServiceHealth(health);
      });
    
    this.webSocketService.on('error_pattern')
      .pipe(takeUntil(this.destroy$))
      .subscribe(pattern => {
        this.updateErrorPatterns(pattern);
      });
    
    // Subscribe to human review notifications
    this.webSocketService.on('code_review_notification')
      .pipe(takeUntil(this.destroy$))
      .subscribe(notification => {
        this.handleReviewNotification(notification);
      });
  }
  
  private setupAutoRefresh(): void {
    interval(30000) // 30 seconds
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.autoRefresh) {
          this.refreshData();
        }
      });
  }
  
  refreshData(): void {
    // Load all monitoring data
    combineLatest([
      this.monitoringService.getSystemMetrics(this.selectedTimeRange),
      this.monitoringService.getServiceHealth(),
      this.monitoringService.getErrorPatterns(this.selectedTimeRange),
      this.appDynamicsService.getBusinessTransactions()
    ]).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ([metrics, serviceHealth, errorPatterns, businessTransactions]) => {
        this.updateChartData(metrics);
        this.serviceHealthData = serviceHealth;
        this.errorPatternsData = errorPatterns;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading monitoring data:', error);
      }
    });
  }
  
  onTimeRangeChange(): void {
    this.telemetryService.trackEvent('monitoring_time_range_changed', {
      newTimeRange: this.selectedTimeRange,
      timestamp: new Date().toISOString()
    });
    
    this.refreshData();
  }
  
  toggleAutoRefresh(): void {
    this.telemetryService.trackEvent('monitoring_auto_refresh_toggled', {
      enabled: this.autoRefresh,
      timestamp: new Date().toISOString()
    });
  }
  
  private updateChartData(metrics: any[]): void {
    const times = metrics.map(m => new Date(m.timestamp));
    
    // Update response time chart
    this.responseTimeChartData = {
      ...this.responseTimeChartData,
      labels: times,
      datasets: [{
        ...this.responseTimeChartData.datasets[0],
        data: metrics.map(m => m.responseTime)
      }]
    };
    
    // Update throughput chart
    this.throughputChartData = {
      ...this.throughputChartData,
      labels: times,
      datasets: [{
        ...this.throughputChartData.datasets[0],
        data: metrics.map(m => m.throughput)
      }]
    };
    
    // Update error rate chart
    this.errorRateChartData = {
      ...this.errorRateChartData,
      labels: times,
      datasets: [{
        ...this.errorRateChartData.datasets[0],
        data: metrics.map(m => m.errorRate)
      }]
    };
    
    // Update system resources chart
    this.systemResourcesChartData = {
      ...this.systemResourcesChartData,
      labels: times,
      datasets: [
        {
          ...this.systemResourcesChartData.datasets[0],
          data: metrics.map(m => m.cpuUsage)
        },
        {
          ...this.systemResourcesChartData.datasets[1],
          data: metrics.map(m => m.memoryUsage)
        }
      ]
    };
  }
  
  private updateMetrics(metrics: any): void {
    this.averageResponseTime = metrics.responseTime;
    this.errorRate = metrics.errorRate;
    this.totalErrors = metrics.totalErrors;
    this.cdr.detectChanges();
  }
  
  private updateServiceHealth(health: ServiceHealth[]): void {
    this.serviceHealthData = health;
    this.systemHealth.healthyServices = health.filter(s => s.status === 'up').length;
    this.systemHealth.totalServices = health.length;
    this.cdr.detectChanges();
  }
  
  private updateErrorPatterns(patterns: ErrorPattern[]): void {
    this.errorPatternsData = patterns;
    this.autoFixesApplied = patterns.filter(p => p.autoFixed).length;
    this.pendingFixes = patterns.filter(p => !p.autoFixed).length;
    this.cdr.detectChanges();
  }
  
  // Utility methods for styling
  getHealthStatusClass(status: string): string {
    return `status-${status}`;
  }
  
  getErrorRateClass(rate: number): string {
    if (rate < 1) return 'text-success';
    if (rate < 5) return 'text-warning';
    return 'text-error';
  }
  
  getServiceStatusClass(status: string): string {
    const classes = {
      'up': 'status-healthy',
      'down': 'status-critical',
      'degraded': 'status-warning'
    };
    return classes[status as keyof typeof classes] || '';
  }
  
  getServiceStatusIcon(status: string): string {
    const icons = {
      'up': 'check_circle',
      'down': 'error',
      'degraded': 'warning'
    };
    return icons[status as keyof typeof icons] || 'help';
  }
  
  getServiceStatusIconClass(status: string): string {
    return this.getServiceStatusClass(status);
  }
  
  getSeverityClass(severity: string): string {
    return `severity-${severity}`;
  }
  
  // Action handlers
  viewServiceDetails(service: ServiceHealth): void {
    this.telemetryService.trackEvent('service_details_viewed', {
      serviceName: service.serviceName,
      timestamp: new Date().toISOString()
    });
    
    // Navigate to service details or show modal
  }
  
  restartService(service: ServiceHealth): void {
    this.telemetryService.trackEvent('service_restart_requested', {
      serviceName: service.serviceName,
      timestamp: new Date().toISOString()
    });
    
    // Implement service restart logic
  }
  
  viewErrorDetails(error: ErrorPattern): void {
    this.telemetryService.trackEvent('error_pattern_details_viewed', {
      errorId: error.id,
      pattern: error.pattern,
      timestamp: new Date().toISOString()
    });
    
    // Show error details modal or navigate to details page
  }
  
  applyFix(error: ErrorPattern): void {
    this.telemetryService.trackEvent('manual_fix_applied', {
      errorId: error.id,
      pattern: error.pattern,
      timestamp: new Date().toISOString()
    });
    
    // Apply automated fix
    this.monitoringService.applyErrorFix(error.id).subscribe({
      next: (result) => {
        error.autoFixed = true;
        error.fixApplied = result.fixDescription;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error applying fix:', err);
      }
    });
  }
  
  suppressPattern(error: ErrorPattern): void {
    this.telemetryService.trackEvent('error_pattern_suppressed', {
      errorId: error.id,
      pattern: error.pattern,
      timestamp: new Date().toISOString()
    });
    
    // Suppress error pattern
  }
  
  reconnectAppDynamics(): void {
    this.appDynamicsService.reconnect().subscribe({
      next: () => {
        this.appDynamicsConnected = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to reconnect to AppDynamics:', err);
      }
    });
  }
  
  // Human Review Methods
  
  private loadHumanReviewData(): void {
    // Load pending reviews count
    this.humanReviewService.getPendingReviews().subscribe({
      next: (response) => {
        if (response.success) {
          this.pendingReviewsCount = response.reviews?.length || 0;
          this.criticalReviewsCount = response.reviews?.filter(r => r.severity === 'CRITICAL').length || 0;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error loading pending reviews:', err);
      }
    });
    
    // Load statistics
    this.humanReviewService.getStatistics().subscribe({
      next: (response) => {
        if (response.success) {
          this.reviewStats = response.statistics;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error loading review statistics:', err);
      }
    });
    
    // Load recent activity
    this.humanReviewService.getReviewHistory(7).subscribe({
      next: (response) => {
        if (response.success) {
          this.recentReviewActivity = response.reviews
            ?.filter(r => r.reviewDecisions && r.reviewDecisions.length > 0)
            .map(r => r.reviewDecisions[r.reviewDecisions.length - 1])
            .sort((a, b) => new Date(b.reviewedAt).getTime() - new Date(a.reviewedAt).getTime()) || [];
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Error loading review history:', err);
      }
    });
  }
  
  private handleReviewNotification(notification: any): void {
    if (notification.type === 'code_review_required') {
      this.pendingReviewsCount++;
      if (notification.severity === 'CRITICAL') {
        this.criticalReviewsCount++;
      }
      this.cdr.detectChanges();
    } else if (notification.type === 'code_fix_approved' || 
               notification.type === 'code_fix_rejected') {
      this.loadHumanReviewData(); // Refresh all data
    }
  }
  
  viewCriticalReviews(): void {
    this.telemetryService.trackEvent('critical_reviews_accessed', {
      count: this.criticalReviewsCount,
      timestamp: new Date().toISOString()
    });
    
    // Navigate to human review component or open modal
    // For now, switch to the human review tab
    this.selectedTab = 4; // Assuming human review is the 5th tab (index 4)
  }
  
  viewAllReviews(): void {
    this.telemetryService.trackEvent('all_reviews_accessed', {
      count: this.pendingReviewsCount,
      timestamp: new Date().toISOString()
    });
    
    // Navigate to human review component or open modal
    // For now, switch to the human review tab
    this.selectedTab = 4; // Assuming human review is the 5th tab (index 4)
  }
  
  getActivityIcon(decision: string): string {
    const icons = {
      'APPROVED': 'check_circle',
      'REJECTED': 'cancel',
      'MODIFICATIONS_REQUESTED': 'edit',
      'AUTO_APPROVED': 'schedule'
    };
    return icons[decision as keyof typeof icons] || 'help';
  }
  
  getActivityIconClass(decision: string): string {
    const classes = {
      'APPROVED': 'text-success',
      'REJECTED': 'text-error',
      'MODIFICATIONS_REQUESTED': 'text-warning',
      'AUTO_APPROVED': 'text-info'
    };
    return classes[decision as keyof typeof classes] || '';
  }
}