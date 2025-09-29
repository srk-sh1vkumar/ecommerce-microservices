import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="home-container">
      <mat-card class="welcome-card">
        <mat-card-header>
          <mat-card-title>Welcome to Ecommerce Platform</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Your one-stop shop for everything you need!</p>
          
          <div *ngIf="!authService.isLoggedIn()" class="auth-buttons">
            <p>Please login or register to start shopping.</p>
            <button mat-raised-button color="primary" routerLink="/login">Login</button>
            <button mat-raised-button color="accent" routerLink="/register">Register</button>
          </div>
          
          <div *ngIf="authService.isLoggedIn()" class="welcome-back">
            <p>Welcome back! Start shopping now.</p>
            <button mat-raised-button color="primary" routerLink="/products">Browse Products</button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .home-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 60vh;
      padding: 20px;
    }
    .welcome-card {
      max-width: 600px;
      text-align: center;
    }
    .auth-buttons, .welcome-back {
      margin-top: 20px;
    }
    .auth-buttons button {
      margin: 0 10px;
    }
  `]
})
export class HomeComponent {
  constructor(public authService: AuthService) {}
}