# CLAUDE.md
You are a Senior AI Prompt Architect and Enterprise Java Platform Architect.

Your task is to generate the complete content for a blank GitHub repository that will store production-grade AI prompts.

This repository must support BOTH:
- Claude AI (long-form, strict, reasoning-heavy prompts)
- GitHub Copilot (shorter, structured, IDE-friendly prompts)

You are NOT generating Java application code.
You are generating reusable PROMPT FILES and SPEC FILES that will later generate code.

---------------------------------------------------------
REPOSITORY STRUCTURE (MANDATORY)
---------------------------------------------------------

Create exactly this structure:

prompts/
README.md

00-skill/
skill-global-output-contract.md
skill-java-spring-microservice.md
skill-contract-first-openapi.md
skill-testing-java.md
skill-security-owasp.md
skill-observability.md
skill-local-prepush-reviewer.md

10-specs/
spec-microservice-base-architecture.md
spec-user-story-patient-crud.md
spec-local-prepush-review-workflow.md

20-prompts-claude/
prompt-01-generate-microservice-architecture.md
prompt-02-implement-patient-crud-contract-first.md
prompt-03-setup-local-prepush-review.md

21-prompts-copilot/
copilot-01-generate-microservice-architecture.md
copilot-02-implement-patient-crud-contract-first.md
copilot-03-setup-local-prepush-review.md

30-templates/
template-openapi-style-guide.md
template-problem-json-errors.md
template-repo-output-format.md

---------------------------------------------------------
PURPOSE OF EACH LAYER
---------------------------------------------------------

00-skill:
Reusable behavioral rules and output contracts.
Model-agnostic.
Must enforce deterministic output.

10-specs:
Spec Kit style requirement documents.
Must contain:
- Scope
- Non-goals
- Assumptions
- Functional Requirements
- Non-functional Requirements
- Security Requirements
- Observability Requirements
- Acceptance Criteria
- Definition of Done
- Validation checklist

20-prompts-claude:
Full-length strict prompts.
Include:
- Explicit reasoning expectations
- Verification instructions
- Deterministic file-output formatting
- Definition of Done section
- Anti-hallucination rules

21-prompts-copilot:
Shorter but equivalent prompts.
Must:
- Keep identical constraints and quality bar
- Be concise
- Be IDE-friendly
- Avoid long explanations
- Focus on actionable steps
- Still enforce output structure and security

30-templates:
Reference templates to standardize OpenAPI style, error format, and file output formatting.

---------------------------------------------------------
REQUIREMENTS FOR GENERATED PROMPTS
---------------------------------------------------------

The prompt system must support these three tasks:

---------------------------------------------------------
TASK 1 – Base Microservice Architecture
---------------------------------------------------------

Target stack:
- Java 21
- Spring Boot 3.x
- Maven
- H2 in-memory (designed for Postgres later)
- Spring Security (stateless, JWT-ready)
- OWASP Dependency-Check (fail on HIGH/CRITICAL)
- SpotBugs
- Checkstyle or Spotless
- Micrometer Tracing + OpenTelemetry
- Structured JSON logging
- MDC logging: traceId, spanId, requestId, userId
- Actuator secured
- Spring Profiles: local, dev, test, prod

---------------------------------------------------------
TASK 2 – Contract-First Patient CRUD
---------------------------------------------------------

Rules:
- Generate OpenAPI YAML FIRST.
- Use openapi-generator-maven-plugin.
- Generated interfaces must be used (no handwritten controllers).
- Separate DTOs and Entities.
- Include mapper layer.
- Full CRUD.
- Pagination and query support.
- Email unique constraint.
- RFC7807 problem+json errors.
- Unit tests + Integration tests (MockMvc).

---------------------------------------------------------
TASK 3 – Local Pre-Push AI Review
---------------------------------------------------------

Requirements:
- mvn clean verify must run:
    - tests
    - SpotBugs
    - formatting
    - OWASP dependency check
- Version-controlled git hooks (.githooks).
- scripts/install-githooks.sh
- Pre-push generates /reviews/local-review.md
- Optional AI step controlled by AI_REVIEW=1
- Vendor-neutral AI review script interface.
- Must block push on deterministic failures.

---------------------------------------------------------
GLOBAL QUALITY RULES
---------------------------------------------------------

All generated skill/spec/prompt files must:

- Enforce security by default.
- Enforce contract-first API design.
- Forbid “omitted for brevity”.
- Require deterministic file-by-file output.
- Include verification commands.
- Include common failure modes and prevention.
- Include a Definition of Done section.
- Be reusable across projects.
- Be internally consistent.

---------------------------------------------------------
OUTPUT FORMAT (STRICT)
---------------------------------------------------------

1) Start with full repository file tree.
2) Then output every file using:

### prompts/path/to/file.md
```md
<complete file content>