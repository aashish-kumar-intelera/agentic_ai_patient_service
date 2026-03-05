# GitHub Copilot Agent Prompt — Generate Base Microservice Project

<!--
  SOURCE FILE : prompts/21-prompts-copilot/copilot-agent-generate-base-project.md
  REUSABLE    : YES — parameterized, invoke for any microservice name
  SCOPE       : Base project scaffold ONLY (no domain logic, no CRUD APIs)
  SKILLS USED : 00-skill/skill-global-output-contract.md
                00-skill/skill-java-spring-microservice.md
                00-skill/skill-security-owasp.md
                00-skill/skill-observability.md
  SPEC USED   : 10-specs/spec-microservice-base-architecture.md
  INVOCATION  : Paste into Copilot Chat (@workspace), Copilot Edits, or
                GitHub Copilot Workspace. Fill INPUT PARAMETERS first.
-->

---

## TASK

Generate a complete production-ready Spring Boot 3.x microservice base scaffold.
**No domain logic. No CRUD APIs. Platform layer only.**

---

## INPUT PARAMETERS

```
SERVICE_NAME  : <e.g., order-service>
BASE_PACKAGE  : <e.g., com.company.order>
PORT          : <e.g., 8081>
DESCRIPTION   : <one-line description>
```

---

## STACK

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x (latest stable) |
| Build | Maven |
| Database | H2 (local/test) → PostgreSQL (prod) |
| Migrations | Flyway (baseline only) |
| Security | Spring Security 6 — stateless, deny-by-default, JWT-ready |
| Quality | SpotBugs + find-sec-bugs, Spotless, OWASP Dependency-Check |
| Tracing | Micrometer Tracing + OpenTelemetry OTLP |
| Metrics | Micrometer + Prometheus |
| Logging | Logback + LogstashEncoder (structured JSON) |

---

## REQUIRED FILES

Output every file completely. No truncation. No placeholders.

```
{SERVICE_NAME}/
├── pom.xml
├── src/main/java/{pkg}/
│   ├── Application.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebMvcConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── TracingConfig.java
│   ├── filter/
│   │   ├── MdcLoggingFilter.java
│   │   └── RequestResponseLoggingFilter.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ErrorCodes.java
│   ├── health/
│   │   └── ApplicationHealthIndicator.java
│   └── metrics/
│       └── ApplicationMetrics.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── application-prod.yml
│   └── logback-spring.xml
├── src/test/java/{pkg}/
│   ├── ApplicationTests.java
│   ├── config/
│   │   ├── SecurityConfigTest.java
│   │   └── WebMvcConfigTest.java
│   ├── filter/
│   │   └── MdcLoggingFilterTest.java
│   └── health/
│       └── ApplicationHealthIndicatorTest.java
├── db/migration/
│   └── V1__baseline.sql
└── .gitignore
```

---

## KEY IMPLEMENTATION RULES

### pom.xml
- Java 21, Spring Boot 3.x parent
- Dependencies: web, security, actuator, data-jpa, h2, flyway, validation,
  springdoc-openapi, logstash-logback-encoder, micrometer-tracing-bridge-otel,
  opentelemetry-exporter-otlp, micrometer-registry-prometheus, lombok
- Plugins: spring-boot-maven-plugin, spotless (or checkstyle), spotbugs (find-sec-bugs),
  owasp-dependency-check (failBuildOnCVSS=7), surefire, failsafe

### SecurityConfig.java
```java
// Must use SecurityFilterChain — NOT WebSecurityConfigurerAdapter (removed in Spring 6)
// CSRF disabled with comment: "stateless JWT API — CSRF not applicable"
// Session: STATELESS
// Permits: /actuator/health, /actuator/info
// All else: authenticated()
```

### MdcLoggingFilter.java
```java
// OncePerRequestFilter
// Set MDC: requestId (from X-Request-ID or UUID), traceId, spanId, userId
// userId: SecurityContext principal or "anonymous"
// MUST call MDC.clear() in finally block
```

