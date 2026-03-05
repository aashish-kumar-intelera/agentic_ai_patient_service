package com.intelera.agentic.patient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Stateless security filter chain.
   *
   * <p>CSRF disabled: stateless JWT API — CSRF tokens are not applicable for token-based auth.
   * Sessions disabled: each request is independently authenticated via JWT Bearer token.
   *
   * <p>CORS: enabled using WebMvcConfig CorsRegistry configuration via Customizer.withDefaults().
   * OPTIONS preflight requests are permitted so browsers can complete the CORS handshake.
   *
   * <p>Authentication entry point: returns HTTP 401 (not 403) for unauthenticated requests,
   * conforming to the RFC7807 "unauthorized" problem type.
   *
   * <p>Permitted without authentication: /actuator/health, /actuator/info, OPTIONS /** All other
   * endpoints require a valid authenticated principal.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        // CSRF disabled: stateless JWT API — CSRF not applicable
        .csrf(csrf -> csrf.disable())
        // Enable CORS using the CorsRegistry defined in WebMvcConfig
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Return 401 Unauthorized for unauthenticated requests (default Spring Security returns
        // 403)
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Allow CORS preflight requests without authentication
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .build();
  }
}
