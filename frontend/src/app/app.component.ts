import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, 
    RouterOutlet, 
    RouterModule, 
    MatToolbarModule, 
    MatButtonModule, 
    MatIconModule
  ],
  template: `
    <mat-toolbar color="primary">
      <span>Ecommerce Platform</span>
      <span class="spacer"></span>
      
      <div *ngIf="!authService.isLoggedIn()">
        <button mat-button routerLink="/login">Login</button>
        <button mat-button routerLink="/register">Register</button>
      </div>
      
      <div *ngIf="authService.isLoggedIn()">
        <button mat-button routerLink="/products">Products</button>
        <button mat-button routerLink="/cart">
          <mat-icon>shopping_cart</mat-icon>
          Cart
        </button>
        <button mat-button routerLink="/orders">Orders</button>
        <button mat-button (click)="logout()">Logout</button>
      </div>
    </mat-toolbar>
    
    <main class="main-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
    .main-content {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }
  `]
})
export class AppComponent {
  title = 'ecommerce-frontend';

  constructor(public authService: AuthService) {}

  logout() {
    this.authService.logout();
  }
}