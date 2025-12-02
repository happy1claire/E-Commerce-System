package com.cs6650.leaderless.controller;

import com.cs6650.leaderless.model.VersionedValue;
import com.cs6650.leaderless.service.KVStore;
import com.cs6650.leaderless.service.PropagationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * This controller is a leaderless replication model where any node
 * can handle client requests. Writes are propagated to all peers,
 * and each node maintains versioned values for conflict resolution.
 */
@RestController
public class ProductDbController {

  private final KVStore kvStore;
  private final PropagationService propagationService;

  /**
   * Simulated delay (in milliseconds) to represent follower processing latency
   * when handling propagation requests.
   */
  @Value("${follower.handler.delay.ms}")
  private long followerHandlerDelayMs;

  /** Simulated delay for the coordinator to sleep after sending a message to a peer. */
  @Value("${coordinator.post.peer.sleep.ms:200}")
  private long coordinatorPostPeerSleepMs;

  /**
   * Timeout (in milliseconds) for the coordinator to wait
   * for peer acknowledgments during propagation.
   */
  @Value("${propagation.timeout.ms:5000}")
  private long propagationTimeoutMs;

  /**
   * Constructs a new {@code LeaderlessController} with dependencies injected.
   *
   * @param kvStore             the key-value store handling local reads/writes
   * @param propagationService  the service responsible for propagating updates to peers
   */
  @Autowired
  public ProductDbController(KVStore kvStore, PropagationService propagationService) {
    this.kvStore = kvStore;
    this.propagationService = propagationService;
  }

  /**
   * Handles client write requests. (Call by clients)
   * The receiving node becomes the coordinator for this write:
   * it updates its local store with a new versioned value,
   * and then propagates the update to all peer nodes.
   *
   * @param body a JSON map containing "key" and "value" fields
   * @return {@code 201 Created} if successfully written and propagated to all peers;
   *         {@code 400 Bad Request} if the key is missing;
   *         {@code 500 Internal Server Error} if propagation fails
   */
  @PostMapping("/set")
  public ResponseEntity<?> set(@RequestBody Map<String, String> body) {
    String key = body.get("key");
    String value = body.get("value");
    if (key == null || key.isEmpty()) return ResponseEntity.badRequest().body("Key must not be empty");

    // increment version and write locally
    long version = kvStore.nextVersion();
    kvStore.writeLocal(key, value, version);

    // propagate to peers (W = N)
    boolean ok = propagationService.propagateToAll(key, value, version, propagationTimeoutMs, coordinatorPostPeerSleepMs);

    if (ok) {
      VersionedValue stored = kvStore.get(key);
      return ResponseEntity.status(HttpStatus.CREATED).body(
              Map.of(
                      "value", stored.getValue(),
                      "version", stored.getVersion(),
                      "timestamp", stored.getTimestamp()
              ));
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Failed to propagate to all peers");
    }
  }

  /**
   * Handles propagation requests from peer nodes. (Call by other nodes.)
   * When a coordinator sends updates, each follower receives the propagated
   * value and updates its local store only if the received version is newer
   * than its current version. This ensures eventual consistency.
   *
   * @param body a JSON map containing "key", "value", and "version"
   * @return {@code 200 OK} once the propagation is processed
   * @throws InterruptedException if the artificial delay is interrupted
   */
  @PostMapping("/propagate")
  public ResponseEntity<?> propagate(@RequestBody Map<String, Object> body) throws InterruptedException {
    String key = (String) body.get("key");
    String value = (String) body.get("value");
    Number versionNum = (Number) body.get("version");
    long version = versionNum.longValue();

    // simulate follower processing delay
    Thread.sleep(followerHandlerDelayMs);

    // write if newer
    VersionedValue current = kvStore.get(key);
    if (current == null || version > current.getVersion()) {
      kvStore.putIfNewer(key, value, version);
    }

    return ResponseEntity.ok().build();
  }

  /**
   * Handles client read requests.
   * Performs a local read (R=1) on this node and returns the value and version.
   * Reads may reflect slightly stale data
   * depending on propagation delays.
   *
   * @param key the key to retrieve
   * @return {@code 200 OK} with the current value and version;
   *         {@code 404 Not Found} if the key does not exist
   * @throws InterruptedException if future read delays are simulated
   */
  @GetMapping("/get/{key}")
  public ResponseEntity<?> get(@PathVariable String key) throws InterruptedException {
    VersionedValue v = kvStore.get(key);
    if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    // For simulation, follower read delay is not necessary here since this is leaderless,
    // but you can insert a small sleep if you want to create larger inconsistency windows.
    return ResponseEntity.ok(Map.of("value", v.getValue(), "version", v.getVersion(), "timestamp", v.getTimestamp()));
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
    VersionedValue v = kvStore.get(key);
    if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    return ResponseEntity.ok(Map.of("value", v.getValue(), "version", v.getVersion(), "timestamp", v.getTimestamp()));
  }

}
