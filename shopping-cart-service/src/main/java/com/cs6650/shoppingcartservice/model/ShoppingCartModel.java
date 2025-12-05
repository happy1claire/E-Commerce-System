package com.cs6650.shoppingcartservice.model;

import com.cs6650.shoppingcartservice.service.CreditCardAuthService;
import com.cs6650.shoppingcartservice.service.OrderProducerService;
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

@Service
public class ShoppingCartModel {

    // customerId -> cartId
    private final Map<String, String> cartIdByCustomer = new ConcurrentHashMap<>();
    // cartId -> (itemId -> quantity)
    private final Map<String, Map<String, Integer>> cartItems = new ConcurrentHashMap<>();
    // Randomly generate IDs
    private static final SecureRandom RAND = new SecureRandom();
    private final CreditCardAuthService authService;
    // ObjectMapper to convert our order object to a JSON string
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Service responsible for sending order messages to RabbitMQ
     */
    private final OrderProducerService orderProducerService;

    /**
     * Constructor injection ensures that OrderProducerService is provided
     * by Spring when ShoppingCartModel is instantiated.
     *
     * @param orderProducerService the bean responsible for sending order messages
     */
    public ShoppingCartModel(OrderProducerService orderProducerService, CreditCardAuthService authService) {
        this.orderProducerService = orderProducerService;
        this.authService = authService;
    }

    /**
     * Add items with quantities to the cart
     */
    public void addToCart(String customerId, String itemId, int quantity) {
        if (customerId == null || itemId == null) {
            throw new IllegalArgumentException("customerId/itemId must not be null");
        }
        if (quantity < 1 || quantity > 10_000) {
            throw new IllegalArgumentException("quantity must be between 1 and 10000");
        }

        // Find cartId or generate cartId
        String cartId = findCartIdByCustomer(customerId);

        // Extract Cart Items and their quantities; create if absent
        Map<String, Integer> itemsWithQuantity = cartItems.computeIfAbsent(cartId, id -> new ConcurrentHashMap<>());

        // If an item is already in the cart, sum up; otherwise put quantity
        itemsWithQuantity.merge(itemId, quantity, Integer::sum);
    }

    /**
     * Add items with quantities to the cart by cartId
     */
    public void addToCartByCartId(String cartId, String itemId, int quantity) {
        if (cartId == null || itemId == null) {
            throw new IllegalArgumentException("cartId/itemId must not be null");
        }
        if (quantity < 1 || quantity > 10_000) {
            throw new IllegalArgumentException("quantity must be between 1 and 10000");
        }

        // Extract Cart Items and their quantities; create if absent
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
     * - call CreditCardAuthorizer
     * - contact Warehouse
     */
    public void checkout(String cartId, String creditCardNumber) {
        if (cartId == null) {
            throw new IllegalArgumentException("cartId must not be null");
        }

        // credit card must match the format 1234-5678-9012-3456
        if (!creditCardNumber.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("creditCardNumber format invalid");
        }

        authService.authorize(creditCardNumber);

        String cartMsgToQueue = getCartIdAndItems(cartId);
        this.orderProducerService.sendOrderMessage(cartMsgToQueue);

    }
}
