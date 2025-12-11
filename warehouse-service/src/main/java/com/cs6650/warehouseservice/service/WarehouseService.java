package com.cs6650.warehouseservice.service;

import com.cs6650.warehouseservice.dto.ShipRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WarehouseService {

    // The name of the RabbitMQ queue to listen to
    private final static String QUEUE_NAME = "warehouse_orders";

    // Counter for the total number of orders
    private final AtomicLong totalOrders = new AtomicLong(0);

    // Hash Map to track product quantities
    private final ConcurrentHashMap<String, AtomicLong> productInventory = new ConcurrentHashMap<>();

    // For time tracking
    private volatile long startTime = 0;

    // Track last processed message time
    private volatile long lastMessageTime = System.currentTimeMillis();

    // To avoid printing idle report multiple times
    private volatile boolean idleReported = false;

    // Object mapper that is reusable
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Add RestTemplate for making HTTP calls
    private final RestTemplate restTemplate;
    // private final String shipEndpoint = "http://localhost:8082/warehouse/ship";

    // Inject from application.properties - configurable for different environments
    @Value("${warehouse.ship.endpoint:http://localhost:8080/warehouse/ship}")
    private String shipEndpoint;

    public WarehouseService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * This is the multithreaded consumer of RabbitMQ messages.
     *
     * @param order       The order message from RabbitMQ, should be in JSON format
     * @param channel     The RabbitMQ channel (used for ack/nack)
     * @param deliveryTag The message's unique ID (used for ack/nack)
     * @throws IOException If the message not conform the format, send negative
     *                     acknowledgement
     */
    @RabbitListener(queues = QUEUE_NAME, concurrency = "10-20")
    public void handleOrderMessage(String order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            lastMessageTime = System.currentTimeMillis(); // update last message time
            idleReported = false; // reset idle report flag on new message

            if (totalOrders.get() == 0) {
                startTime = System.currentTimeMillis();
            }

            // System.out.println(order);

            JsonNode rootNode = objectMapper.readTree(order);

            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {

                    String productId = itemNode.path("productId").asText();
                    int quantity = itemNode.path("quantity").asInt();

                    if (productId != null && quantity > 0) {
                        productInventory.computeIfAbsent(productId, k -> new AtomicLong(0))
                                .addAndGet(quantity);

                        // Call the ship endpoint for each item
                        try {
                            ShipRequest shipRequest = new ShipRequest();
                            shipRequest.setProductId(Integer.parseInt(productId));
                            shipRequest.setQuantity(quantity);

                            restTemplate.postForEntity(shipEndpoint, shipRequest, String.class);
                        } catch (Exception e) {
                            System.err.println("Failed to call ship endpoint for product " + productId + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Increment total order count
            long currentCount = totalOrders.incrementAndGet();

            // print every 10k messages
            if (currentCount % 10000 == 0) {
                System.out.println(" - Processed " + currentCount + " orders");
            }
            // send a single ack. Prefetch will make this fast.
            channel.basicAck(deliveryTag, false);
            // Batch acknowledge every 1000 messages
            // if (currentCount % 1000 == 0) {
            // channel.basicAck(deliveryTag, true); // multiple = true
            // } else {
            // channel.basicAck(deliveryTag, false);
            // }

        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());

            // Send Manual Negative Acknowledgement
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Scheduled check every 1 second for idle period.
     * If no messages for 10 seconds, print throughput report.
     * Use to print the report once the 200k requests are received
     */
    @Scheduled(fixedRate = 1000)
    public void checkIdle() {
        long now = System.currentTimeMillis();
        if ((now - lastMessageTime) > 10_000 && totalOrders.get() > 0 && !idleReported) { // 10-second quiet period
            idleReported = true; // only print once per idle session
            long endTime = now;
            double totalSeconds = (endTime - startTime) / 1000.0;
            System.out.println("\n=================================");
            System.out.println("No messages received for 10 seconds. Considered done.");
            System.out.println("Processed " + totalOrders.get() + " messages");
            System.out.println("Total time: " + totalSeconds + " seconds");
            System.out.println("Throughput: " + (totalOrders.get() / totalSeconds) + " msg/sec");
            System.out.println("=================================");

            System.out.println("\n---------------------------------");
            System.out.println("  Total Number of Orders Received: " + totalOrders.get());
            System.out.println("--- Product Counts ---");

            productInventory.forEach((productId, count) -> {
                System.out.println("  - Product '" + productId + "': " + count.get());
            });

            System.out.println("---------------------------------");
        }
    }

    /**
     * Generate report on shutdown
     */
    @PreDestroy
    public void report() {
        System.out.println("\n---------------------------------");
        System.out.println("  Total Number of Orders Received: " + totalOrders.get());
        System.out.println("--- Product Counts ---");

        productInventory.forEach((productId, count) -> {
            System.out.println("  - Product '" + productId + "': " + count.get());
        });

        System.out.println("---------------------------------");
    }
}