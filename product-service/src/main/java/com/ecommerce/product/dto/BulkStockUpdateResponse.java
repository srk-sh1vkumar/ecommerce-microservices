package com.ecommerce.product.dto;

import java.util.HashMap;
import java.util.Map;

public class BulkStockUpdateResponse {

    private Map<String, Boolean> results;
    private int successCount;
    private int failureCount;

    public BulkStockUpdateResponse() {
        this.results = new HashMap<>();
        this.successCount = 0;
        this.failureCount = 0;
    }

    public BulkStockUpdateResponse(Map<String, Boolean> results) {
        this.results = results;
        this.successCount = (int) results.values().stream().filter(v -> v).count();
        this.failureCount = (int) results.values().stream().filter(v -> !v).count();
    }

    public Map<String, Boolean> getResults() {
        return results;
    }

    public void setResults(Map<String, Boolean> results) {
        this.results = results;
        this.successCount = (int) results.values().stream().filter(v -> v).count();
        this.failureCount = (int) results.values().stream().filter(v -> !v).count();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public boolean allSuccessful() {
        return failureCount == 0;
    }
}
