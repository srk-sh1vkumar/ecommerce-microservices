# Multi-Portal Frontend with Apache HTTP Server
# Supports: Customer Portal, Admin Dashboard, Vendor Portal, Mobile App

FROM httpd:2.4-alpine

# Install additional packages for security and performance
RUN apk add --no-cache \
    apache2-mod-security \
    apache2-mod-evasive \
    openssl \
    curl \
    tzdata

# Create directory structure for all portals
RUN mkdir -p /usr/local/apache2/htdocs/customer-portal \
             /usr/local/apache2/htdocs/admin-dashboard \
             /usr/local/apache2/htdocs/vendor-portal \
             /usr/local/apache2/htdocs/mobile-app \
             /usr/local/apache2/htdocs/default \
             /usr/local/apache2/htdocs/error \
             /usr/local/apache2/conf/ssl

# Copy Apache configuration
COPY apache/httpd.conf /usr/local/apache2/conf/httpd.conf

# Copy SSL certificates
COPY apache/ssl/server.crt /usr/local/apache2/conf/ssl/server.crt
COPY apache/ssl/server.key /usr/local/apache2/conf/ssl/server.key

# Copy static content for all portals
COPY dist/customer-portal/ /usr/local/apache2/htdocs/customer-portal/
COPY dist/admin-dashboard/ /usr/local/apache2/htdocs/admin-dashboard/
COPY dist/vendor-portal/ /usr/local/apache2/htdocs/vendor-portal/
COPY dist/mobile-app/ /usr/local/apache2/htdocs/mobile-app/

# Copy error pages
COPY error-pages/ /usr/local/apache2/htdocs/error/

# Copy default fallback page
COPY default/ /usr/local/apache2/htdocs/default/

# Set proper permissions
RUN chown -R daemon:daemon /usr/local/apache2/htdocs/ && \
    chmod -R 755 /usr/local/apache2/htdocs/ && \
    chmod 600 /usr/local/apache2/conf/ssl/server.key && \
    chmod 644 /usr/local/apache2/conf/ssl/server.crt

# Expose HTTP and HTTPS ports
EXPOSE 80 443

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/health || exit 1

# Start Apache in foreground
CMD ["httpd-foreground"]