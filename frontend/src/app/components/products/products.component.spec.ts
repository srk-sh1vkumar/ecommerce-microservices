import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ProductsComponent } from './products.component';

describe('ProductsComponent', () => {
  let component: ProductsComponent;
  let fixture: ComponentFixture<ProductsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ProductsComponent,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', () => {
    expect(component.loading).toBeTruthy();
    expect(component.products).toEqual([]);
  });

  it('should load products after initialization', (done) => {
    fixture.detectChanges();
    
    // Wait for the setTimeout in ngOnInit to complete
    setTimeout(() => {
      expect(component.loading).toBeFalsy();
      expect(component.products.length).toBeGreaterThan(0);
      expect(component.products[0].name).toBe('Laptop');
      done();
    }, 1100);
  });

  it('should display products in template after loading', (done) => {
    fixture.detectChanges();
    
    setTimeout(() => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const productCards = compiled.querySelectorAll('.product-card');
      
      expect(productCards.length).toBe(3);
      expect(productCards[0].querySelector('mat-card-title')?.textContent).toBe('Laptop');
      done();
    }, 1100);
  });

  it('should call addToCart when add to cart button is clicked', () => {
    spyOn(component, 'addToCart');
    spyOn(window, 'alert');
    
    const product = {
      id: '1',
      name: 'Test Product',
      description: 'Test Description',
      price: 99.99,
      stock: 5,
      category: 'Test'
    };
    
    component.addToCart(product);
    
    expect(window.alert).toHaveBeenCalledWith('Added Test Product to cart!');
  });

  it('should show loading spinner when loading', () => {
    component.loading = true;
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('mat-spinner');
    const loadingText = compiled.querySelector('.loading p');
    
    expect(spinner).toBeTruthy();
    expect(loadingText?.textContent).toBe('Loading products...');
  });

  it('should show no products message when products array is empty and not loading', () => {
    component.loading = false;
    component.products = [];
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const noProductsText = compiled.querySelector('.no-products p');
    
    expect(noProductsText?.textContent).toBe('No products available at the moment.');
  });

  it('should disable add to cart button when product is out of stock', (done) => {
    fixture.detectChanges();
    
    setTimeout(() => {
      // Modify a product to be out of stock
      component.products[0].stock = 0;
      fixture.detectChanges();
      
      const compiled = fixture.nativeElement as HTMLElement;
      const addToCartButton = compiled.querySelector('.product-card button') as HTMLButtonElement;
      
      expect(addToCartButton.disabled).toBeTruthy();
      done();
    }, 1100);
  });
});