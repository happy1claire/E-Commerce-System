package com.cs6650.Model;

public class Product {
    private int id;
    private String sku;
    private String manufacturer;
    private int categoryId;
    private int weight;
    private int someOtherId;


    public Product(int id, String sku,
                   String manufacturer, int categoryId,
                   int weight, int someOtherId) {
        this.id = id;
        this.sku = sku;
        this.manufacturer = manufacturer;
        this.categoryId = categoryId;
        this.weight = weight;
        this.someOtherId = someOtherId;
    }

    public int getId() {
        return id;
    }

    public void setId(int productId) {
        this.id = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getSomeOtherId() {
        return someOtherId;
    }

    public void setSomeOtherId(int someOtherId) {
        this.someOtherId = someOtherId;
    }
}
