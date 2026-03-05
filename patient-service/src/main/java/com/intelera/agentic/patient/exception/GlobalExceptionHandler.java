package com.intelera.agentic.patient.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler providing RFC7807 Problem Details for all error responses.
 *
 * <p>Security rules enforced: - No stack traces in response body (logged internally only) - No
 * internal class names exposed - No SQL error messages surfaced - Spring internal messages
 * translated to user-friendly equivalents
 *
 * <p>Domain-specific exception handlers (PatientNotFoundException, DuplicateEmailException, etc.)
 * will be added to this class when the Patient CRUD layer is implemented.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TIMESTAMP_KEY = "timestamp";

  @Value("${app.api-domain:api.example.com}")
  private String apiDomain;

  // =========================================================================
  // 400 – Validation Errors
  // =========================================================================

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<FieldErrorDetail> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new FieldErrorDetail(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null))
            .toList();

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed. Check 'errors' for field-level details.");
    problem.setType(problemTypeUri(ErrorCodes.VALIDATION_ERROR));
    problem.setTitle("Validation Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));
    problem.setProperty("errors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    List<FieldErrorDetail> fieldErrors =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    new FieldErrorDetail(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getInvalidValue() != null ? cv.getInvalidValue().toString() : null))
            .toList();

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint validation failed.");
    problem.setType(problemTypeUri(ErrorCodes.VALIDATION_ERROR));
    problem.setTitle("Validation Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));
    problem.setProperty("errors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleMalformedRequest(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    log.warn(
        "Malformed request body on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage());

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Request body is malformed or contains invalid JSON.");
    problem.setType(problemTypeUri(ErrorCodes.MALFORMED_REQUEST));
    problem.setTitle("Malformed Request");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // =========================================================================
  // 404 – Resource Not Found
  // =========================================================================

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, "The requested resource was not found.");
    problem.setType(problemTypeUri(ErrorCodes.NOT_FOUND));
    problem.setTitle("Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // =========================================================================
  // 500 – Internal Server Error (catch-all — must be last)
  // =========================================================================

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(
      Exception ex, HttpServletRequest request) {

    // Log full exception internally — NEVER expose stack trace to client
    log.error(
        "Unhandled exception on {} {}: {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage(),
        ex);

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please contact support if the issue persists.");
    problem.setType(problemTypeUri(ErrorCodes.INTERNAL_ERROR));
    problem.setTitle("Internal Server Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private URI problemTypeUri(String slug) {
    return URI.create("https://" + apiDomain + "/problems/" + slug);
  }

  /** Immutable record representing a single field-level validation error. */
  public record FieldErrorDetail(String field, String message, String rejectedValue) {}
}
