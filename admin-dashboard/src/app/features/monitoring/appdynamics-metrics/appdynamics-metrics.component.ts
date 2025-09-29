import { Component, OnInit, OnDestroy, ChangeDetectorRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { FormsModule } from '@angular/forms';

import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

import { Subject, takeUntil, interval } from 'rxjs';

import { AppDynamicsService } from '../../../core/services/appdynamics.service';
import { MonitoringContext } from '../unified-monitoring/unified-monitoring.component';

export interface BusinessTransaction {
  id: string;
  name: string;
  tier: string;
  averageResponseTime: number; // ART in milliseconds
  callsPerMinute: number; // CPM
  errorsPerMinute: number; // EPM
  errorRate: number; // Error percentage
  throughput: number; // Total calls
  apdexScore: number; // Application Performance Index
  slowestResponseTime: number;
  fastestResponseTime: number;
  standardDeviation: number;
  lastUpdated: Date;
  healthStatus: 'critical' | 'warning' | 'normal' | 'excellent';
  trend: 'up' | 'down' | 'stable';
  violations: string[];
}

export interface ApplicationMetrics {
  applicationName: string;
  overallHealth: 'critical' | 'warning' | 'normal' | 'excellent';
  averageResponseTime: number;
  totalCallsPerMinute: number;
  totalErrorsPerMinute: number;
  overallErrorRate: number;
  apdexScore: number;
  activeAlerts: number;
  businessTransactions: BusinessTransaction[];
  infrastructureMetrics: {
    cpuUtilization: number;
    memoryUtilization: number;
    diskIOPS: number;
    networkThroughput: number;
  };
  databaseMetrics: {
    averageResponseTime: number;
    callsPerMinute: number;
    errorRate: number;
    connectionPoolUtilization: number;
  };
  lastUpdated: Date;
}

export interface PerformanceTrend {
  timestamp: Date;
  responseTime: number;
  throughput: number;
  errorRate: number;
}

@Component({
  selector: 'app-appdynamics-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    FormsModule,
    BaseChartDirective
  ],
  template: `
    <div class="appdynamics-metrics-container">
      <!-- Header -->
      <div class="metrics-header">
        <h3>AppDynamics Performance Metrics</h3>
        <div class="header-actions">
          <mat-form-field appearance="outline" class="refresh-interval">
            <mat-label>Refresh Interval</mat-label>
            <mat-select [(value)]="refreshInterval" (selectionChange)="updateRefreshInterval()">
              <mat-option value="30">30 seconds</mat-option>
              <mat-option value="60">1 minute</mat-option>
              <mat-option value="300">5 minutes</mat-option>
              <mat-option value="600">10 minutes</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="refreshMetrics()" [disabled]="loading">
            <mat-icon>refresh</mat-icon>
            {{ loading ? 'Loading...' : 'Refresh' }}
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading-container" *ngIf="loading && !applicationMetrics">
        <mat-spinner></mat-spinner>
        <p>Loading AppDynamics metrics...</p>
      </div>

      <!-- Error State -->
      <mat-card class="error-card" *ngIf="error && !loading">
        <mat-card-content>
          <div class="error-content">
            <mat-icon class="error-icon">error</mat-icon>
            <div>
              <h4>Unable to Load AppDynamics Metrics</h4>
              <p>{{ error }}</p>
              <button mat-raised-button color="primary" (click)="refreshMetrics()">
                <mat-icon>refresh</mat-icon>
                Retry
              </button>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Metrics Dashboard -->
      <div class="metrics-dashboard" *ngIf="applicationMetrics && !loading">
        
        <!-- Key Performance Indicators -->
        <div class="kpi-cards">
          <mat-card class="kpi-card art-card">
            <mat-card-content>
              <div class="kpi-content">
                <mat-icon class="kpi-icon">speed</mat-icon>
                <div class="kpi-details">
                  <div class="kpi-value" [class]="getARTClass(applicationMetrics.averageResponseTime)">
                    {{ applicationMetrics.averageResponseTime | number:'1.0-0' }}ms
                  </div>
                  <div class="kpi-label">Average Response Time (ART)</div>
                  <div class="kpi-trend">
                    <mat-icon [class]="getARTTrendClass()">{{ getARTTrendIcon() }}</mat-icon>
                    <span>{{ getARTTrendText() }}</span>
                  </div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card cpm-card">
            <mat-card-content>
              <div class="kpi-content">
                <mat-icon class="kpi-icon">timeline</mat-icon>
                <div class="kpi-details">
                  <div class="kpi-value" [class]="getCPMClass(applicationMetrics.totalCallsPerMinute)">
                    {{ applicationMetrics.totalCallsPerMinute | number:'1.0-0' }}
                  </div>
                  <div class="kpi-label">Calls Per Minute (CPM)</div>
                  <div class="kpi-trend">
                    <mat-icon [class]="getCPMTrendClass()">{{ getCPMTrendIcon() }}</mat-icon>
                    <span>{{ getCPMTrendText() }}</span>
                  </div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card error-card">
            <mat-card-content>
              <div class="kpi-content">
                <mat-icon class="kpi-icon">error_outline</mat-icon>
                <div class="kpi-details">
                  <div class="kpi-value" [class]="getErrorRateClass(applicationMetrics.overallErrorRate)">
                    {{ applicationMetrics.overallErrorRate | number:'1.0-2' }}%
                  </div>
                  <div class="kpi-label">Error Rate</div>
                  <div class="kpi-trend">
                    <mat-icon [class]="getErrorTrendClass()">{{ getErrorTrendIcon() }}</mat-icon>
                    <span>{{ getErrorTrendText() }}</span>
                  </div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="kpi-card apdex-card">
            <mat-card-content>
              <div class="kpi-content">
                <mat-icon class="kpi-icon">sentiment_satisfied</mat-icon>
                <div class="kpi-details">
                  <div class="kpi-value" [class]="getApdexClass(applicationMetrics.apdexScore)">
                    {{ applicationMetrics.apdexScore | number:'1.0-2' }}
                  </div>
                  <div class="kpi-label">Apdex Score</div>
                  <div class="apdex-description">{{ getApdexDescription(applicationMetrics.apdexScore) }}</div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Performance Trends Chart -->
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Performance Trends</mat-card-title>
            <mat-card-subtitle>{{ getTimeRangeDescription() }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="chart-container">
              <canvas baseChart
                      [data]="performanceChartData"
                      [options]="chartOptions"
                      [type]="chartType">
              </canvas>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Business Transactions Table -->
        <mat-card class="transactions-card">
          <mat-card-header>
            <mat-card-title>Business Transactions Performance</mat-card-title>
            <mat-card-subtitle>Top business transactions by response time and volume</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="businessTransactions" class="transactions-table">
              
              <!-- Transaction Name Column -->
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>Transaction</th>
                <td mat-cell *matCellDef="let transaction">
                  <div class="transaction-info">
                    <div class="transaction-name">{{ transaction.name }}</div>
                    <div class="transaction-tier">{{ transaction.tier }}</div>
                  </div>
                </td>
              </ng-container>

              <!-- Health Status Column -->
              <ng-container matColumnDef="health">
                <th mat-header-cell *matHeaderCellDef>Health</th>
                <td mat-cell *matCellDef="let transaction">
                  <mat-chip [class]="getHealthChipClass(transaction.healthStatus)">
                    <mat-icon>{{ getHealthIcon(transaction.healthStatus) }}</mat-icon>
                    {{ transaction.healthStatus | titlecase }}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- ART Column -->
              <ng-container matColumnDef="art">
                <th mat-header-cell *matHeaderCellDef>ART (ms)</th>
                <td mat-cell *matCellDef="let transaction">
                  <div class="metric-cell">
                    <span [class]="getARTClass(transaction.averageResponseTime)">
                      {{ transaction.averageResponseTime | number:'1.0-0' }}
                    </span>
                    <mat-icon class="trend-icon" [class]="getTrendClass(transaction.trend)">
                      {{ getTrendIcon(transaction.trend) }}
                    </mat-icon>
                  </div>
                </td>
              </ng-container>

              <!-- CPM Column -->
              <ng-container matColumnDef="cpm">
                <th mat-header-cell *matHeaderCellDef>CPM</th>
                <td mat-cell *matCellDef="let transaction">
                  <div class="metric-cell">
                    <span [class]="getCPMClass(transaction.callsPerMinute)">
                      {{ transaction.callsPerMinute | number:'1.0-0' }}
                    </span>
                    <mat-icon class="trend-icon" [class]="getTrendClass(transaction.trend)">
                      {{ getTrendIcon(transaction.trend) }}
                    </mat-icon>
                  </div>
                </td>
              </ng-container>

              <!-- Error Rate Column -->
              <ng-container matColumnDef="errorRate">
                <th mat-header-cell *matHeaderCellDef>Error Rate</th>
                <td mat-cell *matCellDef="let transaction">
                  <div class="metric-cell">
                    <span [class]="getErrorRateClass(transaction.errorRate)">
                      {{ transaction.errorRate | number:'1.0-2' }}%
                    </span>
                    <div class="error-details">
                      <small>{{ transaction.errorsPerMinute | number:'1.0-0' }} EPM</small>
                    </div>
                  </div>
                </td>
              </ng-container>

              <!-- Apdex Column -->
              <ng-container matColumnDef="apdex">
                <th mat-header-cell *matHeaderCellDef>Apdex</th>
                <td mat-cell *matCellDef="let transaction">
                  <span [class]="getApdexClass(transaction.apdexScore)">
                    {{ transaction.apdexScore | number:'1.0-2' }}
                  </span>
                </td>
              </ng-container>

              <!-- Violations Column -->
              <ng-container matColumnDef="violations">
                <th mat-header-cell *matHeaderCellDef>Violations</th>
                <td mat-cell *matCellDef="let transaction">
                  <div class="violations-cell" *ngIf="transaction.violations.length > 0; else noViolations">
                    <mat-chip-listbox>
                      <mat-chip *ngFor="let violation of transaction.violations" 
                               class="violation-chip"
                               [matTooltip]="violation">
                        {{ violation | slice:0:20 }}{{ violation.length > 20 ? '...' : '' }}
                      </mat-chip>
                    </mat-chip-listbox>
                  </div>
                  <ng-template #noViolations>
                    <span class="no-violations">
                      <mat-icon>check</mat-icon>
                      None
                    </span>
                  </ng-template>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let transaction">
                  <button mat-icon-button (click)="viewTransactionDetails(transaction)" matTooltip="View Details">
                    <mat-icon>visibility</mat-icon>
                  </button>
                  <button mat-icon-button (click)="analyzeTransaction(transaction)" matTooltip="Analyze Performance">
                    <mat-icon>analytics</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="transactionColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: transactionColumns;" 
                  [class]="getRowClass(row)"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <!-- Infrastructure and Database Metrics -->
        <div class="additional-metrics">
          <mat-card class="infrastructure-card">
            <mat-card-header>
              <mat-card-title>Infrastructure Metrics</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="metrics-grid">
                <div class="metric-item">
                  <mat-icon>memory</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value" [class]="getUtilizationClass(applicationMetrics.infrastructureMetrics.cpuUtilization)">
                      {{ applicationMetrics.infrastructureMetrics.cpuUtilization | number:'1.0-1' }}%
                    </div>
                    <div class="metric-label">CPU Utilization</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>storage</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value" [class]="getUtilizationClass(applicationMetrics.infrastructureMetrics.memoryUtilization)">
                      {{ applicationMetrics.infrastructureMetrics.memoryUtilization | number:'1.0-1' }}%
                    </div>
                    <div class="metric-label">Memory Utilization</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>developer_board</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value">
                      {{ applicationMetrics.infrastructureMetrics.diskIOPS | number:'1.0-0' }}
                    </div>
                    <div class="metric-label">Disk IOPS</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>network_check</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value">
                      {{ formatThroughput(applicationMetrics.infrastructureMetrics.networkThroughput) }}
                    </div>
                    <div class="metric-label">Network Throughput</div>
                  </div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="database-card">
            <mat-card-header>
              <mat-card-title>Database Performance</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="metrics-grid">
                <div class="metric-item">
                  <mat-icon>speed</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value" [class]="getARTClass(applicationMetrics.databaseMetrics.averageResponseTime)">
                      {{ applicationMetrics.databaseMetrics.averageResponseTime | number:'1.0-0' }}ms
                    </div>
                    <div class="metric-label">DB Response Time</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>timeline</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value">
                      {{ applicationMetrics.databaseMetrics.callsPerMinute | number:'1.0-0' }}
                    </div>
                    <div class="metric-label">DB Calls/Min</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>error_outline</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value" [class]="getErrorRateClass(applicationMetrics.databaseMetrics.errorRate)">
                      {{ applicationMetrics.databaseMetrics.errorRate | number:'1.0-2' }}%
                    </div>
                    <div class="metric-label">DB Error Rate</div>
                  </div>
                </div>
                
                <div class="metric-item">
                  <mat-icon>pool</mat-icon>
                  <div class="metric-details">
                    <div class="metric-value" [class]="getUtilizationClass(applicationMetrics.databaseMetrics.connectionPoolUtilization)">
                      {{ applicationMetrics.databaseMetrics.connectionPoolUtilization | number:'1.0-1' }}%
                    </div>
                    <div class="metric-label">Connection Pool</div>
                  </div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Last Updated Info -->
        <div class="last-updated">
          <mat-icon>schedule</mat-icon>
          <span>Last updated: {{ applicationMetrics.lastUpdated | date:'medium' }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .appdynamics-metrics-container {
      padding: 20px;
    }

    .metrics-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    .header-actions {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .refresh-interval {
      min-width: 140px;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px;
      text-align: center;
    }

    .error-card {
      margin-bottom: 20px;
      border-left: 4px solid #f44336;
    }

    .error-content {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .error-icon {
      color: #f44336;
      font-size: 2rem;
    }

    .kpi-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .kpi-card {
      border-radius: 8px;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }

    .art-card {
      border-left: 4px solid #2196f3;
    }

    .cpm-card {
      border-left: 4px solid #4caf50;
    }

    .error-card {
      border-left: 4px solid #f44336;
    }

    .apdex-card {
      border-left: 4px solid #ff9800;
    }

    .kpi-content {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .kpi-icon {
      font-size: 3rem;
      width: 3rem;
      height: 3rem;
      color: #666;
    }

    .kpi-details {
      flex: 1;
    }

    .kpi-value {
      font-size: 2.5rem;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .kpi-label {
      color: #666;
      font-size: 0.9rem;
      margin-bottom: 8px;
    }

    .kpi-trend {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.8rem;
    }

    .apdex-description {
      color: #666;
      font-size: 0.8rem;
    }

    .chart-card {
      margin-bottom: 24px;
    }

    .chart-container {
      height: 300px;
      position: relative;
    }

    .transactions-card {
      margin-bottom: 24px;
    }

    .transactions-table {
      width: 100%;
    }

    .transaction-info .transaction-name {
      font-weight: 500;
      font-size: 0.9rem;
    }

    .transaction-info .transaction-tier {
      color: #666;
      font-size: 0.8rem;
    }

    .metric-cell {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .trend-icon {
      font-size: 1rem;
      width: 1rem;
      height: 1rem;
    }

    .error-details {
      margin-top: 2px;
    }

    .violations-cell {
      max-width: 200px;
    }

    .violation-chip {
      margin: 2px;
      font-size: 0.7rem;
      background-color: #ffebee;
      color: #c62828;
    }

    .no-violations {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #4caf50;
      font-size: 0.9rem;
    }

    .additional-metrics {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 24px;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }

    .metric-item {
      display: flex;
      gap: 12px;
      align-items: center;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 6px;
    }

    .metric-item mat-icon {
      color: #666;
    }

    .metric-details .metric-value {
      font-size: 1.4rem;
      font-weight: 500;
      margin-bottom: 2px;
    }

    .metric-details .metric-label {
      color: #666;
      font-size: 0.8rem;
    }

    .last-updated {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-size: 0.9rem;
      margin-top: 16px;
    }

    /* Performance Classes */
    .excellent { color: #4caf50; }
    .good { color: #8bc34a; }
    .normal { color: #ffc107; }
    .warning { color: #ff9800; }
    .critical { color: #f44336; }

    .trend-up { color: #4caf50; }
    .trend-down { color: #f44336; }
    .trend-stable { color: #666; }

    /* Health Status Chips */
    .health-excellent {
      background-color: #e8f5e8;
      color: #2e7d32;
    }

    .health-normal {
      background-color: #f3e5f5;
      color: #7b1fa2;
    }

    .health-warning {
      background-color: #fff3e0;
      color: #ef6c00;
    }

    .health-critical {
      background-color: #ffebee;
      color: #c62828;
    }

    /* Table Row Highlighting */
    .transaction-row.critical {
      background-color: rgba(244, 67, 54, 0.05);
    }

    .transaction-row.warning {
      background-color: rgba(255, 152, 0, 0.05);
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .kpi-cards {
        grid-template-columns: 1fr;
      }

      .additional-metrics {
        grid-template-columns: 1fr;
      }

      .metrics-grid {
        grid-template-columns: 1fr;
      }

      .header-actions {
        flex-direction: column;
        align-items: stretch;
        gap: 12px;
      }
    }
  `]
})
export class AppDynamicsMetricsComponent implements OnInit, OnDestroy, OnChanges {
  @Input() context: MonitoringContext | null = null;

