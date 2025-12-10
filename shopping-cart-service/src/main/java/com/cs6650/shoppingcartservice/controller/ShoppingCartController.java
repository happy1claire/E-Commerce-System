package com.cs6650.shoppingcartservice.controller;

import com.cs6650.shoppingcartservice.model.ShoppingCartModel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shopping-cart")
public class ShoppingCartController {

    private final ShoppingCartModel model;

    public ShoppingCartController(ShoppingCartModel model) {
        this.model = model;
    }

    /**
     * Get cartId or generate a new one
     * e.g. GET /shopping-cart/by-customer/{customerId}
     * Response: {"cartId":"1234567890"}
     */
    @GetMapping("/by-customer/{customerId}")
    public Map<String, String> getOrCreateCartId(@PathVariable String customerId) {
        // 1. Try to read an existing cartId for this customer from the Customer DB
        String existingCartId = model.fetchCartIdFromDb(customerId);

        // 2. If the DB already has a cartId, reuse it and return it to the client
        if (existingCartId != null && !existingCartId.isEmpty()) {
            return Map.of("cartId", existingCartId);
        }

        // 3. The DB has no cartId for this customer:
        //    generate a new cartId and persist it in the Customer DB
        String newCartId = model.createAndPersistCartId(customerId);

        // Return the newly created cartId
        return Map.of("cartId", newCartId);
    }

    /**
     * Add items to the cart
     * e.g. POST /shopping-cart/{cartId}/items?itemId=SKU123&quantity=3
     * 201 Created
     */
    @PostMapping("/{cartId}/items")
    public ResponseEntity<Map<String, Integer>> addToCart(@PathVariable String cartId,
            @RequestParam int itemId,
            @RequestParam int quantity) {

        model.addToCart(cartId, itemId, quantity);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/shopping-cart/" + cartId + "/items");
        // return CartItems
        return new ResponseEntity<>(model.getCartItems(cartId), headers, HttpStatus.CREATED);
    }

    /**
     * Get all items with cartId from the cart
     * e.g. GET /shopping-cart/{cartId}/itemsWithId
     * Response: { "cartId123456" : { "itemA":2, "itemB":3 }}
     */
    @GetMapping("/{cartId}/itemsWithId")
    public String getCartIdAndItems(@PathVariable String cartId) {
        return model.getCartIdAndItems(cartId);
    }

    /**
     * checkout
     * e.g. POST /shopping-cart/{cartId}/checkout?creditCardNumber=4111-1111-1111-1111
     */
    @PostMapping("/{cartId}/checkout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void checkout(@PathVariable String cartId,
            @RequestParam String creditCardNumber) {
        model.checkout(cartId, creditCardNumber);
    }

    /**
     * Mocking one user.
     * Generate a cart ID, add items to the cart multiple times, and checkout.
     * e.g. POST /shopping-cart/shopToOrder
     */
    @PostMapping("/shopToOrder")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void shopToOrder(@RequestParam String creditCardNumber) {
        // Generate a customer ID
        String customerId = model.getCustomerId();

        // Generate a cart ID, using the existing one if the customerID exists
        String cartId = model.findCartIdByCustomer(customerId);

        // Add items to the cart
        // randomly generate 1-3 items, with itemId "SKUxxx" and quantity between 1-100
        int itemCount = (int) (Math.random() * 3) + 1;
        for (int i = 0; i < itemCount; i++) {
            int itemId = String.format("SKU%03d", (int) (Math.random() * 10 + 1));
            int quantity = (int) (Math.random() * 100) + 1;
            model.addToCart(cartId, itemId, quantity);
        }

        // Checkout
        model.checkout(cartId, creditCardNumber);
    }

    /**
     * Catches validation errors (like bad cartId or card format) and returns 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        // Returns 400 Bad Request
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Catches internal errors (like auth service being down) and returns 500.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInternalErrors(IllegalStateException ex) {
        // Returns 500 Internal Server Error
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An internal error occurred: " + ex.getMessage()));
    }
}
