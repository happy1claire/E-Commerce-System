package com.cs6650.productservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreProductRequest {
    private int id;
    private String sku;
    private String manufacturer;
    private int categoryId;
    private int weight;

    public StoreProductRequest() {}
    public StoreProductRequest(int id, String sku, String manufacturer, int categoryId, int weight) {
        this.id = id;
        this.sku = sku;
        this.manufacturer = manufacturer;
        this.categoryId = categoryId;
        this.weight = weight;
    }

}
