# Multi-Portal Apache Frontend Dockerfile
# Supports Customer Portal, Admin Dashboard, Vendor Portal, and Mobile PWA

FROM httpd:2.4

# Install required packages for SSL, security, and monitoring
RUN apt-get update && apt-get install -y \
    openssl \
    curl \
    ca-certificates \
    libapache2-mod-security2 \
    && rm -rf /var/lib/apt/lists/*

# Copy Apache configuration
COPY httpd.conf /usr/local/apache2/conf/httpd.conf

# Create SSL certificate directories
RUN mkdir -p /etc/ssl/certs /etc/ssl/private

# Generate self-signed SSL certificates (for development)
RUN openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/ssl/private/ecommerce.key \
    -out /etc/ssl/certs/ecommerce.crt \
    -subj "/C=US/ST=State/L=City/O=Ecommerce/OU=IT/CN=ecommerce.com/emailAddress=admin@example.com" \
    -extensions v3_req \
    -config <(echo '[req]'; echo 'distinguished_name=req'; echo '[v3_req]'; echo 'subjectAltName=@alt_names'; echo '[alt_names]'; echo 'DNS.1=shop.ecommerce.com'; echo 'DNS.2=admin.ecommerce.com'; echo 'DNS.3=vendor.ecommerce.com'; echo 'DNS.4=m.ecommerce.com'; echo 'DNS.5=localhost')

# Create web directories for each portal
RUN mkdir -p /var/www/customer-portal \
             /var/www/admin-dashboard \
             /var/www/vendor-portal \
             /var/www/mobile-app \
             /var/www/default \
             /var/www/error-pages

# Create default index files
RUN echo '<h1>Customer Portal Loading...</h1><p>Please wait while the application loads.</p>' > /var/www/customer-portal/index.html
RUN echo '<h1>Admin Dashboard Loading...</h1><p>Please wait while the application loads.</p>' > /var/www/admin-dashboard/index.html
RUN echo '<h1>Vendor Portal Loading...</h1><p>Please wait while the application loads.</p>' > /var/www/vendor-portal/index.html
RUN echo '<h1>Mobile App Loading...</h1><p>Please wait while the application loads.</p>' > /var/www/mobile-app/index.html
RUN echo '<h1>Ecommerce Platform</h1><p>Redirecting to main site...</p>' > /var/www/default/index.html

# Create error pages
RUN echo '<!DOCTYPE html><html><head><title>404 - Page Not Found</title></head><body><h1>404 - Page Not Found</h1><p>The requested page could not be found.</p></body></html>' > /var/www/error-pages/404.html
RUN echo '<!DOCTYPE html><html><head><title>500 - Internal Server Error</title></head><body><h1>500 - Internal Server Error</h1><p>An internal server error occurred.</p></body></html>' > /var/www/error-pages/500.html
RUN echo '<!DOCTYPE html><html><head><title>502 - Bad Gateway</title></head><body><h1>502 - Bad Gateway</h1><p>The server received an invalid response from the upstream server.</p></body></html>' > /var/www/error-pages/502.html
RUN echo '<!DOCTYPE html><html><head><title>503 - Service Unavailable</title></head><body><h1>503 - Service Unavailable</h1><p>The service is temporarily unavailable.</p></body></html>' > /var/www/error-pages/503.html

# Create log directories
RUN mkdir -p /var/log/apache2

# Set permissions
RUN chown -R www-data:www-data /var/www/
RUN chmod -R 755 /var/www/
RUN chmod 600 /etc/ssl/private/ecommerce.key
RUN chmod 644 /etc/ssl/certs/ecommerce.crt

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/health-check || exit 1

# Expose ports
EXPOSE 80 443

# Start Apache
CMD ["httpd-foreground"]