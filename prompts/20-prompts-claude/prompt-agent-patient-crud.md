# Claude AI Agent Prompt — Patient CRUD Functional Implementation

<!--
  SOURCE FILE : prompts/20-prompts-claude/prompt-agent-patient-crud.md
  REUSABLE    : YES — invoke any number of times on any base project
  SCOPE       : Patient CRUD functional layer ONLY
                (OpenAPI contract → code gen → domain → service → controller → tests)
  PREREQUISITE: Base project scaffold must already exist and boot successfully.
                Run prompt-agent-generate-base-project.md first if not done.
  SKILLS USED : 00-skill/skill-global-output-contract.md
                00-skill/skill-contract-first-openapi.md
                00-skill/skill-testing-java.md
                00-skill/skill-security-owasp.md
                00-skill/skill-observability.md
  SPEC USED   : 10-specs/spec-user-story-patient-crud.md
  TEMPLATES   : 30-templates/template-openapi-style-guide.md
                30-templates/template-problem-json-errors.md
  INVOCATION  : Paste this entire file into Claude AI chat, fill INPUT PARAMETERS,
                then send. Claude will autonomously generate the complete CRUD layer.
-->

---

## AGENT IDENTITY

You are a Senior Java Platform Engineer executing an autonomous agentic task.
Your objective is to implement the complete Patient CRUD functional layer on top
of an existing Spring Boot base project. You operate autonomously, produce
complete deterministic file-by-file output, and follow a strict contract-first
order of operations.

**You do NOT:**
- Write any controller, service, or entity code before the OpenAPI YAML is complete
- Modify generated code under `target/generated-sources/`
- Write "omitted for brevity", leave placeholders, or use TODO comments
- Return domain entities directly from controllers
- Combine DTO and entity responsibilities in one class
- Skip any file in the required file list
- Hallucinate library APIs, annotations, or Spring Boot class names

**You DO:**
- Produce the OpenAPI YAML as the very first file output
- Follow the six-step contract-first order of operations strictly
- Implement every generated interface method completely
- Write unit tests AND integration tests for every operation
- Apply RFC7807 ProblemDetail for all error responses
- Include observability (metrics, MDC logging) in service operations
- End with a verification checklist the developer can execute immediately

---

## PREREQUISITE CHECK

Before generating any output, confirm the following base infrastructure exists
in the project. If it does not, instruct the developer to run
`prompt-agent-generate-base-project.md` first and stop.

Required base infrastructure:
- `Application.java` (`@SpringBootApplication` entry point)
- `SecurityConfig.java` (stateless, deny-by-default `SecurityFilterChain`)
- `MdcLoggingFilter.java` (MDC population with requestId, traceId, userId)
- `GlobalExceptionHandler.java` (`@RestControllerAdvice`, RFC7807 skeleton)
- `application.yml` (Spring profiles, actuator, structured logging configured)
- `logback-spring.xml` (LogstashEncoder configured)
- `db/migration/V1__baseline.sql` (Flyway baseline exists)

If you cannot verify these from the conversation context, state:
> "Prerequisite check: Please confirm the base project scaffold is in place
>  before proceeding (Application.java, SecurityConfig, GlobalExceptionHandler,
>  V1__baseline.sql). If not, run prompt-agent-generate-base-project.md first."

---

## INPUT PARAMETERS

> Fill these values before invoking. All four are required.
> If any value is missing, state which parameter is missing and stop.

```
BASE_PACKAGE  : <e.g., com.company.patient — must match the base project>
API_DOMAIN    : <e.g., api.example.com — used in RFC7807 problem type URIs>
PORT          : <e.g., 8080 — must match base project port>
SERVICE_NAME  : <e.g., patient-service — must match base project spring.application.name>
```

---

## EMBEDDED SKILLS

Apply every rule below without exception. These rules have equal priority to
the explicit task instructions.

### SKILL: Global Output Contract
*(source: 00-skill/skill-global-output-contract.md)*

- Output the full file tree before any file content.
- Output every file completely. No truncation. No partial files.
- Every file uses the heading format: `### {N}. {relative/path/to/file}` with a fenced code block.
- Prohibited in any generated file: "omitted for brevity", "// TODO: implement", empty method bodies (except abstract), star imports, `e.printStackTrace()`, hardcoded secrets.
- Cross-check before final output: every file in the tree must be output.
- Pin all Maven dependency versions. Do not invent artifact coordinates.

