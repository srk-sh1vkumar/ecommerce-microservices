#!/usr/bin/env python3
"""
Realistic E-commerce Load Generator
Simulates real user journeys with OpenTelemetry tracing
"""

import os
import time
import random
import json
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass
from datetime import datetime, timedelta

import requests
from faker import Faker
from locust import HttpUser, TaskSet, task, between
from locust.env import Environment
from locust.stats import stats_printer, stats_history
from locust.log import setup_logging

from opentelemetry import trace, baggage
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.sdk.resources import Resource

# Configure logging
setup_logging("INFO")
logger = logging.getLogger(__name__)

# Initialize OpenTelemetry
resource = Resource.create({
    "service.name": "load-generator",
    "service.version": "1.0.0",
    "deployment.environment": "docker"
})

trace.set_tracer_provider(TracerProvider(resource=resource))
tracer = trace.get_tracer(__name__)

# Configure OTLP exporter
otlp_exporter = OTLPSpanExporter(
    endpoint=os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4317"),
    insecure=True
)

span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)

# Instrument requests
RequestsInstrumentor().instrument()

# Initialize fake data generator
fake = Faker()


@dataclass
class UserProfile:
    """Represents a user profile with realistic behavior patterns"""
    user_id: str
    email: str
    name: str
    age: int
    location: str
    shopping_frequency: str  # frequent, occasional, rare
    preferred_categories: List[str]
    price_sensitivity: float  # 0.0 to 1.0
    device_type: str  # desktop, mobile, tablet


class UserJourney:
    """Defines realistic user journey patterns"""
    
    JOURNEY_PATTERNS = {
        "browser": {
            "weight": 40,
            "steps": ["homepage", "browse_products", "view_product", "exit"]
        },
        "quick_shopper": {
            "weight": 25,
            "steps": ["homepage", "search_product", "view_product", "add_to_cart", "checkout"]
        },
        "comparison_shopper": {
            "weight": 20,
            "steps": ["homepage", "browse_products", "view_product", "browse_products", 
                     "view_product", "add_to_cart", "view_cart", "continue_shopping", 
                     "view_product", "add_to_cart", "checkout"]
        },
        "returning_user": {
            "weight": 10,
            "steps": ["login", "view_orders", "browse_products", "view_product", "add_to_cart", "checkout"]
        },
        "cart_abandoner": {
            "weight": 5,
            "steps": ["homepage", "browse_products", "view_product", "add_to_cart", "view_cart", "exit"]
        }
    }
    
    @classmethod
    def get_random_journey(cls) -> List[str]:
        """Get a weighted random journey pattern"""
        patterns = list(cls.JOURNEY_PATTERNS.keys())
        weights = [cls.JOURNEY_PATTERNS[p]["weight"] for p in patterns]
        
        chosen_pattern = random.choices(patterns, weights=weights)[0]
        return cls.JOURNEY_PATTERNS[chosen_pattern]["steps"]


