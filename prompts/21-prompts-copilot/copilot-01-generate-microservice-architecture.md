# Copilot Prompt 01: Generate Microservice Base Architecture

**Layer:** 21-prompts-copilot
**Task:** TASK 1 – Base Microservice Architecture
**AI Target:** GitHub Copilot Chat (IDE-inline)
**Quality bar:** Identical to Claude prompt 01

---

## Usage

Paste this into GitHub Copilot Chat in your IDE.
Keep the workspace open at the project root.

---

## PROMPT

Generate a production-grade Java 21 + Spring Boot 3.2.x Maven microservice named `patient-service`.
Group ID: `com.intellera`. Base package: `com.intellera.patientservice`.

**Rules (non-negotiable):**
- Output every file completely — no truncation, no "omitted for brevity"
- No placeholder values, no TODO/FIXME
- No hardcoded secrets — use `${ENV_VAR}` patterns
- Output file tree first, then each file in numbered order

---

### Stack

| | |
|-|-|
| Java | 21 |
| Spring Boot | 3.2.x |
| Build | Maven |
| DB (local/test) | H2 in-memory |
| DB (prod) | PostgreSQL (configured only) |
| Migration | Flyway |
| Security | Spring Security 6, stateless, JWT-ready |
| Quality | SpotBugs (fail HIGH+), Spotless (Google style) |
| Security scan | OWASP Dependency-Check (fail CVSS 7+) |
| Tracing | Micrometer + OpenTelemetry bridge |
| Logging | Logback + LogstashEncoder (JSON) |
| Actuator | Secured |

---

### Files to Generate

```
pom.xml
.gitignore
src/main/java/com/intellera/patientservice/
  Application.java
  config/SecurityConfig.java
  config/JpaConfig.java
  config/OpenApiConfig.java
  filter/MdcRequestFilter.java
  exception/GlobalExceptionHandler.java
src/main/resources/
  application.yml
  application-local.yml
  application-dev.yml
  application-test.yml
  application-prod.yml
  logback-spring.xml
  db/migration/V1__baseline.sql
src/test/java/com/intellera/patientservice/
  ApplicationIT.java
```

---

### Key Requirements per File

**pom.xml:**
- Spring Boot 3.2.x parent, Java 21 properties
- Deps: web, data-jpa, validation, actuator, security, h2, postgresql, flyway,
  micrometer-tracing-bridge-otel, opentelemetry-exporter-otlp, logstash-logback-encoder,
  spring-boot-starter-test, spring-security-test
- Plugins: compiler(21), surefire(exclude IT), failsafe(include IT), spotbugs(MAX/HIGH/fail),
  spotless(Google/fail), dependency-check(cvss=7/fail)

**SecurityConfig.java:**
- `@EnableWebSecurity @EnableMethodSecurity`
- `SecurityFilterChain` bean
- All requests: `.anyRequest().authenticated()`
- Session: `STATELESS`, CSRF: disabled
- Security headers: DENY frame options, nosniff, HSTS
- Actuator: health+info open; rest require `ROLE_ACTUATOR`
- JWT: `JwtDecoder` bean from env var secret
- `PasswordEncoder` BCrypt(12)
- Register `MdcRequestFilter` before `UsernamePasswordAuthenticationFilter`

**MdcRequestFilter.java:**
- `OncePerRequestFilter`, `@Order(HIGHEST_PRECEDENCE)`
- Read/generate `X-Request-ID`; put in MDC and response header
- `MDC.clear()` in finally block
- Set `userId` from `SecurityContextHolder` or "anonymous"

**GlobalExceptionHandler.java:**
- `@RestControllerAdvice`
- `MethodArgumentNotValidException` → 400 + field errors (ProblemDetail)
- `NoResourceFoundException` → 404 (ProblemDetail)
- `Exception` catch-all → 500, no stack trace in body
- All responses: `Content-Type: application/problem+json`
- Add `timestamp` extension to every ProblemDetail

**application.yml:**
- `spring.application.name: patient-service`
- `server.shutdown: graceful`
- `spring.jpa.open-in-view: false`
- Actuator: expose health, info, metrics, prometheus; health show-details: when-authorized

**application-local.yml:**
- H2: `jdbc:h2:mem:patientdb;DB_CLOSE_DELAY=-1`
- H2 console: enabled
- `ddl-auto: validate`, Flyway enabled, baseline-on-migrate: true
- JWT from env (with dev default)
- Tracing: 1.0

**application-test.yml:**
- H2, `ddl-auto: create-drop`
- Tracing: 0.0
- Log: WARN root, INFO app package

**application-dev.yml + application-prod.yml:**
- PostgreSQL using `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
- `ddl-auto: validate`
- JWT from `${JWT_SECRET}`
- OTEL from `${OTEL_EXPORTER_OTLP_ENDPOINT}`
- Prod: tracing `${TRACING_SAMPLE_RATE:0.1}`, log WARN

**logback-spring.xml:**
- Profile `local,dev,test`: ConsoleAppender with LogstashEncoder, include MDC keys
- Profile `prod`: same but root level WARN
- MDC keys to include: traceId, spanId, requestId, userId

**ApplicationIT.java:**
- `@SpringBootTest(webEnvironment=RANDOM_PORT) @ActiveProfiles("test")`
- Test: context loads
- Test: GET /actuator/health → 200

---

### Spring Boot 3 / Security 6 API Notes

- Use `requestMatchers()` not `antMatchers()` (removed)
- Do NOT extend `WebSecurityConfigurerAdapter` (removed)
- Use `ProblemDetail` directly (Spring 6 native)
- Use `@GeneratedValue(strategy = GenerationType.UUID)` for UUID PKs
- Virtual threads: `spring.threads.virtual.enabled: true` (optional, add if desired)

---

### Output Format

```
## File Tree
1. pom.xml
2. ...

### 1. pom.xml
```xml
<complete content>
```

[all files...]

## Verification
- [ ] mvn clean verify passes
- [ ] curl /actuator/health → 200
- [ ] curl /api/any (no token) → 401
- [ ] Logs are valid JSON
```
