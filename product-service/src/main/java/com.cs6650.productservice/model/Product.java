package com.cs6650.productservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
  @JsonProperty("product_id")
  private int productId;
  private String sku;
  private String manufacturer;
  @JsonProperty("category_id")
  private int categoryId;
  private int weight;
  // in grams as per YAML
  @JsonProperty("some_other_id")
  private int someOtherId;

  // Default constructor
  public Product() {}

  // Constructor with all fields
  public Product(Integer productId, String sku, String manufacturer,
      Integer categoryId, Integer weight, Integer someOtherId) {
    this.productId = productId;
    this.sku = sku;
    this.manufacturer = manufacturer;
    this.categoryId = categoryId;
    this.weight = weight;
    this.someOtherId = someOtherId;
  }

  // Getters and Setters
  public Integer getProductId() { return productId; }
  public void setProductId(Integer productId) { this.productId = productId; }

  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }

  public String getManufacturer() { return manufacturer; }
  public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

  public Integer getCategoryId() { return categoryId; }
  public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

  public Integer getWeight() { return weight; }
  public void setWeight(Integer weight) { this.weight = weight; }

  public Integer getSomeOtherId() { return someOtherId; }
  public void setSomeOtherId(Integer someOtherId) { this.someOtherId = someOtherId; }
}