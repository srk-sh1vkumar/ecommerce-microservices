# Load Generator with Python and realistic user simulation
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy load generator scripts
COPY scripts/ ./scripts/
COPY config/ ./config/
COPY data/ ./data/

# Make scripts executable
RUN chmod +x scripts/*.py

# Environment variables
ENV PYTHONPATH=/app
ENV PYTHONUNBUFFERED=1

# Default target URL (can be overridden)
ENV TARGET_BASE_URL=http://frontend
ENV LOAD_PROFILE=realistic
ENV CONCURRENT_USERS=10
ENV TEST_DURATION=300
ENV RAMP_UP_TIME=60

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD python scripts/health_check.py || exit 1

# Default command runs the realistic user journey
CMD ["python", "scripts/realistic_load_test.py"]