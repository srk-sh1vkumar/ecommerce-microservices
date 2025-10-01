package com.ecommerce.order.dto;

import java.util.Map;

public class BulkStockUpdateResponse {
    private Map<String, Boolean> results;
    private int successCount;
    private int failureCount;

    public BulkStockUpdateResponse() {}

    public BulkStockUpdateResponse(Map<String, Boolean> results, int successCount, int failureCount) {
        this.results = results;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public Map<String, Boolean> getResults() {
        return results;
    }

    public void setResults(Map<String, Boolean> results) {
        this.results = results;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public boolean allSuccessful() {
        return failureCount == 0;
    }
}
