package com.cs6650.warehouseservice.controller;

import com.cs6650.warehouseservice.dto.ReserveRequest;
import com.cs6650.warehouseservice.dto.ShipRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.http.HttpStatus;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final Random random = new Random();

    /**
     * Endpoint to reserve items from the warehouse.
     * Simulates a 90% success rate for inventory check.
     */
    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        // Simulate a random delay between 50ms and 200ms
        simulateDelay(50, 200);

        // Validate input
        if (request.getProductId() <= 0 || request.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("Invalid product ID or quantity");
        }

        // Randomly decide: 90% success (0-89), 10% failure (90-99)
        boolean hasInventory = random.nextInt(100) < 90;

        if (hasInventory) {
            System.out.println("Reserved product " + request.getProductId() + ", quantity: " + request.getQuantity());
            return ResponseEntity.ok(Map.of(
                    "message", "Inventory reserved",
                    "productId", request.getProductId(),
                    "quantity", request.getQuantity(),
                    "reserved", true
            ));
        } else {
            System.out.println("Failed to reserve product " + request.getProductId() + ", quantity: " + request.getQuantity() + ". Not enough stock.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Insufficient inventory",
                    "productId", request.getProductId(),
                    "quantity", request.getQuantity(),
                    "reserved", false
            ));
        }
    }

    /**
     * Endpoint to ship items from the warehouse.
     * This operation always succeeds as requirements.
     */
    @PostMapping("/ship")
    public ResponseEntity<?> ship(@RequestBody ShipRequest request) {
        // Simulate a random delay between 100ms and 500ms
        simulateDelay(100, 500);

        // Validate input
        if (request.getProductId() <= 0 || request.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("Invalid product ID or quantity");
        }

        // 100% success rate
        // Just log and return success
        System.out.println("Shipping product " + request.getProductId() +
                ", quantity: " + request.getQuantity());

        return ResponseEntity.ok(Map.of(
                "message", "Shipment successful",
                "productId", request.getProductId(),
                "quantity", request.getQuantity(),
                "shipped", true,
                "trackingNumber", UUID.randomUUID().toString()
        ));
    }

    private void simulateDelay(long min, long max) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(min, max));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Health check endpoint.
     * Returns a 200 OK status to indicate the service is running.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        // A simple health check that returns status "UP"
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}