# Skill: Security & OWASP

**Layer:** 00-skill
**Type:** Security contract
**Scope:** All Java Spring Boot microservice generation tasks

---

## Purpose

Enforces OWASP Top 10 mitigations, Spring Security baseline, dependency security scanning,
and secure coding practices. All generated code MUST pass these requirements.

---

## OWASP Top 10 Mitigations (2021)

### A01 – Broken Access Control

**Requirements:**
- All endpoints denied by default via Spring Security configuration.
- Role-based access control enforced for every endpoint.
- Resource ownership verified at service layer (not just controller).
- Path traversal prevention: never pass user input to `File` or `Path` constructors.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/actuator/**").hasRole("ACTUATOR")
            .anyRequest().authenticated()
        )
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
}
```

### A02 – Cryptographic Failures

**Requirements:**
- No plaintext storage of passwords, tokens, or secrets.
- Use `BCryptPasswordEncoder` with strength 12 minimum.
- TLS enforced in `prod` profile (via infrastructure, not application).
- No MD5 or SHA1 for password hashing.
- Secrets loaded from environment variables or secrets manager.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### A03 – Injection

**Requirements:**
- Never construct SQL strings with user input.
- Use Spring Data JPA named queries or `@Query` with parameters.
- JPQL parameters always bound with `:param` syntax.
- No `EntityManager.createNativeQuery` with string concatenation.
- Input validation at controller boundary via `@Valid`.

```java
// CORRECT
@Query("SELECT p FROM Patient p WHERE p.email = :email")
Optional<Patient> findByEmail(@Param("email") String email);

// FORBIDDEN
// entityManager.createQuery("SELECT p FROM Patient p WHERE p.email = '" + email + "'");
```

### A04 – Insecure Design

**Requirements:**
- Least privilege: every operation grants minimum required permissions.
- Fail open is forbidden: default to deny.
- Rate limiting considered for public endpoints (document if not implemented).
- No debug endpoints exposed in `prod` profile.

### A05 – Security Misconfiguration

**Requirements:**
- No default credentials.
- No unnecessary HTTP methods enabled (disable OPTIONS/TRACE if unused).
- CORS configured explicitly — no wildcard `*` in production.
- Security headers configured.
- Error responses must NOT expose stack traces.

```java
// Security headers
http.headers(headers -> headers
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
);
```

### A06 – Vulnerable and Outdated Components

**Requirements:**
- OWASP Dependency-Check Maven Plugin configured and run in `verify` phase.
- Build FAILS if any dependency has CVSS score >= 7 (HIGH or CRITICAL).
- Suppression file documented for any accepted false positives.

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>9.x.x</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>
    <suppressionFile>owasp-suppressions.xml</suppressionFile>
    <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>
  </configuration>
  <executions>
    <execution>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

### A07 – Identification and Authentication Failures

**Requirements:**
- JWT validation: signature, expiration, issuer, audience.
- No JWT secrets in source code.
- JWT secret loaded from environment variable.
- Token expiry enforced (access token: 15 minutes recommended).
- Refresh token rotation (if implemented).

```java
@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(
        new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        )
    ).build();
}
```

### A08 – Software and Data Integrity Failures

**Requirements:**
- Maven dependency hashes verified (default Maven behavior).
- No use of `@JsonIgnoreProperties(ignoreUnknown = true)` on security-sensitive DTOs.
- Deserialization of untrusted data forbidden.

### A09 – Security Logging and Monitoring Failures

**Requirements:**
- All authentication events logged (success and failure).
- All authorization failures logged.
- MDC populated for all log entries (traceId, requestId, userId).
- No sensitive data (passwords, tokens, PII) in logs.
- Log tampering: structured JSON output to STDOUT for log aggregation.

```java
log.warn("Authentication failure for user: {} from IP: {}",
    maskedUsername, requestIp);  // username masked, never raw password
```

### A10 – Server-Side Request Forgery (SSRF)

**Requirements:**
- Any URL constructed from user input must be validated against an allowlist.
- No user-controlled URLs passed to `RestTemplate` or `WebClient`.
- If integration with external URLs is required, validate against `allowedHosts` list.

---

## SpotBugs Security Rules

SpotBugs with `find-sec-bugs` plugin MUST be enabled:

```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.x.x</version>
  <dependencies>
    <dependency>
      <groupId>com.h3xstream.findsecbugs</groupId>
      <artifactId>findsecbugs-plugin</artifactId>
      <version>1.x.x</version>
    </dependency>
  </dependencies>
  <configuration>
    <effort>Max</effort>
    <threshold>High</threshold>
    <failOnError>true</failOnError>
    <plugins>
      <plugin>
        <groupId>com.h3xstream.findsecbugs</groupId>
        <artifactId>findsecbugs-plugin</artifactId>
        <version>1.x.x</version>
      </plugin>
    </plugins>
  </configuration>
  <executions>
    <execution>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

---

## Input Validation Rules

- All user-supplied input validated at controller entry point using `@Valid`.
- Custom validators for domain-specific rules (e.g., `@ValidEmail`, `@ValidPhoneNumber`).
- Validation errors return 400 with RFC7807 problem+json.
- No business logic executed on unvalidated input.

```java
// DTO
public class PatientRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;
}

// Controller
public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) { }
```

---

## Secrets Management Rules

| Forbidden | Required |
|-----------|----------|
| `password=secret123` in YAML | `password: ${DB_PASSWORD}` |
| JWT secret in code | `secret: ${JWT_SECRET}` |
| API key in test config | Use environment variable in test |
| Default H2 credentials in prod | Profile-specific config only |

---

## Error Response Security

- Never expose stack traces in API responses.
- Never expose internal class names or file paths.
- Generic error messages for server errors (500).
- Specific but non-disclosing messages for client errors (4xx).

```java
// CORRECT
{"title": "Internal Server Error", "status": 500, "detail": "An unexpected error occurred. Please contact support."}

// FORBIDDEN
{"title": "NullPointerException", "status": 500, "detail": "at com.example.service.PatientService.createPatient(PatientService.java:42)"}
```

---

## Definition of Done

- [ ] All endpoints denied by default in `SecurityConfig`
- [ ] CSRF disabled for stateless REST API
- [ ] Security headers configured (X-Frame-Options, HSTS, CSP)
- [ ] No plaintext secrets in any config file
- [ ] All inputs validated with `@Valid` and Bean Validation
- [ ] SpotBugs with find-sec-bugs configured (fail on HIGH)
- [ ] OWASP Dependency-Check configured (fail on CVSS 7+)
- [ ] Error responses do not expose stack traces or internal details
- [ ] Authentication and authorization failures logged
- [ ] `mvn clean verify` passes all security checks

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| `.anyRequest().permitAll()` default | Always use `.anyRequest().authenticated()` |
| Secret in `application.yml` committed to git | Environment variable pattern enforced |
| Stack trace in 500 response | Global exception handler strips internal details |
| Missing `@Valid` on controller params | Code review checklist item |
| OWASP check skipped in CI | Bind check to `verify` phase in POM |
| JWT secret too short | Minimum 256-bit (32 byte) secret enforced |
