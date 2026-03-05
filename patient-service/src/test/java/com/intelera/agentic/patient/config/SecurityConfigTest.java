package com.intelera.agentic.patient.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void actuatorHealth_whenUnauthenticated_shouldReturn200() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void actuatorInfo_whenUnauthenticated_shouldReturn200() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  void arbitraryEndpoint_whenUnauthenticated_shouldReturn401() throws Exception {
    // Confirms default-deny: any unregistered path requires authentication
    mockMvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
  }

  @Test
  void actuatorPrometheus_whenUnauthenticated_shouldReturn401() throws Exception {
    // Prometheus endpoint is not in the permit list — requires authentication
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }
}