class RealisticEcommerceUser(HttpUser):
    """Simulates realistic e-commerce user behavior"""
    
    wait_time = between(1, 5)
    host = os.getenv("TARGET_BASE_URL", "http://frontend")
    
    def on_start(self):
        """Initialize user session"""
        self.user_profile = self.create_user_profile()
        self.session_data = {
            "cart_items": [],
            "viewed_products": [],
            "session_start": datetime.now(),
            "auth_token": None
        }
        
        logger.info(f"Starting session for user: {self.user_profile.email}")
        
        # Create root span for user session
        self.session_span = tracer.start_span(
            "user_session",
            attributes={
                "user.id": self.user_profile.user_id,
                "user.email": self.user_profile.email,
                "user.shopping_frequency": self.user_profile.shopping_frequency,
                "user.device_type": self.user_profile.device_type,
                "session.id": f"session_{int(time.time())}_{random.randint(1000, 9999)}"
            }
        )
    
    def on_stop(self):
        """Clean up user session"""
        if hasattr(self, 'session_span'):
            self.session_span.set_attribute("session.duration", 
                                          (datetime.now() - self.session_data["session_start"]).total_seconds())
            self.session_span.end()
        
        logger.info(f"Ending session for user: {self.user_profile.email}")
    
    def create_user_profile(self) -> UserProfile:
        """Create a realistic user profile"""
        shopping_frequencies = ["frequent", "occasional", "rare"]
        device_types = ["desktop", "mobile", "tablet"]
        categories = ["electronics", "clothing", "books", "home", "sports", "beauty", "toys"]
        
        return UserProfile(
            user_id=fake.uuid4(),
            email=fake.email(),
            name=fake.name(),
            age=random.randint(18, 65),
            location=fake.city(),
            shopping_frequency=random.choice(shopping_frequencies),
            preferred_categories=random.sample(categories, random.randint(1, 3)),
            price_sensitivity=random.uniform(0.2, 0.9),
            device_type=random.choices(device_types, weights=[50, 35, 15])[0]
        )
    
    @task(10)
    def execute_user_journey(self):
        """Execute a complete realistic user journey"""
        journey = UserJourney.get_random_journey()
        
        with tracer.start_span("user_journey", 
                              attributes={"journey.pattern": str(journey)}) as span:
            
            for step in journey:
                try:
                    self.execute_step(step)
                    # Realistic think time between actions
                    self.wait_between_actions()
                except Exception as e:
                    logger.error(f"Error in step {step}: {e}")
                    span.record_exception(e)
                    span.set_status(trace.Status(trace.StatusCode.ERROR))
                    break
    
    def execute_step(self, step: str):
        """Execute a specific journey step with tracing"""
        with tracer.start_span(f"step_{step}") as span:
            
            if step == "homepage":
                self.visit_homepage()
            elif step == "login":
                self.login_user()
            elif step == "browse_products":
                self.browse_products()
            elif step == "search_product":
                self.search_products()
            elif step == "view_product":
                self.view_product()
            elif step == "add_to_cart":
                self.add_to_cart()
            elif step == "view_cart":
                self.view_cart()
            elif step == "checkout":
                self.checkout()
            elif step == "view_orders":
                self.view_orders()
            elif step == "continue_shopping":
                self.browse_products()
            elif step == "exit":
                # User exits - no action needed
                pass
            
            span.set_attribute("step.completed", True)
    
    def visit_homepage(self):
        """Visit the homepage"""
        with tracer.start_span("visit_homepage") as span:
            response = self.client.get("/", 
                                     headers=self.get_request_headers(),
                                     name="Homepage")
            span.set_attribute("http.response.status_code", response.status_code)
            
            if response.status_code == 200:
                self.simulate_page_interaction(random.uniform(2, 8))
    
    def login_user(self):
        """Simulate user login"""
        with tracer.start_span("login_user") as span:
            login_data = {
                "email": self.user_profile.email,
                "password": "testYOUR_SECURE_PASSWORD"
            }
            
            # Try login first (might fail for new users)
            response = self.client.post("/user-service/api/users/login", 
                                      json=login_data,
                                      headers=self.get_request_headers(),
                                      name="Login Attempt")
            
            if response.status_code == 401:  # User doesn't exist, register
                self.register_user()
                # Retry login after registration
                response = self.client.post("/user-service/api/users/login", 
                                          json=login_data,
                                          headers=self.get_request_headers(),
                                          name="Login After Registration")
            
            if response.status_code == 200:
                try:
                    auth_data = response.json()
                    self.session_data["auth_token"] = auth_data.get("token")
                    span.set_attribute("login.success", True)
                except:
                    span.set_attribute("login.success", False)
            
            span.set_attribute("http.response.status_code", response.status_code)
    
    def register_user(self):
        """Register a new user"""
        with tracer.start_span("register_user") as span:
            registration_data = {
                "firstName": self.user_profile.name.split()[0],
                "lastName": self.user_profile.name.split()[-1],
                "email": self.user_profile.email,
                "password": "testYOUR_SECURE_PASSWORD",
                "role": "USER"
            }
            
            response = self.client.post("/user-service/api/users/register", 
                                      json=registration_data,
                                      headers=self.get_request_headers(),
                                      name="User Registration")
            
            span.set_attribute("http.response.status_code", response.status_code)
            span.set_attribute("registration.success", response.status_code == 200)
    
    def browse_products(self):
        """Browse product catalog"""
        with tracer.start_span("browse_products") as span:
            # Random category from user preferences or all categories
            if random.random() < 0.7 and self.user_profile.preferred_categories:
                category = random.choice(self.user_profile.preferred_categories)
                url = f"/product-service/api/products?category={category}"
                name = f"Browse Products - {category}"
            else:
                url = "/product-service/api/products"
                name = "Browse All Products"
            
            response = self.client.get(url, 
                                     headers=self.get_request_headers(),
                                     name=name)
            
            span.set_attribute("http.response.status_code", response.status_code)
            
            if response.status_code == 200:
                self.simulate_page_interaction(random.uniform(3, 12))
    
    def search_products(self):
        """Search for specific products"""
        with tracer.start_span("search_products") as span:
            # Generate realistic search terms
            search_terms = [
                "laptop", "smartphone", "headphones", "book", "dress", 
                "shoes", "watch", "camera", "tablet", "backpack"
            ]
            
            search_query = random.choice(search_terms)
            response = self.client.get(f"/product-service/api/products/search?q={search_query}",
                                     headers=self.get_request_headers(),
                                     name=f"Search - {search_query}")
            
            span.set_attribute("http.response.status_code", response.status_code)
            span.set_attribute("search.query", search_query)
            
            if response.status_code == 200:
                self.simulate_page_interaction(random.uniform(2, 6))
    
    def view_product(self):
        """View a specific product"""
        with tracer.start_span("view_product") as span:
            # Generate a realistic product ID
            product_id = random.randint(1, 100)
            
            response = self.client.get(f"/product-service/api/products/{product_id}",
                                     headers=self.get_request_headers(),
                                     name=f"View Product {product_id}")
            
            span.set_attribute("http.response.status_code", response.status_code)
            span.set_attribute("product.id", product_id)
            
            if response.status_code == 200:
                self.session_data["viewed_products"].append(product_id)
                # Longer interaction time for product pages
                self.simulate_page_interaction(random.uniform(5, 20))
    
    def add_to_cart(self):
        """Add item to shopping cart"""
        with tracer.start_span("add_to_cart") as span:
            if not self.session_data["viewed_products"]:
                # View a product first
                self.view_product()
            
            if self.session_data["viewed_products"]:
                product_id = random.choice(self.session_data["viewed_products"])
                quantity = random.randint(1, 3)
                
                cart_data = {
                    "productId": product_id,
                    "quantity": quantity
                }
                
                response = self.client.post("/cart-service/api/cart/add",
                                          json=cart_data,
                                          headers=self.get_request_headers(),
                                          name="Add to Cart")
                
                span.set_attribute("http.response.status_code", response.status_code)
                span.set_attribute("cart.product_id", product_id)
                span.set_attribute("cart.quantity", quantity)
                
                if response.status_code in [200, 201]:
                    self.session_data["cart_items"].append({
                        "productId": product_id,
                        "quantity": quantity
                    })
    
    def view_cart(self):
        """View shopping cart"""
        with tracer.start_span("view_cart") as span:
            response = self.client.get("/cart-service/api/cart",
                                     headers=self.get_request_headers(),
                                     name="View Cart")
            
            span.set_attribute("http.response.status_code", response.status_code)
            span.set_attribute("cart.items_count", len(self.session_data["cart_items"]))
            
            if response.status_code == 200:
                self.simulate_page_interaction(random.uniform(2, 8))
    
    def checkout(self):
        """Complete the checkout process"""
        with tracer.start_span("checkout") as span:
            if not self.session_data["cart_items"]:
                span.set_attribute("checkout.abandoned", True)
                span.set_attribute("checkout.reason", "empty_cart")
                return
            
            # Simulate checkout form filling time
            self.simulate_page_interaction(random.uniform(10, 30))
            
            checkout_data = {
                "shippingAddress": {
                    "street": fake.street_address(),
                    "city": fake.city(),
                    "state": fake.state(),
                    "zipCode": fake.zipcode(),
                    "country": "US"
                },
                "paymentMethod": "credit_card"
            }
            
            response = self.client.post("/order-service/api/orders/checkout",
                                      json=checkout_data,
                                      headers=self.get_request_headers(),
                                      name="Checkout")
            
            span.set_attribute("http.response.status_code", response.status_code)
            span.set_attribute("checkout.items_count", len(self.session_data["cart_items"]))
            
            if response.status_code in [200, 201]:
                self.session_data["cart_items"] = []  # Clear cart after successful checkout
                span.set_attribute("checkout.success", True)
            else:
                span.set_attribute("checkout.success", False)
    
    def view_orders(self):
        """View order history"""
        with tracer.start_span("view_orders") as span:
            response = self.client.get("/order-service/api/orders",
                                     headers=self.get_request_headers(),
                                     name="View Orders")
            
            span.set_attribute("http.response.status_code", response.status_code)
            
            if response.status_code == 200:
                self.simulate_page_interaction(random.uniform(3, 10))
    
    def get_request_headers(self) -> Dict[str, str]:
        """Get headers for HTTP requests"""
        headers = {
            "User-Agent": self.get_user_agent(),
            "Accept": "application/json, text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.5",
            "Accept-Encoding": "gzip, deflate"
        }
        
        if self.session_data.get("auth_token"):
            headers["Authorization"] = f"Bearer {self.session_data['auth_token']}"
        
        return headers
    
    def get_user_agent(self) -> str:
        """Get realistic user agent based on device type"""
        user_agents = {
            "desktop": [
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
            ],
            "mobile": [
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            ],
            "tablet": [
                "Mozilla/5.0 (iPad; CPU OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1"
            ]
        }
        
        return random.choice(user_agents[self.user_profile.device_type])
    
    def simulate_page_interaction(self, duration: float):
        """Simulate realistic page interaction time"""
        # Add some randomness to make it more realistic
        actual_duration = duration * random.uniform(0.8, 1.2)
        time.sleep(actual_duration)
    
    def wait_between_actions(self):
        """Realistic wait time between user actions"""
        wait_time = random.uniform(0.5, 3.0)
        time.sleep(wait_time)


if __name__ == "__main__":
    # Configuration from environment variables
    target_url = os.getenv("TARGET_BASE_URL", "http://frontend")
    users = int(os.getenv("CONCURRENT_USERS", "10"))
    spawn_rate = float(os.getenv("SPAWN_RATE", "1"))
    run_time = int(os.getenv("TEST_DURATION", "300"))  # 5 minutes default
    
    logger.info(f"Starting load test against {target_url}")
    logger.info(f"Users: {users}, Spawn rate: {spawn_rate}/s, Duration: {run_time}s")
    
    # Set up environment
    env = Environment(user_classes=[RealisticEcommerceUser])
    env.create_local_runner()
    
    # Start the load test
    env.runner.start(users, spawn_rate=spawn_rate)
    
    # Run for specified duration
    time.sleep(run_time)
    
    # Stop the test
    env.runner.stop()
    
    logger.info("Load test completed")
    logger.info(f"Total requests: {env.stats.total.num_requests}")
    logger.info(f"Total failures: {env.stats.total.num_failures}")
    logger.info(f"Average response time: {env.stats.total.avg_response_time:.2f}ms")