import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Home page
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent),
    title: 'Home - Ecommerce Platform',
    data: { 
      breadcrumb: 'Home',
      description: 'Welcome to our ecommerce platform'
    }
  },
  
  // Products
  {
    path: 'products',
    loadChildren: () => import('./features/products/products.routes').then(m => m.PRODUCTS_ROUTES),
    title: 'Products - Ecommerce Platform',
    data: { 
      breadcrumb: 'Products',
      description: 'Browse our product catalog'
    }
  },
  
  // Authentication
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Login - Ecommerce Platform',
    data: { 
      breadcrumb: 'Login',
      description: 'Sign in to your account'
    }
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    title: 'Register - Ecommerce Platform',
    data: { 
      breadcrumb: 'Register',
      description: 'Create a new account'
    }
  },
  
  // Protected routes
  {
    path: 'cart',
    loadComponent: () => import('./features/cart/cart.component').then(m => m.CartComponent),
    canActivate: [AuthGuard],
    title: 'Shopping Cart - Ecommerce Platform',
    data: { 
      breadcrumb: 'Cart',
      description: 'Your shopping cart items'
    }
  },
  {
    path: 'orders',
    loadChildren: () => import('./features/orders/orders.routes').then(m => m.ORDERS_ROUTES),
    canActivate: [AuthGuard],
    title: 'Orders - Ecommerce Platform',
    data: { 
      breadcrumb: 'Orders',
      description: 'Your order history and tracking'
    }
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard],
    title: 'Profile - Ecommerce Platform',
    data: { 
      breadcrumb: 'Profile',
      description: 'Manage your account settings'
    }
  },
  {
    path: 'checkout',
    loadComponent: () => import('./features/checkout/checkout.component').then(m => m.CheckoutComponent),
    canActivate: [AuthGuard],
    title: 'Checkout - Ecommerce Platform',
    data: { 
      breadcrumb: 'Checkout',
      description: 'Complete your purchase'
    }
  },
  
  // Static pages
  {
    path: 'about',
    loadComponent: () => import('./features/static/about/about.component').then(m => m.AboutComponent),
    title: 'About Us - Ecommerce Platform',
    data: { 
      breadcrumb: 'About',
      description: 'Learn more about our company'
    }
  },
  {
    path: 'contact',
    loadComponent: () => import('./features/static/contact/contact.component').then(m => m.ContactComponent),
    title: 'Contact Us - Ecommerce Platform',
    data: { 
      breadcrumb: 'Contact',
      description: 'Get in touch with our support team'
    }
  },
  {
    path: 'privacy',
    loadComponent: () => import('./features/static/privacy/privacy.component').then(m => m.PrivacyComponent),
    title: 'Privacy Policy - Ecommerce Platform',
    data: { 
      breadcrumb: 'Privacy Policy',
      description: 'Our privacy policy and data protection'
    }
  },
  {
    path: 'terms',
    loadComponent: () => import('./features/static/terms/terms.component').then(m => m.TermsComponent),
    title: 'Terms of Service - Ecommerce Platform',
    data: { 
      breadcrumb: 'Terms of Service',
      description: 'Terms and conditions of use'
    }
  },
  
  // Error pages
  {
    path: '404',
    loadComponent: () => import('./shared/components/error-pages/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: '404 - Page Not Found',
    data: { 
      breadcrumb: '404',
      description: 'Page not found'
    }
  },
  {
    path: '500',
    loadComponent: () => import('./shared/components/error-pages/server-error/server-error.component').then(m => m.ServerErrorComponent),
    title: '500 - Server Error',
    data: { 
      breadcrumb: 'Error',
      description: 'Internal server error'
    }
  },
  
  // Wildcard route - must be last
  {
    path: '**',
    redirectTo: '/404'
  }
];