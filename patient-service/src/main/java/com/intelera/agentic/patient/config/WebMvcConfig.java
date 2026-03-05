package com.intelera.agentic.patient.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /**
   * Comma-separated allowed CORS origins. Configured via cors.allowed-origins in application.yml,
   * overridable per environment via the CORS_ALLOWED_ORIGINS environment variable.
   *
   * <p>Never use wildcard (*) in production — always specify exact allowed origins.
   */
  @Value("${cors.allowed-origins:http://localhost:3000}")
  private String allowedOriginsRaw;

  /**
   * Provides a CorsConfigurationSource bean consumed by Spring Security's CORS filter via
   * .cors(Customizer.withDefaults()) in SecurityConfig.
   *
   * <p>This ensures consistent CORS behaviour across ALL endpoints — including Spring MVC
   * controllers and Spring Boot Actuator endpoints which are not handled by the MVC dispatcher.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOriginsRaw.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Location", "X-Request-ID"));
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
