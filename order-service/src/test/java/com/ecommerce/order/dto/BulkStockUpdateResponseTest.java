package com.ecommerce.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BulkStockUpdateResponse DTO.
 */
@DisplayName("BulkStockUpdateResponse Tests")
class BulkStockUpdateResponseTest {

    @Test
    @DisplayName("Default constructor should create empty object")
    void defaultConstructor_ShouldCreateEmptyObject() {
        BulkStockUpdateResponse response = new BulkStockUpdateResponse();

        assertThat(response.getResults()).isNull();
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Parameterized constructor should set all fields")
    void parameterizedConstructor_ShouldSetAllFields() {
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", false);

        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results, 1, 1);

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get("prod1")).isTrue();
        assertThat(response.getResults().get("prod2")).isFalse();
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Setters should update fields correctly")
    void setters_ShouldUpdateFields() {
        BulkStockUpdateResponse response = new BulkStockUpdateResponse();
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod123", true);

        response.setResults(results);
        response.setSuccessCount(5);
        response.setFailureCount(2);

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getSuccessCount()).isEqualTo(5);
        assertThat(response.getFailureCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("allSuccessful should return true when no failures")
    void allSuccessful_WithNoFailures_ShouldReturnTrue() {
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", true);

        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results, 2, 0);

        assertThat(response.allSuccessful()).isTrue();
    }

    @Test
    @DisplayName("allSuccessful should return false when failures exist")
    void allSuccessful_WithFailures_ShouldReturnFalse() {
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", false);

        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results, 1, 1);

        assertThat(response.allSuccessful()).isFalse();
    }
}
