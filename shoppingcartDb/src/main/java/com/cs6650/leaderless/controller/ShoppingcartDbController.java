package com.cs6650.leaderless.controller;

import com.cs6650.leaderless.model.Shoppingcart;
import com.cs6650.leaderless.model.PropagateRequest;
import com.cs6650.leaderless.model.VersionedValue;
import com.cs6650.leaderless.service.ShoppingcartStore;
import com.cs6650.leaderless.service.PropagationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This controller is a leaderless replication model where any node
 * can handle client requests. Writes are propagated to all peers,
 * and each node maintains versioned values for conflict resolution.
 */
@RestController
public class ShoppingcartDbController {

  private final ShoppingcartStore shoppingcartStore;
  private final PropagationService propagationService;

  @Value("${propagation.timeout.ms}")
  private int propagationTimeoutMs;

  /**
   * Constructs a new {@code LeaderlessController} with dependencies injected.
   *
   * @param shoppingcartStore             the key-value store handling local reads/writes
   * @param propagationService  the service responsible for propagating updates to peers
   */
  @Autowired
  public ShoppingcartDbController(ShoppingcartStore shoppingcartStore, PropagationService propagationService) {
    this.shoppingcartStore = shoppingcartStore;
    this.propagationService = propagationService;
  }

  @GetMapping("/get/{key}")
  public ResponseEntity<?> getItems(@PathVariable String key) throws InterruptedException {
    VersionedValue cart = shoppingcartStore.getCart(key);
    if (cart == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    return ResponseEntity.ok(
            Map.of(
                    "items", cart.getShoppingcart().getItems(),
                    "version", cart.getVersion(),
                    "timestamp", cart.getTimestamp()
            )
    );
  }

  @PostMapping("/item/{key}")
  public ResponseEntity<?> setItem(@PathVariable String key, @RequestBody List<HashMap<String, Integer>> items) {
    if (key == null || key.isEmpty()) return ResponseEntity.badRequest().body("Key must not be empty");

    VersionedValue cart = shoppingcartStore.getCart(key);

    if (cart == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cart not found");

    Shoppingcart shoppingcart = cart.getShoppingcart();
    shoppingcart.setItems(items);

    // increment version and write locally
    long version = shoppingcartStore.nextVersion();
    shoppingcartStore.writeLocal(key, shoppingcart, version);

    // propagate to peers (W = 2)
    boolean ok = propagationService.propagateToAll(key, shoppingcart, version, propagationTimeoutMs);

    if (ok) {
      VersionedValue stored = shoppingcartStore.getCart(key);
      return ResponseEntity.status(HttpStatus.CREATED).body(
              Map.of(
                      "items", stored.getShoppingcart().getItems(),
                      "version", stored.getVersion(),
                      "timestamp", stored.getTimestamp()
              ));
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Failed to propagate to all peers");
    }
  }

//  @GetMapping
//  public ResponseEntity<?> getCartId(@PathVariable String key) throws InterruptedException {
//    if (key == null || key.isEmpty()) return ResponseEntity.badRequest().body("Key must not be empty");
//
//    String cartId = shoppingcartStore.getOrCreateCartId(key);
//    return ResponseEntity.ok(Map.of("cartId", cartId));
//  }


  /**
   * Handles propagation requests from peer nodes. (Call by other nodes.)
   * When a coordinator sends updates, each follower receives the propagated
   * value and updates its local store only if the received version is newer
   * than its current version. This ensures eventual consistency.
   *
   * @param request a JSON map containing "key", "product", and "version"
   * @return {@code 200 OK} once the propagation is processed
   * @throws InterruptedException if the artificial delay is interrupted
   */
  @PostMapping("/propagate")
  public ResponseEntity<?> propagate(@RequestBody PropagateRequest request) throws InterruptedException {
    String key = request.getKey();
    Shoppingcart shoppingcart = request.getProduct();
    long version = request.getVersion();

    // write if newer
    VersionedValue current = shoppingcartStore.get(key);
    if (current == null || version > current.getVersion()) {
      shoppingcartStore.putIfNewer(key, product, version);
    }

    return ResponseEntity.ok().build();
  }

  /**
   * Returns the local value for a given key.
   * This endpoint is intended for debugging and testing.
   * It returns the stored value along with version and timestamp metadata.
   *
   * @param key the key to look up
   * @return {@code 200 OK} with local value details;
   *         {@code 404 Not Found} if the key does not exist locally
   */
  @GetMapping("/local_read/{key}")
  public ResponseEntity<?> localRead(@PathVariable String key) {
    VersionedValue v = shoppingcartStore.get(key);
    if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    return ResponseEntity.ok(Map.of("product", v.getProduct(), "version", v.getVersion(), "timestamp", v.getTimestamp()));
  }

}
