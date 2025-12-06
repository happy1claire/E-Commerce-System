package com.cs6650.shoppingcartservice.model;

/**
 * DTO representing the request body for the warehouse /ship API.
 * After inventory is successfully reserved and payment is authorized,
 * ShoppingCartService sends this request to ship the items.
 */
public class ShipRequest {

    private long productId;
    private int quantity;

    public ShipRequest() {}

    public ShipRequest(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
