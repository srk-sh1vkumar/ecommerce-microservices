package com.ecommerce.product.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BulkStockUpdateResponse DTO.
 * Tests response aggregation logic and success/failure counting.
 */
@DisplayName("BulkStockUpdateResponse Tests")
class BulkStockUpdateResponseTest {

    @Test
    @DisplayName("Default constructor should initialize empty response")
    void defaultConstructor_ShouldInitializeEmptyResponse() {
        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse();

        // Assert
        assertThat(response.getResults()).isNotNull();
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Parameterized constructor should calculate counts from results")
    void parameterizedConstructor_ShouldCalculateCounts() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", true);
        results.put("prod3", false);
        results.put("prod4", true);
        results.put("prod5", false);

        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Assert
        assertThat(response.getResults()).hasSize(5);
        assertThat(response.getSuccessCount()).isEqualTo(3);
        assertThat(response.getFailureCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Parameterized constructor with all successes")
    void parameterizedConstructor_WithAllSuccesses_ShouldHaveZeroFailures() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", true);
        results.put("prod3", true);

        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Assert
        assertThat(response.getSuccessCount()).isEqualTo(3);
        assertThat(response.getFailureCount()).isEqualTo(0);
        assertThat(response.allSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Parameterized constructor with all failures")
    void parameterizedConstructor_WithAllFailures_ShouldHaveZeroSuccesses() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", false);
        results.put("prod2", false);
        results.put("prod3", false);

        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Assert
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailureCount()).isEqualTo(3);
        assertThat(response.allSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Parameterized constructor with empty results")
    void parameterizedConstructor_WithEmptyResults_ShouldHaveZeroCounts() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();

        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Assert
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailureCount()).isEqualTo(0);
        assertThat(response.allSuccessful()).isTrue();
    }

    @Test
    @DisplayName("setResults should recalculate counts")
    void setResults_ShouldRecalculateCounts() {
        // Arrange
        BulkStockUpdateResponse response = new BulkStockUpdateResponse();
        Map<String, Boolean> newResults = new HashMap<>();
        newResults.put("prod1", true);
        newResults.put("prod2", false);
        newResults.put("prod3", true);

        // Act
        response.setResults(newResults);

        // Assert
        assertThat(response.getResults()).hasSize(3);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("allSuccessful should return true when no failures")
    void allSuccessful_WithNoFailures_ShouldReturnTrue() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", true);
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Act & Assert
        assertThat(response.allSuccessful()).isTrue();
    }

    @Test
    @DisplayName("allSuccessful should return false when any failures exist")
    void allSuccessful_WithFailures_ShouldReturnFalse() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", false);
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Act & Assert
        assertThat(response.allSuccessful()).isFalse();
    }

    @Test
    @DisplayName("getResults should return the results map")
    void getResults_ShouldReturnResultsMap() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        results.put("prod1", true);
        results.put("prod2", false);
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Act
        Map<String, Boolean> retrievedResults = response.getResults();

        // Assert
        assertThat(retrievedResults).hasSize(2);
        assertThat(retrievedResults).containsEntry("prod1", true);
        assertThat(retrievedResults).containsEntry("prod2", false);
    }

    @Test
    @DisplayName("Setting results to all successes should update allSuccessful")
    void setResults_ToAllSuccesses_ShouldUpdateAllSuccessful() {
        // Arrange
        Map<String, Boolean> initialResults = new HashMap<>();
        initialResults.put("prod1", false);
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(initialResults);
        assertThat(response.allSuccessful()).isFalse();

        // Act
        Map<String, Boolean> newResults = new HashMap<>();
        newResults.put("prod1", true);
        newResults.put("prod2", true);
        response.setResults(newResults);

        // Assert
        assertThat(response.allSuccessful()).isTrue();
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Large batch should calculate counts correctly")
    void largeResults_ShouldCalculateCountsCorrectly() {
        // Arrange
        Map<String, Boolean> results = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            results.put("prod" + i, i % 3 != 0); // Every 3rd product fails
        }

        // Act
        BulkStockUpdateResponse response = new BulkStockUpdateResponse(results);

        // Assert
        assertThat(response.getResults()).hasSize(100);
        assertThat(response.getSuccessCount()).isEqualTo(67); // 100 - 33 = 67
        assertThat(response.getFailureCount()).isEqualTo(33);
        assertThat(response.allSuccessful()).isFalse();
    }
}
