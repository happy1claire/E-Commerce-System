package com.cs6650.shoppingcartservice.model;

import com.cs6650.shoppingcartservice.service.CreditCardAuthService;
import com.cs6650.shoppingcartservice.service.WarehouseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class ShoppingCartModel {

    // customerId -> cartId
    private final Map<String, String> cartIdByCustomer = new ConcurrentHashMap<>();
    // cartId -> (itemId -> quantity)
    private final Map<String, Map<String, Integer>> cartItems = new ConcurrentHashMap<>();
    // Randomly generate IDs
    private static final SecureRandom RAND = new SecureRandom();
    // ObjectMapper to convert our order object to a JSON string
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CreditCardAuthService authService;
    private final WarehouseService warehouseService;
    /**
     * Base URL for the Customer DB service (leaderless KV store).
     * Example:
     *   customer.db.base-url=http://localhost:8080
     *
     * Declared in Shopping Cart Service's application.properties.
     */
    @Value("${customer.db.base-url:http://localhost:8080}")
    private String customerDbBaseUrl;

    /**
     * RestTemplate for making HTTP calls to the Customer DB nodes.
     */
    private final RestTemplate restTemplate = new RestTemplate();
    /**
     * Constructor injection ensures that WarehouseService and CreditCardAuthService are provided.
     *
     */
    public ShoppingCartModel(WarehouseService warehouseService, CreditCardAuthService authService) {
        this.warehouseService = warehouseService;
        this.authService = authService;
    }

    /**
     * Add items with quantities to the cart
     */
    public void addToCart(String cartId, String itemId, int quantity) {
        if (cartId == null || itemId == null) {
            throw new IllegalArgumentException("cartId/itemId must not be null");
        }
        if (quantity < 1 || quantity > 10_000) {
            throw new IllegalArgumentException("quantity must be between 1 and 10000");
        }

        // Find cartId or generate cartId
//        String cartId = findCartIdByCustomer(customerId);

        // Extract Cart Items and their quantities; create if the cart is absent
        Map<String, Integer> itemsWithQuantity = cartItems.computeIfAbsent(cartId, id -> new ConcurrentHashMap<>());

        // If an item is already in the cart, sum up; otherwise put quantity
        itemsWithQuantity.merge(itemId, quantity, Integer::sum);
    }


    /**
     * Get or generate customer's cartId
     */
    public String findCartIdByCustomer(String customerId) {
        String existing = cartIdByCustomer.get(customerId);
        if (existing != null)
            return existing;

        String newCartId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        cartItems.putIfAbsent(newCartId, new ConcurrentHashMap<>());
        cartIdByCustomer.put(customerId, newCartId);
        return newCartId;
    }

    /**
     * Randomly generate a customer ID.
     */
    public String getCustomerId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * Get the items in the cart.
     * Returns an immutable empty map if cart does not exist.
     */
    public Map<String, Integer> getCartItems(String cartId) {
        return cartItems.getOrDefault(cartId, Map.of());
    }

    /**
     * Get cartId and cartItems.
     */
    public String getCartIdAndItems(String cartId) {
        Map<String, Integer> itemsMap = cartItems.getOrDefault(cartId, Map.of());
        List<Map<String, Object>> itemList = itemsMap.entrySet().stream()
                .map(entry -> {
                    // Create a map for each item
                    Map<String, Object> itemAsMap = new HashMap<>();
                    itemAsMap.put("productId", entry.getKey());
                    itemAsMap.put("quantity", entry.getValue());
                    return itemAsMap;
                })
                .collect(Collectors.toList());
        Map<String, Object> resultStructure = new HashMap<>();
        resultStructure.put("cartId", cartId);
        resultStructure.put("items", itemList);

        try {
            return objectMapper.writeValueAsString(resultStructure);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting cart data to JSON", e);
        }
    }

    /**
     * After User clicks checkout:
     * - call Warehouse.reserve to reserve each product
     * - call CreditCardAuthorizer
     * - call Warehouse.ship to ship each product
     */
    public void checkout(String cartId, String creditCardNumber) {
        if (cartId == null) {
            throw new IllegalArgumentException("cartId must not be null");
        }


        // 1. Retrieve items from the cart: Map<itemId(SKUxxx), quantity>
        Map<String, Integer> items = getCartItems(cartId);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty for cartId: " + cartId);
        }

        // 2. Reserve inventory for each item
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemId = entry.getKey();      // e.g. "SKU001"
            int quantity = entry.getValue();

            long productId = parseSkuToProductId(itemId); // convert "SKU001" -> 1

            boolean reserved = warehouseService.reserve(productId, quantity);
            if (!reserved) {
                // Fail fast if any item cannot be reserved
                throw new IllegalStateException("Not enough inventory for itemId: " + itemId);
            }
        }


        // credit card must match the format 1234-5678-9012-3456
        if (!creditCardNumber.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("creditCardNumber format invalid");
        }

        // 3. Authorize payment (you already have this service in your assignment)
        authService.authorize(creditCardNumber);

        // 4. Ship all items
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemId = entry.getKey();
            int quantity = entry.getValue();

            long productId = parseSkuToProductId(itemId);

            boolean shipped = warehouseService.ship(productId, quantity);
            if (!shipped) {
                // For this assignment, shipping is always successful.
                // In a real system, you might log or compensate here.
                throw new IllegalStateException("Shipping failed for itemId: " + itemId);
            }
        }
    }

    /**
     * Converts an itemId in format "SKU001" into a numeric productId.
     * Example:
     *   "SKU001" -> 1
     *   "SKU010" -> 10
     */
    private long parseSkuToProductId(String sku) {
        if (sku == null || !sku.startsWith("SKU")) {
            throw new IllegalArgumentException("Invalid SKU format: " + sku);
        }
        String numericPart = sku.substring(3);  // from index 3 to end, e.g. "001"
        try {
            return Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid SKU numeric part: " + sku, e);
        }
    }

    /**
     * Reads the cartId for a given customer from the Customer DB.
     *
     * It calls the leaderless customer service:
     *   GET {customerDbBaseUrl}/get/{customerId}
     *
     * Expected response body:
     *   {
     *     "shoppingCartsIds": ["cartId1", "cartId2", ...],
     *     "version": ...,
     *     "timestamp": ...
     *   }
     *
     * This method:
     *   - Returns the first cartId in "shoppingCartsIds" if present.
     *   - Returns null if the customer has no cartIds in the DB
     *     or if the record does not exist.
     */
    public String fetchCartIdFromDb(String customerId) {
        try {
            String url = customerDbBaseUrl + "/get/" + customerId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            Object idsObj = response.getBody().get("shoppingCartsIds");
            if (idsObj instanceof List<?> ids && !ids.isEmpty()) {
                // Use the first cartId in the list
                Object first = ids.get(0);
                return first != null ? first.toString() : null;
            }

            return null;
        } catch (HttpClientErrorException.NotFound e) {
            // The customer record does not exist in the DB
            return null;
        } catch (Exception e) {
            // For simplicity, treat any error as "no cartId in DB"
            // You may want to log or rethrow depending on your needs
            return null;
        }
    }

    /**
     * Generates a new cartId for the given customer, writes it to the Customer DB,
     * and returns the new cartId.
     *
     * Steps:
     * 1. Generate a new cartId.
     * 2. Build a list of cartIds (currently just one).
     * 3. Call the leaderless customer service:
     *    POST {customerDbBaseUrl}/customer/{customerId}
     *    Body: ["newCartId"]
     * 4. Optionally cache the mapping in memory.
     */
    public String createAndPersistCartId(String customerId) {
        // Generate a new cartId (you can replace this with your existing logic)
        String newCartId = UUID.randomUUID().toString();

        // Persist to Customer DB
        try {
            String url = customerDbBaseUrl + "/customer/" + customerId;
            List<String> body = List.of(newCartId);
            restTemplate.postForEntity(url, body, Void.class);
        } catch (Exception e) {
            // You may want to throw an IllegalStateException here,
            // so that the controller can translate it into a 500.
            throw new IllegalStateException("Failed to persist cartId to Customer DB", e);
        }

        // Optionally store the mapping in the local map as a cache
        cartIdByCustomer.put(customerId, newCartId);

        return newCartId;
    }

}


