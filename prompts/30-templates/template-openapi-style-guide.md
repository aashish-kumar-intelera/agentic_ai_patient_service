# Template: OpenAPI Style Guide

**Layer:** 30-templates
**Type:** Reference template
**Scope:** All OpenAPI 3.1 specifications in this repository

---

## Purpose

This style guide defines the conventions all OpenAPI specifications in this repository
must follow. Use it as a reference when writing or reviewing API contracts.

---

## Specification File Template

Every API specification starts with this skeleton:

```yaml
openapi: "3.1.0"

info:
  title: "{Service Name} API"
  version: "{semver: 1.0.0}"
  description: |
    {One sentence describing what this API does}.

    ## Authentication
    All endpoints require a Bearer JWT token in the Authorization header.
    Obtain tokens from the identity provider before making API calls.

    ## Error Handling
    All errors follow RFC 7807 Problem Details (application/problem+json).
    See the ProblemDetail schema for the response structure.

    ## Pagination
    List endpoints return paginated results. Use `page` and `size` query parameters.
    Default page size: 20. Maximum page size: 100.
  contact:
    name: "Platform Team"
    email: "platform@example.com"
  license:
    name: "Internal"

servers:
  - url: "http://localhost:8080"
    description: "Local development"
  - url: "https://api-dev.example.com"
    description: "Development environment"
  - url: "https://api.example.com"
    description: "Production"

tags:
  - name: "{resource}"
    description: "{Resource management operations}"

security:
  - bearerAuth: []

paths:
  # --- define paths here ---

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    # --- define schemas here ---

  responses:
    # --- define reusable responses here ---

  parameters:
    # --- define reusable parameters here ---
```

---

## Path Naming Rules

| Rule | Correct | Incorrect |
|------|---------|-----------|
| Lowercase | `/patients` | `/Patients` |
| Hyphens for multi-word | `/patient-records` | `/patientRecords` |
| Plural nouns | `/patients` | `/patient` |
| No verbs | `/patients` | `/getPatients` |
| Sub-resources | `/patients/{id}/notes` | `/getPatientNotes` |
| No trailing slash | `/patients` | `/patients/` |
| Path params in `{}` | `/patients/{patientId}` | `/patients/:id` |

---

## HTTP Method Conventions

```yaml
paths:
  /patients:
    get:
      summary: "List patients"        # Short (< 50 chars), no period
      operationId: "listPatients"     # camelCase, unique across the API
      tags: ["patients"]              # Match tag defined in top-level tags
      description: |
        Returns a paginated list of patients.
        Supports filtering by status.
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
        - $ref: '#/components/parameters/SortParam'
      responses:
        "200":
          description: "Paginated list of patients"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PagedPatientResponse'
        "400":
          $ref: '#/components/responses/BadRequest'
        "401":
          $ref: '#/components/responses/Unauthorized'
        "403":
          $ref: '#/components/responses/Forbidden'
      security:
        - bearerAuth: []

    post:
      summary: "Create a patient"
      operationId: "createPatient"
      tags: ["patients"]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PatientRequest'
      responses:
        "201":
          description: "Patient created successfully"
          headers:
            Location:
              description: "URL of the newly created patient"
              schema:
                type: string
                format: uri
                example: "/patients/550e8400-e29b-41d4-a716-446655440000"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PatientResponse'
        "400":
          $ref: '#/components/responses/BadRequest'
        "401":
          $ref: '#/components/responses/Unauthorized'
        "403":
          $ref: '#/components/responses/Forbidden'
        "409":
          $ref: '#/components/responses/Conflict'

  /patients/{patientId}:
    parameters:
      - $ref: '#/components/parameters/PatientIdParam'

    get:
      summary: "Get a patient by ID"
      operationId: "getPatientById"
      tags: ["patients"]
      responses:
        "200":
          description: "Patient found"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PatientResponse'
        "401":
          $ref: '#/components/responses/Unauthorized'
        "403":
          $ref: '#/components/responses/Forbidden'
        "404":
          $ref: '#/components/responses/NotFound'

    delete:
      summary: "Delete a patient"
      operationId: "deletePatient"
      tags: ["patients"]
      description: |
        Permanently removes a patient record.
        Requires ROLE_ADMIN authority.
      responses:
        "204":
          description: "Patient deleted successfully"
        "401":
          $ref: '#/components/responses/Unauthorized'
        "403":
          $ref: '#/components/responses/Forbidden'
        "404":
          $ref: '#/components/responses/NotFound'
```

---

## Schema Conventions

### Field Naming

