# Spec: Microservice Base Architecture

**Layer:** 10-specs
**Version:** 1.0.0
**Date:** 2026-03-03
**Status:** Approved

---

## Scope

This specification defines the mandatory base architecture for all Java Spring Boot microservices
in this organization. It covers the technology stack, project structure, security baseline,
observability requirements, and quality gates that must be present before any business
functionality is implemented.

This spec is the foundation. All feature specifications build on top of this.

---

## Non-Goals

- Does NOT define business domain logic or API contracts.
- Does NOT define database schema (that is task of domain specs).
- Does NOT define deployment infrastructure (Kubernetes, Docker, etc.).
- Does NOT define service mesh or gateway configuration.
- Does NOT define CI/CD pipeline implementation.

---

## Assumptions

| ID | Assumption |
|----|-----------|
| A-01 | Developers use macOS or Linux. Windows is not a supported development environment. |
| A-02 | Java 21 is available on the developer machine and CI/CD runners. |
| A-03 | Maven 3.9+ is available. |
| A-04 | Docker is available for local infrastructure (optional; H2 used by default). |
| A-05 | An OpenTelemetry collector will be available in `dev` and `prod` environments. |
| A-06 | JWT-based authentication is the chosen mechanism. OAuth2/OIDC provider is external. |
| A-07 | PostgreSQL is the production database target. |

---

## Functional Requirements

### FR-01 – Application Bootstrap

| ID | Requirement |
|----|------------|
| FR-01-1 | The application MUST start with a single `Application.java` annotated with `@SpringBootApplication`. |
| FR-01-2 | The application MUST NOT use component scanning beyond the base package. |
| FR-01-3 | The application MUST declare the application name in `application.yml` under `spring.application.name`. |
| FR-01-4 | The application MUST support graceful shutdown via `server.shutdown=graceful`. |

### FR-02 – Spring Profiles

| ID | Requirement |
|----|------------|
| FR-02-1 | Four profiles MUST be defined: `local`, `dev`, `test`, `prod`. |
| FR-02-2 | Profile activation MUST use `SPRING_PROFILES_ACTIVE` environment variable. |
| FR-02-3 | Each profile MUST have a corresponding `application-{profile}.yml` file. |
| FR-02-4 | `local` profile MUST use H2 in-memory database. |
| FR-02-5 | `dev` and `prod` profiles MUST use PostgreSQL with externalized credentials. |
| FR-02-6 | `test` profile MUST use H2 in-memory database with `create-drop` DDL mode. |

### FR-03 – Database and Migrations

| ID | Requirement |
|----|------------|
| FR-03-1 | Flyway MUST manage all database schema changes. |
| FR-03-2 | Migration scripts MUST follow the naming convention: `V{version}__{description}.sql`. |
| FR-03-3 | The baseline migration MUST create all initial tables. |
| FR-03-4 | No DDL auto-creation (`spring.jpa.hibernate.ddl-auto=validate` in prod). |

### FR-04 – Security Configuration

| ID | Requirement |
|----|------------|
| FR-04-1 | All HTTP endpoints MUST require authentication by default. |
| FR-04-2 | A `SecurityConfig.java` MUST explicitly configure `SecurityFilterChain`. |
| FR-04-3 | Session management MUST be `STATELESS`. |
| FR-04-4 | CSRF protection MUST be disabled for stateless REST. |
| FR-04-5 | A JWT authentication filter MUST be implemented and registered. |
| FR-04-6 | Actuator endpoints MUST require `ROLE_ACTUATOR` authority. |
| FR-04-7 | `/actuator/health` and `/actuator/info` MAY be open for infrastructure probes. |

### FR-05 – Actuator

| ID | Requirement |
|----|------------|
| FR-05-1 | Actuator MUST expose: `health`, `info`, `metrics`, `prometheus`. |
| FR-05-2 | Health details MUST only show to authorized users (`when-authorized`). |
| FR-05-3 | Actuator port MAY be separated from application port via `management.server.port`. |

### FR-06 – Logging

| ID | Requirement |
|----|------------|
| FR-06-1 | All log output MUST be structured JSON via Logstash Logback Encoder. |
| FR-06-2 | Every log statement MUST include: `traceId`, `spanId`, `requestId`, `userId` via MDC. |
| FR-06-3 | An `MdcRequestFilter` MUST populate MDC at request entry and clear on exit. |
| FR-06-4 | `X-Request-ID` header MUST be read from request or generated as UUID. |
| FR-06-5 | The `X-Request-ID` MUST be echoed in the response header. |

### FR-07 – Code Quality Gates

