package com.intelera.agentic.patient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Tracing configuration extension point.
 *
 * <p>Spring Boot 3.x auto-configures distributed tracing via: - micrometer-tracing-bridge-otel:
 * bridges Micrometer Observation API to OpenTelemetry - opentelemetry-exporter-otlp: exports spans
 * to an OTLP-compatible collector
 *
 * <p>Runtime configuration is managed in application.yml: - management.tracing.sampling.probability
 * (1.0 dev, 0.1 prod) - management.otlp.tracing.endpoint (OTEL_EXPORTER_OTLP_ENDPOINT env var in
 * prod)
 *
 * <p>Extend this class to add custom: - Span decorators / ObservationHandlers - Baggage field
 * propagation - Custom sampler logic
 */
@Configuration
public class TracingConfig {

  private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

  @EventListener(ApplicationReadyEvent.class)
  public void logTracingStatus() {
    log.info(
        "Distributed tracing initialized: micrometer-tracing-bridge-otel + OTel OTLP exporter");
  }
}
