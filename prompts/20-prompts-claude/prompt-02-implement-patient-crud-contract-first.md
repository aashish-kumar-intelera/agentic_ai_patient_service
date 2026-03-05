# Claude Prompt 02: Implement Patient CRUD – Contract-First

**Layer:** 20-prompts-claude
**Task:** TASK 2 – Contract-First Patient CRUD
**AI Target:** Claude AI (claude-sonnet-4-6 or claude-opus-4-6)
**Skills:** skill-global-output-contract, skill-contract-first-openapi, skill-testing-java, skill-security-owasp
**Prerequisite:** Prompt 01 (Base Architecture) must be completed first.

---

## How to Use This Prompt

1. Complete Prompt 01 first — this prompt assumes the base project exists.
2. Copy everything in the "PROMPT START" section below.
3. Paste as your first message in a new Claude conversation.
4. After Claude generates output, run `mvn clean verify`.

---

## PROMPT START

---

You are a Senior Enterprise Java Architect implementing a contract-first Patient CRUD API.

## Context

You are adding Patient CRUD functionality to an existing Spring Boot 3.x microservice named
`patient-service` with base package `com.intellera.patientservice`.

The base project structure already exists (from a previous task). You are adding ONLY
the Patient domain components on top of it.

## ABSOLUTE RULES

1. The OpenAPI YAML specification MUST be generated FIRST before any Java code.
2. ALL Java code for controllers MUST implement the generated API interface.
3. No handwritten controller `@RequestMapping` annotations that override the contract.
4. Every file must be output completely. Zero truncation.
5. "Omitted for brevity" and all equivalents are FORBIDDEN.
6. No placeholder values in any file.
7. Output the file tree first, then each file in order.

---

## Task: Contract-First Patient CRUD

Implement full CRUD for the `Patient` resource using the contract-first approach.

---

## Step 1: OpenAPI Specification (GENERATE FIRST)

Generate `src/main/resources/openapi/patient-api.yaml` with:

### API Metadata
- `openapi: "3.1.0"`
- `info.title: "Patient Service API"`
- `info.version: "1.0.0"`
- `info.description`: Include purpose, auth method, content type
- `servers`: localhost:8080 with description "Local development"
- `tags`: `patients` tag with description

### Security Scheme
```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
security:
  - bearerAuth: []
```

### Paths to Define

#### POST /patients
- Accepts: `application/json` with `PatientRequest` body
- Returns: 201 `PatientResponse`, 400 `ProblemDetail`, 401 `ProblemDetail`, 403 `ProblemDetail`, 409 `ProblemDetail`
- 201 response MUST include `Location` header schema

#### GET /patients
- Query params: `page` (default 0), `size` (default 20, max 100), `sort` (default "lastName,asc"), `status` (optional)
- Returns: 200 `PagedPatientResponse`, 400, 401, 403

#### GET /patients/{patientId}
- Path param: `patientId` (UUID format, required)
- Returns: 200 `PatientResponse`, 400, 401, 403, 404

#### PUT /patients/{patientId}
- Path param: `patientId` (UUID)
- Body: `PatientRequest`
- Returns: 200 `PatientResponse`, 400, 401, 403, 404, 409

#### PATCH /patients/{patientId}
- Path param: `patientId` (UUID)
- Body: `PatchPatientRequest`
- Returns: 200 `PatientResponse`, 400, 401, 403, 404

#### DELETE /patients/{patientId}
- Path param: `patientId` (UUID)
- Returns: 204, 401, 403, 404
- Requires `ROLE_ADMIN` (document in description)

#### GET /patients/search
- Query params: `firstName`, `lastName`, `email`, `status`, `page`, `size`, `sort`
- All search params optional
- Returns: 200 `PagedPatientResponse`, 400, 401, 403

### Schemas to Define

#### PatientRequest
Required fields: `firstName`, `lastName`, `email`, `dateOfBirth`
Optional: `phoneNumber`, `gender`, `address`, `status`
All fields must have: `description`, `example`, validation constraints in schema

#### PatchPatientRequest
All fields optional (for JSON Merge Patch)
Same fields as `PatientRequest` but all nullable

#### PatientResponse
Include all `PatientRequest` fields PLUS: `id` (UUID), `status`, `createdAt`, `updatedAt`, `version`

#### PagedPatientResponse
Properties: `content` (array of PatientResponse), `page`, `size`, `totalElements`, `totalPages`, `last`

#### ProblemDetail
Properties: `type`, `title`, `status`, `detail`, `instance`, `timestamp`
Include optional `errors` array of `FieldError` (for validation failures)

#### FieldError
Properties: `field`, `message`, `rejectedValue`

