import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { HomeComponent } from './home.component';
import { AuthService } from '../../services/auth.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn']);

    await TestBed.configureTestingModule({
      imports: [
        HomeComponent,
        RouterTestingModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display welcome message', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('mat-card-title');
    
    expect(title?.textContent).toBe('Welcome to Ecommerce Platform');
  });

  it('should show login/register buttons when user is not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const loginButton = compiled.querySelector('button[routerLink="/login"]');
    const registerButton = compiled.querySelector('button[routerLink="/register"]');
    const authSection = compiled.querySelector('.auth-buttons');
    
    expect(authSection).toBeTruthy();
    expect(loginButton).toBeTruthy();
    expect(registerButton).toBeTruthy();
    expect(loginButton?.textContent?.trim()).toBe('Login');
    expect(registerButton?.textContent?.trim()).toBe('Register');
  });

  it('should show browse products button when user is logged in', () => {
    authService.isLoggedIn.and.returnValue(true);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const browseButton = compiled.querySelector('button[routerLink="/products"]');
    const welcomeBackSection = compiled.querySelector('.welcome-back');
    
    expect(welcomeBackSection).toBeTruthy();
    expect(browseButton).toBeTruthy();
    expect(browseButton?.textContent?.trim()).toBe('Browse Products');
  });

  it('should not show auth buttons when user is logged in', () => {
    authService.isLoggedIn.and.returnValue(true);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const authSection = compiled.querySelector('.auth-buttons');
    
    expect(authSection).toBeFalsy();
  });

  it('should not show welcome back section when user is not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement as HTMLElement;
    const welcomeBackSection = compiled.querySelector('.welcome-back');
    
    expect(welcomeBackSection).toBeFalsy();
  });
});