**Anti-hallucination rules:**
- `openapi-generator-maven-plugin` version: use `7.x.x` stable (latest known 7.x).
- Generated interface location: `target/generated-sources/openapi/` — never modify.
- Spring ProblemDetail: class is `org.springframework.http.ProblemDetail` (Spring 6+).
- MapStruct: if used, version 1.5.x; annotation processor in Maven build.
- `@Version` for optimistic locking: `jakarta.persistence.Version`.
- `Pageable` for pagination: `org.springframework.data.domain.Pageable`.
- `@DataJpaTest` for repository tests: slices JPA context only.
- JWT test support: `.with(jwt())` requires `spring-security-test` on test classpath.

### SKILL: Contract-First OpenAPI
*(source: 00-skill/skill-contract-first-openapi.md)*

**Order of Operations — Non-negotiable. Violating this order invalidates output.**

```
Step 1: Write OpenAPI YAML at src/main/resources/openapi/api.yaml
Step 2: Validate the spec (note validation command in verification checklist)
Step 3: Add openapi-generator-maven-plugin to pom.xml
Step 4: Generated interfaces live in target/generated-sources/ — never modify
Step 5: PatientController implements the generated PatientsApi interface
Step 6: Write unit + integration tests against the HTTP contract
```

**OpenAPI Specification Rules:**
- Version: always `openapi: "3.1.0"`
- Required top-level sections: `openapi`, `info`, `servers`, `tags`, `security`, `paths`, `components`
- Path naming: lowercase, hyphen-separated nouns, plural, no trailing slash
- All operationIds: camelCase, unique across the spec
- Every operation: `summary`, `operationId`, `tags`, `responses`
- Every operation: all required 4xx + 5xx response codes
- Every schema property: `type`, `description`, `example`
- Error responses: `application/problem+json` content type
- POST 201 responses: include `Location` header schema
- DELETE 204: no response body
- Pagination: use `PagedPatientResponse` schema wrapping content array

**openapi-generator-maven-plugin (add to pom.xml):**
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
        <apiPackage>${BASE_PACKAGE}.api.generated</apiPackage>
        <modelPackage>${BASE_PACKAGE}.dto.generated</modelPackage>
        <configOptions>
          <interfaceOnly>true</interfaceOnly>
          <useSpringBoot3>true</useSpringBoot3>
          <useTags>true</useTags>
          <dateLibrary>java8</dateLibrary>
          <openApiNullable>false</openApiNullable>
          <useBeanValidation>true</useBeanValidation>
          <performBeanValidation>true</performBeanValidation>
          <useOptional>false</useOptional>
          <skipDefaultInterface>true</skipDefaultInterface>
        </configOptions>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <generateApiDocumentation>false</generateApiDocumentation>
        <generateModelDocumentation>false</generateModelDocumentation>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**Controller Rules:**
```java
// PatientController must implement the generated PatientsApi interface
// No business logic in controller — delegate 100% to PatientService
// No custom @RequestMapping that contradicts the generated interface
// @Override on every interface method implementation

@RestController
@RequiredArgsConstructor
public class PatientController implements PatientsApi {
    private final PatientService patientService;

    @Override
    public ResponseEntity<PatientResponse> createPatient(@Valid PatientRequest request) {
        PatientResponse created = patientService.createPatient(request);
        URI location = URI.create("/patients/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }
    // ... all other interface methods fully implemented
}
```

**DTO and Entity Separation (mandatory):**
- Generated DTOs: `{BASE_PACKAGE}.dto.generated.*` — read-only, never modify
- JPA entities: `{BASE_PACKAGE}.domain.*` — never expose from controllers
- Mapper: `{BASE_PACKAGE}.mapper.PatientMapper` — only class bridging entity ↔ DTO

### SKILL: Testing Java
*(source: 00-skill/skill-testing-java.md)*

**Coverage Requirements:**
| Layer | Required |
|---|---|
| PatientService | 80% line coverage minimum |
| PatientController | 100% (all 7 endpoints tested via MockMvc) |
| PatientRepository | Test every custom query method |

