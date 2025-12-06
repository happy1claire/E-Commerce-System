package com.cs6650.leaderless.service;

import com.cs6650.leaderless.model.Product;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service responsible for propagating key-value updates among peer nodes
 * in a leaderless replication setup.
 * When a node receives a client write request, it acts as the coordinator
 * and uses this service to forward the update to all other peers.
 * Each propagation is performed asynchronously but waited on collectively
 * to a quorum(W = N) consistency model.
 */
@Service
public class PropagationService {

  /** HTTP client used for sending POST requests to peer nodes. */
  private final RestTemplate rest = new RestTemplate();

  /**
   * List of peer node of comma separated URLs participating in replication.
   * Example:
   * -- GETTER --
   *  Returns the list of peer node URLs.
   *
   {@code http://localhost:8081,http://localhost:8082,http://localhost:8083}
   * @return list of peers
   */
  @Getter
  private final List<String> peers;

  /** The URL of this node itself, used to avoid sending propagation to self.
   * -- GETTER --
   *  Returns the URL of this current node.
   *
   * @return self URL
   */
  @Getter
  private final String selfUrl;

  /** Thread pool executor used to send propagation requests concurrently. */
  private final ExecutorService executor = Executors.newCachedThreadPool();

  /** Maximum retry time when propagation fail. */
  @Value("${max.retries}")
  private int maxRetries ;

  /**
   * Constructs a {@link PropagationService} that handles replication to peer nodes.
   *
   * @param peerList comma-separated list of peer URLs from application properties (e.g. {@code http://localhost:8081,http://localhost:8082})
   * @param selfUrl  the URL of this current node, used to skip self during propagation
   */
  public PropagationService(
      @Value("${peers:}") String peerList,
      @Value("${self.url:}") String selfUrl) {
    this.peers = Arrays.stream(peerList.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    this.selfUrl = selfUrl;
  }

  /**
   * Propagates a key-value update to all peer nodes and waits for all acknowledgements.
   * Each propagation request is sent concurrently using a thread pool, and
   * the method blocks until all peers respond or the given timeout is reached.
   * If any peer fails to acknowledge within the timeout, the propagation is
   * considered unsuccessful.
   *
   * @param key                the key being updated
   * @param product              the new value associated with the key
   * @param version            the version number of this update
   * @param propagateTimeoutMs timeout in milliseconds to wait for all acknowledgements
   * @return {@code true} if all peers successfully acknowledge the update; {@code false} otherwise
   */
  public boolean propagateToAll(String key, Product product, long version, long propagateTimeoutMs) {
    // filter out self from peers
    List<String> otherPeers = peers.stream()
            .filter(peer -> !peer.equalsIgnoreCase(selfUrl))
            .toList();

    // if no other peers, return true (single node case)
    if (otherPeers.isEmpty()) {
      return true;
    }

    List<Callable<Boolean>> tasks = new ArrayList<>();
    for (String peer : otherPeers) {
      tasks.add(() -> {
        // This lambda will be executed by the executor service, each task is a lambda function that returns a boolean
        String url = peer + "/propagate";
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", key);
        payload.put("product", product);
        payload.put("version", version);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        // all peers are attempted in parallel and each peer gets maxRetries attempts
        boolean success = false;
        for (int i = 0; i <= this.maxRetries && !success; i++) {
          try {
            ResponseEntity<String> resp = rest.postForEntity(url, req, String.class);
            success = resp.getStatusCode().is2xxSuccessful();
            if (!success && i < maxRetries) {
              Thread.sleep(100); // Sleep before next retry on non-2xx status
            }
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false; // Stop if interrupted
          } catch (Exception e) {
            // Network error or other exception, success remains false.
            System.err.println("Error propagating to " + peer + ": " + e.getMessage());
          }
          // Sleep before the next retry if the attempt failed and it's not the last retry
          if (!success && i < maxRetries) {
            Thread.sleep(100);
          }
        }
        System.out.println("Peer " + peer + " propagation " + (success ? "succeeded" : "failed"));
        return success;
      });
    }

    // Submit tasks with a delay after each submission
    List<Future<Boolean>> futures = new ArrayList<>();
    try {
      for (Callable<Boolean> task : tasks) {
        futures.add(executor.submit(task));
      }

      long deadline = System.currentTimeMillis() + propagateTimeoutMs;

      // W=N: All replicas must acknowledge for success
      for (Future<Boolean> future : futures) {
        long timeout = Math.max(1, deadline - System.currentTimeMillis()); // at least 1ms
        // A future might be cancelled if timeout is reached, or get() could throw an exception.
        // We use a calculated timeout for each future.get() call.
        if (!future.isCancelled() && future.get(timeout, TimeUnit.MILLISECONDS)) {
          // This one succeeded
        } else {
          // A peer failed or timed out. Since W=N, we fail the whole operation.
          System.out.println("Propagation failed for at least one peer.");
          // Cancel remaining tasks
          futures.forEach(f -> f.cancel(true));
          return false;
        }
      }
    } catch (InterruptedException | ExecutionException | CancellationException | TimeoutException e) {
      System.err.println("Error during propagation: " + e.getMessage());
      // On any failure, cancel outstanding tasks to release resources
      futures.forEach(f -> f.cancel(true));
      Thread.currentThread().interrupt();
      return false;
    }

    return true;
  }
}