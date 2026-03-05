# AI Prompt Repository

Production-grade AI prompt library for enterprise Java microservice development.
Supports both **Claude AI** (long-form reasoning) and **GitHub Copilot** (IDE-inline).

---

## Repository Structure

```
prompts/
├── README.md                          ← This file
│
├── 00-skill/                          ← Reusable behavioral rules (model-agnostic)
│   ├── skill-global-output-contract.md
│   ├── skill-java-spring-microservice.md
│   ├── skill-contract-first-openapi.md
│   ├── skill-testing-java.md
│   ├── skill-security-owasp.md
│   ├── skill-observability.md
│   └── skill-local-prepush-reviewer.md
│
├── 10-specs/                          ← Spec Kit requirement documents
│   ├── spec-microservice-base-architecture.md
│   ├── spec-user-story-patient-crud.md
│   └── spec-local-prepush-review-workflow.md
│
├── 20-prompts-claude/                 ← Full-length Claude AI prompts
│   ├── prompt-01-generate-microservice-architecture.md
│   ├── prompt-02-implement-patient-crud-contract-first.md
│   └── prompt-03-setup-local-prepush-review.md
│
├── 21-prompts-copilot/               ← Concise GitHub Copilot prompts
│   ├── copilot-01-generate-microservice-architecture.md
│   ├── copilot-02-implement-patient-crud-contract-first.md
│   └── copilot-03-setup-local-prepush-review.md
│
└── 30-templates/                      ← Reference templates
    ├── template-openapi-style-guide.md
    ├── template-problem-json-errors.md
    └── template-repo-output-format.md
```

---

## Layer Purposes

| Layer | Purpose |
|-------|---------|
| `00-skill` | Reusable behavioral rules and output contracts. Model-agnostic. Enforce deterministic output. |
| `10-specs` | Spec Kit style requirement documents with scope, FRs, NFRs, security, acceptance criteria. |
| `20-prompts-claude` | Long-form strict prompts with reasoning, verification, anti-hallucination rules. |
| `21-prompts-copilot` | Concise IDE-friendly prompts with identical quality bar. |
| `30-templates` | Reference templates for OpenAPI style, error format, and file output formatting. |

---

## Supported Tasks

### Task 1 – Base Microservice Architecture
Generates a Java 21 + Spring Boot 3.x production microservice scaffold with:
- Maven multi-module project
- H2 (designed for Postgres migration)
- Spring Security (stateless, JWT-ready)
- OWASP Dependency-Check
- SpotBugs, Checkstyle/Spotless
- Micrometer Tracing + OpenTelemetry
- Structured JSON logging with MDC
- Spring Profiles: local, dev, test, prod

### Task 2 – Contract-First Patient CRUD
Implements Patient CRUD using OpenAPI-first approach:
- OpenAPI YAML generated first
- openapi-generator-maven-plugin
- RFC7807 problem+json error handling
- Pagination, email uniqueness
- Unit + Integration tests

### Task 3 – Local Pre-Push AI Review
Implements a pre-push code review pipeline:
- `mvn clean verify` gate (tests, SpotBugs, formatting, OWASP)
- Version-controlled `.githooks`
- Generates `/reviews/local-review.md`
- Optional AI review via `AI_REVIEW=1`
- Blocks push on deterministic failures

---

## How to Use

### Using with Claude AI
1. Open a Claude conversation.
2. Copy the relevant `20-prompts-claude/prompt-XX-*.md` file content.
3. Paste as your first message.
4. Claude will generate complete, file-by-file output.

### Using with GitHub Copilot
1. Open your IDE with Copilot enabled.
2. Create a new file or open Copilot Chat.
3. Copy the relevant `21-prompts-copilot/copilot-XX-*.md` content.
4. Use as a Copilot Chat prompt.

### Composing with Skills
Each prompt references skills from `00-skill/`. You can compose custom prompts by selecting the relevant skill files as context.

---

## Global Quality Rules

All prompts enforce:
- Security by default (OWASP, input validation, least privilege)
- Contract-first API design (OpenAPI YAML before code)
- No "omitted for brevity" — complete file output required
- Deterministic file-by-file output format
- Verification commands for every step
- Common failure modes and prevention guidance
- Definition of Done for every task

---

## Version

Generated: 2026-03-03
Stack: Java 21, Spring Boot 3.x, Maven, OpenAPI 3.1