| Type | Convention | Example |
|------|-----------|---------|
| Schema name | PascalCase | `PatientRequest` |
| Property name | camelCase | `firstName` |
| Enum value | UPPER_SNAKE_CASE | `ACTIVE` |

### Every Property Must Have

- `type`
- `description` (sentence case, ends without period)
- `example` (realistic value, not "string" or "example")

### Required Constraints

```yaml
# String with constraints
firstName:
  type: string
  description: "Patient's first name"
  example: "John"
  minLength: 1
  maxLength: 100

# Email
email:
  type: string
  format: email
  description: "Patient's email address (must be unique)"
  example: "john.doe@example.com"
  maxLength: 255

# UUID
id:
  type: string
  format: uuid
  description: "Unique patient identifier"
  example: "550e8400-e29b-41d4-a716-446655440000"
  readOnly: true

# Date
dateOfBirth:
  type: string
  format: date
  description: "Patient's date of birth (ISO 8601: YYYY-MM-DD)"
  example: "1990-01-15"

# DateTime
createdAt:
  type: string
  format: date-time
  description: "Timestamp when the record was created (UTC)"
  example: "2026-03-03T10:00:00Z"
  readOnly: true

# Enum
status:
  type: string
  enum: [ACTIVE, INACTIVE]
  description: "Patient account status"
  example: "ACTIVE"
  default: "ACTIVE"

# Integer with range
page:
  type: integer
  description: "Page number (0-based)"
  example: 0
  minimum: 0
  default: 0

# Array
errors:
  type: array
  description: "Field-level validation errors"
  items:
    $ref: '#/components/schemas/FieldError'
```

---

## Reusable Components

### Parameters

```yaml
components:
  parameters:
    PatientIdParam:
      name: patientId
      in: path
      required: true
      description: "Unique patient identifier (UUID)"
      schema:
        type: string
        format: uuid
        example: "550e8400-e29b-41d4-a716-446655440000"

    PageParam:
      name: page
      in: query
      required: false
      description: "Page number (0-based)"
      schema:
        type: integer
        minimum: 0
        default: 0
        example: 0

    SizeParam:
      name: size
      in: query
      required: false
      description: "Number of items per page (max 100)"
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
        example: 20

    SortParam:
      name: sort
      in: query
      required: false
      description: "Sort criteria: field,direction (e.g., lastName,asc)"
      schema:
        type: string
        example: "lastName,asc"
```

### Reusable Responses

```yaml
components:
  responses:
    BadRequest:
      description: "Invalid request — validation failed"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/validation-error"
            title: "Validation Error"
            status: 400
            detail: "Request validation failed"
            instance: "/patients"
            timestamp: "2026-03-03T10:00:00Z"
            errors:
              - field: "email"
                message: "must be a valid email address"
                rejectedValue: "not-an-email"

    Unauthorized:
      description: "Authentication required"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/unauthorized"
            title: "Unauthorized"
            status: 401
            detail: "Valid authentication credentials required"
            instance: "/patients"

    Forbidden:
      description: "Insufficient permissions"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/forbidden"
            title: "Forbidden"
            status: 403
            detail: "You do not have permission to perform this action"
            instance: "/patients/delete"

    NotFound:
      description: "Resource not found"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/not-found"
            title: "Not Found"
            status: 404
            detail: "The requested resource does not exist"
            instance: "/patients/unknown-id"

    Conflict:
      description: "Resource conflict"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/conflict"
            title: "Conflict"
            status: 409
            detail: "A resource with the provided identifier already exists"
            instance: "/patients"

    InternalServerError:
      description: "Unexpected server error"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: "https://api.example.com/problems/internal-error"
            title: "Internal Server Error"
            status: 500
            detail: "An unexpected error occurred. Contact support if the issue persists."
            instance: "/patients"
```

---

## Checklist Before Finalizing an OpenAPI Spec

```
□ openapi: "3.1.0" declared
□ info.title, info.version, info.description present
□ At least one server URL defined
□ All tags defined in top-level tags section
□ All paths use lowercase, hyphen-separated nouns
□ All operationIds are unique and camelCase
□ Every operation has: summary, operationId, tags, responses
□ Every operation has 4xx and 5xx responses defined
□ POST operations have requestBody with content
□ POST 201 responses include Location header schema
□ DELETE 204 has no response body
□ All schema properties have type, description, example
□ Required fields listed in schema required array
□ Reusable components used for repeated schemas/responses/params
□ Security scheme declared in components.securitySchemes
□ Global security applied at top level
□ Enums defined with all valid values listed
□ Error responses use application/problem+json content type
```
