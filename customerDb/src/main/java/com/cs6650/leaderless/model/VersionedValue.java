package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionedValue {
    private Customer customer;
    private final long version; // use long for safety
    private final long timestamp;

    public VersionedValue(Customer customer, long version, long timestamp) {
        this.customer = customer;
        this.version = version;
        this.timestamp = timestamp;
    }
}
