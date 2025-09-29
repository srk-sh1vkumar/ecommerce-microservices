package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper for consistent response format.
 * Wraps successful responses with metadata.
 *
 * @param <T> Type of data being returned
 * @author E-commerce Development Team
 * @version 2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Default constructor.
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor for successful response with data.
     */
    public ApiResponse(T data) {
        this();
        this.success = true;
        this.data = data;
    }

    /**
     * Constructor for response with data and message.
     */
    public ApiResponse(T data, String message) {
        this(data);
        this.message = message;
    }

    /**
     * Constructor for response with all fields.
     */
    public ApiResponse(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /**
     * Creates a successful response with data and message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Creates a successful response with just a message.
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /**
     * Creates a failure response with message.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}