# GitHub Copilot Agent Prompt — Patient CRUD Functional Implementation

<!--
  SOURCE FILE : prompts/21-prompts-copilot/copilot-agent-patient-crud.md
  REUSABLE    : YES — invoke on any base project scaffold
  SCOPE       : Patient CRUD functional layer ONLY
                (OpenAPI contract → code gen → domain → service → controller → tests)
  PREREQUISITE: Base project scaffold must already exist and boot successfully.
                Run copilot-agent-generate-base-project.md first if not done.
  SKILLS USED : 00-skill/skill-global-output-contract.md
                00-skill/skill-contract-first-openapi.md
                00-skill/skill-testing-java.md
                00-skill/skill-security-owasp.md
                00-skill/skill-observability.md
  SPEC USED   : 10-specs/spec-user-story-patient-crud.md
  TEMPLATES   : 30-templates/template-openapi-style-guide.md
                30-templates/template-problem-json-errors.md
  INVOCATION  : Paste into Copilot Chat (@workspace), Copilot Edits, or
                GitHub Copilot Workspace. Fill INPUT PARAMETERS first.
-->

---

## TASK

Implement the complete Patient CRUD functional layer on top of an existing Spring Boot
base project. **Contract-first: OpenAPI YAML must be the first file output.**

**Scope:** Domain layer only — OpenAPI spec, JPA entity, repository, service, controller,
mapper, exception handlers, security update, Flyway migration, and full tests.

**Not in scope:** Base infrastructure (Application.java, SecurityConfig base, filters,
logback, Actuator) — these already exist in the base project.

---

## PREREQUISITE

Base project must already have:
- `Application.java`, `SecurityConfig.java`, `MdcLoggingFilter.java`
- `GlobalExceptionHandler.java` (will be extended, not replaced)
- `application.yml`, `logback-spring.xml`
- `db/migration/V1__baseline.sql`

If missing, run `copilot-agent-generate-base-project.md` first.

---

## INPUT PARAMETERS

```
BASE_PACKAGE  : <e.g., com.company.patient>
API_DOMAIN    : <e.g., api.company.com>
PORT          : <e.g., 8080>
SERVICE_NAME  : <e.g., patient-service>
```

---

## CONTRACT-FIRST ORDER OF OPERATIONS

```
1. src/main/resources/openapi/api.yaml        ← FIRST. No Java before this.
2. pom.xml update                             ← Add openapi-generator-maven-plugin
3. db/migration/V2__create_patients_table.sql ← Flyway migration
4. domain/  enums + Patient entity            ← JPA layer
5. repository/ exception/ mapper/ service/   ← Business layer
6. controller/ + update GlobalExceptionHandler + SecurityConfig ← API layer
7. test/  unit + integration + repository     ← Test layer
```

Violating this order invalidates the output. Output Step 1 completely before any Java.

---

## REQUIRED FILES

Output every file completely. No truncation. No placeholders.

```
src/main/resources/openapi/
└── api.yaml                                   ← Step 1 (FIRST output)

pom.xml                                        ← Step 2 additions only

src/main/resources/db/migration/
└── V2__create_patients_table.sql              ← Step 3

src/main/java/{pkg}/
├── domain/
│   ├── PatientStatus.java                     ← enum: ACTIVE, INACTIVE
│   ├── Gender.java                            ← enum: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
│   └── Patient.java                           ← @Entity with @Version
├── repository/
│   └── PatientRepository.java                 ← JpaRepository + custom queries
├── exception/
│   ├── PatientNotFoundException.java
│   └── DuplicateEmailException.java
├── mapper/
│   └── PatientMapper.java                     ← entity ↔ generated DTO
├── service/
│   └── PatientService.java                    ← all 6 CRUD ops + search + metrics
├── controller/
│   └── PatientController.java                 ← implements generated PatientsApi
├── exception/
│   └── GlobalExceptionHandler.java            ← UPDATED (extends base handlers)
└── config/
    └── SecurityConfig.java                    ← UPDATED (adds patient endpoint rules)

src/test/java/{pkg}/
├── TestDataFactory.java                       ← shared builder methods
├── service/
│   └── PatientServiceTest.java               ← @ExtendWith(MockitoExtension.class)
├── repository/
│   └── PatientRepositoryTest.java            ← @DataJpaTest
└── controller/
    └── PatientControllerIT.java              ← @SpringBootTest + @AutoConfigureMockMvc
```

---

## api.yaml SPECIFICATION

File: `src/main/resources/openapi/api.yaml`

