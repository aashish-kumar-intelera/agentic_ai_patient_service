# Template: Problem JSON Errors (RFC 7807)

**Layer:** 30-templates
**Type:** Reference template
**Standard:** RFC 7807 – Problem Details for HTTP APIs
**Spring:** Uses `ProblemDetail` (Spring 6 native)

---

## Purpose

Defines the standard error response format for all REST APIs in this repository.
All error responses MUST use `Content-Type: application/problem+json`.
All error responses MUST follow RFC 7807 structure.

---

## RFC 7807 Schema

```json
{
  "type": "URI reference identifying the problem type",
  "title": "Short, human-readable summary (MUST NOT change between occurrences)",
  "status": 400,
  "detail": "Human-readable explanation specific to this occurrence",
  "instance": "URI reference identifying the specific occurrence",
  "timestamp": "2026-03-03T10:00:00Z",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "not-an-email"
    }
  ]
}
```

---

## Problem Type URI Convention

All `type` URIs follow this pattern:
```
https://api.{domain}/problems/{problem-slug}
```

| HTTP Status | Slug | Example |
|-------------|------|---------|
| 400 | validation-error | `https://api.example.com/problems/validation-error` |
| 400 | malformed-request | `https://api.example.com/problems/malformed-request` |
| 401 | unauthorized | `https://api.example.com/problems/unauthorized` |
| 403 | forbidden | `https://api.example.com/problems/forbidden` |
| 404 | not-found | `https://api.example.com/problems/not-found` |
| 404 | patient-not-found | `https://api.example.com/problems/patient-not-found` |
| 409 | email-conflict | `https://api.example.com/problems/email-conflict` |
| 409 | resource-conflict | `https://api.example.com/problems/resource-conflict` |
| 409 | optimistic-lock | `https://api.example.com/problems/optimistic-lock` |
| 422 | business-rule-violation | `https://api.example.com/problems/business-rule-violation` |
| 500 | internal-error | `https://api.example.com/problems/internal-error` |

---

## Spring 6 Implementation: GlobalExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";

    // =========================================================================
    // 400 – Validation Errors
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new FieldErrorDetail(
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null
            ))
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed. Check 'errors' for field-level details."
        );
        problem.setType(URI.create("https://api.example.com/problems/validation-error"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(cv -> new FieldErrorDetail(
                cv.getPropertyPath().toString(),
                cv.getMessage(),
                cv.getInvalidValue() != null ? cv.getInvalidValue().toString() : null
            ))
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Constraint validation failed."
        );
        problem.setType(URI.create("https://api.example.com/problems/validation-error"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body is malformed or contains invalid JSON."
        );
        problem.setType(URI.create("https://api.example.com/problems/malformed-request"));
        problem.setTitle("Malformed Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    // =========================================================================
    // 404 – Not Found
    // =========================================================================

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePatientNotFound(
            PatientNotFoundException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.example.com/problems/patient-not-found"));
        problem.setTitle("Patient Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "The requested resource was not found."
        );
        problem.setType(URI.create("https://api.example.com/problems/not-found"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    // =========================================================================
    // 409 – Conflict
    // =========================================================================

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.example.com/problems/email-conflict"));
        problem.setTitle("Email Already Exists");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The resource was modified by another request. Please reload and retry."
        );
        problem.setType(URI.create("https://api.example.com/problems/optimistic-lock"));
        problem.setTitle("Concurrent Modification Conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    // =========================================================================
    // 500 – Internal Server Error (catch-all)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Log full exception internally — NEVER expose stack trace to client
        log.error("Unhandled exception on {} {}: {}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please contact support if the issue persists."
        );
        problem.setType(URI.create("https://api.example.com/problems/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC));

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    // =========================================================================
    // Inner record for field errors
    // =========================================================================

    public record FieldErrorDetail(
        String field,
        String message,
        String rejectedValue
    ) {}
}
```

---

## Error Response Examples

### 400 – Validation Error

```json
{
  "type": "https://api.example.com/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed. Check 'errors' for field-level details.",
  "instance": "/patients",
  "timestamp": "2026-03-03T10:00:00Z",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "not-an-email"
    },
    {
      "field": "firstName",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

### 401 – Unauthorized

```json
{
  "type": "https://api.example.com/problems/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Valid authentication credentials are required to access this resource.",
  "instance": "/patients",
  "timestamp": "2026-03-03T10:00:00Z"
}
```

### 404 – Patient Not Found

```json
{
  "type": "https://api.example.com/problems/patient-not-found",
  "title": "Patient Not Found",
  "status": 404,
  "detail": "Patient with ID 550e8400-e29b-41d4-a716-446655440000 does not exist.",
  "instance": "/patients/550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-03T10:00:00Z"
}
```

### 409 – Email Conflict

```json
{
  "type": "https://api.example.com/problems/email-conflict",
  "title": "Email Already Exists",
  "status": 409,
  "detail": "A patient with email j***@example.com already exists.",
  "instance": "/patients",
  "timestamp": "2026-03-03T10:00:00Z"
}
```

### 500 – Internal Server Error

```json
{
  "type": "https://api.example.com/problems/internal-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please contact support if the issue persists.",
  "instance": "/patients",
  "timestamp": "2026-03-03T10:00:00Z"
}
```

---

## Security Rules for Error Responses

| Forbidden | Required |
|-----------|----------|
| Stack trace in body | Log internally, generic message to client |
| Internal class names | Use generic "Internal Server Error" |
| SQL error messages | Map to 500 with generic message |
| Full email in conflict | Mask: `j***@example.com` |
| File paths | Never expose |
| Spring internal messages | Translate to user-friendly messages |

---

## Integration Test Pattern for Error Responses

```java
@Test
void createPatient_whenEmailInvalid_shouldReturn400WithProblemDetail() throws Exception {
    PatientRequest request = PatientRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("not-valid-email")       // invalid
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();

    mockMvc.perform(post("/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.instance").value("/patients"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.errors[0].field").value("email"))
        .andExpect(jsonPath("$.errors[0].message").exists());
}
```

---

## Checklist

```
□ All error responses use Content-Type: application/problem+json
□ All ProblemDetail instances have: type, title, status, detail, instance, timestamp
□ 400 responses include errors array with field, message, rejectedValue
□ 500 responses never expose stack traces or internal details
□ Email in error messages is masked (first char + ***)
□ GlobalExceptionHandler has catch-all Exception handler at the bottom
□ Exception catch-all logs at ERROR level with full stack trace internally
□ All exception types thrown in service have corresponding handler
□ Integration tests verify Content-Type: application/problem+json
□ Integration tests verify error JSON structure
```
