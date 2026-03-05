# Template: Repository Output Format

**Layer:** 30-templates
**Type:** Reference template
**Scope:** All AI-generated code output in this repository

---

## Purpose

Defines the exact format that AI models must use when generating repository file content.
Reference this template when writing or reviewing prompts to ensure consistent, complete output.

---

## Output Structure

Every AI response generating files MUST follow this structure:

```
1. [Title and context — 1-3 sentences max]
2. File Tree (numbered, complete)
3. Files (numbered, in order, complete)
4. Verification section
```

---

## Section 1: File Tree Format

```markdown
## File Tree

1. pom.xml
2. .gitignore
3. src/main/java/com/example/Application.java
4. src/main/java/com/example/config/SecurityConfig.java
5. src/main/java/com/example/filter/MdcRequestFilter.java
6. src/main/java/com/example/exception/GlobalExceptionHandler.java
7. src/main/resources/application.yml
8. src/main/resources/application-local.yml
9. src/main/resources/application-dev.yml
10. src/main/resources/application-test.yml
11. src/main/resources/application-prod.yml
12. src/main/resources/logback-spring.xml
13. src/main/resources/db/migration/V1__baseline.sql
14. src/test/java/com/example/ApplicationIT.java
```

Rules:
- Number every file.
- Use full path from project root.
- Include ALL files — no "etc." or "similar files not listed".
- Order: configuration files first, then by package depth.

---

## Section 2: File Content Format

Each file uses this exact format:

````markdown
### {N}. {path/to/File.ext}

```{language}
{complete file content}
```
````

### Language identifiers

| Extension | Language identifier |
|-----------|-------------------|
| `.java` | `java` |
| `.xml` (pom) | `xml` |
| `.yml` / `.yaml` | `yaml` |
| `.sql` | `sql` |
| `.md` | `markdown` |
| `.sh` | `bash` |
| `.json` | `json` |
| `.properties` | `properties` |
| `.gitignore` | `gitignore` |
| `Dockerfile` | `dockerfile` |

### File header comment (mandatory for Java files)

Every Java file MUST begin with a package declaration followed by imports.
No copyright header required unless specified.

```java
package com.example.config;

import org.springframework.context.annotation.Bean;
// all other imports
```

### Decision comments (when applicable)

If a non-obvious design decision is made, document it with a single comment:

```java
// [DECISION] Using OncePerRequestFilter instead of HandlerInterceptor to ensure
// MDC is set before Spring Security's filter chain evaluates the request.
```

Place the decision comment at the top of the file, before the class declaration.

---

## Section 3: Verification Format

Every output MUST end with this section:

```markdown
## Verification

### Build Verification
```bash
mvn clean verify
# Expected: BUILD SUCCESS
```

### Smoke Test Commands
```bash
# Start the application
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Test health endpoint
curl -s http://localhost:8080/actuator/health | jq .

# Test authentication (expect 401)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/patients

# Test with token (expect 200 or relevant response)
curl -s -H "Authorization: Bearer {test-token}" http://localhost:8080/api/patients | jq .
```

### Completeness Checklist
- [ ] All {N} files from the file tree have been output
- [ ] No file contains "omitted for brevity" or equivalent
- [ ] All methods are fully implemented
- [ ] All imports are present
- [ ] No TODO, FIXME, or placeholder values
- [ ] No hardcoded secrets or passwords
- [ ] Build command: `mvn clean verify`
```

---

## Prohibited Output Patterns

These phrases and patterns INVALIDATE the output and require regeneration:

### Forbidden Phrases

```
// omitted for brevity
// ... rest of the implementation
// similar to above
// TODO: implement this
// FIXME:
// left as an exercise
// you can add more methods here
// etc.
// similar methods follow the same pattern
```

### Forbidden Code Patterns

```java
// FORBIDDEN: Unimplemented method
@Override
public PatientResponse createPatient(PatientRequest request) {
    // TODO: implement
    return null;
}

// FORBIDDEN: Placeholder value
jwt:
  secret: your-jwt-secret-here

// FORBIDDEN: Incomplete switch/if
if (status == PatientStatus.ACTIVE) {
    // handle active
}
// (missing INACTIVE handling)
```

### Forbidden Structural Patterns

