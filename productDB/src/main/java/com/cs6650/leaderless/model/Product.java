package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {
    private int id;
    private String sku;
    private String manufacturer;
    private int categoryId;
    private int weight;

    public Product() {}

}
