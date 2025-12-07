package com.cs6650.shoppingcartservice.model;

/**
 * DTO representing the request body for the warehouse /reserve API.
 * ShoppingCartService sends this to WarehouseService when trying to
 * reserve inventory before a checkout operation proceeds.
 */
public class ReserveRequest {

    private long productId;
    private int quantity;

    public ReserveRequest() {}

    public ReserveRequest(long productId, int quantity) {
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
