import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="cart-container">
      <h2>Shopping Cart</h2>
      
      <mat-card class="cart-placeholder">
        <mat-card-content>
          <p>Your cart is empty.</p>
          <p>Add some products to get started!</p>
          <button mat-raised-button color="primary" routerLink="/products">
            Continue Shopping
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .cart-container {
      padding: 20px;
    }
    .cart-placeholder {
      text-align: center;
      padding: 40px;
      margin-top: 20px;
    }
  `]
})
export class CartComponent {}