package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return product by ID when product exists"

    request {
        method GET()
        url '/api/products/test-product-123'
        headers {
            contentType(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 'test-product-123',
            name: 'Test Wireless Mouse',
            description: 'A high-quality wireless mouse',
            price: 29.99,
            category: 'Electronics',
            stockQuantity: 150,
            imageUrl: 'https://example.com/images/mouse.jpg'
        ])
        bodyMatchers {
            jsonPath('$.id', byRegex('[a-zA-Z0-9-]+'))
            jsonPath('$.price', byRegex('[0-9]+\\.[0-9]{2}'))
            jsonPath('$.stockQuantity', byRegex('[0-9]+'))
        }
    }
}
