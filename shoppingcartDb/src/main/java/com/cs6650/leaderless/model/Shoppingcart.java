package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class Shoppingcart {
    private String cartId;
    private String costumerId;
    private List<HashMap<String, Integer>> items;

    // For new established shopping cart
    public Shoppingcart(String cartId, String costumerId) {
        this.cartId = cartId;
        this.costumerId = costumerId;
    }

    public Shoppingcart(String cartId, String costumerId, List<HashMap<String, Integer>> items) {
        this.cartId = cartId;
        this.costumerId = costumerId;
        this.items = items;
    }

    public Shoppingcart() {}
}
