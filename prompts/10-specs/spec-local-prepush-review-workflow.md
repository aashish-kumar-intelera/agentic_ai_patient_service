# Spec: Local Pre-Push Review Workflow

**Layer:** 10-specs
**Version:** 1.0.0
**Date:** 2026-03-03
**Status:** Approved

---

## Scope

This specification defines the local developer pre-push quality review pipeline.
It covers the git hook mechanism, Maven verification pipeline, review report generation,
optional AI-assisted review, and developer onboarding requirements.

The goal is to catch all deterministic quality failures locally before code reaches
the remote repository or CI/CD pipeline.

---

## Non-Goals

- Does NOT replace CI/CD pipeline checks.
- Does NOT define CI/CD configuration (Jenkins, GitHub Actions, etc.).
- Does NOT define code review processes beyond the automated local check.
- Does NOT define deployment gates or production quality checks.
- Does NOT require specific AI provider integration.
- Does NOT block push based on non-deterministic AI review output.

---

## Assumptions

| ID | Assumption |
|----|-----------|
| A-01 | Developers use bash-compatible shell (macOS/Linux). |
| A-02 | Git 2.9+ is installed (supports `core.hooksPath`). |
| A-03 | Maven 3.9+ is installed and on PATH. |
| A-04 | Java 21 is installed and on PATH. |
| A-05 | `mvn clean verify` is the canonical quality check command. |
| A-06 | The `reviews/` directory is tracked in git but generated files are gitignored. |
| A-07 | AI review is optional and may not be configured on all machines. |

---

## Functional Requirements

### FR-01 – Hook Installation

| ID | Requirement |
|----|------------|
| FR-01-1 | A script `scripts/install-githooks.sh` MUST configure `git config core.hooksPath .githooks`. |
| FR-01-2 | The script MUST make `.githooks/pre-push` executable via `chmod +x`. |
| FR-01-3 | The script MUST print confirmation of the hooks path after installation. |
| FR-01-4 | Installation MUST be a one-time developer setup step documented in README. |
| FR-01-5 | Hooks MUST be version-controlled in `.githooks/` directory. |
| FR-01-6 | The `.git/hooks/` directory MUST NOT be used (not version-controllable). |

### FR-02 – Pre-Push Hook Execution

| ID | Requirement |
|----|------------|
| FR-02-1 | The `pre-push` hook MUST run on every `git push` command. |
| FR-02-2 | The hook MUST print progress to terminal (user feedback). |
| FR-02-3 | The hook MUST create the `reviews/` directory if it does not exist. |
| FR-02-4 | The hook MUST generate `reviews/local-review.md` regardless of pass/fail. |
| FR-02-5 | The hook MUST include the branch name and short commit SHA in the review file. |
| FR-02-6 | The hook MUST exit with code 1 on any deterministic failure (blocks push). |
| FR-02-7 | The hook MUST exit with code 0 on success (allows push). |

### FR-03 – Maven Quality Gate

| ID | Requirement |
|----|------------|
| FR-03-1 | The hook MUST run `mvn clean verify --batch-mode`. |
| FR-03-2 | `mvn clean verify` MUST include: unit tests, integration tests, SpotBugs, code formatting, OWASP. |
| FR-03-3 | Any failure in `mvn clean verify` MUST block the push. |
| FR-03-4 | The last 50 lines of Maven output MUST be included in the review file on failure. |
| FR-03-5 | SpotBugs MUST fail the build on HIGH or above findings. |
| FR-03-6 | OWASP Dependency-Check MUST fail the build on CVSS 7+ vulnerabilities. |
| FR-03-7 | Code formatting check MUST fail the build on any violations. |

### FR-04 – Review Report Generation

| ID | Requirement |
|----|------------|
| FR-04-1 | Review file MUST be written to `reviews/local-review.md`. |
| FR-04-2 | Review file MUST contain: timestamp, branch name, commit SHA. |
| FR-04-3 | Review file MUST contain: Maven build result (PASSED or FAILED). |
| FR-04-4 | On failure: review file MUST contain Maven output tail (last 50 lines). |
| FR-04-5 | Review file MUST contain: list of files changed since last commit. |
| FR-04-6 | Review file MUST be human-readable Markdown. |
| FR-04-7 | Review file MUST be excluded from git commits via `.gitignore`. |

### FR-05 – AI Review Integration

| ID | Requirement |
|----|------------|
| FR-05-1 | AI review MUST be disabled by default. |
| FR-05-2 | AI review MUST be enabled by setting `AI_REVIEW=1` environment variable. |
| FR-05-3 | `scripts/ai-review.sh` MUST be the vendor-neutral AI integration point. |
| FR-05-4 | The AI script MUST accept the review file path as argument `$1`. |
| FR-05-5 | The AI script MUST always exit 0 (never block push). |
| FR-05-6 | If AI script is absent, a notice MUST be written to the review file. |
| FR-05-7 | AI output MUST be appended to `reviews/local-review.md`. |
| FR-05-8 | The AI script interface MUST support at minimum: Claude CLI and REST API patterns. |

---