#### Gender enum
Values: `MALE`, `FEMALE`, `OTHER`, `PREFER_NOT_TO_SAY`

#### PatientStatus enum
Values: `ACTIVE`, `INACTIVE`

---

## Step 2: Maven Plugin Configuration (Update pom.xml)

Add `openapi-generator-maven-plugin` configuration:

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
        <inputSpec>${project.basedir}/src/main/resources/openapi/patient-api.yaml</inputSpec>
        <generatorName>spring</generatorName>
        <apiPackage>com.intellera.patientservice.api.generated</apiPackage>
        <modelPackage>com.intellera.patientservice.dto.generated</modelPackage>
        <configOptions>
          <interfaceOnly>true</interfaceOnly>
          <useSpringBoot3>true</useSpringBoot3>
          <useTags>true</useTags>
          <dateLibrary>java8</dateLibrary>
          <openApiNullable>false</openApiNullable>
          <useBeanValidation>true</useBeanValidation>
          <performBeanValidation>true</performBeanValidation>
          <skipDefaultInterface>true</skipDefaultInterface>
          <useOptional>false</useOptional>
        </configOptions>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <generateApiDocumentation>false</generateApiDocumentation>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Also add `build-helper-maven-plugin` to add generated sources to compile path.

---

## Step 3: Java Implementation Files

Generate exactly these files (all complete, no truncation):

### Patient Entity: `domain/Patient.java`
- `@Entity @Table(name = "patients")`
- Fields: `id` (UUID, `@GeneratedValue`), `firstName`, `lastName`, `email` (`@Column(unique=true)`), `phoneNumber`, `dateOfBirth` (LocalDate), `gender` (enum, `@Enumerated`), `address`, `status` (enum, default ACTIVE)
- Auditing: `createdAt`, `updatedAt` with `@CreatedDate`, `@LastModifiedDate`
- `version` field with `@Version` for optimistic locking
- Proper JPA annotations on all fields
- `equals()` and `hashCode()` based on `id`

### Enums: `domain/Gender.java` and `domain/PatientStatus.java`
- Full enum classes with all values

### Repository: `repository/PatientRepository.java`
- Extends `JpaRepository<Patient, UUID>`
- `Optional<Patient> findByEmail(String email)`
- `boolean existsByEmail(String email)`
- `boolean existsByEmailAndIdNot(String email, UUID id)` (for update validation)
- `Page<Patient> findByStatus(PatientStatus status, Pageable pageable)`
- Custom search query using `@Query` with JPQL:
  - Accepts optional firstName (ILIKE), lastName (ILIKE), email (ILIKE), status
  - Use `LOWER(COALESCE(:param, ''))` pattern or Spring Data JPA Specification

Prefer `JpaSpecificationExecutor<Patient>` for the search query:
- Add `PatientSpecification.java` with static methods for each filter criterion

### Mapper: `mapper/PatientMapper.java`
- Manual mapper (no MapStruct — keep dependencies minimal)
- `Patient toEntity(PatientRequest request)` — creates new entity
- `void updateEntity(PatientRequest request, Patient entity)` — updates existing entity
- `void patchEntity(PatchPatientRequest request, Patient entity)` — only updates non-null fields
- `PatientResponse toResponse(Patient entity)`
- `Page<PatientResponse> toPagedResponse(Page<Patient> page)` — returns mapped page

### Custom DTOs: `dto/PatchPatientRequest.java`
(if not fully captured by generated code — create a manual DTO for patch semantics)

### Exception Classes:
- `exception/PatientNotFoundException.java` — extends `RuntimeException`
- `exception/DuplicateEmailException.java` — extends `RuntimeException`
- `exception/PatientUpdateConflictException.java` — extends `RuntimeException` (for optimistic lock)

### Update GlobalExceptionHandler.java (add handlers for new exceptions):
- `PatientNotFoundException` → 404 `ProblemDetail`
- `DuplicateEmailException` → 409 `ProblemDetail`
- `PatientUpdateConflictException` → 409 `ProblemDetail`
- `ObjectOptimisticLockingFailureException` → 409 `ProblemDetail`

### Service: `service/PatientService.java` (interface) and `service/impl/PatientServiceImpl.java`

Interface methods:
```java
PatientResponse createPatient(PatientRequest request);
Page<PatientResponse> getAllPatients(Pageable pageable, PatientStatus status);
PatientResponse getPatientById(UUID id);
PatientResponse updatePatient(UUID id, PatientRequest request);
PatientResponse patchPatient(UUID id, PatchPatientRequest request);
void deletePatient(UUID id);
Page<PatientResponse> searchPatients(String firstName, String lastName, String email, PatientStatus status, Pageable pageable);
```

