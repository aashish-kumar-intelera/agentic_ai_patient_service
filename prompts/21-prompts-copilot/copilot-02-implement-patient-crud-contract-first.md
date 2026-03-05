# Copilot Prompt 02: Patient CRUD – Contract-First

**Layer:** 21-prompts-copilot
**Task:** TASK 2 – Contract-First Patient CRUD
**AI Target:** GitHub Copilot Chat (IDE-inline)
**Quality bar:** Identical to Claude prompt 02
**Prerequisite:** Copilot Prompt 01 completed (base project exists)

---

## Usage

Paste into GitHub Copilot Chat with the `patient-service` project open in the IDE.

---

## PROMPT

Add contract-first Patient CRUD to the existing `patient-service` Spring Boot project.
Base package: `com.intellera.patientservice`.

**Rules:**
- OpenAPI YAML FIRST — before any Java code
- Controllers MUST implement generated interfaces — no custom `@RequestMapping`
- Every file complete — no truncation, no "omitted for brevity"
- No TODOs, placeholders, or unimplemented methods
- Output file tree first, then each file numbered

---

### Step 1 — OpenAPI Spec (FIRST)

Generate: `src/main/resources/openapi/patient-api.yaml`

```yaml
openapi: "3.1.0"
# Full spec — include ALL of the following
```

**Operations (all 7 required):**

| Method | Path | Body | Success | Errors |
|--------|------|------|---------|--------|
| POST | /patients | PatientRequest | 201 + Location header | 400, 401, 403, 409 |
| GET | /patients | — | 200 PagedPatientResponse | 400, 401, 403 |
| GET | /patients/{patientId} | — | 200 PatientResponse | 400, 401, 403, 404 |
| PUT | /patients/{patientId} | PatientRequest | 200 PatientResponse | 400, 401, 403, 404, 409 |
| PATCH | /patients/{patientId} | PatchPatientRequest | 200 PatientResponse | 400, 401, 403, 404 |
| DELETE | /patients/{patientId} | — | 204 | 401, 403, 404 |
| GET | /patients/search | — | 200 PagedPatientResponse | 400, 401, 403 |

**Query params for GET /patients:** `page`(0), `size`(20,max100), `sort`(lastName,asc), `status`
**Query params for GET /patients/search:** `firstName`, `lastName`, `email`, `status`, `page`, `size`, `sort`

**Schemas (all required with description + example on every property):**
- `PatientRequest`: firstName*, lastName*, email*, dateOfBirth*, phoneNumber, gender, address, status
- `PatchPatientRequest`: same fields, all optional
- `PatientResponse`: all PatientRequest fields + id(UUID), createdAt, updatedAt, version
- `PagedPatientResponse`: content(array of PatientResponse), page, size, totalElements, totalPages, last
- `ProblemDetail`: type, title, status, detail, instance, timestamp, errors(optional)
- `FieldError`: field, message, rejectedValue
- `Gender` enum: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
- `PatientStatus` enum: ACTIVE, INACTIVE

Security: bearerAuth (Bearer JWT) on all operations.

---

### Step 2 — Update pom.xml

Add `openapi-generator-maven-plugin` (version 7.4.0):
- inputSpec: `src/main/resources/openapi/patient-api.yaml`
- generatorName: spring
- apiPackage: `com.intellera.patientservice.api.generated`
- modelPackage: `com.intellera.patientservice.dto.generated`
- configOptions: `interfaceOnly=true`, `useSpringBoot3=true`, `useTags=true`,
  `dateLibrary=java8`, `openApiNullable=false`, `useBeanValidation=true`,
  `skipDefaultInterface=true`

Add `build-helper-maven-plugin` to include `target/generated-sources/openapi` in compile path.

---

### Step 3 — Java Files

**All files complete. No truncation.**

**domain/Patient.java**
- `@Entity @Table(name="patients")`
- id: UUID, `@GeneratedValue(strategy=UUID)`
- firstName, lastName: `@NotBlank, @Size(max=100), @Column(nullable=false)`
- email: `@Email, @Column(unique=true, nullable=false, length=255)`
- phoneNumber: nullable, max 20 chars
- dateOfBirth: `LocalDate, @Past, @Column(nullable=false)`
- gender: `@Enumerated(EnumType.STRING)`, nullable
- address: nullable, length 500
- status: `@Enumerated(EnumType.STRING), @Column(nullable=false)`, default ACTIVE
- createdAt: `OffsetDateTime, @CreatedDate, @Column(updatable=false)`
- updatedAt: `OffsetDateTime, @LastModifiedDate`
- version: `Long, @Version`
- equals/hashCode on id

**domain/Gender.java** — enum with all 4 values
**domain/PatientStatus.java** — enum with ACTIVE, INACTIVE

**repository/PatientRepository.java**
- Extends `JpaRepository<Patient, UUID>` + `JpaSpecificationExecutor<Patient>`
- `Optional<Patient> findByEmail(String email)`
- `boolean existsByEmail(String email)`
- `boolean existsByEmailAndIdNot(String email, UUID id)`

