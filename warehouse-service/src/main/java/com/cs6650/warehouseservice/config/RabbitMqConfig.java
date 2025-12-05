package com.cs6650.warehouseservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // You can make the QUEUE_NAME from WarehouseService public static
    // and reference it here, or just redefine it.
    private final static String QUEUE_NAME = "warehouse_orders";

    /**
     * Declares the warehouse_orders queue.
     * Spring AMQP will automatically create this queue on the
     * RabbitMQ broker if it doesn't already exist.
     */
    @Bean
    public Queue warehouseOrdersQueue() {
        // The 'true' argument makes the queue durable (it survives broker restarts)
        // This is generally recommended.
        return new Queue(QUEUE_NAME, true);
    }
}