```markdown
// FORBIDDEN: Referencing a file without outputting it
See `SecurityConfig.java` for the security configuration.

// FORBIDDEN: Abbreviated file list
1. pom.xml
2. Application.java
3. ... (other files similar to above)

// FORBIDDEN: Skipping test files
[Note: Test files follow the same pattern as above and are not shown here]
```

---

## File Content Quality Rules

### Java Files Must

- Declare the correct `package` on line 1.
- Include all necessary `import` statements (no star imports except static for test assertions).
- Implement every method body completely.
- Use proper Spring Boot 3 / Java 21 APIs.
- Include `@Override` on all interface method implementations.
- Not use raw types (use generics).
- Use `final` on fields where appropriate.

### YAML Files Must

- Use 2-space indentation consistently.
- Wrap string values containing special characters in quotes.
- Use `${ENV_VAR}` syntax for externalized values.
- Use `${ENV_VAR:default}` when a safe default exists for development.
- Not contain trailing whitespace.

### SQL Files Must

- Use `IF NOT EXISTS` for `CREATE TABLE` statements.
- Define all NOT NULL constraints explicitly.
- Include all indexes mentioned in specifications.
- End each statement with `;`.
- Use ANSI SQL where possible (H2 + PostgreSQL compatible).

### Shell Scripts Must

- Start with `#!/usr/bin/env bash`.
- Use `set -euo pipefail` on line 2.
- Use `IFS=$'\n\t'` for safe word splitting.
- Quote all variable references: `"$VAR"` not `$VAR`.
- Use `command -v` to check for tools, not `which`.
- Have explicit exit codes.

---

## Multi-File Output Ordering

When generating multiple files, follow this order:

1. Build files (`pom.xml`, `build.gradle`)
2. Configuration files (`.gitignore`, `README.md`)
3. Main application entry point (`Application.java`)
4. Configuration classes (`config/`)
5. Security and filters (`config/Security*`, `filter/`)
6. Domain layer (`domain/`, `repository/`)
7. Business layer (`service/`)
8. API layer (`controller/`, `api/`)
9. Exception handlers (`exception/`)
10. Resources (`application.yml`, profile yamls, `logback-spring.xml`)
11. Database migrations (`db/migration/`)
12. Test files (mirror the main source order)

---

## Incremental Output Pattern

When adding to an existing project (not generating from scratch):

```markdown
## Changes to Existing Files

### pom.xml — Add the following dependency

```xml
<!-- Add inside <dependencies> -->
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.4.0</version>
</dependency>
```

### pom.xml — Add the following plugin

```xml
<!-- Add inside <build><plugins> -->
<plugin>
    ...complete plugin definition...
</plugin>
```

## New Files

[proceed with new files as above]
```

Rules for incremental output:
- NEVER say "add the following to GlobalExceptionHandler" without showing the COMPLETE updated file.
- For small additions (< 5 lines) to existing files, showing only the addition with clear context is acceptable.
- For significant changes, always output the COMPLETE updated file.

---

## Example of Correct Output

```markdown
## File Tree

1. src/main/java/com/example/domain/Patient.java
2. src/main/java/com/example/repository/PatientRepository.java
3. src/main/resources/db/migration/V2__create_patients_table.sql

### 1. src/main/java/com/example/domain/Patient.java

```java
package com.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    // ... all other fields fully declared ...

    @Version
    private Long version;

    // Constructors, getters, setters, equals, hashCode
    // [all fully implemented]
}
```

[continues with all files...]

## Verification

```bash
mvn clean verify
# Expected: BUILD SUCCESS
```

- [x] All 3 files output
- [x] No truncation
- [x] All imports present
- [x] No TODO/FIXME
```

---

## Checklist for Prompt Authors

Use this when writing new prompts to ensure they will produce compliant output:

```
□ Prompt includes explicit "no omitted for brevity" rule
□ Prompt specifies "output file tree first"
□ Prompt specifies "output each file numbered and in full"
□ Prompt includes "no TODO/FIXME/placeholder" rule
□ Prompt ends with explicit verification section requirement
□ Prompt references skill-global-output-contract.md
□ File list in prompt is exhaustive (no "and similar files")
□ Prompt states the complete list of required tests
```
