package com.ecommerce.order.client;

import com.ecommerce.order.dto.BulkStockUpdateResponse;
import com.ecommerce.order.dto.StockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PutMapping("/api/products/{id}/stock")
    Boolean updateStock(@PathVariable("id") String id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/products/stock/bulk")
    BulkStockUpdateResponse bulkUpdateStock(@RequestBody List<StockUpdateRequest> updates);
}