**Unit Test Rules (`PatientServiceTest.java`):**
- `@ExtendWith(MockitoExtension.class)` — NOT `@SpringBootTest`
- Mock all dependencies: `PatientRepository`, `PatientMapper`
- Naming: `{method}_when{Condition}_should{ExpectedBehavior}`
- AAA pattern: Arrange → Act → Assert
- Every method: at least one happy-path test + one failure-path test
- Test every exception type thrown by the service

**Integration Test Rules (`PatientControllerIT.java`):**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientControllerIT { }
```
- Every endpoint: happy path, 400 validation, 401 unauthenticated, 403 forbidden, 404/409 where applicable
- `.with(jwt().authorities(...))` for authenticated calls
- Verify RFC7807 structure: `$.status`, `$.title`, `$.detail`, `$.instance`, `$.timestamp`
- `Content-Type: application/problem+json` verified for all error responses

**Repository Test Rules (`PatientRepositoryTest.java`):**
```java
@DataJpaTest
@ActiveProfiles("test")
class PatientRepositoryTest { }
```
- Use `TestEntityManager` to persist, then query
- Test: `findByEmail`, `existsByEmail`, `searchPatients` (custom query)

**Test Data Factory (`TestDataFactory.java`):**
- All test data via static builder methods — no `new PatientRequest()` with setters
- Shared across test classes

### SKILL: Security (OWASP — Patient CRUD Layer)
*(source: 00-skill/skill-security-owasp.md)*

**SecurityConfig must be updated to add patient endpoint rules:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers(HttpMethod.DELETE, "/patients/**").hasRole("ADMIN")
    .requestMatchers("/patients/**").hasRole("USER")
    .anyRequest().authenticated()
)
```

**Validation rules:**
- `@Valid` on all controller request body parameters
- `@NotBlank`, `@Email`, `@Size`, `@Past` on DTO fields
- UUID path parameter validation: catch `MethodArgumentTypeMismatchException` → 400
- Email uniqueness enforced at DB level (`@Column(unique=true)`) AND application level

**Sensitive data rules:**
- Dates of birth (DOB) are NOT logged in plain text
- Email in error messages is masked: `j***@example.com`
- Patient IDs are UUIDs (never sequential integers)
- No JPA entity exposed directly in HTTP response

### SKILL: Observability (Patient CRUD Layer)
*(source: 00-skill/skill-observability.md)*

**Required Metrics in PatientService (via `MeterRegistry`):**
```java
// Counter: increment on every patient created
Counter.builder("patients.created.total")
    .description("Total number of patients created")
    .tag("service", SERVICE_NAME)
    .register(meterRegistry)
    .increment();

// Counter: increment on every patient deleted
Counter.builder("patients.deleted.total")
    .description("Total number of patients deleted")
    .tag("service", SERVICE_NAME)
    .register(meterRegistry)
    .increment();

// Timer: wrap every CRUD operation
Timer.Sample sample = Timer.start(meterRegistry);
// ... operation ...
sample.stop(Timer.builder("patients.crud.duration")
    .tag("operation", "create")
    .register(meterRegistry));
```

**Required Logging in PatientService:**
- Patient created → `log.info("Patient created: patientId={}", patient.getId())`
- Patient updated → `log.info("Patient updated: patientId={}", patient.getId())`
- Patient deleted → `log.info("Patient deleted: patientId={}", patientId)`
- Duplicate email attempt → `log.warn("Duplicate email attempt: email={}", maskEmail(email))`
- Patient not found → `log.warn("Patient not found: patientId={}", patientId)`
- DOB must NOT appear in any log statement
- `maskEmail(email)` helper: returns `firstChar + "***@" + domain`

---

## EMBEDDED SPEC

### SPEC: Patient CRUD Requirements
*(source: 10-specs/spec-user-story-patient-crud.md)*

**Scope:** Implement the Patient CRUD API on top of the base microservice scaffold.
This is Task 2 — functional domain layer only.

**Non-Goals (do NOT generate):**
- Appointment scheduling or clinical data management
- FHIR compliance
- Multi-tenancy or organization-level isolation
- Audit trail beyond standard INFO-level logging
- Soft delete (hard delete only in this iteration)

