# Skill: Observability

**Layer:** 00-skill
**Type:** Observability contract
**Scope:** All Java Spring Boot microservice generation tasks

---

## Purpose

Defines mandatory observability requirements including structured logging, distributed tracing,
MDC context propagation, and metrics exposure. All generated microservices MUST implement
these requirements to be production-ready.

---

## Three Pillars of Observability

| Pillar | Implementation | Tool |
|--------|---------------|------|
| Logs | Structured JSON | Logback + Logstash Logback Encoder |
| Traces | Distributed tracing | Micrometer Tracing + OpenTelemetry |
| Metrics | Application metrics | Micrometer + Prometheus |

---

## Structured Logging

### Dependencies

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.x</version>
</dependency>
```

### Logback Configuration

`src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <springProfile name="local,dev,test">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
      </encoder>
    </appender>
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
  </springProfile>

  <springProfile name="prod">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
      </encoder>
    </appender>
    <root level="WARN">
      <appender-ref ref="CONSOLE"/>
    </root>
  </springProfile>
</configuration>
```

### JSON Log Output Example

```json
{
  "@timestamp": "2026-03-03T10:00:00.000Z",
  "@version": "1",
  "message": "Patient created successfully",
  "logger_name": "com.example.service.PatientService",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "requestId": "req-550e8400-e29b-41d4-a716",
  "userId": "user-123"
}
```

---

## MDC Context Propagation

### Mandatory MDC Fields

| Field | Value Source | Required In |
|-------|-------------|------------|
| `traceId` | OpenTelemetry span context | All requests |
| `spanId` | OpenTelemetry span context | All requests |
| `requestId` | `X-Request-ID` header or generated UUID | All requests |
| `userId` | JWT `sub` claim or `anonymous` | All authenticated requests |

### MDC Filter Implementation

`src/main/java/com/example/filter/MdcRequestFilter.java`:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
            .filter(h -> !h.isBlank())
            .orElse(UUID.randomUUID().toString());

        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // userId populated after authentication
        // traceId/spanId populated by Micrometer Tracing

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### User ID Propagation

After JWT authentication succeeds, populate MDC with user ID in a security filter or
`@EventListener` for `InteractiveAuthenticationSuccessEvent`:

```java
public void populateUserMdc(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()) {
        String userId = extractUserId(authentication);
        MDC.put("userId", userId);
    }
}
```

---

## Distributed Tracing

### Dependencies

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% in dev; set to 0.1 in prod

spring:
  application:
    name: patient-service

# OpenTelemetry exporter (configure per environment)
otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
  resource:
    attributes:
      service.name: ${spring.application.name}
      deployment.environment: ${SPRING_PROFILES_ACTIVE:local}
```

### Propagation

Micrometer Tracing automatically propagates `traceId` and `spanId` via:
- W3C `traceparent` header (preferred)
- B3 headers (if legacy compatibility required)

---

## Metrics

### Required Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, loggers
  endpoint:
    health:
      show-details: when-authorized
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### Custom Metrics Pattern

For business-level metrics, use Micrometer:

```java
@Service
public class PatientService {

    private final Counter patientCreatedCounter;
    private final Timer patientCreationTimer;

    public PatientService(MeterRegistry meterRegistry) {
        this.patientCreatedCounter = Counter.builder("patients.created.total")
            .description("Total number of patients created")
            .register(meterRegistry);

        this.patientCreationTimer = Timer.builder("patients.creation.duration")
            .description("Time taken to create a patient")
            .register(meterRegistry);
    }

    public PatientResponse createPatient(PatientRequest request) {
        return patientCreationTimer.record(() -> {
            PatientResponse response = doCreatePatient(request);
            patientCreatedCounter.increment();
            return response;
        });
    }
}
```

---

## Health Checks

### Custom Health Indicator Pattern

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            return Health.up()
                .withDetail("database", "responsive")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "unreachable")
                .withException(e)
                .build();
        }
    }
}
```

---

## Request/Response Logging

Log at INFO level for every request:
- HTTP method
- Request path (no query params containing sensitive data)
- Response status
- Duration in milliseconds
- Request ID

```java
@Aspect
@Component
public class HttpRequestLoggingAspect {
    // Or use a HandlerInterceptor — not both
}
```

Log at DEBUG level only:
- Request headers (exclude Authorization)
- Request body (exclude password fields)
- Response body

---

## Correlation ID Propagation

When calling downstream services:

```java
@Bean
public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add((request, body, execution) -> {
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            request.getHeaders().add("X-Request-ID", requestId);
        }
        return execution.execute(request, body);
    });
    return restTemplate;
}
```

---

## Log Level Guidelines

| Level | Use Case |
|-------|---------|
| `ERROR` | Unrecoverable failures, exceptions that affect correctness |
| `WARN` | Recoverable issues, degraded behavior, security events |
| `INFO` | Business events (entity created/updated/deleted), lifecycle events |
| `DEBUG` | Diagnostic data, request/response detail, query parameters |
| `TRACE` | Extremely verbose internal state (development only) |

---

## Definition of Done

- [ ] Logback configured with JSON output via Logstash Logback Encoder
- [ ] MDC filter implemented and registered at `HIGHEST_PRECEDENCE`
- [ ] All four MDC fields populated: traceId, spanId, requestId, userId
- [ ] Micrometer Tracing + OTel bridge configured
- [ ] Prometheus endpoint exposed at `/actuator/prometheus`
- [ ] Health endpoint configured with `when-authorized` detail
- [ ] Custom business metric (counter/timer) for at least one key operation
- [ ] Request/response logging at appropriate levels
- [ ] Correlation ID forwarded to downstream calls
- [ ] Sampling rate configured per profile (1.0 dev, 0.1 prod)

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| MDC cleared before response is written | Always clear in `finally` block |
| TraceId missing from logs | Verify Micrometer bridge on classpath |
| Sensitive data in DEBUG logs | Audit log statements before merge |
| Actuator endpoints open in prod | Require `ROLE_ACTUATOR` for management endpoints |
| Missing `requestId` in response header | Set response header in MDC filter |
