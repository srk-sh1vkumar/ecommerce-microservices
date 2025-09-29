package com.ecommerce.order.dto;

public class CheckoutRequest {
    private String userEmail;
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