package com.cs6650.productservice.controller;

import com.cs6650.productservice.model.Product;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class ProductController {

  // Add configuration to enable "bad" behavior mode
  // Set environment variable BAD_SERVICE_MODE=true for the bad instance
  @Value("${bad.service.mode:false}")
  private boolean badServiceMode;

  private final Random random = new Random();

  @GetMapping("/products/{productId}")
  public ResponseEntity<?> getProduct(@PathVariable Integer productId) {

    // validate that productId is positive (productId >= 1)
    if (productId < 1) {
      Map<String, String> error = new HashMap<>();
      error.put("error", "INVALID_PRODUCT_ID");
      error.put("message", "Product ID must be a positive integer");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    log.info("Request to get product with ID: {}", productId);
    Product product = new Product();
    product.setProductId(productId);
    product.setSku("ABC123XYZ");
    product.setManufacturer("Acme Corporation");
    product.setCategoryId(4568);
    product.setWeight(1250);
    product.setSomeOtherId(789);
    return ResponseEntity.ok(product);
  }


  // The ? wildcard allows returning either Product (success) or Map (error)
  @PostMapping("/product")
  public ResponseEntity<?> createProduct(@RequestBody Product productData) {
    log.info("Request to create product with data: {}", productData);

    // If in "bad service mode", return 503 error 50% of the time
    // This simulates a failing service for load balancer testing
    if (badServiceMode && random.nextBoolean()) {
      log.warn("BAD SERVICE MODE: Returning 503 Service Unavailable");
      Map<String, Object> error = new HashMap<>();
      error.put("error", "SERVICE_UNAVAILABLE");
      error.put("message", "Service temporarily unavailable (simulated failure)");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // Generate random productId (positive 32-bit int)
    int newProductId = random.nextInt(Integer.MAX_VALUE - 1) + 1;

    // Set the generated product_id on the Product object
    productData.setProductId(newProductId);

    return ResponseEntity.status(HttpStatus.CREATED).body(productData);
  }

  // Health check endpoint for load balancer
  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Product Service is healthy");
  }
}