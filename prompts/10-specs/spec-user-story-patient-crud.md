# Spec: Patient CRUD – User Story & Requirements

**Layer:** 10-specs
**Version:** 1.0.0
**Date:** 2026-03-03
**Status:** Approved
**Depends on:** spec-microservice-base-architecture.md

---

## Scope

This specification defines the contract-first Patient CRUD API for a healthcare patient
management service. It covers the API contract, data model, business rules, validation,
error handling, and test requirements for all Create, Read, Update, and Delete operations
on the Patient resource.

---

## Non-Goals

- Does NOT define appointment scheduling or clinical data management.
- Does NOT define patient portal (consumer-facing UI).
- Does NOT define FHIR compliance (out of scope for this iteration).
- Does NOT define multi-tenancy or organization-level data isolation.
- Does NOT define audit trail beyond standard logging.

---

## Assumptions

| ID | Assumption |
|----|-----------|
| A-01 | A patient is uniquely identified by email address within the system. |
| A-02 | Authentication is handled by an external identity provider; JWT is passed in `Authorization: Bearer` header. |
| A-03 | All users with `ROLE_USER` can perform CRUD operations on patients. |
| A-04 | Soft delete is NOT implemented in this iteration (hard delete). |
| A-05 | Pagination is required for all list endpoints; max page size is 100. |
| A-06 | Phone number format validation is country-agnostic (basic format only). |
| A-07 | Date of birth is stored as ISO 8601 date (`YYYY-MM-DD`). |

---

## Data Model

### Patient Entity

| Field | Type | Constraints | Notes |
|-------|------|------------|-------|
| `id` | UUID | Primary key, generated | System-generated; not settable by client |
| `firstName` | String | NOT NULL, max 100 chars | |
| `lastName` | String | NOT NULL, max 100 chars | |
| `email` | String | NOT NULL, UNIQUE, valid email, max 255 chars | Unique constraint at DB level |
| `phoneNumber` | String | Nullable, max 20 chars | E.164 format preferred |
| `dateOfBirth` | LocalDate | NOT NULL | Must be in the past |
| `gender` | Enum | Nullable | MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY |
| `address` | String | Nullable, max 500 chars | Free-form address string |
| `status` | Enum | NOT NULL, default ACTIVE | ACTIVE, INACTIVE |
| `createdAt` | OffsetDateTime | NOT NULL, system-generated | Set on INSERT, never updated |
| `updatedAt` | OffsetDateTime | NOT NULL, system-managed | Updated on every UPDATE |
| `version` | Long | NOT NULL, for optimistic locking | `@Version` field |

---

## API Contract Summary

Base path: `/patients`

| Operation | Method | Path | Request | Success | Error Codes |
|-----------|--------|------|---------|---------|------------|
| Create patient | POST | `/patients` | `PatientRequest` | 201 + `PatientResponse` | 400, 401, 403, 409 |
| Get all patients | GET | `/patients` | Query params | 200 + `Page<PatientResponse>` | 400, 401, 403 |
| Get patient by ID | GET | `/patients/{id}` | Path param | 200 + `PatientResponse` | 401, 403, 404 |
| Update patient | PUT | `/patients/{id}` | `PatientRequest` | 200 + `PatientResponse` | 400, 401, 403, 404, 409 |
| Patch patient | PATCH | `/patients/{id}` | `PatchPatientRequest` | 200 + `PatientResponse` | 400, 401, 403, 404 |
| Delete patient | DELETE | `/patients/{id}` | Path param | 204 | 401, 403, 404 |
| Search patients | GET | `/patients/search` | Query params | 200 + `Page<PatientResponse>` | 400, 401, 403 |

---

## Functional Requirements

### FR-01 – Create Patient

| ID | Requirement |
|----|------------|
| FR-01-1 | POST `/patients` MUST accept `application/json` body. |
| FR-01-2 | `id`, `createdAt`, `updatedAt`, `version` MUST be system-generated; ignored if provided. |
| FR-01-3 | Duplicate `email` MUST return HTTP 409 with problem+json body. |
| FR-01-4 | Successful creation MUST return HTTP 201 with `Location` header pointing to new resource. |
| FR-01-5 | `status` defaults to `ACTIVE` if not provided. |

