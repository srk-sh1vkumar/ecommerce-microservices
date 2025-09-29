// Initialize MongoDB database for ecommerce application
use('ecommerce');

// Create collections with indexes
db.createCollection('users');
db.users.createIndex({ "email": 1 }, { unique: true });

db.createCollection('products');
db.products.createIndex({ "category": 1 });
db.products.createIndex({ "name": "text", "description": "text" });

db.createCollection('cart_items');
db.cart_items.createIndex({ "userEmail": 1 });
db.cart_items.createIndex({ "userEmail": 1, "productId": 1 }, { unique: true });

db.createCollection('orders');
db.orders.createIndex({ "userEmail": 1 });
db.orders.createIndex({ "orderDate": -1 });

// Insert sample data
// Sample products
db.products.insertMany([
    {
        "name": "iPhone 15 Pro",
        "description": "Latest Apple iPhone with A17 Pro chip and titanium design",
        "price": NumberDecimal("999.99"),
        "category": "Electronics",
        "stockQuantity": 50,
        "imageUrl": "https://example.com/iphone15pro.jpg",
        "createdAt": new Date()
    },
    {
        "name": "Samsung Galaxy S24",
        "description": "Premium Android smartphone with AI features",
        "price": NumberDecimal("899.99"),
        "category": "Electronics",
        "stockQuantity": 30,
        "imageUrl": "https://example.com/galaxys24.jpg",
        "createdAt": new Date()
    },
    {
        "name": "MacBook Air M3",
        "description": "Ultra-thin laptop with M3 chip and all-day battery life",
        "price": NumberDecimal("1299.99"),
        "category": "Computers",
        "stockQuantity": 20,
        "imageUrl": "https://example.com/macbookair.jpg",
        "createdAt": new Date()
    },
    {
        "name": "Nike Air Max 270",
        "description": "Comfortable running shoes with Air Max cushioning",
        "price": NumberDecimal("150.00"),
        "category": "Footwear",
        "stockQuantity": 100,
        "imageUrl": "https://example.com/nikeairmax.jpg",
        "createdAt": new Date()
    },
    {
        "name": "Levi's 501 Jeans",
        "description": "Classic straight-fit jeans in premium denim",
        "price": NumberDecimal("79.99"),
        "category": "Clothing",
        "stockQuantity": 75,
        "imageUrl": "https://example.com/levis501.jpg",
        "createdAt": new Date()
    },
    {
        "name": "Sony WH-1000XM5",
        "description": "Industry-leading noise canceling wireless headphones",
        "price": NumberDecimal("349.99"),
        "category": "Electronics",
        "stockQuantity": 40,
        "imageUrl": "https://example.com/sony-headphones.jpg",
        "createdAt": new Date()
    },
    {
        "name": "The Great Gatsby",
        "description": "Classic American novel by F. Scott Fitzgerald",
        "price": NumberDecimal("12.99"),
        "category": "Books",
        "stockQuantity": 200,
        "imageUrl": "https://example.com/gatsby-book.jpg",
        "createdAt": new Date()
    },
    {
        "name": "Coffee Maker Pro",
        "description": "Programmable coffee maker with thermal carafe",
        "price": NumberDecimal("89.99"),
        "category": "Home & Kitchen",
        "stockQuantity": 60,
        "imageUrl": "https://example.com/coffee-maker.jpg",
        "createdAt": new Date()
    }
]);

// Sample user (password will be hashed when created through API)
db.users.insertOne({
    "email": "admin@example.com",
    "password": "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEyE7cVKIWuHsB6", // "YOUR_ADMIN_PASSWORD" hashed
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN",
    "createdAt": new Date()
});

print("MongoDB initialization completed successfully!");
print("Created collections: users, products, cart_items, orders");
print("Inserted sample data: 8 products and 1 admin user");
print("Admin login: admin@example.com / YOUR_ADMIN_PASSWORD");