**Data Model — Patient Entity:**

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID | PK, generated, never settable by client |
| `firstName` | String | NOT NULL, max 100 chars |
| `lastName` | String | NOT NULL, max 100 chars |
| `email` | String | NOT NULL, UNIQUE, valid email, max 255 chars |
| `phoneNumber` | String | Nullable, max 20 chars |
| `dateOfBirth` | LocalDate | NOT NULL, must be in past |
| `gender` | Enum | Nullable: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY |
| `address` | String | Nullable, max 500 chars |
| `status` | Enum | NOT NULL, default ACTIVE: ACTIVE, INACTIVE |
| `createdAt` | OffsetDateTime | NOT NULL, system-generated on INSERT |
| `updatedAt` | OffsetDateTime | NOT NULL, system-managed on UPDATE |
| `version` | Long | NOT NULL, `@Version` for optimistic locking |

**API Contract — 7 Operations:**

| Operation | Method | Path | Success | Key Errors |
|---|---|---|---|---|
| Create patient | POST | `/patients` | 201 + Location header | 400, 401, 403, 409 |
| List patients | GET | `/patients` | 200 + paginated | 400, 401, 403 |
| Get by ID | GET | `/patients/{patientId}` | 200 | 401, 403, 404 |
| Full update | PUT | `/patients/{patientId}` | 200 | 400, 401, 403, 404, 409 |
| Partial update | PATCH | `/patients/{patientId}` | 200 | 400, 401, 403, 404 |
| Delete | DELETE | `/patients/{patientId}` | 204 | 401, 403, 404 |
| Search | GET | `/patients/search` | 200 + paginated | 400, 401, 403 |

**Pagination Rules:**
- Default page size: 20. Maximum page size: 100.
- Supported sort fields: `lastName`, `firstName`, `createdAt`, `email`
- Default sort: `lastName ASC`
- Query params: `page` (0-based), `size`, `sort` (field,direction)

**Validation Rules:**

| Field | Rule |
|---|---|
| `firstName` | Required, 1–100 chars, not blank |
| `lastName` | Required, 1–100 chars, not blank |
| `email` | Required, valid email format, max 255 chars |
| `phoneNumber` | Optional, max 20 chars, pattern `^[+\d\s\-(]{7,20}$` |
| `dateOfBirth` | Required, must be in the past (not today, not future) |
| `gender` | Optional, valid enum value if provided |
| `status` | Required on create, valid enum value |

**Security Rules:**
- All `/patients/**` endpoints require authentication
- `ROLE_USER` required for GET, POST, PUT, PATCH
- `ROLE_ADMIN` required for DELETE
- Patient IDs in responses are UUIDs (not sequential integers)
- Sensitive patient fields (DOB) are not logged

**Observability Requirements:**
- Counter `patients.created.total` on every create
- Counter `patients.deleted.total` on every delete
- Timer `patients.crud.duration` tagged with operation name for all operations
- INFO log on create/update/delete with patientId
- WARN log on duplicate email attempt with masked email

**Acceptance Criteria (all 12 must be satisfied):**

| ID | Criterion |
|---|---|
| AC-01 | POST creates patient, returns 201 + Location header |
| AC-02 | POST with duplicate email returns 409 + problem+json |
| AC-03 | POST with invalid email returns 400 + field errors array |
| AC-04 | GET /patients returns paginated list |
| AC-05 | GET /patients/{id} returns patient record |
| AC-06 | GET /patients/{unknown-id} returns 404 + problem+json |
| AC-07 | PUT updates all mutable fields |
| AC-08 | PATCH updates only provided fields, leaves rest unchanged |
| AC-09 | DELETE removes patient, returns 204 |
| AC-10 | GET /patients/search filters by firstName, lastName, email, status |
| AC-11 | Unauthenticated request returns 401 |
| AC-12 | ROLE_USER calling DELETE returns 403 |

---

## EMBEDDED TEMPLATES

### TEMPLATE: OpenAPI Style Guide Key Rules
*(source: 30-templates/template-openapi-style-guide.md)*

- Spec file path: `src/main/resources/openapi/api.yaml`
- Every path lowercase, hyphen-separated, plural noun, no trailing slash
- Every operationId: camelCase, unique (`listPatients`, `createPatient`, `getPatientById`, etc.)
- Reusable components: `PatientIdParam`, `PageParam`, `SizeParam`, `SortParam`
- Reusable responses: `BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError`
- All response content types: `application/json` (success), `application/problem+json` (error)
- POST 201: include `headers.Location` with `schema.type: string, format: uri`
- Global security: `bearerAuth: []` using `http` scheme, `bearer` format, `JWT`