### FR-02 – Get All Patients (Paginated)

| ID | Requirement |
|----|------------|
| FR-02-1 | GET `/patients` MUST return paginated results. |
| FR-02-2 | Default page size: 20. Maximum page size: 100. |
| FR-02-3 | Supported sort fields: `lastName`, `firstName`, `createdAt`, `email`. |
| FR-02-4 | Default sort: `lastName ASC`. |
| FR-02-5 | Query parameters: `page` (0-based), `size`, `sort` (field,direction). |
| FR-02-6 | Inactive patients MUST be included unless `status=ACTIVE` filter is applied. |

### FR-03 – Get Patient by ID

| ID | Requirement |
|----|------------|
| FR-03-1 | GET `/patients/{id}` MUST return the full patient record. |
| FR-03-2 | Non-existent `id` MUST return HTTP 404 with problem+json. |
| FR-03-3 | `id` MUST be a valid UUID format; invalid format returns HTTP 400. |

### FR-04 – Update Patient (Full)

| ID | Requirement |
|----|------------|
| FR-04-1 | PUT `/patients/{id}` MUST replace all mutable fields. |
| FR-04-2 | `id`, `createdAt`, `version` are immutable. |
| FR-04-3 | Email change to an already-used email MUST return HTTP 409. |
| FR-04-4 | If patient does not exist, return HTTP 404. |
| FR-04-5 | Optimistic locking: if version mismatch, return HTTP 409 with conflict explanation. |

### FR-05 – Patch Patient (Partial)

| ID | Requirement |
|----|------------|
| FR-05-1 | PATCH `/patients/{id}` MUST accept partial updates. |
| FR-05-2 | Only provided fields are updated; omitted fields are unchanged. |
| FR-05-3 | JSON Merge Patch semantics (RFC 7396). |
| FR-05-4 | Setting a nullable field to `null` explicitly clears it. |

### FR-06 – Delete Patient

| ID | Requirement |
|----|------------|
| FR-06-1 | DELETE `/patients/{id}` MUST permanently remove the patient. |
| FR-06-2 | Successful deletion returns HTTP 204 with no body. |
| FR-06-3 | Deletion of non-existent patient returns HTTP 404. |
| FR-06-4 | No cascade to appointments (not in scope). |

### FR-07 – Search Patients

| ID | Requirement |
|----|------------|
| FR-07-1 | GET `/patients/search` accepts query params: `firstName`, `lastName`, `email`, `status`. |
| FR-07-2 | All search params are optional; multiple can be combined (AND logic). |
| FR-07-3 | String search is case-insensitive, contains match. |
| FR-07-4 | Response is paginated. Same pagination rules as FR-02. |

---

## Non-Functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-01 | POST `/patients` response time | p95 < 200ms |
| NFR-02 | GET `/patients` (20 results) response time | p95 < 100ms |
| NFR-03 | DELETE `/patients/{id}` response time | p95 < 100ms |
| NFR-04 | All operations correct under concurrent access | Optimistic locking prevents data corruption |

---

## Security Requirements

| ID | Requirement |
|----|------------|
| SEC-01 | All `/patients/**` endpoints require authentication. |
| SEC-02 | `ROLE_USER` required for all CRUD operations. |
| SEC-03 | `ROLE_ADMIN` required for DELETE operations. |
| SEC-04 | Email uniqueness enforced at both application and database level. |
| SEC-05 | Patient IDs in responses are UUIDs (not sequential integers — prevents enumeration). |
| SEC-06 | Sensitive patient fields (e.g., DOB) are not logged. |

---

## Observability Requirements

| ID | Requirement |
|----|------------|
| OBS-01 | Counter metric `patients.created.total` incremented on each create. |
| OBS-02 | Counter metric `patients.deleted.total` incremented on each delete. |
| OBS-03 | Timer metric `patients.crud.duration` for all operations (tagged with operation). |
| OBS-04 | Patient create/update/delete events logged at INFO with patientId. |
| OBS-05 | Duplicate email attempt logged at WARN with masked email. |