**Header:**
```yaml
openapi: "3.1.0"
info:
  title: "Patient Service API"
  version: "1.0.0"
  description: |
    Patient CRUD API. All endpoints require Bearer JWT.
    Errors follow RFC 7807 (application/problem+json).
    List endpoints return paginated results.
  contact:
    name: "Platform Team"
    email: "platform@example.com"
servers:
  - url: "http://localhost:{PORT}"
    description: "Local"
tags:
  - name: "patients"
    description: "Patient management operations"
security:
  - bearerAuth: []
```

**7 Operations:**

| operationId | Method | Path | Success | Error codes |
|---|---|---|---|---|
| `createPatient` | POST | `/patients` | 201 + Location header | 400, 401, 403, 409 |
| `listPatients` | GET | `/patients` | 200 paged | 400, 401, 403 |
| `getPatientById` | GET | `/patients/{patientId}` | 200 | 401, 403, 404 |
| `updatePatient` | PUT | `/patients/{patientId}` | 200 | 400, 401, 403, 404, 409 |
| `patchPatient` | PATCH | `/patients/{patientId}` | 200 | 400, 401, 403, 404 |
| `deletePatient` | DELETE | `/patients/{patientId}` | 204 | 401, 403, 404 |
| `searchPatients` | GET | `/patients/search` | 200 paged | 400, 401, 403 |

**Schemas (all with type, description, example on every property):**

- `PatientRequest` — required: firstName, lastName, email, dateOfBirth, status
- `PatchPatientRequest` — all fields optional (JSON Merge Patch RFC 7396)
- `PatientResponse` — all 12 entity fields; id and timestamps readOnly
- `PagedPatientResponse` — content: PatientResponse[], page, size, totalElements, totalPages, last
- `ProblemDetail` — type(uri), title, status, detail, instance, timestamp, errors[]
- `FieldError` — field, message, rejectedValue

**Reusable components:**
```yaml
components:
  parameters:
    PatientIdParam: {in: path, name: patientId, required: true, schema: {type: string, format: uuid}}
    PageParam:      {in: query, name: page,  schema: {type: integer, default: 0, minimum: 0}}
    SizeParam:      {in: query, name: size,  schema: {type: integer, default: 20, minimum: 1, maximum: 100}}
    SortParam:      {in: query, name: sort,  schema: {type: string, example: "lastName,asc"}}
  responses:
    BadRequest:          {description: "400 validation error",     content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
    Unauthorized:        {description: "401 unauthorized",          content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
    Forbidden:           {description: "403 forbidden",             content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
    NotFound:            {description: "404 not found",             content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
    Conflict:            {description: "409 conflict",              content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
    InternalServerError: {description: "500 internal server error", content: {application/problem+json: {schema: {$ref: ProblemDetail}}}}
  securitySchemes:
    bearerAuth: {type: http, scheme: bearer, bearerFormat: JWT}
```

---

## pom.xml ADDITIONS (Step 2)

Add to `<build><plugins>`:

```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <version>7.4.0</version>
  <executions>
    <execution>
      <id>generate-patient-api</id>
      <goals><goal>generate</goal></goals>
      <configuration>
        <inputSpec>${project.basedir}/src/main/resources/openapi/api.yaml</inputSpec>
        <generatorName>spring</generatorName>
        <apiPackage>{BASE_PACKAGE}.api.generated</apiPackage>
        <modelPackage>{BASE_PACKAGE}.dto.generated</modelPackage>
        <configOptions>
          <interfaceOnly>true</interfaceOnly>
          <useSpringBoot3>true</useSpringBoot3>
          <useTags>true</useTags>
          <dateLibrary>java8</dateLibrary>
          <openApiNullable>false</openApiNullable>
          <useBeanValidation>true</useBeanValidation>
          <skipDefaultInterface>true</skipDefaultInterface>
        </configOptions>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

## IMPLEMENTATION RULES

### V2__create_patients_table.sql
```sql
-- Must include: id UUID PK, email UNIQUE, version BIGINT NOT NULL,
--               created_at / updated_at TIMESTAMPTZ NOT NULL,
--               gender/status as VARCHAR with CHECK constraints,
--               index on email, index on (last_name, first_name)
```

### Patient.java (JPA entity)
- `@Entity @Table(name = "patients")`
- `id`: `@Id @GeneratedValue(strategy = UUID)`, type `UUID`
- `email`: `@Column(unique = true, nullable = false)`
- `version`: `@Version`, type `Long`
- `createdAt`: `@CreationTimestamp`, `@Column(updatable = false)`
- `updatedAt`: `@UpdateTimestamp`
- `gender`, `status`: `@Enumerated(EnumType.STRING)`
- Lombok `@Builder @NoArgsConstructor @AllArgsConstructor @Getter @Setter`
- **No @Data** (avoid equals/hashCode issues with JPA)

### PatientRepository.java
```java
// Extends JpaRepository<Patient, UUID>
Optional<Patient> findByEmail(String email);
boolean existsByEmail(String email);