**repository/PatientSpecification.java**
- Static methods: `hasFirstName(String)`, `hasLastName(String)`, `hasEmail(String)`, `hasStatus(PatientStatus)`
- Each returns `Specification<Patient>`
- String searches: case-insensitive LIKE (`lower(root.get("field")).like(lower(param))`)
- Null param returns `null` (caller uses `Specification.where()` chaining)

**mapper/PatientMapper.java** — Manual mapper (no MapStruct)
- `Patient toEntity(PatientRequest request)` — map fields, set status default ACTIVE if null
- `void updateEntity(PatientRequest request, Patient entity)` — update all mutable fields
- `void patchEntity(PatchPatientRequest request, Patient entity)` — only update non-null fields
- `PatientResponse toResponse(Patient entity)` — map all fields including id, createdAt, updatedAt, version
- Annotate with `@Component`

**exception/PatientNotFoundException.java** — `extends RuntimeException`, constructor with UUID id
**exception/DuplicateEmailException.java** — `extends RuntimeException`, constructor with email
**exception/PatientUpdateConflictException.java** — `extends RuntimeException`

**exception/GlobalExceptionHandler.java** — ADD to existing handler:
- `PatientNotFoundException` → 404 ProblemDetail (include patient ID in detail)
- `DuplicateEmailException` → 409 ProblemDetail (mask email: show first char + ***)
- `PatientUpdateConflictException` → 409 ProblemDetail
- `ObjectOptimisticLockingFailureException` → 409 ProblemDetail

**service/PatientService.java** — interface with 7 methods:
```java
PatientResponse createPatient(PatientRequest request);
Page<PatientResponse> getAllPatients(Pageable pageable, PatientStatus status);
PatientResponse getPatientById(UUID id);
PatientResponse updatePatient(UUID id, PatientRequest request);
PatientResponse patchPatient(UUID id, PatchPatientRequest request);
void deletePatient(UUID id);
Page<PatientResponse> searchPatients(String firstName, String lastName, String email, PatientStatus status, Pageable pageable);
```

**service/impl/PatientServiceImpl.java**
- `@Service @Transactional`
- Inject: `PatientRepository`, `PatientMapper`, `MeterRegistry`
- Register in constructor: `patients.created.total` counter, `patients.deleted.total` counter, `patients.operation.duration` timer (with `operation` tag)
- createPatient: check `existsByEmail` → throw `DuplicateEmailException`; save; increment counter; log INFO
- getPatientById: `findById` → throw `PatientNotFoundException` if empty
- updatePatient: verify exists; check email conflict; update entity; save; log INFO
- patchPatient: verify exists; patch entity (only non-null fields); save
- deletePatient: verify exists; delete; increment counter; log INFO
- searchPatients: build spec using `Specification.where()`; return page.map

**controller/PatientController.java**
- `@RestController`
- Implements `PatientsApi` (generated interface)
- Constructor inject `PatientService`
- Delegate ALL logic to service
- POST createPatient: return 201 + Location header `/patients/{id}`
- DELETE: log at INFO when patient deleted (controller level: log requestId from MDC)

**db/migration/V2__create_patients_table.sql**
```sql
CREATE TABLE patients (
  id UUID PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone_number VARCHAR(20),
  date_of_birth DATE NOT NULL,
  gender VARCHAR(30),
  address VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_patients_email UNIQUE (email)
);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_last_name ON patients(last_name);
CREATE INDEX idx_patients_status ON patients(status);
```

---

### Step 4 — Tests

**test/.../service/PatientServiceTest.java** — Unit tests
- `@ExtendWith(MockitoExtension.class)`
- Mock: `PatientRepository`, `PatientMapper`, `MeterRegistry` (use `SimpleMeterRegistry`)
- Test all 11 scenarios (AAA pattern):
  1. createPatient success
  2. createPatient duplicate email → DuplicateEmailException
  3. getPatientById found
  4. getPatientById not found → PatientNotFoundException
  5. updatePatient success
  6. updatePatient not found → PatientNotFoundException
  7. updatePatient email conflict → DuplicateEmailException
  8. patchPatient success (only non-null fields updated)
  9. deletePatient success
  10. deletePatient not found → PatientNotFoundException
  11. searchPatients returns page

**test/.../controller/PatientControllerIT.java** — Integration tests
- `@SpringBootTest(webEnvironment=RANDOM_PORT) @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional`
- Use `.with(jwt().authorities(...))` for auth
- Test all 18 scenarios listed in Claude prompt 02 (create, read, update, patch, delete, search — happy + error + auth)

---

### Output Format

File tree (numbered) → each file in full → verification checklist:
```
- [ ] OpenAPI YAML: all 7 operations, all schemas with description+example
- [ ] PatientController implements PatientsApi
- [ ] Patient entity has @Version and UNIQUE email constraint
- [ ] V2 migration creates patients table with all columns and indexes
- [ ] 11 unit tests in PatientServiceTest
- [ ] 18 integration tests in PatientControllerIT
- [ ] mvn clean verify passes
```
