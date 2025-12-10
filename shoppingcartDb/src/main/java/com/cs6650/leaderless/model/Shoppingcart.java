package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class Shoppingcart {
    private String customerId;
    private String shoppingcartId;
    private HashMap<Integer, Integer> items;

    public Shoppingcart() {}
}