// Custom search query (case-insensitive contains):
@Query("""
    SELECT p FROM Patient p WHERE
    (:firstName IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
    AND (:lastName IS NULL OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
    AND (:email IS NULL OR LOWER(p.email) LIKE LOWER(CONCAT('%', :email, '%')))
    AND (:status IS NULL OR p.status = :status)
    """)
Page<Patient> searchPatients(
    @Param("firstName") String firstName,
    @Param("lastName") String lastName,
    @Param("email") String email,
    @Param("status") PatientStatus status,
    Pageable pageable
);
```

### PatientService.java
- Constructor injection of: `PatientRepository`, `PatientMapper`, `MeterRegistry`
- Every method wrapped with `Timer.Sample`
- `createPatient`: check email exists → throw `DuplicateEmailException` → save → increment counter → return DTO
- `getPatientById`: find or throw `PatientNotFoundException`
- `listPatients`: `repository.findAll(pageable)` → map page
- `updatePatient`: find or 404 → check email collision (excluding self) → save
- `patchPatient`: find or 404 → apply only non-null fields → save
- `deletePatient`: find or 404 → delete → increment `patients.deleted.total`
- `searchPatients`: delegate to repository custom query
- **maskEmail helper:** `email.charAt(0) + "***@" + email.split("@")[1]`
- Log: INFO on create/update/delete with `patientId`, WARN on duplicate with masked email
- **DOB must NOT appear in any log statement**

### PatientController.java
```java
@RestController
@RequiredArgsConstructor
public class PatientController implements PatientsApi {
    private final PatientService patientService;

