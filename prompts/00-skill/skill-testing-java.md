# Skill: Testing Java

**Layer:** 00-skill
**Type:** Testing standards contract
**Scope:** All Java microservice testing tasks

---

## Purpose

Defines mandatory testing standards, coverage requirements, naming conventions,
and test structure for all generated Java Spring Boot code.
Tests are NOT optional. They are part of the Definition of Done.

---

## Test Pyramid Mandate

```
         /\
        /  \
       / E2E\         ← Out of scope for local generation
      /______\
     /        \
    / Integration\    ← MockMvc + @SpringBootTest (slice)
   /____________\
  /              \
 /   Unit Tests   \   ← JUnit 5 + Mockito (majority)
/________________\
```

### Coverage Requirements

| Layer | Required Coverage |
|-------|-----------------|
| Service layer | 80% line coverage minimum |
| Controller layer | 100% (all endpoints tested via MockMvc) |
| Repository layer | Integration test for each custom query |
| Utility classes | 90% line coverage minimum |

---

## Test Dependencies

All test dependencies declared with `<scope>test</scope>`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

Included transitively via `spring-boot-starter-test`:
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- MockMvc
- Testcontainers (add separately for integration tests with real DB)

---

## Unit Test Rules

### Naming Convention

```
{ClassUnderTest}Test.java

Example: PatientServiceTest.java
```

### Method Naming

```java
@Test
void {methodName}_when{Condition}_should{ExpectedBehavior}() { }

// Examples:
void createPatient_whenEmailAlreadyExists_shouldThrowDuplicateEmailException()
void getPatient_whenPatientNotFound_shouldThrowPatientNotFoundException()
void createPatient_whenValidRequest_shouldReturnPatientResponse()
```

### Unit Test Structure (AAA Pattern)

```java
@Test
void methodName_whenCondition_shouldBehavior() {
    // Arrange
    PatientRequest request = buildValidPatientRequest();
    when(patientRepository.existsByEmail(request.getEmail())).thenReturn(false);
    when(patientRepository.save(any())).thenReturn(buildPatientEntity());
    when(patientMapper.toResponse(any())).thenReturn(buildPatientResponse());

    // Act
    PatientResponse result = patientService.createPatient(request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo(request.getEmail());
    verify(patientRepository).save(any(Patient.class));
}
```

### Unit Test Requirements

- Every public service method MUST have at least one happy-path test.
- Every public service method MUST have at least one failure-path test.
- Every exception type thrown MUST be tested.
- Use `@ExtendWith(MockitoExtension.class)` — not `@SpringBootTest`.
- All dependencies of the class under test MUST be mocked.
- Do NOT use `@Autowired` in unit tests.

---

## Integration Test Rules

### Naming Convention

```
{ClassUnderTest}IT.java

Example: PatientControllerIT.java
```

### Configuration

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
}
```

### Integration Test Requirements

For every REST endpoint:
- Test the happy path with valid input → expect correct status + response body.
- Test validation failures → expect 400 + RFC7807 error body.
- Test not-found scenarios → expect 404 + RFC7807 error body.
- Test unauthorized access → expect 401.
- Test forbidden access → expect 403.
- Test conflict scenarios → expect 409 (where applicable).

### MockMvc Pattern

```java
@Test
void createPatient_whenValidRequest_shouldReturn201() throws Exception {
    PatientRequest request = buildValidPatientRequest();

    mockMvc.perform(post("/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.email").value(request.getEmail()))
        .andExpect(jsonPath("$.id").isNotEmpty());
}

@Test
void createPatient_whenEmailInvalid_shouldReturn400WithProblemDetail() throws Exception {
    PatientRequest request = buildPatientRequestWithInvalidEmail();

    mockMvc.perform(post("/patients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.detail").exists());
}
```

---

## Repository Test Rules

### Pattern

```java
@DataJpaTest
@ActiveProfiles("test")
class PatientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void findByEmail_whenExists_shouldReturnPatient() {
        Patient patient = buildPatient();
        entityManager.persistAndFlush(patient);

        Optional<Patient> result = patientRepository.findByEmail(patient.getEmail());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(patient.getEmail());
    }
}
```

---

## Test Data Builders

Use static builder methods instead of constructors in tests:

```java
// In test class or shared TestDataFactory
private static PatientRequest buildValidPatientRequest() {
    return PatientRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .build();
}
```

Do NOT use `new PatientRequest()` with individual setters in tests.

---

## Test Profile Configuration

`src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    root: WARN
    com.example: INFO
```

---

## Maven Surefire / Failsafe Configuration

```xml
<!-- Unit tests -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <excludes>
      <exclude>**/*IT.java</exclude>
    </excludes>
  </configuration>
</plugin>

<!-- Integration tests -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <includes>
      <include>**/*IT.java</include>
    </includes>
  </configuration>
</plugin>
```

---

## Definition of Done

- [ ] Every public service method has unit tests (happy + failure paths)
- [ ] Every REST endpoint has integration tests via MockMvc
- [ ] Every custom repository query has a `@DataJpaTest` test
- [ ] Tests follow `AAA` (Arrange-Act-Assert) pattern
- [ ] Method names follow `method_whenCondition_shouldBehavior` convention
- [ ] Test data uses builder methods, not raw constructors
- [ ] `application-test.yml` exists with H2 config
- [ ] Surefire and Failsafe plugins configured
- [ ] `mvn test` runs unit tests only
- [ ] `mvn verify` runs unit + integration tests
- [ ] No `Thread.sleep()` in tests
- [ ] No `@Disabled` without a linked issue/explanation

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| `@SpringBootTest` used for unit tests | Use `@ExtendWith(MockitoExtension.class)` for unit tests |
| Missing failure-path tests | For each method, explicitly list failure scenarios |
| Tests share state | Use `@Transactional` + rollback in integration tests |
| `Thread.sleep()` to wait for async | Use Awaitility or synchronous test design |
| Missing auth in MockMvc tests | Always include `.with(jwt())` for secured endpoints |
| Hardcoded test data causing flakiness | Use builder methods; avoid dates relative to "now" without fixed clocks |
