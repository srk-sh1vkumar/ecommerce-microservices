# Ecommerce Frontend Application

## Overview

This is an Angular 17 frontend application for the ecommerce microservices platform, served via Apache HTTP Server with proper SDLC testing integration.

## Architecture

- **Framework**: Angular 17 with standalone components
- **UI Library**: Angular Material
- **Web Server**: Apache HTTP Server (for production)
- **Build Tool**: Angular CLI
- **Testing**: Jasmine + Karma
- **Authentication**: JWT with HTTP interceptors

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── components/          # Angular components
│   │   │   ├── home/            # Home page component
│   │   │   ├── login/           # User login component
│   │   │   ├── register/        # User registration component
│   │   │   ├── products/        # Product catalog component
│   │   │   ├── cart/            # Shopping cart component
│   │   │   └── orders/          # Order history component
│   │   ├── services/            # Angular services
│   │   │   └── auth.service.ts  # Authentication service
│   │   ├── guards/              # Route guards
│   │   │   └── auth.guard.ts    # Authentication guard
│   │   ├── interceptors/        # HTTP interceptors
│   │   │   └── auth.interceptor.ts  # JWT token interceptor
│   │   ├── app.component.ts     # Root component
│   │   └── app.routes.ts        # Application routing
│   ├── index.html               # Main HTML file
│   ├── main.ts                  # Application bootstrap
│   └── styles.scss              # Global styles
├── apache/
│   └── httpd.conf               # Apache configuration
├── scripts/
│   └── sdlc-pipeline.sh         # SDLC automation script
├── Dockerfile                   # Multi-stage Docker build
├── package.json                 # NPM dependencies and scripts
├── angular.json                 # Angular CLI configuration
├── karma.conf.js                # Test configuration
└── tsconfig.json                # TypeScript configuration
```

## Features

### 🔐 Authentication
- User registration and login
- JWT token management
- Protected routes with guards
- Automatic token injection via interceptors

### 🛍️ E-commerce Functionality
- Product catalog browsing
- Shopping cart management
- Order history tracking
- Responsive design with Angular Material

### 🧪 Testing & Quality Assurance
- Comprehensive unit tests for all components
- Code coverage reporting
- Automated testing in Docker builds
- SDLC pipeline integration

## Development

### Prerequisites
- Node.js 18+
- npm or yarn
- Angular CLI 17

### Local Development Setup

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Start development server**:
   ```bash
   npm start
   # or
   ng serve
   ```

3. **Access the application**:
   - Frontend: http://localhost:4200
   - API calls will be made to the backend at http://localhost:8081/api

### Testing

#### Unit Tests
```bash
# Run tests once
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

#### View Coverage Reports
```bash
# After running coverage tests, open:
open coverage/index.html
```

### Building

#### Development Build
```bash
npm run build
```

#### Production Build
```bash
npm run build:prod
```

## SDLC Process

This project follows a comprehensive Software Development Life Cycle (SDLC) process:

### Automated Pipeline

Run the complete SDLC pipeline:
```bash
./scripts/sdlc-pipeline.sh
```

### Manual Steps

1. **Phase 1: Code Quality**
   ```bash
   # Linting (when configured)
   npm run lint
   ```

2. **Phase 2: Testing**
   ```bash
   npm run test:coverage
   ```

3. **Phase 3: Building**
   ```bash
   npm run build:prod
   ```

4. **Phase 4: Security Audit**
   ```bash
   npm audit
   ```

## Docker Deployment

### Multi-stage Build with Testing

The Dockerfile implements a complete SDLC process:

```bash
# Build with integrated testing
docker-compose build frontend

# Run the application
docker-compose up -d frontend
```

### Build Stages

1. **Development Stage**: Install dependencies, run tests
2. **Testing Stage**: Execute unit tests with coverage
3. **Build Stage**: Create production build
4. **Production Stage**: Serve via Apache HTTP Server

### Access Points

Once deployed:
- **Application**: http://localhost
- **Coverage Reports**: http://localhost/coverage
- **Build Info**: http://localhost/build-info.txt

## API Integration

The frontend integrates with the microservices backend:

- **API Gateway**: http://api-gateway:8081/api
- **Authentication**: JWT tokens stored in localStorage
- **CORS**: Handled by Apache proxy configuration

### API Endpoints Used

- `POST /api/users/register` - User registration
- `POST /api/users/login` - User authentication
- `GET /api/products` - Product catalog
- `POST /api/cart/add` - Add items to cart
- `GET /api/orders` - Order history

## Configuration

### Apache Configuration (`apache/httpd.conf`)

- **Static File Serving**: Angular built files
- **API Proxying**: Routes `/api/*` to backend services
- **SPA Routing**: Fallback to `index.html` for client-side routing
- **CORS Headers**: Proper cross-origin configuration

### Environment Configuration

- **Development**: Direct API calls to localhost:8081
- **Production**: API calls proxied through Apache to backend services

## Testing Strategy

### Unit Testing Coverage

- **Components**: All components have comprehensive tests
- **Services**: Authentication and HTTP services tested
- **Guards**: Route protection logic tested
- **Interceptors**: JWT token injection tested

### Test Coverage Goals

- **Statements**: 80%+
- **Branches**: 80%+
- **Functions**: 80%+
- **Lines**: 80%+

## Deployment Architecture

```
┌─────────────────┐
│   User Browser  │
└─────────┬───────┘
          │ HTTP/HTTPS
          │
┌─────────▼───────┐
│ Apache Server   │
│ (Port 80)       │
├─────────────────┤
│ Static Files    │ (/*)
│ Angular App     │
├─────────────────┤
│ API Proxy       │ (/api/*)
│ → Backend       │
└─────────────────┘
```

## Future Enhancements

- [ ] End-to-end testing with Cypress
- [ ] PWA capabilities
- [ ] Real-time features with WebSockets
- [ ] Advanced caching strategies
- [ ] Performance monitoring integration
- [ ] Accessibility improvements (WCAG 2.1)
- [ ] Internationalization (i18n)

## Contributing

1. Follow the SDLC process for all changes
2. Ensure all tests pass before committing
3. Maintain code coverage above 80%
4. Update documentation as needed

## Support

For issues related to:
- **Frontend bugs**: Check browser console and coverage reports
- **API connectivity**: Verify backend services are running
- **Authentication**: Check JWT token in localStorage
- **Build issues**: Review Docker build logs