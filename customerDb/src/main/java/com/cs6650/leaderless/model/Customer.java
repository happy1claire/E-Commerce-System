package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Customer {
    private String customerId;
    private List<String> shoppingcartIds;

    public Customer() {}
}