---

## Validation Rules

| Field | Rule |
|-------|------|
| `firstName` | Required, 1-100 characters, not blank |
| `lastName` | Required, 1-100 characters, not blank |
| `email` | Required, valid email format, max 255 chars |
| `phoneNumber` | Optional, max 20 chars, matches `^[+\d\s\-()]{7,20}$` |
| `dateOfBirth` | Required, must be in the past (not today, not future) |
| `gender` | Optional, must be valid enum value if provided |
| `status` | Required, must be valid enum value |

---

## Error Response Specification

All error responses use `Content-Type: application/problem+json`.

### 400 – Validation Error
```json
{
  "type": "https://api.example.com/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/patients",
  "timestamp": "2026-03-03T10:00:00Z",
  "errors": [
    {"field": "email", "message": "must be a valid email address", "rejectedValue": "not-email"}
  ]
}
```

### 404 – Not Found
```json
{
  "type": "https://api.example.com/problems/not-found",
  "title": "Patient Not Found",
  "status": 404,
  "detail": "Patient with ID 550e8400-e29b-41d4-a716-446655440000 does not exist",
  "instance": "/patients/550e8400-e29b-41d4-a716-446655440000"
}
```

### 409 – Conflict
```json
{
  "type": "https://api.example.com/problems/email-conflict",
  "title": "Email Already Exists",
  "status": 409,
  "detail": "A patient with email j***@example.com already exists",
  "instance": "/patients"
}
```

---

## Acceptance Criteria

| ID | Criterion | Verification |
|----|----------|-------------|
| AC-01 | POST creates patient, returns 201 + Location | Integration test |
| AC-02 | POST with duplicate email returns 409 | Integration test |
| AC-03 | POST with invalid email returns 400 + field errors | Integration test |
| AC-04 | GET /patients returns paginated list | Integration test |
| AC-05 | GET /patients/{id} returns patient | Integration test |
| AC-06 | GET /patients/{unknown-id} returns 404 | Integration test |
| AC-07 | PUT updates all fields | Integration test |
| AC-08 | PATCH updates only provided fields | Integration test |
| AC-09 | DELETE removes patient, returns 204 | Integration test |
| AC-10 | GET /patients/search filters correctly | Integration test |
| AC-11 | Unauthenticated request returns 401 | Integration test |
| AC-12 | ROLE_USER cannot DELETE | Integration test |

---

## Definition of Done

- [ ] OpenAPI YAML at `src/main/resources/openapi/api.yaml` fully defines all endpoints
- [ ] `PatientRequest` and `PatchPatientRequest` DTOs validated
- [ ] `PatientResponse` DTO matches entity fields (no entity exposure)
- [ ] `Patient` JPA entity with all fields and constraints
- [ ] Flyway migration `V1__create_patients_table.sql`
- [ ] `PatientRepository` with custom query for search
- [ ] `PatientService` with all CRUD operations
- [ ] `PatientController` implements generated API interface
- [ ] `PatientMapper` maps entity ↔ DTO
- [ ] Global exception handler maps all exceptions to problem+json
- [ ] Unit tests for `PatientService` (all operations, all failure paths)
- [ ] Integration tests for all endpoints (happy + error paths)
- [ ] Email uniqueness enforced at DB and application level
- [ ] Optimistic locking (`@Version`) on entity

---

## Validation Checklist

```
□ OpenAPI YAML has all 7 operations defined
□ Each operation has all required response codes
□ PatientRequest has @Valid annotations on all fields
□ Patient entity has @Version field
□ Email has @Column(unique = true) on entity
□ Flyway migration file exists and creates correct schema
□ PatientRepository.findByEmail() exists
□ PatientService throws PatientNotFoundException (extends RuntimeException)
□ PatientService throws DuplicateEmailException (extends RuntimeException)
□ GlobalExceptionHandler handles MethodArgumentNotValidException → 400
□ GlobalExceptionHandler handles PatientNotFoundException → 404
□ GlobalExceptionHandler handles DuplicateEmailException → 409
□ All 12 acceptance criteria covered by integration tests
□ mvn clean verify passes
```
