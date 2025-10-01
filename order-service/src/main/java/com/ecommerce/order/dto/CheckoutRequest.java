package com.ecommerce.order.dto;

import jakarta.validation.constraints.*;

public class CheckoutRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "Invalid email format")
    private String userEmail;

    @NotBlank(message = "Shipping address is required")
    @Size(min = 10, max = 200, message = "Shipping address must be between 10 and 200 characters")
    private String shippingAddress;

    public CheckoutRequest() {}

    public CheckoutRequest(String userEmail, String shippingAddress) {
        this.userEmail = userEmail;
        this.shippingAddress = shippingAddress;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}