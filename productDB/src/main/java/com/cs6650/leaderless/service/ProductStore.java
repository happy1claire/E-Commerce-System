package com.cs6650.leaderless.service;

import com.cs6650.leaderless.model.Product;
import com.cs6650.leaderless.model.VersionedValue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ProductStore {
  private final ConcurrentHashMap<Integer, VersionedValue> productStorage = new ConcurrentHashMap<>();

  private final AtomicLong versionCounter = new AtomicLong(0);
  private final int nodeId;

  public ProductStore(@Value("${server.port}") int port) {
    // A simple way to get a unique node ID from the port (e.g., 8081 -> 81)
    // This assumes ports are in a reasonable range where this is unique.
    this.nodeId = port % 1000;
  }

  // increment and return new version
  public long nextVersion() {
    long counter = versionCounter.incrementAndGet();
    // Combine the counter with the node ID to create a unique, sortable version.
    // Use top 48 bits for counter, bottom 16 for node ID.
    return (counter << 16) | nodeId;
  }

  public VersionedValue get(Integer key) {
    return productStorage.get(key);
  }

  // write if newer (or absent)
  public void putIfNewer(Integer key, Product product, long version) throws InterruptedException {
    productStorage.compute(key, (k, existing) -> {
      if (existing == null || version > existing.getVersion()) {
        return new VersionedValue(product, version, System.currentTimeMillis());
      } else {
        return existing;
      }
    });
  }

  // local immediate write (Coordinator writes first)
  public void writeLocal(Integer key, Product product, long version) {
    productStorage.put(key, new VersionedValue(product, version, System.currentTimeMillis()));
  }
}
