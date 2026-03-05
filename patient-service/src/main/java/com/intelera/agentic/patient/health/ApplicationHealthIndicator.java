package com.intelera.agentic.patient.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the patient-service application.
 *
 * <p>Provides service-level health details beyond the default Spring Boot indicators. Extend this
 * class to add dependency checks (e.g., downstream service availability) as the service grows.
 *
 * <p>Accessible at: GET /actuator/health (permitted without authentication)
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

  @Override
  public Health health() {
    return Health.up()
        .withDetail("service", "patient-service")
        .withDetail("status", "running")
        .build();
  }
}
