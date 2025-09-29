package com.ecommerce.cart.dto;

public class AddToCartRequest {
    private String userEmail;
    private String productId;
    private Integer quantity;
    
    public AddToCartRequest() {}
    
    public AddToCartRequest(String userEmail, String productId, Integer quantity) {
        this.userEmail = userEmail;
        this.productId = productId;
        this.quantity = quantity;
    }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}