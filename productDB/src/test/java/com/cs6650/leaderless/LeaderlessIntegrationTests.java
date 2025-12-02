package com.cs6650.leaderless;

import com.cs6650.leaderless.model.VersionedValue;
import com.cs6650.leaderless.service.KVStore;
import com.cs6650.leaderless.service.PropagationService;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LeaderlessIntegrationTests {

  private static KVStore kvStore;
  private static PropagationService propagationService;

  @BeforeAll
  public static void setup() {
    kvStore = new KVStore(8081);

    String peers = "http://node2:8082,http://node3:8083";
    propagationService = new PropagationService(peers, "http://node1:8081");
  }

  private final RestTemplate rest = new RestTemplate();
  private final String node1 = "http://localhost:8081";
  private final String node2 = "http://localhost:8082";
  private final String node3 = "http://localhost:8083"; // Coordinator for the write
  private final String node4 = "http://localhost:8084";
  private final String node5 = "http://localhost:8085";


  @Test
  @Order(1)
  public void writeToNode3_thenImmediateReadFromNode1_mayBeStale() throws InterruptedException {
    String key = "testkey";
    String value = "v1";

    // write to node3
    HttpEntity<Map<String,String>> req = new HttpEntity<>(Map.of("key", key, "value", value));
    ResponseEntity<String> r = rest.postForEntity(node3 + "/set", req, String.class);
    assertTrue(r.getStatusCode().is2xxSuccessful());

    // Immediately read from node1 — it might be stale (404) or have the value.
    // We must handle the 404 case, as getForEntity throws an exception for it.
    try {
      ResponseEntity<Map> readResp = rest.getForEntity(node1 + "/local_read/" + key, Map.class);
      // If we get here, the value has propagated.
      System.out.println("Node1 local_read (fast propagation): " + readResp.getStatusCode() + " body: " + readResp.getBody());
      assertEquals(HttpStatus.OK, readResp.getStatusCode());
    } catch (HttpClientErrorException.NotFound e) {
      // This is the expected "stale read" outcome where the key is not yet present.
      System.out.println("Node1 local_read (stale): " + e.getStatusCode());
      assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  @Order(2)
  public void afterWait_allNodesShouldAgree() throws InterruptedException {
    String key = "testkey";

    // wait longer than propagation time
    Thread.sleep(2000);

    ResponseEntity<Map> r1 = rest.getForEntity(node1 + "/get/" + key, Map.class);
    ResponseEntity<Map> r2 = rest.getForEntity(node2 + "/get/" + key, Map.class);
    ResponseEntity<Map> r3 = rest.getForEntity(node3 + "/get/" + key, Map.class);
    ResponseEntity<Map> r4 = rest.getForEntity(node4 + "/get/" + key, Map.class);
    ResponseEntity<Map> r5 = rest.getForEntity(node5 + "/get/" + key, Map.class);

    assertTrue(r1.getStatusCode().is2xxSuccessful());
    assertEquals(r1.getBody().get("version"), r2.getBody().get("version"));
    assertEquals(r2.getBody().get("version"), r3.getBody().get("version"));
    assertEquals(r3.getBody().get("version"), r4.getBody().get("version"));
    assertEquals(r4.getBody().get("version"), r5.getBody().get("version"));
  }

  @Test
  @Order(3)
  public void writeThenImmediateReadFromDifferentNodes_mayBeStale() {
    String key = "foo";
    String value = "bar";

    // Sent set request to node3
    HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("key", key, "value", value));
    ResponseEntity<String> writeResp = rest.postForEntity(node3 + "/set", req, String.class);
    assertTrue(writeResp.getStatusCode().is2xxSuccessful(), "Write should succeed");

    // read from every other node
    String[] otherNodes = {node1, node2, node4, node5};

    for (String node : otherNodes) {
      try {
        ResponseEntity<Map> readResp = rest.getForEntity(node + "/local_read/" + key, Map.class);
        System.out.println(node + " local_read: " + readResp.getBody());
        String readValue = (String) readResp.getBody().get("value");
        long readVersion = ((Number) readResp.getBody().get("version")).longValue();
        System.out.println(node + " version: " + readVersion);

        // Assert that if value exists, it must match what we wrote
        assertEquals(value, readValue, "Value read from " + node + " should match written value");

      } catch (HttpClientErrorException.NotFound e) {
        System.out.println(node + " local_read not found (stale)");
      }
    }
  }

  @Test
  @Order(4)
  public void testConcurrentWrites() throws InterruptedException {
    int nThreads = 10;
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < nThreads; i++) {
      final int idx = i;
      threads.add(new Thread(() -> {
        try {
          long version = kvStore.nextVersion();
          kvStore.putIfNewer("concurrent", "val" + idx, version);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }));
    }

    threads.forEach(Thread::start);
    for (Thread t : threads) t.join();

    VersionedValue finalVal = kvStore.get("concurrent");
    assertNotNull(finalVal);
    System.out.println("Concurrent final value: " + finalVal.getValue() + ", version: " + finalVal.getVersion());
  }

  @Test
  @Order(5)
  public void testConcurrentWritesAcrossNodes() throws InterruptedException {
    int nThreads = 5;
    String[] nodes = {node1, node2, node3, node4, node5};
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < nThreads; i++) {
      final int idx = i;
      threads.add(new Thread(() -> {
        try {
          String node = nodes[idx % nodes.length];
          String key = "concurrentSystemKey";
          String value = "val" + idx;
          HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("key", key, "value", value));
          ResponseEntity<String> r = rest.postForEntity(node + "/set", req, String.class);
          assertTrue(r.getStatusCode().is2xxSuccessful());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }));
    }

    threads.forEach(Thread::start);
    for (Thread t : threads) t.join();

    // wait for propagation
    Thread.sleep(2000);

    ResponseEntity<Map> r1 = rest.getForEntity(node1 + "/get/concurrentSystemKey", Map.class);
    ResponseEntity<Map> r2 = rest.getForEntity(node2 + "/get/concurrentSystemKey", Map.class);
    ResponseEntity<Map> r3 = rest.getForEntity(node3 + "/get/concurrentSystemKey", Map.class);
    ResponseEntity<Map> r4 = rest.getForEntity(node4 + "/get/concurrentSystemKey", Map.class);
    ResponseEntity<Map> r5 = rest.getForEntity(node5 + "/get/concurrentSystemKey", Map.class);

    long versionMax = Math.max(
            Math.max(((Number) r1.getBody().get("version")).longValue(), ((Number) r2.getBody().get("version")).longValue()),
            Math.max(Math.max(((Number) r3.getBody().get("version")).longValue(), ((Number) r4.getBody().get("version")).longValue()),
                    ((Number) r5.getBody().get("version")).longValue())
    );

    // All version from each node should be the maximum value
    assertEquals(versionMax, ((Number) r1.getBody().get("version")).longValue());
    assertEquals(versionMax, ((Number) r2.getBody().get("version")).longValue());
    assertEquals(versionMax, ((Number) r3.getBody().get("version")).longValue());
    assertEquals(versionMax, ((Number) r4.getBody().get("version")).longValue());
    assertEquals(versionMax, ((Number) r5.getBody().get("version")).longValue());
  }

  @Test
  @Order(6)
  public void testInconsistencyWindowAndEventualConsistency() throws InterruptedException {
    String key = "consistency-test";
    String value = "final-value";

    // 1. Write a key-value pair to a random node (node3 is write coordinator).
    HttpEntity<Map<String, String>> writeReq = new HttpEntity<>(Map.of("key", key, "value", value));
    ResponseEntity<String> writeResp = rest.postForEntity(node3 + "/set", writeReq, String.class);

    // The coordinator has acknowledged the write.
    assertTrue(writeResp.getStatusCode().is2xxSuccessful(), "Write to coordinator should succeed.");

    // 2. Within the update time window, read from other nodes. This should be inconsistent.
    try {
      rest.getForEntity(node1 + "/get/" + key, Map.class);
      // If this succeeds, propagation was unexpectedly fast. This is not a failure, but we expect a 404.
      System.out.println("INCONSISTENCY TEST: Read from replica was immediately consistent (fast propagation).");
    } catch (HttpClientErrorException.NotFound e) {
      // This is the EXPECTED outcome, demonstrating a stale read from a replica.
      System.out.println("INCONSISTENCY TEST: Correctly observed a stale read (404) from a replica.");
      assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // 3. After the Coordinator acknowledges the write, read from the Coordinator. That should be consistent.
    ResponseEntity<Map> coordinatorReadResp = rest.getForEntity(node3 + "/get/" + key, Map.class);
    assertTrue(coordinatorReadResp.getStatusCode().is2xxSuccessful(), "Read from coordinator should be consistent.");
    assertEquals(value, coordinatorReadResp.getBody().get("value"), "Coordinator should have the correct value.");

    // Wait for propagation to complete
    Thread.sleep(2000);

    // 4. After the Coordinator acknowledges the write, read from another node. That should be consistent.
    ResponseEntity<Map> replicaReadResp = rest.getForEntity(node1 + "/get/" + key, Map.class);
    assertTrue(replicaReadResp.getStatusCode().is2xxSuccessful(), "Read from replica should be consistent after propagation.");
    assertEquals(value, replicaReadResp.getBody().get("value"), "Replica should have the correct value after propagation.");

    // Verify that the versions also match
    assertEquals(coordinatorReadResp.getBody().get("version"), replicaReadResp.getBody().get("version"));
  }

}