package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should update product stock when sufficient stock is available"

    request {
        method PUT()
        url '/api/products/test-product-123/stock'
        urlPath('/api/products/test-product-123/stock') {
            queryParameters {
                parameter('quantity', 5)
            }
        }
        headers {
            contentType(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(true)
    }
}
