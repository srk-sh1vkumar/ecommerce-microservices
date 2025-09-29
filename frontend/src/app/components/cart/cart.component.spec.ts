import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { CartComponent } from './cart.component';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CartComponent,
        RouterTestingModule,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display cart title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('h2');
    
    expect(title?.textContent).toBe('Shopping Cart');
  });

  it('should display empty cart message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const emptyMessage = compiled.querySelector('.cart-placeholder p');
    
    expect(emptyMessage?.textContent).toBe('Your cart is empty.');
  });

  it('should have continue shopping button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const continueButton = compiled.querySelector('button[routerLink="/products"]');
    
    expect(continueButton).toBeTruthy();
    expect(continueButton?.textContent?.trim()).toBe('Continue Shopping');
  });
});