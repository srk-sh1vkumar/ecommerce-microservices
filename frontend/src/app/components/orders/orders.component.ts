import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="orders-container">
      <h2>My Orders</h2>
      
      <mat-card class="orders-placeholder">
        <mat-card-content>
          <p>You haven't placed any orders yet.</p>
          <p>Start shopping to see your order history here!</p>
          <button mat-raised-button color="primary" routerLink="/products">
            Start Shopping
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .orders-container {
      padding: 20px;
    }
    .orders-placeholder {
      text-align: center;
      padding: 40px;
      margin-top: 20px;
    }
  `]
})
export class OrdersComponent {}