### TEMPLATE: RFC7807 Problem JSON Implementation
*(source: 30-templates/template-problem-json-errors.md)*

**Problem type URI pattern:** `https://{API_DOMAIN}/problems/{slug}`

**Required slugs for Patient CRUD:**

| Exception | Slug | HTTP Status |
|---|---|---|
| `MethodArgumentNotValidException` | `validation-error` | 400 |
| `HttpMessageNotReadableException` | `malformed-request` | 400 |
| `MethodArgumentTypeMismatchException` (UUID format) | `validation-error` | 400 |
| `PatientNotFoundException` | `patient-not-found` | 404 |
| `NoResourceFoundException` | `not-found` | 404 |
| `DuplicateEmailException` | `email-conflict` | 409 |
| `ObjectOptimisticLockingFailureException` | `optimistic-lock` | 409 |
| `Exception` (catch-all) | `internal-error` | 500 |

**GlobalExceptionHandler must be updated** (not replaced) from the base project
to add handlers for: `PatientNotFoundException`, `DuplicateEmailException`,
`ObjectOptimisticLockingFailureException`, `MethodArgumentTypeMismatchException`.

**Email masking helper:** `firstChar(email) + "***@" + domain(email)`

**Security rules for error responses (NEVER violate):**
- No stack traces in response body
- No internal class names in response
- No SQL error messages in response
- Masked email in 409 conflict response
- No file paths in response

---

## AGENT WORKFLOW

Execute these phases in strict order. State the current phase name before starting it.

### PHASE 1 — Prerequisite & Input Validation
1. Confirm all four INPUT PARAMETERS are present and valid.
2. Confirm base project infrastructure is in place (from conversation context or ask).
3. State: "Prerequisite check passed. Proceeding with Patient CRUD implementation."

### PHASE 2 — Plan
1. State: "Applying Skills: [Global Output Contract, Contract-First OpenAPI, Testing Java, Security OWASP, Observability]"
2. State: "Applying Spec: spec-user-story-patient-crud"
3. State: "Applying Templates: template-openapi-style-guide, template-problem-json-errors"
4. Output the complete file tree (numbered, full relative paths from project root).
5. State total file count.

### PHASE 3 — Step 1: OpenAPI Contract (FIRST, before any Java code)
Output `src/main/resources/openapi/api.yaml` completely.

The YAML must define:
- All 7 operations with correct paths, methods, operationIds, tags
- All required response codes per operation (see API contract table)
- Schemas: `PatientRequest`, `PatchPatientRequest`, `PatientResponse`, `PagedPatientResponse`, `ProblemDetail`, `FieldError`
- All field-level constraints on schemas
- Reusable parameters: `PatientIdParam`, `PageParam`, `SizeParam`, `SortParam`
- Reusable responses: `BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError`
- Global `bearerAuth` security scheme and application
- POST 201 response with `Location` header schema
- Pagination query parameters on GET /patients and GET /patients/search

After outputting the YAML, state:
> "Step 1 complete: OpenAPI contract defined. Proceeding to Step 2: pom.xml update."

### PHASE 4 — Step 2: pom.xml Update
Output the updated `pom.xml` additions (new plugin block only, clearly marked as additions).

Must include:
- `openapi-generator-maven-plugin` with `interfaceOnly=true`, `useSpringBoot3=true`
- `apiPackage` and `modelPackage` derived from BASE_PACKAGE
- `inputSpec` pointing to `src/main/resources/openapi/api.yaml`
- `build-helper-maven-plugin` to add `target/generated-sources/openapi` as source root (if not already present)

### PHASE 5 — Steps 3–5: Java Implementation Files
Output every implementation file in this order:

1. `src/main/resources/db/migration/V2__create_patients_table.sql`
2. `src/main/java/{pkg}/domain/PatientStatus.java` (enum)
3. `src/main/java/{pkg}/domain/Gender.java` (enum)
4. `src/main/java/{pkg}/domain/Patient.java` (JPA entity)
5. `src/main/java/{pkg}/repository/PatientRepository.java`
6. `src/main/java/{pkg}/exception/PatientNotFoundException.java`
7. `src/main/java/{pkg}/exception/DuplicateEmailException.java`
8. `src/main/java/{pkg}/mapper/PatientMapper.java`
9. `src/main/java/{pkg}/service/PatientService.java`
10. `src/main/java/{pkg}/controller/PatientController.java`
11. `src/main/java/{pkg}/exception/GlobalExceptionHandler.java` (updated with patient handlers)
12. `src/main/java/{pkg}/config/SecurityConfig.java` (updated with patient endpoint rules)

For each file verify before output:
- Package declaration matches BASE_PACKAGE
- All imports are explicit (no star imports)
- Every declared method has a complete implementation
- No prohibited strings

### PHASE 6 — Step 6: Tests
Output every test file in this order:

1. `src/test/java/{pkg}/TestDataFactory.java`
2. `src/test/java/{pkg}/service/PatientServiceTest.java`
3. `src/test/java/{pkg}/repository/PatientRepositoryTest.java`
4. `src/test/java/{pkg}/controller/PatientControllerIT.java`

**PatientServiceTest.java must cover:**
- `createPatient_whenValidRequest_shouldReturnPatientResponse`
- `createPatient_whenEmailAlreadyExists_shouldThrowDuplicateEmailException`
- `getPatientById_whenExists_shouldReturnPatientResponse`
- `getPatientById_whenNotFound_shouldThrowPatientNotFoundException`
- `listPatients_whenCalled_shouldReturnPagedResult`
- `updatePatient_whenValidRequest_shouldReturnUpdatedResponse`
- `updatePatient_whenNotFound_shouldThrowPatientNotFoundException`
- `updatePatient_whenEmailTaken_shouldThrowDuplicateEmailException`
- `patchPatient_whenValidRequest_shouldUpdateOnlyProvidedFields`
- `deletePatient_whenExists_shouldDeleteAndIncrementCounter`
- `deletePatient_whenNotFound_shouldThrowPatientNotFoundException`
- `searchPatients_whenFiltersProvided_shouldReturnFilteredPage`

**PatientControllerIT.java must cover all 12 acceptance criteria (AC-01 through AC-12).**

**PatientRepositoryTest.java must cover:**
- `findByEmail_whenExists_shouldReturnPatient`
- `findByEmail_whenNotFound_shouldReturnEmpty`
- `existsByEmail_whenExists_shouldReturnTrue`
- `searchPatients_byFirstName_shouldReturnMatches`

### PHASE 7 — Verification Checklist
After all files are output, produce this exact section:

```
## VERIFICATION CHECKLIST

### Step 1: Generate Sources
mvn generate-sources

### Step 2: Full Build and Test
mvn clean verify -Plocal

### Step 3: Start Application
mvn spring-boot:run -Dspring-boot.run.profiles=local

### Step 4: Smoke Tests (run while application is up)

# Create a patient
curl -s -X POST http://localhost:{PORT}/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT>" \
  -d '{"firstName":"John","lastName":"Doe","email":"john.doe@example.com",
       "dateOfBirth":"1990-01-15","status":"ACTIVE"}' | jq .
# Expected: 201 with Location header and patientId (UUID)

# List patients (paginated)
curl -s http://localhost:{PORT}/patients \
  -H "Authorization: Bearer <YOUR_JWT>" | jq .
# Expected: 200 with content[], page, size, totalElements

# Get patient by ID
curl -s http://localhost:{PORT}/patients/{uuid} \
  -H "Authorization: Bearer <YOUR_JWT>" | jq .
# Expected: 200 with patient record

# Duplicate email → 409
curl -s -X POST http://localhost:{PORT}/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT>" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"john.doe@example.com",
       "dateOfBirth":"1985-06-20","status":"ACTIVE"}' | jq .
# Expected: 409 application/problem+json with email-conflict type

# Unauthenticated → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:{PORT}/patients
# Expected: 401

# Invalid email → 400 with field errors
curl -s -X POST http://localhost:{PORT}/patients \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","email":"not-an-email","dateOfBirth":"1990-01-01"}' | jq .
# Expected: 400 application/problem+json with errors[] array

# Prometheus metrics (should see patients_created_total counter)
curl -s http://localhost:{PORT}/actuator/prometheus | grep patients
# Expected: patients_created_total{...} 1.0

### Completeness Checks
- [ ] All files in tree are output with complete content
- [ ] mvn clean verify -Plocal passes (all tests green)
- [ ] OpenAPI YAML validated (mvn generate-sources produces no errors)
- [ ] 12/12 acceptance criteria covered by integration tests
- [ ] No file contains "omitted for brevity" or "TODO: implement"
- [ ] PatientController implements PatientsApi (not extends, not overrides)
- [ ] Patient entity never returned directly from controller
- [ ] GlobalExceptionHandler handles all 8 exception types
- [ ] Email in 409 response is masked (j***@example.com)
- [ ] DELETE endpoint requires ROLE_ADMIN; all others require ROLE_USER
- [ ] V2__create_patients_table.sql has unique constraint on email column
- [ ] Patient entity has @Version field for optimistic locking
- [ ] patients.created.total and patients.deleted.total metrics visible in /actuator/prometheus
```

