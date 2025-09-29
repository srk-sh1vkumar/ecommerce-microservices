import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Subject, takeUntil, interval, BehaviorSubject } from 'rxjs';

import { MonitoringService } from '../../../core/services/monitoring.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { TelemetryService } from '../../../core/services/telemetry.service';
import { AppDynamicsMetricsComponent } from '../appdynamics-metrics/appdynamics-metrics.component';

export interface MonitoringContext {
  scope: 'global' | 'service' | 'user' | 'transaction' | 'infrastructure';
  timeRange: string;
  serviceName?: string;
  userId?: string;
  transactionType?: string;
  environment: 'all' | 'production' | 'staging' | 'development';
  filters: {
    severity?: string[];
    tags?: string[];
    regions?: string[];
  };
}

export interface MonitoringService {
  id: string;
  name: string;
  description: string;
  baseUrl: string;
  healthUrl: string;
  icon: string;
  category: 'metrics' | 'logs' | 'traces' | 'alerts' | 'dashboards';
  supportsContext: boolean;
  isAvailable: boolean;
  lastHealthCheck?: Date;
}

@Component({
  selector: 'app-unified-monitoring',
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
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    FormsModule,
    ReactiveFormsModule,
    AppDynamicsMetricsComponent
  ],
  template: `
    <div class="unified-monitoring-container">
      <!-- Global Context Bar -->
      <div class="context-bar">
        <mat-card class="context-card">
          <mat-card-content>
            <div class="context-controls">
              <!-- Monitoring Scope -->
              <mat-form-field appearance="outline" class="scope-field">
                <mat-label>Monitoring Scope</mat-label>
                <mat-select [(value)]="currentContext.scope" (selectionChange)="onContextChange()">
                  <mat-option value="global">
                    <mat-icon>public</mat-icon>
                    Global Overview
                  </mat-option>
                  <mat-option value="service">
                    <mat-icon>apps</mat-icon>
                    Service Focus
                  </mat-option>
                  <mat-option value="user">
                    <mat-icon>person</mat-icon>
                    User Journey
                  </mat-option>
                  <mat-option value="transaction">
                    <mat-icon>swap_horiz</mat-icon>
                    Transaction Analysis
                  </mat-option>
                  <mat-option value="infrastructure">
                    <mat-icon>dns</mat-icon>
                    Infrastructure
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <!-- Time Range -->
              <mat-form-field appearance="outline" class="time-field">
                <mat-label>Time Range</mat-label>
                <mat-select [(value)]="currentContext.timeRange" (selectionChange)="onContextChange()">
                  <mat-option value="5m">Last 5 minutes</mat-option>
                  <mat-option value="15m">Last 15 minutes</mat-option>
                  <mat-option value="1h">Last hour</mat-option>
                  <mat-option value="6h">Last 6 hours</mat-option>
                  <mat-option value="24h">Last 24 hours</mat-option>
                  <mat-option value="7d">Last 7 days</mat-option>
                  <mat-option value="30d">Last 30 days</mat-option>
                </mat-select>
              </mat-form-field>

              <!-- Service Selection (when scope is service) -->
              <mat-form-field appearance="outline" class="service-field" *ngIf="currentContext.scope === 'service'">
                <mat-label>Select Service</mat-label>
                <mat-select [(value)]="currentContext.serviceName" (selectionChange)="onContextChange()">
                  <mat-option value="api-gateway">API Gateway</mat-option>
                  <mat-option value="user-service">User Service</mat-option>
                  <mat-option value="product-service">Product Service</mat-option>
                  <mat-option value="cart-service">Cart Service</mat-option>
                  <mat-option value="order-service">Order Service</mat-option>
                  <mat-option value="intelligent-monitoring-service">Monitoring Service</mat-option>
                </mat-select>
              </mat-form-field>

              <!-- User ID (when scope is user) -->
              <mat-form-field appearance="outline" class="user-field" *ngIf="currentContext.scope === 'user'">
                <mat-label>User ID</mat-label>
                <mat-input [(ngModel)]="currentContext.userId" (blur)="onContextChange()" 
                          placeholder="Enter user ID or email"></mat-input>
              </mat-form-field>

              <!-- Transaction Type (when scope is transaction) -->
              <mat-form-field appearance="outline" class="transaction-field" *ngIf="currentContext.scope === 'transaction'">
                <mat-label>Transaction Type</mat-label>
                <mat-select [(value)]="currentContext.transactionType" (selectionChange)="onContextChange()">
                  <mat-option value="checkout">Checkout Process</mat-option>
                  <mat-option value="product-browse">Product Browsing</mat-option>
                  <mat-option value="user-registration">User Registration</mat-option>
                  <mat-option value="cart-management">Cart Management</mat-option>
                  <mat-option value="order-processing">Order Processing</mat-option>
                </mat-select>
              </mat-form-field>

              <!-- Environment -->
              <mat-form-field appearance="outline" class="env-field">
                <mat-label>Environment</mat-label>
                <mat-select [(value)]="currentContext.environment" (selectionChange)="onContextChange()">
                  <mat-option value="all">All Environments</mat-option>
                  <mat-option value="production">Production</mat-option>
                  <mat-option value="staging">Staging</mat-option>
                  <mat-option value="development">Development</mat-option>
                </mat-select>
              </mat-form-field>

              <!-- Advanced Filters -->
              <button mat-button (click)="toggleAdvancedFilters()" class="filters-toggle">
                <mat-icon>filter_list</mat-icon>
                Filters
                <mat-icon>{{ showAdvancedFilters ? 'expand_less' : 'expand_more' }}</mat-icon>
              </button>

              <!-- Apply Context -->
              <button mat-raised-button color="primary" (click)="applyContext()" class="apply-button">
                <mat-icon>refresh</mat-icon>
                Apply Context
              </button>
            </div>

            <!-- Advanced Filters Panel -->
            <div class="advanced-filters" *ngIf="showAdvancedFilters">
              <div class="filter-row">
                <mat-form-field appearance="outline">
                  <mat-label>Severity Levels</mat-label>
                  <mat-select [(value)]="currentContext.filters.severity" multiple>
                    <mat-option value="critical">Critical</mat-option>
                    <mat-option value="high">High</mat-option>
                    <mat-option value="medium">Medium</mat-option>
                    <mat-option value="low">Low</mat-option>
                    <mat-option value="info">Info</mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Tags</mat-label>
                  <mat-input [(ngModel)]="tagsInput" (blur)="updateTags()" 
                            placeholder="Enter tags (comma-separated)"></mat-input>
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Regions</mat-label>
                  <mat-select [(value)]="currentContext.filters.regions" multiple>
                    <mat-option value="us-east-1">US East</mat-option>
                    <mat-option value="us-west-2">US West</mat-option>
                    <mat-option value="eu-west-1">EU West</mat-option>
                    <mat-option value="ap-southeast-1">Asia Pacific</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>
            </div>

            <!-- Context Summary -->
            <div class="context-summary">
              <mat-chip-listbox>
                <mat-chip class="context-chip scope-chip">
                  <mat-icon>{{ getScopeIcon(currentContext.scope) }}</mat-icon>
                  {{ getScopeLabel(currentContext.scope) }}
                </mat-chip>
                <mat-chip class="context-chip time-chip">
                  <mat-icon>schedule</mat-icon>
                  {{ getTimeRangeLabel(currentContext.timeRange) }}
                </mat-chip>
                <mat-chip class="context-chip env-chip">
                  <mat-icon>cloud</mat-icon>
                  {{ getEnvironmentLabel(currentContext.environment) }}
                </mat-chip>
                <mat-chip *ngIf="currentContext.serviceName" class="context-chip service-chip">
                  <mat-icon>apps</mat-icon>
                  {{ currentContext.serviceName }}
                </mat-chip>
                <mat-chip *ngIf="currentContext.userId" class="context-chip user-chip">
                  <mat-icon>person</mat-icon>
                  {{ currentContext.userId }}
                </mat-chip>
                <mat-chip *ngIf="currentContext.transactionType" class="context-chip transaction-chip">
                  <mat-icon>swap_horiz</mat-icon>
                  {{ getTransactionTypeLabel(currentContext.transactionType) }}
                </mat-chip>
              </mat-chip-listbox>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Service Health Overview -->
      <div class="services-health" *ngIf="!allServicesHealthy">
        <mat-card class="health-card warning">
          <mat-card-content>
            <div class="health-alert">
              <mat-icon class="warning-icon">warning</mat-icon>
              <div class="health-info">
                <strong>Monitoring Services Status</strong>
                <div class="health-services">
                  <span *ngFor="let service of monitoringServices" 
                        [class]="getServiceHealthClass(service)">
                    {{ service.name }}: {{ service.isAvailable ? 'Available' : 'Unavailable' }}
                  </span>
                </div>
              </div>
              <button mat-button color="primary" (click)="checkAllServicesHealth()">
                <mat-icon>refresh</mat-icon>
                Check Status
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Unified Monitoring Tabs -->
      <mat-tab-group class="monitoring-tabs" 
                     [(selectedIndex)]="selectedTab" 
                     (selectedTabChange)="onTabChange($event)">
        
        <!-- Grafana Dashboards -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>dashboard</mat-icon>
            Grafana Dashboards
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('grafana')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('grafana')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content">
            <div class="iframe-container" *ngIf="getServiceById('grafana')?.isAvailable; else grafanaUnavailable">
              <div class="iframe-toolbar">
                <mat-form-field appearance="outline">
                  <mat-label>Dashboard</mat-label>
                  <mat-select [(value)]="selectedGrafanaDashboard" (selectionChange)="updateGrafanaUrl()">
                    <mat-option value="overview">System Overview</mat-option>
                    <mat-option value="services">Services Dashboard</mat-option>
                    <mat-option value="infrastructure">Infrastructure Metrics</mat-option>
                    <mat-option value="business">Business Metrics</mat-option>
                    <mat-option value="errors">Error Analysis</mat-option>
                    <mat-option value="performance">Performance Metrics</mat-option>
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button (click)="refreshGrafana()" matTooltip="Refresh Dashboard">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button (click)="openInNewTab(grafanaUrl)" matTooltip="Open in New Tab">
                  <mat-icon>open_in_new</mat-icon>
                </button>
              </div>
              <iframe [src]="grafanaUrl" 
                      class="monitoring-iframe"
                      frameborder="0"
                      #grafanaFrame>
              </iframe>
            </div>
            <ng-template #grafanaUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>Grafana Unavailable</h3>
                <p>Grafana service is currently unavailable. Please check the service status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('grafana')">
                  Retry Connection
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

        <!-- Prometheus Metrics -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>trending_up</mat-icon>
            Prometheus Metrics
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('prometheus')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('prometheus')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content">
            <div class="iframe-container" *ngIf="getServiceById('prometheus')?.isAvailable; else prometheusUnavailable">
              <div class="iframe-toolbar">
                <mat-form-field appearance="outline">
                  <mat-label>Query Type</mat-label>
                  <mat-select [(value)]="selectedPrometheusQuery" (selectionChange)="updatePrometheusUrl()">
                    <mat-option value="targets">Targets Status</mat-option>
                    <mat-option value="alerts">Active Alerts</mat-option>
                    <mat-option value="rules">Recording Rules</mat-option>
                    <mat-option value="config">Configuration</mat-option>
                    <mat-option value="flags">Runtime Flags</mat-option>
                    <mat-option value="graph">Query Browser</mat-option>
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button (click)="refreshPrometheus()" matTooltip="Refresh">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button (click)="openInNewTab(prometheusUrl)" matTooltip="Open in New Tab">
                  <mat-icon>open_in_new</mat-icon>
                </button>
              </div>
              <iframe [src]="prometheusUrl" 
                      class="monitoring-iframe"
                      frameborder="0"
                      #prometheusFrame>
              </iframe>
            </div>
            <ng-template #prometheusUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>Prometheus Unavailable</h3>
                <p>Prometheus service is currently unavailable. Please check the service status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('prometheus')">
                  Retry Connection
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

        <!-- Alertmanager -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>notifications</mat-icon>
            Alert Manager
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('alertmanager')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('alertmanager')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content">
            <div class="iframe-container" *ngIf="getServiceById('alertmanager')?.isAvailable; else alertmanagerUnavailable">
              <div class="iframe-toolbar">
                <mat-form-field appearance="outline">
                  <mat-label>Alert View</mat-label>
                  <mat-select [(value)]="selectedAlertmanagerView" (selectionChange)="updateAlertmanagerUrl()">
                    <mat-option value="alerts">Active Alerts</mat-option>
                    <mat-option value="silences">Silences</mat-option>
                    <mat-option value="status">Status</mat-option>
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button (click)="refreshAlertmanager()" matTooltip="Refresh">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button (click)="openInNewTab(alertmanagerUrl)" matTooltip="Open in New Tab">
                  <mat-icon>open_in_new</mat-icon>
                </button>
              </div>
              <iframe [src]="alertmanagerUrl" 
                      class="monitoring-iframe"
                      frameborder="0"
                      #alertmanagerFrame>
              </iframe>
            </div>
            <ng-template #alertmanagerUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>Alertmanager Unavailable</h3>
                <p>Alertmanager service is currently unavailable. Please check the service status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('alertmanager')">
                  Retry Connection
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

        <!-- Tempo Tracing -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>timeline</mat-icon>
            Tempo Tracing
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('tempo')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('tempo')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content">
            <div class="iframe-container" *ngIf="getServiceById('tempo')?.isAvailable; else tempoUnavailable">
              <div class="iframe-toolbar">
                <mat-form-field appearance="outline">
                  <mat-label>Trace View</mat-label>
                  <mat-select [(value)]="selectedTempoView" (selectionChange)="updateTempoUrl()">
                    <mat-option value="search">Trace Search</mat-option>
                    <mat-option value="recent">Recent Traces</mat-option>
                    <mat-option value="metrics">Trace Metrics</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" *ngIf="currentContext.scope === 'user' && currentContext.userId">
                  <mat-label>Trace ID</mat-label>
                  <mat-input [(ngModel)]="traceId" placeholder="Enter trace ID"></mat-input>
                </mat-form-field>
                <button mat-icon-button (click)="refreshTempo()" matTooltip="Refresh">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button (click)="openInNewTab(tempoUrl)" matTooltip="Open in New Tab">
                  <mat-icon>open_in_new</mat-icon>
                </button>
              </div>
              <iframe [src]="tempoUrl" 
                      class="monitoring-iframe"
                      frameborder="0"
                      #tempoFrame>
              </iframe>
            </div>
            <ng-template #tempoUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>Tempo Unavailable</h3>
                <p>Tempo tracing service is currently unavailable. Please check the service status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('tempo')">
                  Retry Connection
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

        <!-- Logs (Logstash/ELK) -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>article</mat-icon>
            Logs Analysis
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('elasticsearch')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('elasticsearch')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content">
            <div class="iframe-container" *ngIf="getServiceById('elasticsearch')?.isAvailable; else elasticsearchUnavailable">
              <div class="iframe-toolbar">
                <mat-form-field appearance="outline">
                  <mat-label>Log View</mat-label>
                  <mat-select [(value)]="selectedLogView" (selectionChange)="updateElasticsearchUrl()">
                    <mat-option value="discover">Log Discovery</mat-option>
                    <mat-option value="dashboard">Log Dashboard</mat-option>
                    <mat-option value="visualize">Log Visualization</mat-option>
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button (click)="refreshElasticsearch()" matTooltip="Refresh">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button (click)="openInNewTab(elasticsearchUrl)" matTooltip="Open in New Tab">
                  <mat-icon>open_in_new</mat-icon>
                </button>
              </div>
              <iframe [src]="elasticsearchUrl" 
                      class="monitoring-iframe"
                      frameborder="0"
                      #elasticsearchFrame>
              </iframe>
            </div>
            <ng-template #elasticsearchUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>Elasticsearch/Kibana Unavailable</h3>
                <p>Elasticsearch/Kibana service is currently unavailable. Please check the service status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('elasticsearch')">
                  Retry Connection
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

        <!-- AppDynamics Integration -->
        <mat-tab>
          <ng-template mat-tab-label>
            <mat-icon>insights</mat-icon>
            AppDynamics Metrics
            <mat-icon class="health-indicator" 
                      [class]="getServiceById('appdynamics')?.isAvailable ? 'available' : 'unavailable'">
              {{ getServiceById('appdynamics')?.isAvailable ? 'check_circle' : 'error' }}
            </mat-icon>
          </ng-template>
          <div class="tab-content appdynamics-tab">
            <div *ngIf="getServiceById('appdynamics')?.isAvailable; else appdynamicsUnavailable">
              <app-appdynamics-metrics [context]="currentContext"></app-appdynamics-metrics>
            </div>
            <ng-template #appdynamicsUnavailable>
              <div class="service-unavailable">
                <mat-icon>error_outline</mat-icon>
                <h3>AppDynamics Unavailable</h3>
                <p>AppDynamics service is currently unavailable or not configured. Please check the integration status.</p>
                <button mat-raised-button color="primary" (click)="checkServiceHealth('appdynamics')">
                  Check Integration
                </button>
              </div>
            </ng-template>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .unified-monitoring-container {
      padding: 20px;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .context-bar {
      margin-bottom: 20px;
      flex-shrink: 0;
    }

    .context-card {
      background: #f8f9fa;
      border-left: 4px solid #1976d2;
    }

    .context-controls {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }

    .scope-field {
      min-width: 200px;
    }

    .time-field {
      min-width: 160px;
    }

    .service-field, .user-field, .transaction-field, .env-field {
      min-width: 180px;
    }

    .filters-toggle {
      margin-left: auto;
    }

    .apply-button {
      margin-left: 8px;
    }

    .advanced-filters {
      border-top: 1px solid #e0e0e0;
      padding-top: 16px;
      margin-top: 16px;
    }

    .filter-row {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
    }

    .context-summary {
      margin-top: 12px;
    }

    .context-chip {
      margin: 2px 4px;
    }

    .scope-chip {
      background-color: #e3f2fd;
      color: #1976d2;
    }

    .time-chip {
      background-color: #f3e5f5;
      color: #7b1fa2;
    }

    .env-chip {
      background-color: #e8f5e8;
      color: #388e3c;
    }

    .service-chip {
      background-color: #fff3e0;
      color: #f57c00;
    }

    .user-chip {
      background-color: #fce4ec;
      color: #c2185b;
    }

    .transaction-chip {
      background-color: #e0f2f1;
      color: #00695c;
    }

    .services-health {
      margin-bottom: 16px;
      flex-shrink: 0;
    }

    .health-card.warning {
      border-left: 4px solid #ff9800;
      background-color: #fff8e1;
    }

    .health-alert {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .warning-icon {
      color: #ff9800;
      font-size: 2rem;
    }

    .health-info {
      flex: 1;
    }

    .health-services {
      display: flex;
      gap: 16px;
      margin-top: 8px;
      font-size: 0.9rem;
    }

    .service-available {
      color: #4caf50;
    }

    .service-unavailable {
      color: #f44336;
    }

    .monitoring-tabs {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .monitoring-tabs ::ng-deep .mat-mdc-tab-group {
      height: 100%;
    }

    .monitoring-tabs ::ng-deep .mat-mdc-tab-body-wrapper {
      flex: 1;
    }

    .monitoring-tabs ::ng-deep .mat-mdc-tab-body {
      height: 100%;
    }

    .tab-content {
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .iframe-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .iframe-toolbar {
      display: flex;
      gap: 16px;
      align-items: center;
      padding: 16px;
      background: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;
      flex-shrink: 0;
    }

    .monitoring-iframe {
      flex: 1;
      width: 100%;
      min-height: 600px;
    }

    .service-unavailable {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 40px;
    }

    .service-unavailable mat-icon {
      font-size: 4rem;
      width: 4rem;
      height: 4rem;
      color: #f44336;
      margin-bottom: 16px;
    }

    .health-indicator {
      font-size: 1rem;
      margin-left: 4px;
    }

    .health-indicator.available {
      color: #4caf50;
    }

    .health-indicator.unavailable {
      color: #f44336;
    }

    .mat-tab-label {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .context-controls {
        flex-direction: column;
        align-items: stretch;
      }

      .scope-field, .time-field, .service-field, 
      .user-field, .transaction-field, .env-field {
        min-width: auto;
        width: 100%;
      }

      .filters-toggle, .apply-button {
        margin-left: 0;
        width: 100%;
      }

      .health-alert {
        flex-direction: column;
        text-align: center;
      }

      .health-services {
        flex-direction: column;
        gap: 8px;
      }

      .iframe-toolbar {
        flex-direction: column;
        align-items: stretch;
        gap: 12px;
      }
    }

    /* Additional Styles */
    .text-info {
      color: #1976d2;
    }

    /* AppDynamics Tab Specific Styles */
    .appdynamics-tab {
      height: 100%;
      overflow-y: auto;
    }

    .appdynamics-tab app-appdynamics-metrics {
      display: block;
      height: 100%;
    }
  `]
})
export class UnifiedMonitoringComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Component state
  selectedTab = 0;
  showAdvancedFilters = false;
  tagsInput = '';
  traceId = '';

  // Context management
  currentContext: MonitoringContext = {
    scope: 'global',
    timeRange: '1h',
    environment: 'all',
    filters: {
      severity: [],
      tags: [],
      regions: []
    }
  };

  // Service selections
  selectedGrafanaDashboard = 'overview';
  selectedPrometheusQuery = 'targets';
  selectedAlertmanagerView = 'alerts';
  selectedTempoView = 'search';
  selectedLogView = 'discover';
  selectedAppDynamicsView = 'dashboard';

  // Service URLs
  grafanaUrl: SafeResourceUrl = '';
  prometheusUrl: SafeResourceUrl = '';
  alertmanagerUrl: SafeResourceUrl = '';
  tempoUrl: SafeResourceUrl = '';
  elasticsearchUrl: SafeResourceUrl = '';
  appDynamicsUrl: SafeResourceUrl = '';

  // Monitoring services configuration
  monitoringServices: MonitoringService[] = [
    {
      id: 'grafana',
      name: 'Grafana',
      description: 'Visualization and dashboards',
      baseUrl: 'http://localhost:3000',
      healthUrl: 'http://localhost:3000/api/health',
      icon: 'dashboard',
      category: 'dashboards',
      supportsContext: true,
      isAvailable: false
    },
    {
      id: 'prometheus',
      name: 'Prometheus',
      description: 'Metrics collection and storage',
      baseUrl: 'http://localhost:9090',
      healthUrl: 'http://localhost:9090/-/healthy',
      icon: 'trending_up',
      category: 'metrics',
      supportsContext: true,
      isAvailable: false
    },
    {
      id: 'alertmanager',
      name: 'Alertmanager',
      description: 'Alert management and routing',
      baseUrl: 'http://localhost:9093',
      healthUrl: 'http://localhost:9093/-/healthy',
      icon: 'notifications',
      category: 'alerts',
      supportsContext: true,
      isAvailable: false
    },
    {
      id: 'tempo',
      name: 'Tempo',
      description: 'Distributed tracing backend',
      baseUrl: 'http://localhost:3200',
      healthUrl: 'http://localhost:3200/ready',
      icon: 'timeline',
      category: 'traces',
      supportsContext: true,
      isAvailable: false
    },
    {
      id: 'elasticsearch',
      name: 'Elasticsearch/Kibana',
      description: 'Log storage and analysis',
      baseUrl: 'http://localhost:5601',
      healthUrl: 'http://localhost:5601/api/status',
      icon: 'article',
      category: 'logs',
      supportsContext: true,
      isAvailable: false
    },
    {
      id: 'appdynamics',
      name: 'AppDynamics',
      description: 'Application performance monitoring',
      baseUrl: 'https://your-account.saas.appdynamics.com',
      healthUrl: '/api/monitoring/appdynamics/health',
      icon: 'insights',
      category: 'metrics',
      supportsContext: true,
      isAvailable: false
    }
  ];

  constructor(
    private monitoringService: MonitoringService,
    private webSocketService: WebSocketService,
    private telemetryService: TelemetryService,
    private sanitizer: DomSanitizer,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeMonitoring();
    this.checkAllServicesHealth();
    this.setupRealTimeUpdates();
    this.updateAllUrls();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeMonitoring(): void {
    this.telemetryService.trackEvent('unified_monitoring_accessed', {
      timestamp: new Date().toISOString(),
      context: this.currentContext
    });
  }

  private setupRealTimeUpdates(): void {
    // Subscribe to service health updates
    this.webSocketService.on('service_health')
      .pipe(takeUntil(this.destroy$))
      .subscribe(health => {
        this.updateServiceHealth(health);
      });

    // Subscribe to monitoring events
    this.webSocketService.on('monitoring_alert')
      .pipe(takeUntil(this.destroy$))
      .subscribe(alert => {
        this.handleMonitoringAlert(alert);
      });
  }

  // Context management methods
  onContextChange(): void {
    this.telemetryService.trackEvent('monitoring_context_changed', {
      newContext: this.currentContext,
      timestamp: new Date().toISOString()
    });
    
    this.updateAllUrls();
  }

  applyContext(): void {
    this.updateAllUrls();
    this.refreshCurrentTab();
    
    this.snackBar.open('Context applied to all monitoring services', 'Close', {
      duration: 3000
    });
  }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  updateTags(): void {
    this.currentContext.filters.tags = this.tagsInput
      .split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0);
  }

  // Service health management
  checkAllServicesHealth(): void {
    this.monitoringServices.forEach(service => {
      this.checkServiceHealth(service.id);
    });
  }

  checkServiceHealth(serviceId: string): void {
    const service = this.getServiceById(serviceId);
    if (!service) return;

    if (serviceId === 'appdynamics') {
      // Check AppDynamics through our monitoring service
      this.monitoringService.checkAppDynamicsHealth().subscribe({
        next: (response) => {
          service.isAvailable = response.healthy;
          service.lastHealthCheck = new Date();
          this.cdr.detectChanges();
        },
        error: () => {
          service.isAvailable = false;
          service.lastHealthCheck = new Date();
          this.cdr.detectChanges();
        }
      });
    } else {
      // Mock health check for other services
      // In a real implementation, you would make actual health check requests
      setTimeout(() => {
        service.isAvailable = Math.random() > 0.2; // 80% availability simulation
        service.lastHealthCheck = new Date();
        this.cdr.detectChanges();
      }, 1000);
    }
  }

  get allServicesHealthy(): boolean {
    return this.monitoringServices.every(service => service.isAvailable);
  }

  getServiceById(id: string): MonitoringService | undefined {
    return this.monitoringServices.find(service => service.id === id);
  }

  getServiceHealthClass(service: MonitoringService): string {
    return service.isAvailable ? 'service-available' : 'service-unavailable';
  }

  // URL generation methods
  private updateAllUrls(): void {
    this.updateGrafanaUrl();
    this.updatePrometheusUrl();
    this.updateAlertmanagerUrl();
    this.updateTempoUrl();
    this.updateElasticsearchUrl();
    this.updateAppDynamicsUrl();
  }

  updateGrafanaUrl(): void {
    const service = this.getServiceById('grafana');
    if (!service) return;

    let url = `${service.baseUrl}/d/${this.selectedGrafanaDashboard}`;
    
    // Add context parameters
    const params = new URLSearchParams();
    params.set('from', this.getFromTimestamp());
    params.set('to', 'now');
    params.set('refresh', '30s');
    
    if (this.currentContext.serviceName) {
      params.set('var-service', this.currentContext.serviceName);
    }
    
    if (this.currentContext.environment !== 'all') {
      params.set('var-environment', this.currentContext.environment);
    }

    url += `?${params.toString()}`;
    this.grafanaUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  updatePrometheusUrl(): void {
    const service = this.getServiceById('prometheus');
    if (!service) return;

    let url = `${service.baseUrl}`;
    
    switch (this.selectedPrometheusQuery) {
      case 'graph':
        url += '/graph';
        break;
      case 'alerts':
        url += '/alerts';
        break;
      case 'targets':
        url += '/targets';
        break;
      case 'rules':
        url += '/rules';
        break;
      case 'config':
        url += '/config';
        break;
      case 'flags':
        url += '/flags';
        break;
      default:
        url += '/targets';
    }

    this.prometheusUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  updateAlertmanagerUrl(): void {
    const service = this.getServiceById('alertmanager');
    if (!service) return;

    let url = `${service.baseUrl}`;
    
    switch (this.selectedAlertmanagerView) {
      case 'alerts':
        url += '/#/alerts';
        break;
      case 'silences':
        url += '/#/silences';
        break;
      case 'status':
        url += '/#/status';
        break;
      default:
        url += '/#/alerts';
    }

    this.alertmanagerUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  updateTempoUrl(): void {
    const service = this.getServiceById('tempo');
    if (!service) return;

    // For Tempo, we'll use Grafana's Explore interface
    const grafanaService = this.getServiceById('grafana');
    if (!grafanaService) return;

    let url = `${grafanaService.baseUrl}/explore`;
    
    const params = new URLSearchParams();
    params.set('orgId', '1');
    params.set('left', JSON.stringify({
      datasource: 'tempo',
      queries: [{
        queryType: '',
        refId: 'A'
      }],
      range: {
        from: this.getFromTimestamp(),
        to: 'now'
      }
    }));

    if (this.traceId) {
      // If trace ID is provided, search for specific trace
      const query = params.get('left');
      if (query) {
        const parsed = JSON.parse(query);
        parsed.queries[0].query = this.traceId;
        params.set('left', JSON.stringify(parsed));
      }
    }

    url += `?${params.toString()}`;
    this.tempoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  updateElasticsearchUrl(): void {
    const service = this.getServiceById('elasticsearch');
    if (!service) return;

    let url = `${service.baseUrl}/app/kibana`;
    
    switch (this.selectedLogView) {
      case 'discover':
        url += '#/discover';
        break;
      case 'dashboard':
        url += '#/dashboard';
        break;
      case 'visualize':
        url += '#/visualize';
        break;
      default:
        url += '#/discover';
    }

    // Add time range and filters
    const params = new URLSearchParams();
    params.set('_g', JSON.stringify({
      time: {
        from: this.getFromTimestamp(),
        to: 'now'
      }
    }));

    if (this.currentContext.serviceName) {
      params.set('_a', JSON.stringify({
        query: {
          match: {
            'service.name': this.currentContext.serviceName
          }
        }
      }));
    }

    url += `?${params.toString()}`;
    this.elasticsearchUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  updateAppDynamicsUrl(): void {
    const service = this.getServiceById('appdynamics');
    if (!service) return;

    let url = `${service.baseUrl}/controller`;
    
    switch (this.selectedAppDynamicsView) {
      case 'dashboard':
        url += '#/location=APP_DASHBOARD';
        break;
      case 'flowmap':
        url += '#/location=APP_FLOW_MAP';
        break;
      case 'business-transactions':
        url += '#/location=APP_BT_LIST';
        break;
      case 'errors':
        url += '#/location=APP_ERROR_LIST';
        break;
      case 'infrastructure':
        url += '#/location=APP_INFRASTRUCTURE_DASHBOARD';
        break;
      default:
        url += '#/location=APP_DASHBOARD';
    }

    // Add context parameters
    if (this.currentContext.serviceName) {
      url += `&service=${this.currentContext.serviceName}`;
    }

    url += `&timeRange=${this.currentContext.timeRange}`;

    this.appDynamicsUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  // Utility methods
  private getFromTimestamp(): string {
    const now = new Date();
    const timeRange = this.currentContext.timeRange;
    
    switch (timeRange) {
      case '5m':
        return new Date(now.getTime() - 5 * 60 * 1000).toISOString();
      case '15m':
        return new Date(now.getTime() - 15 * 60 * 1000).toISOString();
      case '1h':
        return new Date(now.getTime() - 60 * 60 * 1000).toISOString();
      case '6h':
        return new Date(now.getTime() - 6 * 60 * 60 * 1000).toISOString();
      case '24h':
        return new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
      case '7d':
        return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
      case '30d':
        return new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
      default:
        return new Date(now.getTime() - 60 * 60 * 1000).toISOString();
    }
  }

  // Tab management
  onTabChange(event: any): void {
    this.selectedTab = event.index;
    this.telemetryService.trackEvent('monitoring_tab_changed', {
      tabIndex: this.selectedTab,
      tabName: this.getTabName(this.selectedTab),
      timestamp: new Date().toISOString()
    });
  }

  private getTabName(index: number): string {
    const tabNames = ['grafana', 'prometheus', 'alertmanager', 'tempo', 'logs', 'appdynamics'];
    return tabNames[index] || 'unknown';
  }

  private refreshCurrentTab(): void {
    const tabName = this.getTabName(this.selectedTab);
    switch (tabName) {
      case 'grafana':
        this.refreshGrafana();
        break;
      case 'prometheus':
        this.refreshPrometheus();
        break;
      case 'alertmanager':
        this.refreshAlertmanager();
        break;
      case 'tempo':
        this.refreshTempo();
        break;
      case 'logs':
        this.refreshElasticsearch();
        break;
      case 'appdynamics':
        this.refreshAppDynamics();
        break;
    }
  }

  // Refresh methods
  refreshGrafana(): void {
    this.updateGrafanaUrl();
    // Force iframe reload
    const iframe = document.querySelector('iframe[src*="localhost:3000"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  refreshPrometheus(): void {
    this.updatePrometheusUrl();
    const iframe = document.querySelector('iframe[src*="localhost:9090"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  refreshAlertmanager(): void {
    this.updateAlertmanagerUrl();
    const iframe = document.querySelector('iframe[src*="localhost:9093"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  refreshTempo(): void {
    this.updateTempoUrl();
    const iframe = document.querySelector('iframe[src*="explore"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  refreshElasticsearch(): void {
    this.updateElasticsearchUrl();
    const iframe = document.querySelector('iframe[src*="localhost:5601"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  refreshAppDynamics(): void {
    this.updateAppDynamicsUrl();
    const iframe = document.querySelector('iframe[src*="appdynamics"]') as HTMLIFrameElement;
    if (iframe) {
      iframe.src = iframe.src;
    }
  }

  openInNewTab(url: SafeResourceUrl): void {
    const urlString = (url as any).changingThisBreaksApplicationSecurity;
    window.open(urlString, '_blank');
  }

  // Event handlers
  private updateServiceHealth(health: any): void {
    // Update service health based on WebSocket updates
    if (health.serviceName && health.status) {
      const service = this.monitoringServices.find(s => s.name.toLowerCase() === health.serviceName.toLowerCase());
      if (service) {
        service.isAvailable = health.status === 'up';
        service.lastHealthCheck = new Date();
        this.cdr.detectChanges();
      }
    }
  }

  private handleMonitoringAlert(alert: any): void {
    this.snackBar.open(`Monitoring Alert: ${alert.message}`, 'View', {
      duration: 10000,
      panelClass: ['alert-snackbar']
    });
  }

  // Label getters for display
  getScopeIcon(scope: string): string {
    const icons = {
      'global': 'public',
      'service': 'apps',
      'user': 'person',
      'transaction': 'swap_horiz',
      'infrastructure': 'dns'
    };
    return icons[scope as keyof typeof icons] || 'help';
  }

  getScopeLabel(scope: string): string {
    const labels = {
      'global': 'Global Overview',
      'service': 'Service Focus',
      'user': 'User Journey',
      'transaction': 'Transaction Analysis',
      'infrastructure': 'Infrastructure'
    };
    return labels[scope as keyof typeof labels] || scope;
  }

  getTimeRangeLabel(timeRange: string): string {
    const labels = {
      '5m': 'Last 5 minutes',
      '15m': 'Last 15 minutes',
      '1h': 'Last hour',
      '6h': 'Last 6 hours',
      '24h': 'Last 24 hours',
      '7d': 'Last 7 days',
      '30d': 'Last 30 days'
    };
    return labels[timeRange as keyof typeof labels] || timeRange;
  }

  getEnvironmentLabel(environment: string): string {
    const labels = {
      'all': 'All Environments',
      'production': 'Production',
      'staging': 'Staging',
      'development': 'Development'
    };
    return labels[environment as keyof typeof labels] || environment;
  }

  getTransactionTypeLabel(transactionType: string): string {
    const labels = {
      'checkout': 'Checkout Process',
      'product-browse': 'Product Browsing',
      'user-registration': 'User Registration',
      'cart-management': 'Cart Management',
      'order-processing': 'Order Processing'
    };
    return labels[transactionType as keyof typeof labels] || transactionType;
  }
}