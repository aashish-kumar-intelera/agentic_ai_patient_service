package com.intelera.agentic.patient.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class ApplicationHealthIndicatorTest {

  private final ApplicationHealthIndicator indicator = new ApplicationHealthIndicator();

  @Test
  void health_whenCalled_shouldReturnUp() {
    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void health_whenCalled_shouldIncludeServiceDetail() {
    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getDetails()).containsKey("service");
    assertThat(health.getDetails().get("service")).isEqualTo("patient-service");
  }

  @Test
  void health_whenCalled_shouldIncludeStatusDetail() {
    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getDetails()).containsKey("status");
    assertThat(health.getDetails().get("status")).isEqualTo("running");
  }
}
