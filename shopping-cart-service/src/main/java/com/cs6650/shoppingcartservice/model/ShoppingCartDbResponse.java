package com.cs6650.shoppingcartservice.model;

import java.util.Map;

/**
 * DTO representing the shopping cart value returned by the leaderless Shoppingcart DB.
 * Expected JSON structure:
 * {
 *   "items": { "1": 2, "3": 5 },
 *   "version": 123,
 *   "timestamp": 1700000000
 * }
 */
public class ShoppingCartDbResponse {

    private Map<Integer, Integer> items;
    private long version;
    private long timestamp;

    public Map<Integer, Integer> getItems() {
        return items;
    }

    public void setItems(Map<Integer, Integer> items) {
        this.items = items;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
