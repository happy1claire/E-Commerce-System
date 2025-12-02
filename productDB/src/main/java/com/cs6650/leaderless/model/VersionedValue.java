package com.cs6650.leaderless.model;

public class VersionedValue {
  private final String value;
  private final long version; // use long for safety
  private final long timestamp;

  public VersionedValue(String value, long version, long timestamp) {
    this.value = value;
    this.version = version;
    this.timestamp = timestamp;
  }

  public String getValue() { return value; }
  public long getVersion() { return version; }
  public long getTimestamp() { return timestamp; }
}