## Non-Functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-01 | Hook execution overhead (excluding Maven) | < 5 seconds |
| NFR-02 | Total pre-push time (including Maven) | < 5 minutes |
| NFR-03 | Hook must not hang | Timeout via `mvn` and script `set -euo pipefail` |
| NFR-04 | Hook must be idempotent | Running twice produces consistent result |
| NFR-05 | Review file format must be consistent across runs | Stable Markdown sections |

---

## Security Requirements

| ID | Requirement |
|----|------------|
| SEC-01 | Hook scripts MUST use `set -euo pipefail` to prevent silent failures. |
| SEC-02 | No credentials, API keys, or secrets MUST be hardcoded in scripts. |
| SEC-03 | AI review script MUST read credentials from environment variables only. |
| SEC-04 | Git diff output passed to AI MUST be bounded (max lines) to prevent token abuse. |
| SEC-05 | `reviews/local-review.md` MUST NOT contain authentication tokens or secrets. |

---

## Observability Requirements

| ID | Requirement |
|----|------------|
| OBS-01 | Terminal output MUST indicate each step completion with `[PASS]` or `[FAIL]`. |
| OBS-02 | Terminal output MUST show total duration of the pipeline. |
| OBS-03 | Review file serves as audit log for local quality checks. |

---

## File Structure Requirements

```
project-root/
├── .githooks/
│   └── pre-push              ← Executable bash script
├── scripts/
│   ├── install-githooks.sh   ← Sets core.hooksPath
│   └── ai-review.sh          ← Vendor-neutral AI integration
├── reviews/
│   ├── .gitkeep              ← Tracked
│   └── local-review.md       ← NOT tracked (gitignored)
└── .gitignore                ← Must include reviews/local-review.md
```

---

## .gitignore Requirements

The following MUST be in `.gitignore`:

```
# Pre-push review reports
reviews/local-review.md
reviews/*.md
!reviews/.gitkeep
```

---

## Developer Onboarding

The README MUST include:

```markdown
## Development Setup

### 1. Install Git Hooks (Required, one-time)

\`\`\`bash
bash scripts/install-githooks.sh
\`\`\`

### 2. Verify Installation

\`\`\`bash
git config core.hooksPath
# Expected output: .githooks
\`\`\`

### 3. Push with AI Review (Optional)

\`\`\`bash
AI_REVIEW=1 git push origin my-branch
\`\`\`

### 4. View Review Report

\`\`\`bash
cat reviews/local-review.md
\`\`\`
```

---

## Acceptance Criteria

| ID | Criterion | Verification |
|----|----------|-------------|
| AC-01 | `bash scripts/install-githooks.sh` sets `core.hooksPath` | `git config core.hooksPath` → `.githooks` |
| AC-02 | `git push` triggers pre-push hook | Hook output visible in terminal |
| AC-03 | Test failure blocks push | Introduce failing test → push rejected |
| AC-04 | SpotBugs HIGH finding blocks push | Introduce known bug → push rejected |
| AC-05 | OWASP HIGH CVE blocks push | Introduce vulnerable dependency → push rejected |
| AC-06 | `reviews/local-review.md` generated after push attempt | File exists with correct sections |
| AC-07 | `AI_REVIEW=1 git push` appends AI section to report | AI section present in review file |
| AC-08 | AI failure does NOT block push | Kill AI script → push proceeds |
| AC-09 | `reviews/local-review.md` not in `git status` | `.gitignore` entry effective |
| AC-10 | Clean build allows push | All checks pass → push succeeds |

---

## Definition of Done

- [ ] `.githooks/pre-push` exists, is executable, uses `set -euo pipefail`
- [ ] `scripts/install-githooks.sh` exists, sets `core.hooksPath`, confirms success
- [ ] `scripts/ai-review.sh` exists, always exits 0, accepts `$1` as review file path
- [ ] `reviews/` directory with `.gitkeep` committed
- [ ] `reviews/local-review.md` in `.gitignore`
- [ ] Hook generates review file with all required sections
- [ ] Hook blocks push on: test failure, SpotBugs HIGH, OWASP HIGH, formatting failure
- [ ] Hook allows push when all checks pass
- [ ] `AI_REVIEW=1` enables optional AI section
- [ ] README includes complete onboarding steps
- [ ] All 10 acceptance criteria pass

---

## Validation Checklist

```
□ .githooks/pre-push exists and is executable (chmod +x)
□ scripts/install-githooks.sh runs without error
□ git config core.hooksPath returns .githooks
□ reviews/.gitkeep is committed
□ reviews/local-review.md is in .gitignore
□ pre-push hook uses set -euo pipefail
□ Hook runs mvn clean verify with --batch-mode
□ Hook exits 1 on mvn failure
□ Hook exits 0 on mvn success
□ Hook generates reviews/local-review.md in both pass and fail cases
□ Review file contains: timestamp, branch, commit, status, changed files
□ AI_REVIEW=1 triggers ai-review.sh
□ ai-review.sh exits 0 even on error
□ README documents install-githooks.sh
□ All acceptance criteria verified manually
```
