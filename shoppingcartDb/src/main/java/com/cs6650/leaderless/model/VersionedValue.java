package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionedValue {
    private Shoppingcart shoppingcart;
    private final long version; // use long for safety
    private final long timestamp;

    public VersionedValue(Shoppingcart shoppingcart, long version, long timestamp) {
        this.shoppingcart = shoppingcart;
        this.version = version;
        this.timestamp = timestamp;
    }
}
