import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: any;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'logout']);

    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        RouterTestingModule,
        NoopAnimationsModule,
        HttpClientTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have title ecommerce-frontend', () => {
    expect(component.title).toEqual('ecommerce-frontend');
  });

  it('should render toolbar with title', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-toolbar span')?.textContent).toContain('Ecommerce Platform');
  });

  it('should show login/register buttons when user is not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const loginButton = compiled.querySelector('button[routerLink="/login"]');
    const registerButton = compiled.querySelector('button[routerLink="/register"]');
    
    expect(loginButton).toBeTruthy();
    expect(registerButton).toBeTruthy();
  });

  it('should show user menu when user is logged in', () => {
    authService.isLoggedIn.and.returnValue(true);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const productsButton = compiled.querySelector('button[routerLink="/products"]');
    const cartButton = compiled.querySelector('button[routerLink="/cart"]');
    const logoutButton = compiled.querySelector('button[ng-reflect-text="Logout"]');
    
    expect(productsButton).toBeTruthy();
    expect(cartButton).toBeTruthy();
    expect(logoutButton).toBeTruthy();
  });

  it('should call logout when logout button is clicked', () => {
    authService.isLoggedIn.and.returnValue(true);
    fixture.detectChanges();
    
    component.logout();
    
    expect(authService.logout).toHaveBeenCalled();
  });
});