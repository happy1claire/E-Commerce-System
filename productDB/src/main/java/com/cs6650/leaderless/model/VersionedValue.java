package com.cs6650.leaderless.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionedValue {
  private Product product;
  private final long version; // use long for safety
  private final long timestamp;

  public VersionedValue(Product product, long version, long timestamp) {
    this.product = product;
    this.version = version;
    this.timestamp = timestamp;
  }
}
