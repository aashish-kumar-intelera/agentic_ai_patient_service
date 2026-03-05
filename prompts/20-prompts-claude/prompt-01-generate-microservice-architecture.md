# Claude Prompt 01: Generate Microservice Base Architecture

**Layer:** 20-prompts-claude
**Task:** TASK 1 – Base Microservice Architecture
**AI Target:** Claude AI (claude-sonnet-4-6 or claude-opus-4-6)
**Skills:** skill-global-output-contract, skill-java-spring-microservice, skill-security-owasp, skill-observability

---

## How to Use This Prompt

1. Copy everything in the "PROMPT START" section below.
2. Paste it as your first message in a Claude conversation.
3. Do not modify the prompt — it is designed to produce deterministic output.
4. After Claude generates output, run `mvn clean verify` to validate.

---

## PROMPT START

---

You are a Senior Enterprise Java Architect generating a production-grade Spring Boot microservice.

## Your Role and Constraints

You are NOT creating a prototype. You are generating production-grade code that will be
deployed to real environments. Every file must be complete. No shortcuts. No placeholders.

**ABSOLUTE RULES — violation invalidates the entire output:**
1. Output every file in full. Zero truncation.
2. Never write "omitted for brevity", "// ... rest of implementation", "similar to above", or equivalent.
3. Every method declared must be fully implemented.
4. Every import must be present.
5. No placeholder values: no `TODO`, `FIXME`, `your-value-here`, `<placeholder>`.
6. No hardcoded secrets, passwords, or credentials in non-test files.
7. Start by outputting the complete file tree. Then output each file in order.
8. End with a verification checklist.

---

## Task: Generate Base Microservice Architecture

Generate a complete Java 21 Spring Boot 3.x microservice project named `patient-service`
with group ID `com.intellera` and artifact ID `patient-service`.

This is the FOUNDATION project. No business domain entities yet — only the architecture scaffold.

---

## Technology Stack (Non-negotiable)

| Component | Requirement |
|-----------|------------|
| Java | 21 |
| Spring Boot | 3.2.x (use latest stable) |
| Build Tool | Maven |
| Base Package | `com.intellera.patientservice` |
| Database (local/test) | H2 in-memory |
| Database (dev/prod) | PostgreSQL (configured, not active) |
| Migration | Flyway |
| Security | Spring Security 6.x — stateless, JWT-ready |
| Code Quality | SpotBugs + Spotless (Google Java Style) |
| Security Scan | OWASP Dependency-Check (fail on CVSS 7+) |
| Tracing | Micrometer Tracing + OpenTelemetry bridge |
| Logging | Logback + Logstash Logback Encoder (JSON) |
| Actuator | Spring Boot Actuator (secured) |

---

## Required Project Structure

Generate exactly this structure:

```
patient-service/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/intellera/patientservice/
    │   │   ├── Application.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── JpaConfig.java
    │   │   │   └── OpenApiConfig.java
    │   │   ├── filter/
    │   │   │   └── MdcRequestFilter.java
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-dev.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       ├── logback-spring.xml
    │       └── db/migration/
    │           └── V1__baseline.sql
    └── test/
        ├── java/com/intellera/patientservice/
        │   └── ApplicationIT.java
        └── resources/
            └── application-test.yml
```

---

## Detailed Requirements

### pom.xml

Include ALL of the following:
- Spring Boot 3.2.x parent
- Java 21 properties
- Dependencies: web, data-jpa, validation, actuator, security, h2, postgresql, flyway-core,
  micrometer-tracing-bridge-otel, opentelemetry-exporter-otlp, logstash-logback-encoder,
  spring-boot-starter-test, spring-security-test
- Plugins:
  - maven-compiler-plugin (Java 21)
  - maven-surefire-plugin (exclude *IT.java)
  - maven-failsafe-plugin (include *IT.java, goals: integration-test, verify)
  - spotbugs-maven-plugin (effort=Max, threshold=High, failOnError=true, bound to verify)
  - spotless-maven-plugin (Google Java Style, fail on violation, bound to verify)
  - dependency-check-maven (failBuildOnCVSS=7, bound to verify)

### SecurityConfig.java

Must implement:
- `@EnableWebSecurity`
- `@EnableMethodSecurity`
- `SecurityFilterChain` bean
- All endpoints denied by default (`.anyRequest().authenticated()`)
- Session policy: `STATELESS`
- CSRF: disabled
- Security headers: X-Frame-Options DENY, X-Content-Type-Options nosniff, HSTS
- Actuator endpoints: `/actuator/health` and `/actuator/info` open, rest require `ROLE_ACTUATOR`
- JWT authentication: configure `JwtDecoder` bean with environment-variable secret
- CORS: configured but restricted (no wildcard in prod pattern)
- Add `MdcRequestFilter` before `UsernamePasswordAuthenticationFilter`
- `PasswordEncoder` bean using BCrypt strength 12

### JpaConfig.java

Must implement:
- `@EnableJpaAuditing`
- `AuditorAware` bean that reads `userId` from `SecurityContextHolder`

