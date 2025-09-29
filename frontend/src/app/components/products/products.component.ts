import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  stock: number;
  category: string;
}

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="products-container">
      <h2>Products</h2>
      
      <div *ngIf="loading" class="loading">
        <mat-spinner></mat-spinner>
        <p>Loading products...</p>
      </div>

      <div *ngIf="!loading" class="products-grid">
        <mat-card *ngFor="let product of products" class="product-card">
          <mat-card-header>
            <mat-card-title>{{ product.name }}</mat-card-title>
            <mat-card-subtitle>{{ product.category }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p>{{ product.description }}</p>
            <div class="price">\${{ product.price.toFixed(2) }}</div>
            <div class="stock">In Stock: {{ product.stock }}</div>
          </mat-card-content>
          <mat-card-actions>
            <button 
              mat-raised-button 
              color="primary"
              [disabled]="product.stock === 0"
              (click)="addToCart(product)">
              <mat-icon>add_shopping_cart</mat-icon>
              Add to Cart
            </button>
          </mat-card-actions>
        </mat-card>
      </div>

      <div *ngIf="!loading && products.length === 0" class="no-products">
        <p>No products available at the moment.</p>
      </div>
    </div>
  `,
  styles: [`
    .products-container {
      padding: 20px;
    }
    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px;
    }
    .products-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
      margin-top: 20px;
    }
    .product-card {
      height: fit-content;
    }
    .price {
      font-size: 1.2em;
      font-weight: bold;
      color: #2e7d32;
      margin: 10px 0;
    }
    .stock {
      font-size: 0.9em;
      color: #666;
    }
    .no-products {
      text-align: center;
      padding: 40px;
      color: #666;
    }
  `]
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  loading = true;

  ngOnInit() {
    // Simulate loading products - in real app, call product service
    setTimeout(() => {
      this.products = [
        {
          id: '1',
          name: 'Laptop',
          description: 'High-performance laptop for work and gaming',
          price: 999.99,
          stock: 10,
          category: 'Electronics'
        },
        {
          id: '2',
          name: 'Smartphone',
          description: 'Latest smartphone with advanced features',
          price: 699.99,
          stock: 25,
          category: 'Electronics'
        },
        {
          id: '3',
          name: 'Headphones',
          description: 'Wireless noise-canceling headphones',
          price: 199.99,
          stock: 15,
          category: 'Accessories'
        }
      ];
      this.loading = false;
    }, 1000);
  }

  addToCart(product: Product) {
    // TODO: Implement add to cart functionality
    console.log('Adding to cart:', product);
    alert(\`Added \${product.name} to cart!\`);
  }
}