package com.intelera.agentic.patient.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Application-level metric beans for the patient-service.
 *
 * <p>Provides baseline request counters and timers. Domain-specific metrics (e.g.,
 * patients.created.total) will be added in the Patient CRUD layer.
 *
 * <p>Metrics exposed at: GET /actuator/prometheus (requires ROLE_ACTUATOR or management port)
 */
@Component
public class ApplicationMetrics {

  private final Counter requestCounter;
  private final Timer requestTimer;

  public ApplicationMetrics(MeterRegistry meterRegistry) {
    this.requestCounter =
        Counter.builder("app.requests.total")
            .description("Total number of HTTP requests processed by the application")
            .tag("service", "patient-service")
            .register(meterRegistry);

    this.requestTimer =
        Timer.builder("app.request.duration")
            .description("Duration of HTTP request processing in seconds")
            .tag("service", "patient-service")
            .register(meterRegistry);
  }

  /** Increments the total request counter. Call once per completed request. */
  public void incrementRequestCount() {
    requestCounter.increment();
  }

  /**
   * Returns the request duration Timer for recording operation latency.
   *
   * <p>Usage: {@code Timer.Sample sample = Timer.start(meterRegistry);
   * sample.stop(metrics.getRequestTimer());}
   */
  public Timer getRequestTimer() {
    return requestTimer;
  }
}
