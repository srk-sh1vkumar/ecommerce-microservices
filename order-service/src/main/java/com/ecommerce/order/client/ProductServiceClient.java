package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    @PutMapping("/api/products/{id}/stock")
    Boolean updateStock(@PathVariable("id") String id, @RequestParam("quantity") Integer quantity);
}