    @Override
    public ResponseEntity<PatientResponse> createPatient(PatientRequest request) {
        PatientResponse created = patientService.createPatient(request);
        URI location = URI.create("/patients/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }
    // All 7 interface methods must be @Override-implemented
    // No business logic — delegate 100% to patientService
}
```

### PatientMapper.java
```java
// Manual mapper (or MapStruct if available)
// toResponse(Patient) → PatientResponse (all 12 fields)
// toEntity(PatientRequest) → Patient (exclude id, createdAt, updatedAt, version)
// updateEntity(Patient, PatientRequest) → void (mutates entity for PUT)
// patchEntity(Patient, PatchPatientRequest) → void (only non-null fields for PATCH)
// Null-safe: return null if input is null (no NPE)
```

### GlobalExceptionHandler.java (add to existing base handlers)
```java
// Add these handlers (do NOT remove existing handlers from base project):
@ExceptionHandler(PatientNotFoundException.class)      → 404 patient-not-found
@ExceptionHandler(DuplicateEmailException.class)       → 409 email-conflict (mask email)
@ExceptionHandler(ObjectOptimisticLockingFailureException.class) → 409 optimistic-lock
@ExceptionHandler(MethodArgumentTypeMismatchException.class)     → 400 validation-error
// Problem type URI: https://{API_DOMAIN}/problems/{slug}
// Email in 409 detail: maskEmail() helper
```

### SecurityConfig.java (update existing base config)
```java
// Add before .anyRequest().authenticated():
.requestMatchers(HttpMethod.DELETE, "/patients/**").hasRole("ADMIN")
.requestMatchers("/patients/**").hasRole("USER")
```

---

## CONSTRAINTS (ENFORCE ALL)

| Rule | Detail |
|---|---|
| OpenAPI YAML first | No Java before `api.yaml` is complete |
| interfaceOnly=true | Generated code is read-only; never modify `target/` |
| Controller implements interface | `PatientController implements PatientsApi` |
| No entity in response | `Patient` entity never returned from controller |
| No DTO as entity | Generated DTOs never annotated with `@Entity` |
| UUID IDs | Patient IDs are UUID, not Long |
| RFC7807 errors | All error responses: `application/problem+json` |
| Mask email in logs/errors | `j***@example.com` format |
| No DOB in logs | Privacy violation |
| @Version on entity | Required for optimistic locking |
| UNIQUE on email column | Both DB constraint and application check |
| ROLE_ADMIN for DELETE | ROLE_USER for all other `/patients/**` operations |
| Complete file output | No truncation, no "omitted for brevity" |
| No star imports | Explicit imports only |
| No `e.printStackTrace()` | Use `log.error("...", e)` |

---

## TEST REQUIREMENTS

### PatientServiceTest.java (`@ExtendWith(MockitoExtension.class)`)
Naming: `method_whenCondition_shouldBehavior`

Required test methods:
1. `createPatient_whenValidRequest_shouldReturnPatientResponse`
2. `createPatient_whenEmailAlreadyExists_shouldThrowDuplicateEmailException`
3. `getPatientById_whenExists_shouldReturnPatientResponse`
4. `getPatientById_whenNotFound_shouldThrowPatientNotFoundException`
5. `listPatients_whenCalled_shouldReturnPagedResult`
6. `updatePatient_whenValidRequest_shouldReturnUpdatedResponse`
7. `updatePatient_whenNotFound_shouldThrowPatientNotFoundException`
8. `updatePatient_whenEmailTakenByOther_shouldThrowDuplicateEmailException`
9. `patchPatient_whenPartialFields_shouldUpdateOnlyProvidedFields`
10. `deletePatient_whenExists_shouldCallRepositoryDelete`
11. `deletePatient_whenNotFound_shouldThrowPatientNotFoundException`
12. `searchPatients_withFilters_shouldReturnFilteredPage`

### PatientControllerIT.java (`@SpringBootTest + @AutoConfigureMockMvc + @Transactional`)
Must cover all 12 acceptance criteria (AC-01 to AC-12):
- AC-01 to AC-03: POST happy path, duplicate email 409, invalid email 400
- AC-04 to AC-06: GET list paged, GET by ID, GET unknown 404
- AC-07 to AC-09: PUT update, PATCH partial, DELETE 204
- AC-10: Search with filters
- AC-11: No auth → 401
- AC-12: ROLE_USER DELETE → 403

Auth: `.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))` or `ROLE_ADMIN`
Error assertions: verify `$.status`, `$.title`, `$.detail`, `$.instance`, `$.timestamp`
Error content type: `application/problem+json`

### PatientRepositoryTest.java (`@DataJpaTest`)
- `findByEmail_whenExists_shouldReturnPatient`
- `findByEmail_whenNotFound_shouldReturnEmpty`
- `existsByEmail_whenExists_shouldReturnTrue`
- `searchPatients_byFirstName_shouldReturnMatches`

### TestDataFactory.java (shared, package-private)
```java
static PatientRequest buildValidPatientRequest() { ... }      // all required fields
static PatientRequest buildPatientRequestWithInvalidEmail() { ... }
static Patient buildPatient() { ... }                         // saved entity
static Patient buildPatient(String email) { ... }             // with specific email
```

---

## VERIFICATION CHECKLIST

```bash
# Generate sources from OpenAPI spec
mvn generate-sources

# Full build with all tests
mvn clean verify -Plocal

# Start and smoke test
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Create patient → 201
curl -s -X POST http://localhost:{PORT}/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com",
       "dateOfBirth":"1990-01-15","status":"ACTIVE"}' | jq .

# Duplicate email → 409 problem+json
curl -s -X POST http://localhost:{PORT}/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"john@example.com",
       "dateOfBirth":"1985-06-20","status":"ACTIVE"}' | jq .

# Unauthenticated → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:{PORT}/patients

# Metrics
curl -s http://localhost:{PORT}/actuator/prometheus | grep patients
```

**Completeness checks:**
- [ ] All files output completely
- [ ] `mvn clean verify` passes
- [ ] OpenAPI YAML generates valid sources (`mvn generate-sources`)
- [ ] All 12 acceptance criteria have integration tests
- [ ] `PatientController implements PatientsApi` (not hand-written)
- [ ] `Patient` entity never returned from controller
- [ ] 409 email response contains masked email (`j***@example.com`)
- [ ] `@Version` on `Patient.java`
- [ ] `@Column(unique=true)` on email in `Patient.java`
- [ ] DELETE requires ROLE_ADMIN; other ops require ROLE_USER
- [ ] `patients.created.total` metric visible in Prometheus

---

## HOW TO USE

**Copilot Chat (@workspace):**
```
Paste this prompt into Copilot Chat.
Fill INPUT PARAMETERS. Copilot will generate all files in order.
```

**Copilot Edits (Ctrl+Shift+I):**
```
Paste into instruction box → Generate.
Files appear in workspace in contract-first order.
```

**GitHub Copilot Workspace:**
```
New task → paste this prompt → fill INPUT PARAMETERS → Run.
```

**Reusable:** Works on any base project with the scaffold in place.
Same INPUT PARAMETERS always produce the same output (idempotent).
