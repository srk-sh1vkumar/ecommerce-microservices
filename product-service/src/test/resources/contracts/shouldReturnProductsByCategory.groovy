package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return products by category"

    request {
        method GET()
        url '/api/products/category/Electronics'
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
            [
                id: 'prod-001',
                name: 'Wireless Keyboard',
                description: 'Ergonomic wireless keyboard',
                price: 49.99,
                category: 'Electronics',
                stockQuantity: 75,
                imageUrl: 'https://example.com/images/keyboard.jpg'
            ],
            [
                id: 'prod-002',
                name: 'USB Mouse',
                description: 'Optical USB mouse',
                price: 19.99,
                category: 'Electronics',
                stockQuantity: 120,
                imageUrl: 'https://example.com/images/mouse.jpg'
            ]
        ])
        bodyMatchers {
            jsonPath('$[*].id', byType())
            jsonPath('$[*].price', byRegex('[0-9]+\\.[0-9]{2}'))
            jsonPath('$[*].category', byEquality())
        }
    }
}
