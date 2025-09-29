import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { OrdersComponent } from './orders.component';

describe('OrdersComponent', () => {
  let component: OrdersComponent;
  let fixture: ComponentFixture<OrdersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        OrdersComponent,
        RouterTestingModule,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display orders title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('h2');
    
    expect(title?.textContent).toBe('My Orders');
  });

  it('should display no orders message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const noOrdersMessage = compiled.querySelector('.orders-placeholder p');
    
    expect(noOrdersMessage?.textContent).toBe('You haven\'t placed any orders yet.');
  });

  it('should have start shopping button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const startButton = compiled.querySelector('button[routerLink="/products"]');
    
    expect(startButton).toBeTruthy();
    expect(startButton?.textContent?.trim()).toBe('Start Shopping');
  });
});