Implementation must:
- Check for duplicate email on create (throw `DuplicateEmailException` if exists)
- Check for duplicate email on update (throw if exists for different patient)
- Throw `PatientNotFoundException` if patient not found
- Use `patientMapper` for all entity↔DTO conversions
- Log INFO for: patient created, updated, deleted (include patientId, mask email)
- Increment Micrometer counters: `patients.created.total`, `patients.deleted.total`
- Register timer `patients.operation.duration` with `operation` tag

### Controller: `controller/PatientController.java`
- MUST implement `PatientsApi` (generated interface)
- Delegate all logic to `PatientService`
- NO business logic in controller
- Return `ResponseEntity` with appropriate status codes
- POST: include `Location` header = `URI.create("/patients/" + response.getId())`

### Flyway Migration: `V2__create_patients_table.sql`
Create the `patients` table with:
- All columns matching the entity
- UUID primary key
- Unique constraint on `email`
- Indexes: email, last_name, status
- `created_at`, `updated_at` timestamp columns
- `version` bigint column (not null, default 0)

---

## Step 4: Test Files

### Unit Test: `test/.../service/PatientServiceTest.java`
Using `@ExtendWith(MockitoExtension.class)`.

Must test ALL of the following (AAA pattern for each):
- `createPatient` — success
- `createPatient` — email already exists → `DuplicateEmailException`
- `createPatient` — repository save fails → exception propagates
- `getPatientById` — found
- `getPatientById` — not found → `PatientNotFoundException`
- `updatePatient` — success
- `updatePatient` — patient not found → `PatientNotFoundException`
- `updatePatient` — email conflict → `DuplicateEmailException`
- `patchPatient` — success, only provided fields updated
- `deletePatient` — success
- `deletePatient` — not found → `PatientNotFoundException`

### Integration Test: `test/.../controller/PatientControllerIT.java`
Using `@SpringBootTest(webEnvironment=RANDOM_PORT)`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Transactional`.

Must test ALL of the following:
- `POST /patients` — 201 with Location header
- `POST /patients` — 400 with field errors (invalid email)
- `POST /patients` — 400 with missing required field
- `POST /patients` — 409 on duplicate email
- `POST /patients` — 401 when unauthenticated
- `GET /patients` — 200 with paginated results
- `GET /patients?status=ACTIVE` — 200 filtered
- `GET /patients/{id}` — 200
- `GET /patients/{id}` — 404 not found
- `GET /patients/{invalid-uuid}` — 400
- `PUT /patients/{id}` — 200 with updated data
- `PUT /patients/{id}` — 404
- `PUT /patients/{id}` — 409 on email conflict
- `PATCH /patients/{id}` — 200 with only changed fields
- `DELETE /patients/{id}` — 204 with ROLE_ADMIN
- `DELETE /patients/{id}` — 403 without ROLE_ADMIN
- `DELETE /patients/{id}` — 404 not found
- `GET /patients/search?firstName=John` — 200 filtered

---

## Reasoning Instructions

Before generating each file, explicitly state:
- What this file does
- How it connects to the contract (OpenAPI spec)
- Any Spring Boot 3 / JPA-specific decisions

Label non-obvious decisions as `[DECISION]: reason`.

---

## Anti-Hallucination Rules

- `PatientsApi` is the generated interface name (from `useTags: true` and tag `patients`).
- Spring Data Specification: use `Specification.where()` chaining pattern.
- `Page<PatientResponse>` from `Page<Patient>`: use `page.map(patientMapper::toResponse)`.
- `@CreatedDate` requires `@EnableJpaAuditing` on the config class (already done in base).
- UUID primary key generation: use `@GeneratedValue(strategy = GenerationType.UUID)` (Java 21 + Hibernate 6).
- `ProblemDetail.forStatusAndDetail(HttpStatus, String)` is the Spring 6 factory method.
- `ObjectOptimisticLockingFailureException` is thrown by Spring Data on version mismatch.
- `@Transactional` on service implementation class, NOT on the interface.

---

## Output Format

Start with complete file tree (numbered). Then each file in full. End with verification checklist.

Final verification must include:
```
- [ ] OpenAPI YAML has all 7 operations
- [ ] openapi-generator-maven-plugin configured
- [ ] Patient entity has @Version and unique email constraint
- [ ] PatientController implements PatientsApi
- [ ] 11 unit tests in PatientServiceTest
- [ ] 18 integration tests in PatientControllerIT
- [ ] V2__create_patients_table.sql migration exists
- [ ] GlobalExceptionHandler handles all new exceptions
- [ ] mvn clean verify passes
```

---

## PROMPT END
