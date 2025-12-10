package com.cs6650.productservice; // Use your actual package name

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Random;

/**
 * A custom health indicator that simulates a flaky service.
 * It will randomly report "DOWN" 50% of the time.
 */
@Component
public class RandomHealthIndicator implements HealthIndicator {
    // Add configuration to enable "bad" behavior mode
    // Set environment variable BAD_SERVICE_MODE=true for the bad instance
    @Value("${bad.service.mode:false}")
    private boolean badServiceMode;

    private final Random random = new Random();

    @Override
    public Health health() {
        // 50% chance to be "down"
        if (badServiceMode && random.nextBoolean()) {
            // Report "DOWN" status. This will cause /actuator/health
            // to return an HTTP 503 status code.
            return Health.down()
                    .withDetail("reason", "Simulated random failure")
                    .build();
        }

        // 50% chance to be "up"
        return Health.up().build();
    }
}