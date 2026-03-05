# Skill: Java Spring Microservice

**Layer:** 00-skill
**Type:** Technology stack contract
**Scope:** All Java Spring Boot microservice generation tasks

---

## Purpose

Defines the mandatory technology stack, conventions, and structural rules for all
generated Java Spring Boot microservices. Reference this skill in every microservice prompt.

---

## Technology Stack (Mandatory)

| Component | Version / Specification |
|-----------|------------------------|
| Java | 21 (LTS) — use virtual threads where applicable |
| Spring Boot | 3.x (latest 3.x release) |
| Build Tool | Maven (not Gradle) |
| Database (dev) | H2 in-memory |
| Database (prod) | PostgreSQL (schema must be portable) |
| ORM | Spring Data JPA + Hibernate |
| Migration | Flyway |
| Security | Spring Security 6.x (stateless, JWT-ready) |
| Code Quality | SpotBugs, Checkstyle or Spotless |
| Dependency Security | OWASP Dependency-Check Maven Plugin |
| Tracing | Micrometer Tracing + OpenTelemetry |
| Logging | Logback + Logstash Logback Encoder (JSON) |
| Actuator | Spring Boot Actuator (secured) |

---

## Maven POM Rules

### Parent POM

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.x</version>
  <relativePath/>
</parent>
```

### Required Properties

```xml
<properties>
  <java.version>21</java.version>
  <maven.compiler.source>21</maven.compiler.source>
  <maven.compiler.target>21</maven.compiler.target>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Required Dependencies

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-security

<!-- Database -->
h2 (scope: runtime, profile: local/test)
postgresql (scope: runtime, profile: dev/prod)
flyway-core

<!-- Observability -->
micrometer-tracing-bridge-otel
opentelemetry-exporter-otlp
logstash-logback-encoder

<!-- Test -->
spring-boot-starter-test (scope: test)
spring-security-test (scope: test)
```

### Required Maven Plugins

```xml
<!-- Compiler -->
maven-compiler-plugin (Java 21)

<!-- Test -->
maven-surefire-plugin

<!-- Integration Test -->
maven-failsafe-plugin

<!-- Code Quality -->
spotbugs-maven-plugin
maven-checkstyle-plugin OR spotless-maven-plugin

<!-- Security -->
dependency-check-maven (failBuildOnCVSS: 7)

<!-- Code Generation -->
openapi-generator-maven-plugin (when applicable)
```

---

## Package Structure (Mandatory)

```
com.{company}.{service}/
├── Application.java                    ← @SpringBootApplication
├── config/                             ← Spring configuration classes
│   ├── SecurityConfig.java
│   ├── JpaConfig.java
│   └── OpenApiConfig.java
├── api/                                ← OpenAPI-generated interfaces (DO NOT modify)
│   └── generated/
├── controller/                         ← Controller implementations only
├── service/                            ← Business logic
│   └── impl/
├── repository/                         ← Spring Data repositories
├── domain/                             ← JPA entities
├── dto/                                ← Request/Response DTOs
├── mapper/                             ← MapStruct or manual mappers
├── exception/                          ← Custom exceptions + global handler
├── filter/                             ← Servlet filters (MDC, request ID)
└── util/                               ← Pure utility classes (no Spring beans)
```

---

## Spring Profiles

| Profile | Purpose | Database | Log Level |
|---------|---------|----------|-----------|
| `local` | Local development | H2 | DEBUG |
| `dev` | Shared dev environment | PostgreSQL | DEBUG |
| `test` | CI/automated tests | H2 | INFO |
| `prod` | Production | PostgreSQL | WARN |

Profile activation via `SPRING_PROFILES_ACTIVE` environment variable.
Never hardcode `local` as default in production images.

Application configuration files:
```
src/main/resources/
├── application.yml          ← shared config
├── application-local.yml    ← local overrides
├── application-dev.yml      ← dev overrides
├── application-test.yml     ← test overrides
└── application-prod.yml     ← prod overrides
```

---

## Security Baseline Rules

- All endpoints are DENIED by default.
- Actuator endpoints require `ROLE_ACTUATOR` or restrict to internal network.
- No credentials in `application.yml` — use environment variables.
- CSRF disabled for stateless REST APIs.
- CORS configured explicitly (no wildcard `*` in production).
- JWT filter must be stateless — no session creation.

---

## Logging Rules

### MDC Fields (Mandatory)

Every request must populate these MDC fields:

| Field | Source |
|-------|--------|
| `traceId` | OpenTelemetry propagation |
| `spanId` | OpenTelemetry propagation |
| `requestId` | `X-Request-ID` header or UUID generated |
| `userId` | Extracted from JWT or anonymous |

### Log Format

Structured JSON via Logstash Logback Encoder. Example output:
```json
{
  "timestamp": "2026-03-03T10:00:00.000Z",
  "level": "INFO",
  "logger": "com.example.service.PatientService",
  "message": "Patient created",
  "traceId": "abc123",
  "spanId": "def456",
  "requestId": "req-789",
  "userId": "user-001"
}
```

### Logging Prohibitions

- Do NOT log passwords, tokens, or PII fields.
- Do NOT log raw request bodies unless in DEBUG and explicitly filtered for sensitive fields.
- Do NOT log full stack traces to user-facing responses.

---

## Actuator Security

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
```

Actuator endpoints must require authentication in `dev` and `prod` profiles.

---

## Virtual Threads (Java 21)

Enable virtual threads for I/O-bound workloads:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

---

## Code Quality Rules

### SpotBugs

- Fail build on `HIGH` and above.
- Effort: `max`.
- Exclude test classes from SpotBugs via exclusion filter.

### Checkstyle / Spotless

- Google Java Style or project-defined style.
- Fail build on any style violation.
- Format check runs in `verify` phase.

---

## Definition of Done

- [ ] Java 21 configured in compiler plugin
- [ ] Spring Boot 3.x parent POM present
- [ ] All required dependencies declared
- [ ] Package structure matches specification
- [ ] All four Spring Profiles configured
- [ ] MDC filter implemented and registered
- [ ] Structured JSON logging enabled
- [ ] Spring Security baseline applied
- [ ] Actuator secured
- [ ] SpotBugs configured (fail on HIGH)
- [ ] OWASP Dependency-Check configured (fail on CVSS 7+)
- [ ] `mvn clean verify` passes
