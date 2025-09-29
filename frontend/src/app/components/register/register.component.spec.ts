import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty values', () => {
    expect(component.registerForm.get('username')?.value).toBe('');
    expect(component.registerForm.get('email')?.value).toBe('');
    expect(component.registerForm.get('firstName')?.value).toBe('');
    expect(component.registerForm.get('lastName')?.value).toBe('');
    expect(component.registerForm.get('password')?.value).toBe('');
  });

  it('should mark form as invalid when fields are empty', () => {
    expect(component.registerForm.valid).toBeFalsy();
  });

  it('should mark form as valid when all fields are filled correctly', () => {
    component.registerForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      password: 'YOUR_SECURE_PASSWORD'
    });
    expect(component.registerForm.valid).toBeTruthy();
  });

  it('should validate email format', () => {
    const emailControl = component.registerForm.get('email');
    emailControl?.setValue('invalid-email');
    expect(emailControl?.hasError('email')).toBeTruthy();

    emailControl?.setValue('valid@example.com');
    expect(emailControl?.hasError('email')).toBeFalsy();
  });

  it('should validate password minimum length', () => {
    const passwordControl = component.registerForm.get('password');
    passwordControl?.setValue('123');
    expect(passwordControl?.hasError('minlength')).toBeTruthy();

    passwordControl?.setValue('123456');
    expect(passwordControl?.hasError('minlength')).toBeFalsy();
  });

  it('should call authService.register on form submit', () => {
    const mockResponse = {
      token: 'mock-token',
      user: { 
        id: '1', 
        username: 'testuser', 
        email: 'test@example.com', 
        firstName: 'Test', 
        lastName: 'User' 
      }
    };
    
    authService.register.and.returnValue(of(mockResponse));
    
    const formData = {
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      password: 'YOUR_SECURE_PASSWORD'
    };
    
    component.registerForm.patchValue(formData);
    component.onSubmit();

    expect(authService.register).toHaveBeenCalledWith(formData);
  });

  it('should navigate to products on successful registration', () => {
    const mockResponse = {
      token: 'mock-token',
      user: { 
        id: '1', 
        username: 'testuser', 
        email: 'test@example.com', 
        firstName: 'Test', 
        lastName: 'User' 
      }
    };
    
    authService.register.and.returnValue(of(mockResponse));
    
    component.registerForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      password: 'YOUR_SECURE_PASSWORD'
    });

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
    expect(component.loading).toBeFalsy();
    expect(component.error).toBe('');
  });

  it('should show error on failed registration', () => {
    authService.register.and.returnValue(throwError(() => new Error('Registration failed')));
    
    component.registerForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      password: 'YOUR_SECURE_PASSWORD'
    });

    component.onSubmit();

    expect(component.error).toBe('Registration failed. Please try again.');
    expect(component.loading).toBeFalsy();
  });

  it('should not submit form when invalid', () => {
    component.onSubmit();
    expect(authService.register).not.toHaveBeenCalled();
  });
});