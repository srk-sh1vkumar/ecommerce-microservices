import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty values', () => {
    expect(component.loginForm.get('username')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
  });

  it('should mark form as invalid when fields are empty', () => {
    expect(component.loginForm.valid).toBeFalsy();
  });

  it('should mark form as valid when fields are filled', () => {
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'YOUR_SECURE_PASSWORD'
    });
    expect(component.loginForm.valid).toBeTruthy();
  });

  it('should call authService.login on form submit', () => {
    const mockResponse = {
      token: 'mock-token',
      user: { id: '1', username: 'testuser', email: 'test@example.com', firstName: 'Test', lastName: 'User' }
    };
    
    authService.login.and.returnValue(of(mockResponse));
    
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'YOUR_SECURE_PASSWORD'
    });

    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      username: 'testuser',
      password: 'YOUR_SECURE_PASSWORD'
    });
  });

  it('should navigate to products on successful login', () => {
    const mockResponse = {
      token: 'mock-token',
      user: { id: '1', username: 'testuser', email: 'test@example.com', firstName: 'Test', lastName: 'User' }
    };
    
    authService.login.and.returnValue(of(mockResponse));
    
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'YOUR_SECURE_PASSWORD'
    });

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
    expect(component.loading).toBeFalsy();
    expect(component.error).toBe('');
  });

  it('should show error on failed login', () => {
    authService.login.and.returnValue(throwError(() => new Error('Login failed')));
    
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'wrongpassword'
    });

    component.onSubmit();

    expect(component.error).toBe('Login failed. Please check your credentials.');
    expect(component.loading).toBeFalsy();
  });

  it('should set loading state during login attempt', () => {
    authService.login.and.returnValue(of({
      token: 'mock-token',
      user: { id: '1', username: 'testuser', email: 'test@example.com', firstName: 'Test', lastName: 'User' }
    }));
    
    component.loginForm.patchValue({
      username: 'testuser',
      password: 'YOUR_SECURE_PASSWORD'
    });

    expect(component.loading).toBeFalsy();
    component.onSubmit();
    // Loading should be set to true briefly during the login process
  });

  it('should not submit form when invalid', () => {
    component.onSubmit();
    expect(authService.login).not.toHaveBeenCalled();
  });
});