| ID | Requirement |
|----|------------|
| FR-07-1 | SpotBugs MUST run in `verify` phase and fail on HIGH or above. |
| FR-07-2 | Code formatting (Spotless or Checkstyle) MUST run in `verify` phase. |
| FR-07-3 | OWASP Dependency-Check MUST run in `verify` phase and fail on CVSS >= 7. |
| FR-07-4 | All quality plugins MUST be bound to Maven lifecycle phases, not manual execution. |

---

## Non-Functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-01 | Application startup time | < 10 seconds on developer machine |
| NFR-02 | Health endpoint response time | < 100ms |
| NFR-03 | `mvn clean verify` completion time | < 5 minutes (excluding OWASP NVD download) |
| NFR-04 | Test coverage | > 80% line coverage (service layer) |
| NFR-05 | Memory footprint | < 512MB RSS in idle state |
| NFR-06 | Java version compliance | Compiled to Java 21 bytecode only |

---

## Security Requirements

| ID | Requirement |
|----|------------|
| SEC-01 | No credentials in any committed file. Use environment variables. |
| SEC-02 | JWT validation: signature, expiry, issuer, audience. |
| SEC-03 | All inputs validated with Bean Validation (`@Valid`). |
| SEC-04 | Error responses must not expose stack traces or internal paths. |
| SEC-05 | Security headers: X-Frame-Options, X-XSS-Protection, HSTS, CSP. |
| SEC-06 | CORS configured explicitly. No wildcard `*` in prod. |
| SEC-07 | SpotBugs with find-sec-bugs plugin enabled. |
| SEC-08 | OWASP Dependency-Check: fail on HIGH/CRITICAL CVEs. |

---

## Observability Requirements

| ID | Requirement |
|----|------------|
| OBS-01 | Micrometer Tracing with OpenTelemetry bridge on classpath. |
| OBS-02 | MDC fields populated for every request: traceId, spanId, requestId, userId. |
| OBS-03 | Prometheus metrics endpoint exposed and accessible. |
| OBS-04 | At least one custom business metric registered in `MeterRegistry`. |
| OBS-05 | Log sampling: 100% in local/dev/test, configurable in prod. |
| OBS-06 | OTLP exporter endpoint configured via environment variable. |

---

## Acceptance Criteria

| ID | Criterion | Verification |
|----|----------|-------------|
| AC-01 | Application starts on `local` profile | `mvn spring-boot:run` succeeds |
| AC-02 | `/actuator/health` returns 200 | `curl http://localhost:8080/actuator/health` |
| AC-03 | Unauthenticated request to `/api/**` returns 401 | `curl http://localhost:8080/api/any` |
| AC-04 | `mvn clean verify` passes | Zero test failures, SpotBugs, OWASP clean |
| AC-05 | Logs are valid JSON | `mvn spring-boot:run | jq '.'` |
| AC-06 | MDC fields present in logs | Check log output for traceId, requestId |
| AC-07 | JWT filter rejects invalid token | 401 with problem+json body |

---

## Definition of Done

- [ ] Maven project created with Java 21 compiler config
- [ ] Spring Boot 3.x parent POM
- [ ] All four Spring Profiles configured with correct datasource
- [ ] Flyway migration baseline script exists
- [ ] `SecurityConfig.java` — deny-by-default, stateless, JWT filter
- [ ] `MdcRequestFilter.java` — MDC populated and cleared
- [ ] `logback-spring.xml` — JSON output with MDC fields
- [ ] Actuator configured with secured endpoints
- [ ] SpotBugs configured and bound to verify
- [ ] Spotless/Checkstyle configured and bound to verify
- [ ] OWASP Dependency-Check configured (fail on CVSS 7+)
- [ ] `mvn clean verify` passes with zero violations
- [ ] README includes: prerequisites, setup, run, and verification commands

---

## Validation Checklist

```
□ java.version = 21 in pom.xml properties
□ Spring Boot parent version declared
□ application.yml has spring.application.name
□ application-local.yml uses H2
□ application-prod.yml uses PostgreSQL with ${env.var}
□ SecurityConfig.java exists in config/ package
□ SessionCreationPolicy.STATELESS set
□ CSRF disabled
□ All endpoints denied by default
□ JWT filter registered in filter chain
□ MdcRequestFilter extends OncePerRequestFilter
□ MDC.clear() in finally block
□ logback-spring.xml uses LogstashEncoder
□ management.endpoints.web.exposure.include declared
□ SpotBugs plugin in pom.xml with failOnError=true
□ OWASP plugin in pom.xml with failBuildOnCVSS=7
□ mvn clean verify exits 0
□ curl /actuator/health returns 200
□ curl /api/any (no token) returns 401
```
