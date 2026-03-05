# Skill: Contract-First OpenAPI

**Layer:** 00-skill
**Type:** API design contract
**Scope:** All REST API generation tasks

---

## Purpose

Enforces contract-first API design. The OpenAPI YAML contract MUST be defined
and finalized before any controller, service, or test code is written.
No exceptions. No "code-first" or "annotation-driven" API generation.

---

## Contract-First Mandate

### Order of Operations (Non-negotiable)

```
Step 1: Write OpenAPI YAML specification
Step 2: Validate the specification (openapi-generator validate)
Step 3: Configure openapi-generator-maven-plugin
Step 4: Generate server-side interfaces from the spec
Step 5: Implement the generated interfaces (no modification to generated code)
Step 6: Write integration tests against the contract
```

If this order is violated, the output is INVALID and must be regenerated.

---

## OpenAPI Specification Rules

### Version

Always use OpenAPI 3.1.0:

```yaml
openapi: "3.1.0"
```

### Required Top-Level Sections

```yaml
openapi: "3.1.0"
info:
  title: ""
  version: ""
  description: ""
  contact:
    name: ""
    email: ""
servers:
  - url: ""
    description: ""
tags: []
paths: {}
components:
  schemas: {}
  responses: {}
  parameters: {}
  securitySchemes: {}
security: []
```

### Path Naming Rules

| Rule | Example |
|------|---------|
| Lowercase, hyphen-separated | `/patient-records` |
| Resource nouns, not verbs | `/patients` not `/getPatients` |
| Plural resources | `/patients` not `/patient` |
| Sub-resources with parent ID | `/patients/{patientId}/appointments` |
| No trailing slash | `/patients` not `/patients/` |
| No version in path (use Accept header or URL prefix at gateway) | `/v1/patients` only if required |

### HTTP Method Semantics

| Method | Semantics | Idempotent | Body |
|--------|-----------|------------|------|
| `GET` | Read | Yes | No |
| `POST` | Create | No | Yes |
| `PUT` | Full replace | Yes | Yes |
| `PATCH` | Partial update | No | Yes |
| `DELETE` | Remove | Yes | No |

### Required Response Codes per Operation

| Operation | Success | Client Error | Server Error |
|-----------|---------|--------------|--------------|
| GET collection | 200 | 400, 401, 403 | 500 |
| GET single | 200 | 400, 401, 403, 404 | 500 |
| POST | 201 | 400, 401, 403, 409 | 500 |
| PUT | 200 | 400, 401, 403, 404 | 500 |
| PATCH | 200 | 400, 401, 403, 404 | 500 |
| DELETE | 204 | 401, 403, 404 | 500 |

---

## Schema Rules

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Schema names | PascalCase | `PatientRequest`, `PatientResponse` |
| Property names | camelCase | `firstName`, `dateOfBirth` |
| Enum values | UPPER_SNAKE_CASE | `ACTIVE`, `INACTIVE` |
| Path parameters | camelCase | `patientId` |
| Query parameters | camelCase | `pageSize`, `sortBy` |

### Required Schema Fields

Every schema MUST include:
- `type`
- `description` for every property
- `example` for every property
- `required` array for mandatory fields
- String length constraints where applicable
- Numeric range constraints where applicable

### Pagination Schema (Mandatory for Collections)

```yaml
components:
  schemas:
    PagedResponse:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/PatientResponse'
        page:
          type: integer
          description: Current page number (0-based)
          example: 0
        size:
          type: integer
          description: Page size
          example: 20
        totalElements:
          type: integer
          format: int64
          description: Total number of elements
          example: 150
        totalPages:
          type: integer
          description: Total number of pages
          example: 8
        last:
          type: boolean
          description: Whether this is the last page
          example: false
```

### Error Response Schema (RFC7807)

All error responses MUST use `application/problem+json` content type:

```yaml
components:
  schemas:
    ProblemDetail:
      type: object
      required: [type, title, status, detail, instance]
      properties:
        type:
          type: string
          format: uri
          description: Problem type URI
          example: "https://api.example.com/problems/validation-error"
        title:
          type: string
          description: Short, human-readable summary
          example: "Validation Error"
        status:
          type: integer
          description: HTTP status code
          example: 400
        detail:
          type: string
          description: Human-readable explanation
          example: "Email address is already in use"
        instance:
          type: string
          format: uri
          description: URI reference identifying the specific occurrence
          example: "/patients/create"
        timestamp:
          type: string
          format: date-time
          example: "2026-03-03T10:00:00Z"
        errors:
          type: array
          description: Field-level validation errors
          items:
            $ref: '#/components/schemas/FieldError'
    FieldError:
      type: object
      properties:
        field:
          type: string
          example: "email"
        message:
          type: string
          example: "must be a valid email address"
        rejectedValue:
          type: string
          example: "not-an-email"
```

---

## openapi-generator-maven-plugin Configuration

```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <version>7.x.x</version>
  <executions>
    <execution>
      <id>generate-api</id>
      <goals><goal>generate</goal></goals>
      <configuration>
        <inputSpec>${project.basedir}/src/main/resources/openapi/api.yaml</inputSpec>
        <generatorName>spring</generatorName>
        <apiPackage>com.example.api.generated</apiPackage>
        <modelPackage>com.example.dto.generated</modelPackage>
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

---

## Implementation Rules

### Controller Rules

- Controller class MUST implement the generated API interface.
- Controller MUST NOT contain business logic.
- Controller MUST delegate all logic to the service layer.
- No additional annotations on methods beyond what the interface provides.
- No custom `@RequestMapping` that contradicts the generated interface.

```java
@RestController
public class PatientController implements PatientsApi {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Override
    public ResponseEntity<PatientResponse> createPatient(PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(patientService.createPatient(request));
    }
}
```

### DTO and Entity Separation Rules

- Generated DTOs live in `dto/generated/` — never modify these.
- Custom request/response DTOs (if needed beyond generated) live in `dto/`.
- JPA entities live in `domain/`.
- DTOs must NEVER be used as JPA entities.
- Entities must NEVER be returned directly from controllers.

### Mapper Rules

- One mapper per entity/DTO pair.
- Mappers live in `mapper/`.
- Prefer MapStruct if available.
- Manual mappers are acceptable — must be fully implemented.
- Mapper must handle null inputs without throwing NPE.

---

## Definition of Done

- [ ] OpenAPI YAML file exists at `src/main/resources/openapi/api.yaml`
- [ ] OpenAPI spec validates with `openapi-generator validate`
- [ ] All paths include all required response codes
- [ ] All schemas include `description` and `example` for every property
- [ ] Error responses use `application/problem+json`
- [ ] `openapi-generator-maven-plugin` configured with `interfaceOnly=true`
- [ ] Controllers implement generated interfaces (not extend or bypass)
- [ ] DTOs and Entities are separate classes
- [ ] Mapper layer exists for every entity/DTO pair
- [ ] `mvn generate-sources` produces valid Java code
- [ ] Integration tests verify HTTP contract compliance

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| Writing controller before OpenAPI spec | Enforce Step 1 always |
| Missing error response definitions | Always include 4xx/5xx in every operation |
| Modifying generated code | Generated code is read-only; extend via interface impl |
| Returning entities from controllers | Enforce DTO/Entity separation in code review |
| Missing pagination | Use `PagedResponse` schema for all collection endpoints |
| Code-first annotations | Forbid `@Operation`, `@Schema` on entity classes |
