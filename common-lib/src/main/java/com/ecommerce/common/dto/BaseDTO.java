package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base DTO class with common fields for all data transfer objects.
 * Provides timestamp tracking for audit purposes.
 *
 * @author E-commerce Development Team
 * @version 2.0
 */
public abstract class BaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    protected BaseDTO() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Updates the updatedAt timestamp to current time.
     */
    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}