# Skill: Global Output Contract

**Layer:** 00-skill
**Type:** Behavioral contract
**Scope:** All AI-generated code and file output
**Model compatibility:** Claude AI, GitHub Copilot, any LLM

---

## Purpose

This skill defines the mandatory output contract for ALL AI-generated content in this repository.
Every prompt that references this skill MUST comply with all rules below.
No exceptions. No partial compliance.

---

## Output Format Rules

### Rule 1 – File-by-File Output

- Output every file completely, one at a time.
- Each file must begin with a header comment block identifying its path.
- Do NOT group multiple files into a single code block.
- Do NOT output partial files.

Format for each file:

```
### path/to/File.java
```java
<complete file content — no placeholders>
```
```

### Rule 2 – No "Omitted for Brevity"

The following phrases are FORBIDDEN in output:

- "omitted for brevity"
- "// ... existing code ..."
- "// rest of the implementation"
- "similar to above"
- "etc."
- "you can implement this yourself"
- "left as an exercise"

If a file is referenced, it MUST be fully generated.
If a method is declared, it MUST be fully implemented.

### Rule 3 – Deterministic File Tree First

Before outputting any file content:
1. Output the complete file tree for the task.
2. Number each file.
3. Then output each file in numbered order.

Format:

```
## File Tree

1. pom.xml
2. src/main/java/com/example/Application.java
3. src/main/resources/application.yml
...

## Files

### 1. pom.xml
```xml
<complete content>
```

### 2. src/main/java/com/example/Application.java
```java
<complete content>
```
```

### Rule 4 – No Placeholder Values

The following are FORBIDDEN:

- `TODO`
- `FIXME`
- `your-value-here`
- `<placeholder>`
- `example.com` used as a real domain
- Hardcoded secrets or passwords in non-test files

Use environment variable references instead:
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
```

### Rule 5 – Completeness Verification

After outputting all files, you MUST output a verification section:

```
## Verification

- [ ] All files in the file tree have been output
- [ ] No file contains "omitted for brevity" or equivalents
- [ ] All methods are fully implemented
- [ ] All imports are present
- [ ] No placeholder values exist
- [ ] Build command to verify: `mvn clean verify`
```

---

## Anti-Hallucination Rules

### Rule H-1 – Do Not Invent APIs

Only use APIs, annotations, and classes that exist in the stated dependency versions.
If unsure whether an API exists, use the most conservative known alternative.

### Rule H-2 – Do Not Invent Configuration Properties

Only use Spring Boot configuration properties documented in the stated Spring Boot version.
Do NOT invent property names.

### Rule H-3 – Version Pinning

All dependencies MUST include explicit version numbers or be managed by the Spring Boot BOM.
Never use `LATEST` or `RELEASE` as a version.

### Rule H-4 – No Silent Assumptions

If a requirement is ambiguous:
1. State the assumption explicitly.
2. Label it as `[ASSUMPTION]`.
3. Proceed with the stated assumption.

Example:
```
[ASSUMPTION] Database schema migration uses Flyway.
If Liquibase is preferred, replace flyway-core with liquibase-core
and update the migration file format accordingly.
```

### Rule H-5 – Conflict Detection

Before generating output, verify:
- No two files define the same class
- No two beans would have the same bean name
- No circular dependency would be introduced
- Package names are consistent across all files

---

## Definition of Done

A task output is DONE when:

- [ ] All files in the declared file tree are present and complete
- [ ] Zero placeholder text exists in any file
- [ ] All imports compile (no missing imports)
- [ ] Build command succeeds: `mvn clean verify`
- [ ] Tests are present and would pass
- [ ] Security rules from `skill-security-owasp.md` are applied
- [ ] Observability rules from `skill-observability.md` are applied
- [ ] Verification checklist is included at the end

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| Partial file output | Always complete every file before starting the next |
| Missing imports | Scan each class for unqualified references before output |
| Invented Spring properties | Cross-reference Spring Boot docs for stated version |
| Bean name collisions | Use explicit `@Qualifier` or distinct bean names |
| Broken file tree | Output tree first, then verify all tree entries are output |
| Test files omitted | Tests are mandatory; list them in the file tree |
| Security skipped | Apply security skill before finalizing any output |
