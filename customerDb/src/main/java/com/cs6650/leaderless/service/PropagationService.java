package com.cs6650.leaderless.service;

import com.cs6650.leaderless.model.Customer;
import com.cs6650.leaderless.model.VersionedValue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Handles propagation of Customer updates and quorum-based reads
 * in a leaderless replication system.
 */
@Service
public class PropagationService {

    /** HTTP client for communicating with peer replicas */
    private final RestTemplate rest = new RestTemplate();

    @Value("${propagation.write.quorum}")
    private int propagationWriteQuorum;

    @Value("${propagation.read.quorum}")
    private int propagationReadQuorum;

    /** List of peer node URLs */
    @Getter @Setter
    public List<String> peers;

    /** URL of this node, used to skip self during quorum operations */
    @Getter @Setter
    public String selfAddress;

    /** Thread pool for concurrent propagation */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${max.retries}")
    private int maxRetries;

    private final CustomerStore customerStore;

    /**
     * Creates a PropagationService.
     */
    @Autowired
    public PropagationService(
            @Value("${peers:}") String peerList,
            @Value("${self.url:}") String selfUrl,
            CustomerStore customerStore) {

        this.peers = Arrays.stream(peerList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        this.selfAddress = selfUrl;
        this.customerStore = customerStore;
    }

    /**
     * Performs a quorum-based read (R) across all replicas.
     *
     * @param key customerId
     * @param timeoutMs timeout for quorum
     * @return newest VersionedValue, or null if quorum fails
     */
    public VersionedValue readWithQuorum(String key, long timeoutMs) {

        int quorum = this.propagationReadQuorum;

        List<String> otherPeers = peers.stream()
                .filter(peer -> !peer.equalsIgnoreCase(selfAddress))
                .toList();

        List<Callable<VersionedValue>> tasks = new ArrayList<>();

        tasks.add(() -> customerStore.getCustomer(key));

        for (String peer : otherPeers) {
            tasks.add(() -> {
                try {
                    String url = peer + "/local_read/" + key;
                    ResponseEntity<Map> resp = rest.getForEntity(url, Map.class);

                    if (!resp.getStatusCode().is2xxSuccessful()) return null;
                    if (resp.getBody() == null) return null;

                    Map<String, Object> body = resp.getBody();
                    Map<String, Object> customerMap = (Map<String, Object>) body.get("customer");
                    if (customerMap == null) return null;

                    // Construct Customer object
                    Customer c = new Customer();
                    c.setCustomerId((String) customerMap.get("customerId"));
                    c.setShoppingcartIds((List<String>) customerMap.get("shoppingcartIds"));

                    long version = ((Number) body.get("version")).longValue();
                    long timestamp = ((Number) body.get("timestamp")).longValue();

                    return new VersionedValue(c, version, timestamp);

                } catch (Exception e) {
                    return null;
                }
            });
        }

        ExecutorService exec = Executors.newCachedThreadPool();
        List<Future<VersionedValue>> futures = tasks.stream()
                .map(exec::submit)
                .toList();

        long deadline = System.currentTimeMillis() + timeoutMs;
        int success = 0;
        VersionedValue best = null;

        for (Future<VersionedValue> f : futures) {
            long remaining = Math.max(1, deadline - System.currentTimeMillis());

            VersionedValue result = null;
            try {
                result = f.get(remaining, TimeUnit.MILLISECONDS);
            } catch (Exception ignore) {}

            if (result != null) {
                success++;

                if (best == null || result.getVersion() > best.getVersion()) {
                    best = result;
                }

                if (success >= quorum) {
                    exec.shutdownNow();
                    return best;
                }
            }
        }

        exec.shutdownNow();
        return null;
    }

    /**
     * Propagates a Customer update to peer nodes until write quorum is achieved.
     *
     * @param key customerId
     * @param customer new Customer object
     * @param version version of update
     * @param propagateTimeoutMs timeout for propagation
     * @return true if write quorum is met
     */
    public boolean propagate(String key, Customer customer, long version, long propagateTimeoutMs) {

        List<String> otherPeers = peers.stream()
                .filter(peer -> !peer.equalsIgnoreCase(selfAddress))
                .toList();

        if (otherPeers.isEmpty()) return true;

        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (String peer : otherPeers) {
            tasks.add(() -> {
                String url = peer + "/propagate";

                Map<String, Object> payload = new HashMap<>();
                payload.put("key", key);
                payload.put("customer", customer);  // FIXED
                payload.put("version", version);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

                boolean success = false;

                for (int i = 0; i <= maxRetries && !success; i++) {
                    try {
                        ResponseEntity<String> resp = rest.postForEntity(url, req, String.class);
                        success = resp.getStatusCode().is2xxSuccessful();

                        if (!success && i < maxRetries) Thread.sleep(100);

                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    } catch (Exception e) {
                        System.err.println("Error propagating to " + peer + ": " + e.getMessage());
                    }
                }

                System.out.println("Peer " + peer + " propagation " + (success ? "succeeded" : "failed"));
                return success;
            });
        }

        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();
        long deadline = System.currentTimeMillis() + propagateTimeoutMs;

        int successCount = 0;

        for (Future<Boolean> future : futures) {
            long timeout = Math.max(1, deadline - System.currentTimeMillis());

            boolean ok = false;
            try {
                ok = !future.isCancelled() && future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception ignore) {}

            if (ok) {
                successCount++;
                if (successCount >= propagationWriteQuorum) {
                    futures.forEach(f -> f.cancel(true));
                    return true;
                }
            }
        }

        return false;
    }
}