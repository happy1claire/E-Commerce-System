package com.cs6650.leaderless.service;

import com.cs6650.leaderless.model.Customer;
import com.cs6650.leaderless.model.VersionedValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CustomerStore {
  /** ConcurrentHashMap<String CartId, VersionedValue>*/
  private final ConcurrentHashMap<String, VersionedValue> customers = new ConcurrentHashMap<>();

//  /** ConcurrentHashMap<String CustomerId, CartId>*/
//  private final ConcurrentHashMap<String, String> customerToCart =  new ConcurrentHashMap<>();

  private final AtomicLong versionCounter = new AtomicLong(0);
  private final int nodeId;

  public CustomerStore(@Value("${server.port}") int port) {
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

  public VersionedValue getCustomer(String key) {
    return customers.get(key);
  }

//  public String getOrCreateCartId(String key) {
//    if (customerToCart.containsKey(key)) {
//      return customerToCart.get(key);
//    }
//
//
//    // Randomly generate a new ID
//    UUID newCartId = UUID.randomUUID();
//    String cartId = newCartId.toString();
//
//    // Create a new shoppingcart
//    Customer shoppingcart = new Customer(cartId, key);
//    long version = nextVersion();
//    writeLocal(cartId, shoppingcart, version);
//    customerToCart.put(key, cartId);
//
//    return customerToCart.get(key);
//  }

  // write if newer (or absent)
  public void putIfNewer(String key, Customer customer, long version) throws InterruptedException {
    customers.compute(key, (k, existing) -> {
      if (existing == null || version > existing.getVersion()) {
        return new VersionedValue(customer, version, System.currentTimeMillis());
      } else {
        return existing;
      }
    });
  }

  // local immediate write (Coordinator writes first)
  public void writeLocal(String key, Customer customer, long version) {
    customers.put(key, new VersionedValue(customer, version, System.currentTimeMillis()));
  }
}