### OpenApiConfig.java

Must implement:
- SpringDoc OpenAPI configuration
- Bearer token security scheme
- API title, version, description

### MdcRequestFilter.java

Must implement:
- Extend `OncePerRequestFilter`
- Registered with `@Component @Order(Ordered.HIGHEST_PRECEDENCE)`
- Read `X-Request-ID` header; generate UUID if absent
- Set `MDC.put("requestId", requestId)`
- Set response header `X-Request-ID`
- After filter chain: MDC.clear() in finally block
- userId: extract from `SecurityContextHolder` after auth; set `MDC.put("userId", ...)` or "anonymous"

### GlobalExceptionHandler.java

Must implement:
- `@RestControllerAdvice`
- Handle: `MethodArgumentNotValidException` → 400 with field errors
- Handle: `ConstraintViolationException` → 400
- Handle: `NoResourceFoundException` → 404
- Handle: `Exception` (catch-all) → 500 (no stack trace in response body)
- All responses use `ProblemDetail` (Spring 6 native RFC7807) with `application/problem+json`
- Include `timestamp` extension on every problem detail response

### application.yml (shared config)

Include:
- `spring.application.name: patient-service`
- `server.shutdown: graceful`
- `server.port: 8080`
- `management` section with actuator exposure
- `spring.jpa.open-in-view: false`
- `spring.jpa.properties.hibernate.format_sql: true`
- Logging at INFO level for root, DEBUG for `com.intellera`

### application-local.yml

Include:
- H2 datasource: `jdbc:h2:mem:patientdb;DB_CLOSE_DELAY=-1`
- H2 console enabled (dev only)
- `spring.jpa.hibernate.ddl-auto: validate`
- Flyway enabled, baseline-on-migrate: true
- JWT secret from environment (with local default for convenience)
- Tracing sampling: 1.0

### application-dev.yml

Include:
- PostgreSQL datasource using `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
- `spring.jpa.hibernate.ddl-auto: validate`
- Flyway enabled
- JWT secret from `${JWT_SECRET}`
- OTEL endpoint from `${OTEL_EXPORTER_OTLP_ENDPOINT}`

### application-test.yml

Include:
- H2 datasource
- `spring.jpa.hibernate.ddl-auto: create-drop`
- Flyway enabled
- Tracing sampling: 0.0 (disable in tests)
- Logging: WARN for root, INFO for application package

### application-prod.yml

Include:
- PostgreSQL datasource using `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
- `spring.jpa.hibernate.ddl-auto: validate`
- H2 console: disabled
- Flyway enabled, out-of-order: false
- JWT secret from `${JWT_SECRET}`
- Tracing sampling: `${TRACING_SAMPLE_RATE:0.1}`
- Log level: WARN for root

### logback-spring.xml

Include:
- `<springProfile name="local,dev,test">` — ConsoleAppender with LogstashEncoder
- `<springProfile name="prod">` — ConsoleAppender with LogstashEncoder, root WARN
- Include MDC keys: traceId, spanId, requestId, userId

### V1__baseline.sql

Include:
- A minimal placeholder migration (empty `-- baseline` comment is acceptable)
- This is the schema baseline; actual domain tables added in feature migrations

### ApplicationIT.java

Include:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@ActiveProfiles("test")`
- Test: context loads
- Test: `GET /actuator/health` returns 200

---

## Reasoning Instructions

Before generating each file, think through:
1. What does this file need to accomplish?
2. What are its dependencies on other files?
3. Are there any Spring Boot 3 / Spring Security 6 API changes I need to account for?
4. Does this file comply with the security and observability rules above?

State your reasoning for any non-obvious decisions as a single `[DECISION]` comment at the top of the file.

---

## Anti-Hallucination Instructions

- Only use Spring Boot 3.x APIs. Do NOT use Spring Boot 2.x APIs.
- Only use Spring Security 6.x APIs. Methods like `antMatchers()` are removed — use `requestMatchers()`.
- `WebSecurityConfigurerAdapter` is removed in Spring Security 6 — do NOT extend it.
- `spring.security.oauth2.resourceserver.jwt` properties are valid — use them correctly.
- `ProblemDetail` is a native Spring 6 class — use it directly.
- `@EnableJpaAuditing` goes on a `@Configuration` class or `@SpringBootApplication`.
- `LogstashEncoder` fully qualified class: `net.logstash.logback.encoder.LogstashEncoder`.

---

## Output Format

Begin with:

```
## File Tree
[numbered list of all files]
```

Then for each file:

```
### {N}. {path/to/file}
```{language}
{complete file content}
```
```

End with:

```
## Verification

Run: `mvn clean verify`
Expected: BUILD SUCCESS

- [ ] All files output
- [ ] No truncation
- [ ] All imports present
- [ ] No TODO/FIXME
- [ ] No hardcoded secrets
- [ ] Security headers configured
- [ ] MDC filter implemented
- [ ] JSON logging configured
```

---

## PROMPT END
