package com.intelera.agentic.patient.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH_SCHEME = "bearerAuth";

  /**
   * Configures the OpenAPI specification metadata for SpringDoc.
   *
   * <p>Defines service-level info (title, version, description) and the global JWT Bearer
   * authentication scheme. Individual API paths and schemas will be added when domain endpoints are
   * implemented.
   */
  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Patient Service API")
                .version("1.0.0")
                .description("Manages patient records for the healthcare platform")
                .contact(new Contact().name("Platform Team").email("platform@example.com")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH_SCHEME,
                    new SecurityScheme()
                        .name(BEARER_AUTH_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
