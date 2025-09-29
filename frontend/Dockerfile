# Simple Apache HTTP Server with static HTML frontend
FROM httpd:2.4-alpine

# Copy custom Apache configuration
COPY apache/httpd.conf /usr/local/apache2/conf/httpd.conf

# Copy static HTML files (pre-built)
COPY dist/ /usr/local/apache2/htdocs/

# Create favicon
RUN touch /usr/local/apache2/htdocs/favicon.ico

# Add build info
RUN echo "Build completed at: $(date)" > /usr/local/apache2/htdocs/build-info.txt && \
    echo "Static HTML application with Apache HTTP Server" >> /usr/local/apache2/htdocs/build-info.txt

# Expose port 80
EXPOSE 80

# Health check for the web server
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start Apache
CMD ["httpd-foreground"]