### application.yml (base)
```yaml
spring.application.name: ${SERVICE_NAME}
server.port: ${PORT:8080}
server.shutdown: graceful
server.error.include-message: never
server.error.include-stacktrace: never
management.endpoints.web.exposure.include: health,info,metrics,prometheus
management.endpoint.health.show-details: when-authorized
management.tracing.sampling.probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
```

### application-prod.yml
```yaml
# All values via env vars — no hardcoded secrets
spring.datasource.url: ${POSTGRES_URL}
spring.datasource.username: ${POSTGRES_USER}
spring.datasource.password: ${POSTGRES_PASSWORD}
management.tracing.sampling.probability: ${TRACING_SAMPLING_PROBABILITY:0.1}
```

### logback-spring.xml
```xml
<!-- Use net.logstash.logback.encoder.LogstashEncoder for all appenders -->
<!-- Include MDC fields: traceId, spanId, requestId, userId -->
<!-- Console appender for local/dev. File appender for prod. -->
```

### GlobalExceptionHandler.java
```java
// @RestControllerAdvice
// Use Spring 6 ProblemDetail (RFC7807) for all error responses
// No stack traces, no class names, no SQL errors in responses
// Handle: MethodArgumentNotValidException, generic Exception (500)
```

### Spring Profiles
| Profile | DB | Log Level | Actuator | OWASP |
|---|---|---|---|---|
| local | H2 | DEBUG | all | skip |
| dev | H2 or env var | INFO | limited | warn |
| test | H2 | WARN | none | skip |
| prod | PostgreSQL (env vars) | WARN | health+info | fail |

---

## CONSTRAINTS (ENFORCE ALL)

- NO domain entities, repositories, or service classes
- NO REST API endpoints except actuator
- NO hardcoded credentials — all via `${ENV_VAR:default}` syntax
- NO `WebSecurityConfigurerAdapter` — deprecated, removed in Spring Security 6
- NO `spring.datasource.initialization-mode` — removed in Spring Boot 3
- NO empty method bodies (except abstract/interface declarations)
- NO star imports (`import java.util.*`)
- NO `e.printStackTrace()` — use `log.error("message", e)`
- NO stack traces in HTTP responses
- NO "omitted for brevity" or "TODO: implement" in any file

---

## OUTPUT FORMAT

1. Print the complete file tree first.
2. Output each file as:
   ```
   ### {N}. {relative/path/to/file}
   ```language
   {complete file content}
   ```
   ```
3. After all files: print the verification checklist.

---

## VERIFICATION CHECKLIST

After generating, the user should run:

```bash
# Build and test
mvn clean verify -Plocal

# Start application
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Smoke tests (while app is running)
curl http://localhost:{PORT}/actuator/health
# → {"status":"UP"}

curl -o /dev/null -w "%{http_code}" http://localhost:{PORT}/api/test
# → 401

curl -o /dev/null -w "%{http_code}" http://localhost:{PORT}/actuator/prometheus
# → 401
```

**Completeness checks:**
- [ ] All files in tree are output
- [ ] `mvn clean verify` passes
- [ ] `/actuator/health` → 200 (no auth)
- [ ] All other endpoints → 401
- [ ] JSON log lines include `traceId`, `requestId` fields
- [ ] No file contains "omitted for brevity" or hardcoded secrets

---

## HOW TO USE

**Copilot Chat (@workspace):**
```
Paste this file content into Copilot Chat.
Fill in the INPUT PARAMETERS at the top.
Send. Copilot will generate the complete project file by file.
```

**Copilot Edits:**
```
Open Copilot Edits (Ctrl+Shift+I).
Paste this prompt into the instruction box.
Select "Generate" to produce all files into your workspace.
```

**GitHub Copilot Workspace:**
```
Create a new Copilot Workspace task.
Paste this file content as the task description.
Fill INPUT PARAMETERS. Run the task.
```

**Reusable:** Invoke with different INPUT PARAMETERS to generate a new
microservice base project. Each invocation is independent and idempotent.
