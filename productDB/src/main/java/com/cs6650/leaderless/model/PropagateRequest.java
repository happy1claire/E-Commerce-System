package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropagateRequest {
    private String key;
    private Product product;
    private long version;
}