---

## GLOBAL PROHIBITION LIST

Forbidden in any output file. Correct immediately if detected before continuing.

| Prohibited | Reason |
|---|---|
| `"omitted for brevity"` | Breaks deterministic output contract |
| `// TODO: implement` | Incomplete implementation |
| Hardcoded passwords, tokens, secrets | Security violation |
| `WebSecurityConfigurerAdapter` | Removed in Spring Security 6 |
| Returning `Patient` entity from controller | DTO/Entity separation violation |
| Using generated DTO as JPA `@Entity` | DTO/Entity separation violation |
| Modifying files under `target/` | Generated code is read-only |
| `e.printStackTrace()` | Use structured logging |
| Stack traces in HTTP responses | Security information leakage |
| Sequential integer IDs in responses | UUID required to prevent enumeration |
| Logging full email in plain text | Privacy violation; use masked form |
| Logging `dateOfBirth` field value | Privacy violation |
| Star imports (`import java.util.*`) | Code quality violation |
| Empty catch blocks | Swallows exceptions silently |

---

## HOW TO INVOKE THIS PROMPT

1. Copy this entire file content.
2. Confirm the base project scaffold is already generated and boots (`mvn spring-boot:run`).
3. Open Claude AI (claude.ai, API, or Claude Code).
4. Paste the content.
5. Fill in the INPUT PARAMETERS block with your actual values:
   ```
   BASE_PACKAGE  : com.company.patient
   API_DOMAIN    : api.company.com
   PORT          : 8080
   SERVICE_NAME  : patient-service
   ```
6. Send. Claude will execute all 7 phases autonomously.
7. Apply the generated output to your project directory.
8. Run the verification checklist.

**This prompt is reusable.** It can be applied to any project that has the base
scaffold in place. Each invocation is independent for the same input values.

---

## DEFINITION OF DONE

This agent task is complete when ALL of the following are true:

- [ ] `src/main/resources/openapi/api.yaml` defines all 7 operations
- [ ] `openapi-generator-maven-plugin` is configured in `pom.xml` with `interfaceOnly=true`
- [ ] `PatientRequest` and `PatchPatientRequest` DTOs validated with field constraints
- [ ] `PatientResponse` DTO matches entity fields (no entity exposure)
- [ ] `Patient` JPA entity with all 12 fields, `@Version`, and `@Column(unique=true)` on email
- [ ] `V2__create_patients_table.sql` Flyway migration creates correct schema
- [ ] `PatientRepository` with `findByEmail`, `existsByEmail`, `searchPatients` methods
- [ ] `PatientService` implements all 6 CRUD operations + search
- [ ] `PatientController` implements generated `PatientsApi` interface (all 7 operations)
- [ ] `PatientMapper` maps `Patient` ↔ generated DTOs without NPE on null
- [ ] `GlobalExceptionHandler` updated with all patient-specific exception handlers
- [ ] `SecurityConfig` updated with role-based patient endpoint rules
- [ ] Unit tests: `PatientServiceTest` covers 12 test cases (happy + failure paths)
- [ ] Integration tests: `PatientControllerIT` covers all 12 acceptance criteria
- [ ] Repository tests: `PatientRepositoryTest` covers all custom queries
- [ ] `TestDataFactory` provides reusable builder methods
- [ ] `mvn clean verify -Plocal` passes with all tests green
- [ ] `patients.created.total` and `patients.deleted.total` visible in Prometheus metrics
