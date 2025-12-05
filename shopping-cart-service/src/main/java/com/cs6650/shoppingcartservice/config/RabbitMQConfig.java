package com.cs6650.shoppingcartservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Properties from RabbitMQConfig ---
    @Value("${rabbitmq.queue.name}")
    private String queueName;

    // --- Properties from RabbitMQConnectionConfig ---
    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.mqChannelSize}")
    private int channelSize;

    @Value("${spring.rabbitmq.mqConnectionLimit}")
    private int connectionLimit;

    /**
     * Bean for the order queue
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(queueName, true);
    }

    /**
     * Bean for the connection factory (from your original RabbitMQConnectionConfig)
     * Spring Boot will automatically find and use this factory.
     */
    @Bean
    public CachingConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setChannelCacheSize(channelSize);

        factory.setConnectionLimit(connectionLimit);

        return factory;
    }
}