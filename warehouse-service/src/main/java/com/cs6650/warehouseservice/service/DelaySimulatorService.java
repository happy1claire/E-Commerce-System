package com.cs6650.warehouseservice.service;

import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class DelaySimulatorService {

  private final Random random = new Random();

  /**
   * Simulate business logic delay (100-1000ms)
   */
  public void simulateDelay() {
    try {
      // Linear random delay
      int delay = 100 + random.nextInt(901);
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Log-normal distribution delay
   */
  public void simulateLogNormalDelay() {
    try {
      double mu = 5.3;    // ln(200) - median 200ms
      double sigma = 0.6; // spread
      double normal = random.nextGaussian();
      double delay = Math.exp(mu + sigma * normal);
      Thread.sleep((long) delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}