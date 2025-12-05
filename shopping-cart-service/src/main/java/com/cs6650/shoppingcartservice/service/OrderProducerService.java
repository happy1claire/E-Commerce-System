package com.cs6650.shoppingcartservice.service;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Service
public class OrderProducerService {
    private static final Logger log = LoggerFactory.getLogger(OrderProducerService.class);

    /**
     * The Spring AMQP template used for sending messages to RabbitMQ.
     */
    @Autowired
    private final RabbitTemplate rabbitTemplate;

    // ObjectMapper to convert our order object to a JSON string
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Random generator
    private final Random random = new Random();

    /**
     * The name of the target RabbitMQ queue where order messages will be sent.
     * This value is injected from the {@code rabbitmq.queue.name} property defined
     * in the application configuration (application.properties).
     */
    @Value("${rabbitmq.queue.name}")
    private String queueName;

    /**
     * Constructs the OrderProducerService with the necessary RabbitTemplate
     * dependency.
     *
     * @param rabbitTemplate The auto-configured RabbitTemplate instance.
     */
    public OrderProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends the provided order data as a JSON string to the configured message
     * queue.
     *
     * @param orderJson The raw JSON string representing the order data to be sent.
     *                  Must not be null.
     * @throws AmqpException If there is an issue during the message publishing
     *                       process
     *                       (e.g., connection failure, broker error). The specific
     *                       subclass, AmqpException will provide more details
     */
    public void sendOrderMessage(String orderJson) throws AmqpException {
        // Basic null check for the input JSON
        if (orderJson == null || orderJson.isEmpty()) {
            log.warn("- Attempted to send a null or empty order message.");
            throw new IllegalArgumentException("Cannot send a null or empty order message.");
        }

        try {
            // Send the raw JSON string to Message Queue.
            rabbitTemplate.convertAndSend(queueName, orderJson);
            // log.info("Sent order message to queue '{}': {}", queueName, orderJson);

        } catch (AmqpException e) {
            log.error("Failed to send order message to queue '{}'. Payload: {}", queueName, orderJson, e);
            // Re-throw the exception so the caller knows the operation failed
            throw e;
        }
    }
}