  private destroy$ = new Subject<void>();

  // Component state
  loading = false;
  error: string | null = null;
  refreshInterval = 60; // seconds
  private refreshTimer?: any;

  // Data
  applicationMetrics: ApplicationMetrics | null = null;
  businessTransactions: BusinessTransaction[] = [];
  performanceTrends: PerformanceTrend[] = [];

  // Chart configuration
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
    },
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  };

  performanceChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: 'Response Time (ms)',
        data: [],
        borderColor: '#2196f3',
        backgroundColor: 'rgba(33, 150, 243, 0.1)',
        tension: 0.4,
        yAxisID: 'y'
      },
      {
        label: 'Throughput (CPM)',
        data: [],
        borderColor: '#4caf50',
        backgroundColor: 'rgba(76, 175, 80, 0.1)',
        tension: 0.4,
        yAxisID: 'y1'
      },
      {
        label: 'Error Rate (%)',
        data: [],
        borderColor: '#f44336',
        backgroundColor: 'rgba(244, 67, 54, 0.1)',
        tension: 0.4,
        yAxisID: 'y2'
      }
    ]
  };

  // Table configuration
  transactionColumns = ['name', 'health', 'art', 'cpm', 'errorRate', 'apdex', 'violations', 'actions'];

  constructor(
    private appDynamicsService: AppDynamicsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.clearRefreshTimer();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['context'] && this.context) {
      this.refreshMetrics();
    }
  }

  private initializeComponent(): void {
    this.refreshMetrics();
    this.setupAutoRefresh();
  }

  private setupAutoRefresh(): void {
    this.clearRefreshTimer();
    this.refreshTimer = setInterval(() => {
      this.refreshMetrics();
    }, this.refreshInterval * 1000);
  }

  private clearRefreshTimer(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  updateRefreshInterval(): void {
    this.setupAutoRefresh();
  }

  refreshMetrics(): void {
    this.loading = true;
    this.error = null;

    // Simulate AppDynamics API calls
    // In a real implementation, these would be actual API calls
    this.loadApplicationMetrics();
  }

  private loadApplicationMetrics(): void {
    // Simulate API call delay
    setTimeout(() => {
      try {
        this.applicationMetrics = this.generateMockApplicationMetrics();
        this.businessTransactions = this.applicationMetrics.businessTransactions;
        this.generatePerformanceTrends();
        this.updateChartData();
        this.loading = false;
        this.cdr.detectChanges();
      } catch (error) {
        this.error = 'Failed to load AppDynamics metrics. Please check the connection.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    }, 1500);
  }

  private generateMockApplicationMetrics(): ApplicationMetrics {
    // Generate realistic mock data based on context
    const baseART = this.getContextualBaseART();
    const baseCPM = this.getContextualBaseCPM();
    const baseErrorRate = this.getContextualBaseErrorRate();

    return {
      applicationName: 'ecommerce-microservices',
      overallHealth: this.determineOverallHealth(baseART, baseErrorRate),
      averageResponseTime: baseART,
      totalCallsPerMinute: baseCPM,
      totalErrorsPerMinute: Math.round(baseCPM * baseErrorRate / 100),
      overallErrorRate: baseErrorRate,
      apdexScore: this.calculateApdex(baseART, baseErrorRate),
      activeAlerts: Math.floor(Math.random() * 5),
      businessTransactions: this.generateBusinessTransactions(),
      infrastructureMetrics: {
        cpuUtilization: 45 + Math.random() * 40,
        memoryUtilization: 55 + Math.random() * 30,
        diskIOPS: 150 + Math.random() * 200,
        networkThroughput: 50 + Math.random() * 100 // MB/s
      },
      databaseMetrics: {
        averageResponseTime: baseART * 0.3,
        callsPerMinute: baseCPM * 0.8,
        errorRate: baseErrorRate * 0.5,
        connectionPoolUtilization: 60 + Math.random() * 25
      },
      lastUpdated: new Date()
    };
  }

  private getContextualBaseART(): number {
    if (!this.context) return 150;
    
    switch (this.context.scope) {
      case 'service':
        return this.getServiceSpecificART();
      case 'user':
        return 120 + Math.random() * 100;
      case 'transaction':
        return this.getTransactionSpecificART();
      default:
        return 150 + Math.random() * 100;
    }
  }

  private getServiceSpecificART(): number {
    const serviceARTs = {
      'api-gateway': 50 + Math.random() * 30,
      'user-service': 80 + Math.random() * 40,
      'product-service': 70 + Math.random() * 35,
      'cart-service': 60 + Math.random() * 30,
      'order-service': 120 + Math.random() * 60,
      'intelligent-monitoring-service': 90 + Math.random() * 45
    };
    
    return serviceARTs[this.context?.serviceName as keyof typeof serviceARTs] || 150;
  }

  private getContextualBaseCPM(): number {
    if (!this.context) return 250;
    
    switch (this.context.scope) {
      case 'service':
        return this.getServiceSpecificCPM();
      case 'transaction':
        return this.getTransactionSpecificCPM();
      default:
        return 200 + Math.random() * 100;
    }
  }

  private getServiceSpecificCPM(): number {
    const serviceCPMs = {
      'api-gateway': 800 + Math.random() * 200,
      'user-service': 150 + Math.random() * 50,
      'product-service': 300 + Math.random() * 100,
      'cart-service': 200 + Math.random() * 80,
      'order-service': 100 + Math.random() * 40,
      'intelligent-monitoring-service': 50 + Math.random() * 25
    };
    
    return serviceCPMs[this.context?.serviceName as keyof typeof serviceCPMs] || 250;
  }

  private getContextualBaseErrorRate(): number {
    if (!this.context) return 2.5;
    
    const timeBasedMultiplier = this.getTimeBasedErrorMultiplier();
    const baseRate = 1.5 + Math.random() * 2;
    
    return baseRate * timeBasedMultiplier;
  }

  private getTimeBasedErrorMultiplier(): number {
    const hour = new Date().getHours();
    if (hour >= 9 && hour <= 17) {
      return 1.2; // Business hours - higher load, more errors
    } else if (hour >= 18 && hour <= 22) {
      return 1.5; // Evening peak
    } else {
      return 0.8; // Off-peak hours
    }
  }

  private generateBusinessTransactions(): BusinessTransaction[] {
    const transactions = [
      { name: 'ProductController.getProducts', tier: 'product-service' },
      { name: 'UserController.login', tier: 'user-service' },
      { name: 'CartController.addToCart', tier: 'cart-service' },
      { name: 'OrderController.createOrder', tier: 'order-service' },
      { name: 'GatewayController.routeRequest', tier: 'api-gateway' },
      { name: 'ProductController.getProductDetails', tier: 'product-service' },
      { name: 'UserController.register', tier: 'user-service' },
      { name: 'CartController.getCart', tier: 'cart-service' },
      { name: 'OrderController.getOrderHistory', tier: 'order-service' }
    ];

    return transactions.map((tx, index) => {
      const art = 50 + Math.random() * 200;
      const cpm = 20 + Math.random() * 100;
      const errorRate = Math.random() * 5;
      
      return {
        id: `bt_${index}`,
        name: tx.name,
        tier: tx.tier,
        averageResponseTime: art,
        callsPerMinute: cpm,
        errorsPerMinute: cpm * errorRate / 100,
        errorRate: errorRate,
        throughput: cpm,
        apdexScore: this.calculateApdex(art, errorRate),
        slowestResponseTime: art * (2 + Math.random()),
        fastestResponseTime: art * (0.3 + Math.random() * 0.4),
        standardDeviation: art * 0.2,
        lastUpdated: new Date(),
        healthStatus: this.determineTransactionHealth(art, errorRate),
        trend: this.generateTrend(),
        violations: this.generateViolations(art, errorRate)
      };
    });
  }

  private generatePerformanceTrends(): void {
    const now = new Date();
    const trends: PerformanceTrend[] = [];
    
    for (let i = 30; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * 60000); // Every minute
      trends.push({
        timestamp,
        responseTime: this.applicationMetrics!.averageResponseTime + (Math.random() - 0.5) * 50,
        throughput: this.applicationMetrics!.totalCallsPerMinute + (Math.random() - 0.5) * 20,
        errorRate: this.applicationMetrics!.overallErrorRate + (Math.random() - 0.5) * 1
      });
    }
    
    this.performanceTrends = trends;
  }

  private updateChartData(): void {
    const labels = this.performanceTrends.map(trend => trend.timestamp);
    
    this.performanceChartData = {
      labels,
      datasets: [
        {
          ...this.performanceChartData.datasets[0],
          data: this.performanceTrends.map(trend => ({ x: trend.timestamp, y: trend.responseTime }))
        },
        {
          ...this.performanceChartData.datasets[1],
          data: this.performanceTrends.map(trend => ({ x: trend.timestamp, y: trend.throughput }))
        },
        {
          ...this.performanceChartData.datasets[2],
          data: this.performanceTrends.map(trend => ({ x: trend.timestamp, y: trend.errorRate }))
        }
      ]
    };

    // Update chart options for multiple y-axes
    this.chartOptions = {
      ...this.chartOptions,
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'minute'
          }
        },
        y: {
          type: 'linear',
          display: true,
          position: 'left',
          title: {
            display: true,
            text: 'Response Time (ms)'
          }
        },
        y1: {
          type: 'linear',
          display: true,
          position: 'right',
          title: {
            display: true,
            text: 'Throughput (CPM)'
          },
          grid: {
            drawOnChartArea: false,
          },
        },
        y2: {
          type: 'linear',
          display: false,
          position: 'right',
          title: {
            display: true,
            text: 'Error Rate (%)'
          }
        }
      }
    };
  }

  // Utility methods for health determination
  private determineOverallHealth(art: number, errorRate: number): 'critical' | 'warning' | 'normal' | 'excellent' {
    if (art > 1000 || errorRate > 5) return 'critical';
    if (art > 500 || errorRate > 2) return 'warning';
    if (art > 200 || errorRate > 0.5) return 'normal';
    return 'excellent';
  }

  private determineTransactionHealth(art: number, errorRate: number): 'critical' | 'warning' | 'normal' | 'excellent' {
    return this.determineOverallHealth(art, errorRate);
  }

  private calculateApdex(art: number, errorRate: number): number {
    // Simplified Apdex calculation
    const threshold = 500; // 500ms threshold
    const tolerating = threshold * 4; // 2000ms
    
    if (art <= threshold && errorRate < 1) return 0.9 + Math.random() * 0.1;
    if (art <= tolerating && errorRate < 3) return 0.7 + Math.random() * 0.2;
    if (art <= tolerating * 2 && errorRate < 5) return 0.5 + Math.random() * 0.2;
    return 0.2 + Math.random() * 0.3;
  }

  private generateTrend(): 'up' | 'down' | 'stable' {
    const rand = Math.random();
    if (rand < 0.3) return 'up';
    if (rand < 0.6) return 'down';
    return 'stable';
  }

  private generateViolations(art: number, errorRate: number): string[] {
    const violations: string[] = [];
    
    if (art > 1000) violations.push('Response time threshold exceeded');
    if (errorRate > 5) violations.push('Error rate threshold exceeded');
    if (Math.random() < 0.3) violations.push('Memory utilization high');
    if (Math.random() < 0.2) violations.push('CPU utilization warning');
    
    return violations;
  }

  // UI Helper Methods
  getARTClass(art: number): string {
    if (art < 100) return 'excellent';
    if (art < 300) return 'good';
    if (art < 500) return 'normal';
    if (art < 1000) return 'warning';
    return 'critical';
  }

  getCPMClass(cpm: number): string {
    if (cpm > 100) return 'excellent';
    if (cpm > 50) return 'good';
    if (cpm > 20) return 'normal';
    if (cpm > 10) return 'warning';
    return 'critical';
  }

  getErrorRateClass(errorRate: number): string {
    if (errorRate < 0.5) return 'excellent';
    if (errorRate < 1) return 'good';
    if (errorRate < 2) return 'normal';
    if (errorRate < 5) return 'warning';
    return 'critical';
  }

  getApdexClass(apdex: number): string {
    if (apdex >= 0.9) return 'excellent';
    if (apdex >= 0.7) return 'good';
    if (apdex >= 0.5) return 'normal';
    if (apdex >= 0.25) return 'warning';
    return 'critical';
  }

  getUtilizationClass(utilization: number): string {
    if (utilization < 50) return 'excellent';
    if (utilization < 70) return 'good';
    if (utilization < 80) return 'normal';
    if (utilization < 90) return 'warning';
    return 'critical';
  }

  getHealthChipClass(health: string): string {
    return `health-${health}`;
  }

  getHealthIcon(health: string): string {
    const icons = {
      'excellent': 'sentiment_very_satisfied',
      'normal': 'sentiment_satisfied',
      'warning': 'sentiment_neutral',
      'critical': 'sentiment_very_dissatisfied'
    };
    return icons[health as keyof typeof icons] || 'help';
  }

  getTrendClass(trend: string): string {
    return `trend-${trend}`;
  }

  getTrendIcon(trend: string): string {
    const icons = {
      'up': 'trending_up',
      'down': 'trending_down',
      'stable': 'trending_flat'
    };
    return icons[trend as keyof typeof icons] || 'trending_flat';
  }

  getRowClass(transaction: BusinessTransaction): string {
    return `transaction-row ${transaction.healthStatus}`;
  }

  getApdexDescription(apdex: number): string {
    if (apdex >= 0.9) return 'Excellent user experience';
    if (apdex >= 0.7) return 'Good user experience';
    if (apdex >= 0.5) return 'Fair user experience';
    if (apdex >= 0.25) return 'Poor user experience';
    return 'Unacceptable user experience';
  }

  formatThroughput(throughput: number): string {
    if (throughput < 1) return `${(throughput * 1000).toFixed(0)} KB/s`;
    if (throughput < 1000) return `${throughput.toFixed(1)} MB/s`;
    return `${(throughput / 1000).toFixed(1)} GB/s`;
  }

  getTimeRangeDescription(): string {
    if (!this.context) return 'Last 30 minutes';
    return `Performance trends for ${this.context.timeRange}`;
  }

  // Mock trend methods for KPI cards
  getARTTrendClass(): string { return 'trend-down'; }
  getARTTrendIcon(): string { return 'trending_down'; }
  getARTTrendText(): string { return '5% improvement'; }

  getCPMTrendClass(): string { return 'trend-up'; }
  getCPMTrendIcon(): string { return 'trending_up'; }
  getCPMTrendText(): string { return '12% increase'; }

  getErrorTrendClass(): string { return 'trend-down'; }
  getErrorTrendIcon(): string { return 'trending_down'; }
  getErrorTrendText(): string { return '8% decrease'; }

  // Action handlers
  viewTransactionDetails(transaction: BusinessTransaction): void {
    console.log('View transaction details:', transaction);
    // Implement navigation to detailed transaction view
  }

  analyzeTransaction(transaction: BusinessTransaction): void {
    console.log('Analyze transaction:', transaction);
    // Implement transaction analysis modal or navigation
  }

  // Context-specific methods
  private getTransactionSpecificART(): number {
    const transactionARTs = {
      'checkout': 200 + Math.random() * 100,
      'product-browse': 80 + Math.random() * 40,
      'user-registration': 150 + Math.random() * 75,
      'cart-management': 90 + Math.random() * 45,
      'order-processing': 300 + Math.random() * 150
    };
    
    return transactionARTs[this.context?.transactionType as keyof typeof transactionARTs] || 150;
  }

  private getTransactionSpecificCPM(): number {
    const transactionCPMs = {
      'checkout': 50 + Math.random() * 20,
      'product-browse': 200 + Math.random() * 100,
      'user-registration': 30 + Math.random() * 15,
      'cart-management': 100 + Math.random() * 50,
      'order-processing': 80 + Math.random() * 40
    };
    
    return transactionCPMs[this.context?.transactionType as keyof typeof transactionCPMs] || 150;
  }
}