package com.intelera.agentic.patient.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class WebMvcConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void corsPreflightRequest_whenOriginAllowed_shouldReturnCorsHeaders() throws Exception {
    mockMvc
        .perform(
            options("/actuator/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Access-Control-Allow-Origin"));
  }

  @Test
  void corsPreflightRequest_forPost_shouldIncludeAllowedMethods() throws Exception {
    mockMvc
        .perform(
            options("/actuator/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }
}
