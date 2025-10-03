package com.ecommerce.order.dto;

import java.math.BigDecimal;

/**
 * Payment response DTO containing payment transaction details.
 * Returned after successful payment processing.
 *
 * @author E-commerce Development Team
 * @version 1.0
 */
public class PaymentResponse {

    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String orderId;
    private boolean success;
    private String errorMessage;

    public PaymentResponse() {
    }

    private PaymentResponse(Builder builder) {
        this.transactionId = builder.transactionId;
        this.status = builder.status;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.orderId = builder.orderId;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Builder Pattern

    public static class Builder {
        private String transactionId;
        private String status;
        private BigDecimal amount;
        private String currency;
        private String orderId;
        private boolean success;
        private String errorMessage;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public PaymentResponse build() {
            return new PaymentResponse(this);
        }
    }
}
