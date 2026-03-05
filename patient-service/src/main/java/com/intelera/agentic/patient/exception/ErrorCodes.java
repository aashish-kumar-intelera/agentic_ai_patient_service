package com.intelera.agentic.patient.exception;

/**
 * Application-level error code constants for RFC7807 problem type URI slugs.
 *
 * <p>Use these constants in GlobalExceptionHandler when constructing ProblemDetail type URIs:
 * {@code URI.create("https://api.example.com/problems/" + ErrorCodes.VALIDATION_ERROR)}
 */
public final class ErrorCodes {

  private ErrorCodes() {
    // Utility class — do not instantiate
  }

  public static final String VALIDATION_ERROR = "validation-error";
  public static final String MALFORMED_REQUEST = "malformed-request";
  public static final String UNAUTHORIZED = "unauthorized";
  public static final String FORBIDDEN = "forbidden";
  public static final String NOT_FOUND = "not-found";
  public static final String INTERNAL_ERROR = "internal-error";
}
