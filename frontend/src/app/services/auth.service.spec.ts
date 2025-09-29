import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, LoginRequest, RegisterRequest, AuthResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should send login request and store session', () => {
      const loginRequest: LoginRequest = {
        username: 'testuser',
        password: 'YOUR_SECURE_PASSWORD'
      };

      const mockResponse: AuthResponse = {
        token: 'mock-jwt-token',
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User'
        }
      };

      service.login(loginRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(service.getToken()).toBe('mock-jwt-token');
        expect(service.getCurrentUser()).toEqual(mockResponse.user);
      });

      const req = httpMock.expectOne('/api/users/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginRequest);
      req.flush(mockResponse);
    });
  });

  describe('register', () => {
    it('should send register request and store session', () => {
      const registerRequest: RegisterRequest = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'YOUR_SECURE_PASSWORD',
        firstName: 'New',
        lastName: 'User'
      };

      const mockResponse: AuthResponse = {
        token: 'mock-jwt-token',
        user: {
          id: '2',
          username: 'newuser',
          email: 'new@example.com',
          firstName: 'New',
          lastName: 'User'
        }
      };

      service.register(registerRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(service.getToken()).toBe('mock-jwt-token');
        expect(service.getCurrentUser()).toEqual(mockResponse.user);
      });

      const req = httpMock.expectOne('/api/users/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      req.flush(mockResponse);
    });
  });

  describe('logout', () => {
    it('should clear session and navigate to home', () => {
      // Set up initial state
      localStorage.setItem('auth_token', 'test-token');
      localStorage.setItem('current_user', JSON.stringify({ id: '1', username: 'test' }));

      service.logout();

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('current_user')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when token exists', () => {
      localStorage.setItem('auth_token', 'test-token');
      expect(service.isLoggedIn()).toBe(true);
    });

    it('should return false when token does not exist', () => {
      expect(service.isLoggedIn()).toBe(false);
    });
  });

  describe('getToken', () => {
    it('should return token from localStorage', () => {
      localStorage.setItem('auth_token', 'test-token');
      expect(service.getToken()).toBe('test-token');
    });

    it('should return null when no token exists', () => {
      expect(service.getToken()).toBeNull();
    });
  });

  describe('getCurrentUser', () => {
    it('should return user from localStorage', () => {
      const user = { id: '1', username: 'test', email: 'test@example.com', firstName: 'Test', lastName: 'User' };
      localStorage.setItem('current_user', JSON.stringify(user));
      expect(service.getCurrentUser()).toEqual(user);
    });

    it('should return null when no user exists', () => {
      expect(service.getCurrentUser()).toBeNull();
    });
  });
});