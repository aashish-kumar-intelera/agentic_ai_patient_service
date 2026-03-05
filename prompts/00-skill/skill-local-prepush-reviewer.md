# Skill: Local Pre-Push Reviewer

**Layer:** 00-skill
**Type:** Development workflow contract
**Scope:** Local development pre-push quality gate

---

## Purpose

Defines the mandatory local pre-push review pipeline that runs before any code reaches
the remote repository. This pipeline ensures consistent code quality, security compliance,
and test passage without relying on CI/CD as the first gate.

---

## Pipeline Overview

```
git push
    │
    ▼
.githooks/pre-push
    │
    ├── [1] mvn clean verify
    │       ├── Unit tests (maven-surefire-plugin)
    │       ├── Integration tests (maven-failsafe-plugin)
    │       ├── SpotBugs check
    │       ├── Code formatting check
    │       └── OWASP Dependency-Check
    │
    ├── [2] Generate /reviews/local-review.md
    │       ├── Test results summary
    │       ├── SpotBugs findings
    │       ├── Dependency vulnerabilities
    │       └── Changed files list
    │
    ├── [3] AI Review (optional: AI_REVIEW=1)
    │       └── Append AI analysis to local-review.md
    │
    └── [4] Gate decision
            ├── FAIL → block push, print failure reason
            └── PASS → allow push to proceed
```

---

## Git Hooks Setup

### Version-Controlled Hooks Directory

All hooks are stored in `.githooks/` at the repository root.
Never store hooks in `.git/hooks/` (not version-controlled).

Directory structure:
```
.githooks/
└── pre-push

scripts/
└── install-githooks.sh

reviews/
└── .gitkeep           ← reviews/ tracked, generated files not committed
```

### .githooks/pre-push

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REVIEW_FILE="$PROJECT_ROOT/reviews/local-review.md"
EXIT_CODE=0

echo "=================================================="
echo "  Pre-push quality gate"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=================================================="

# Initialize review file
mkdir -p "$PROJECT_ROOT/reviews"
cat > "$REVIEW_FILE" << HEADER
# Local Pre-Push Review
**Generated:** $(date '+%Y-%m-%d %H:%M:%S')
**Branch:** $(git rev-parse --abbrev-ref HEAD)
**Commit:** $(git rev-parse --short HEAD)

HEADER

# Step 1: Maven build and verification
echo ""
echo "[1/3] Running mvn clean verify..."
echo "## Build & Verify" >> "$REVIEW_FILE"

if mvn clean verify --batch-mode --no-transfer-progress 2>&1 | tee /tmp/mvn-output.txt; then
    echo "**Status:** PASSED" >> "$REVIEW_FILE"
    echo "[PASS] mvn clean verify"
else
    echo "**Status:** FAILED" >> "$REVIEW_FILE"
    echo "" >> "$REVIEW_FILE"
    echo "### Failure Output" >> "$REVIEW_FILE"
    echo '```' >> "$REVIEW_FILE"
    tail -50 /tmp/mvn-output.txt >> "$REVIEW_FILE"
    echo '```' >> "$REVIEW_FILE"
    echo "[FAIL] mvn clean verify — push blocked"
    EXIT_CODE=1
fi

# Step 2: Changed files summary
echo ""
echo "[2/3] Generating changed files summary..."
echo "" >> "$REVIEW_FILE"
echo "## Changed Files" >> "$REVIEW_FILE"
git diff --name-only HEAD~1 HEAD 2>/dev/null >> "$REVIEW_FILE" || \
  git diff --name-only $(git rev-parse HEAD) 2>/dev/null >> "$REVIEW_FILE" || \
  echo "No previous commit (first push)" >> "$REVIEW_FILE"

# Step 3: AI Review (optional)
echo "" >> "$REVIEW_FILE"
if [ "${AI_REVIEW:-0}" = "1" ]; then
    echo "[3/3] Running AI review..."
    echo "## AI Review" >> "$REVIEW_FILE"
    if [ -f "$PROJECT_ROOT/scripts/ai-review.sh" ]; then
        bash "$PROJECT_ROOT/scripts/ai-review.sh" "$REVIEW_FILE" 2>&1 | tee -a "$REVIEW_FILE"
    else
        echo "_AI review script not found at scripts/ai-review.sh_" >> "$REVIEW_FILE"
    fi
else
    echo "[3/3] AI review skipped (set AI_REVIEW=1 to enable)"
    echo "## AI Review" >> "$REVIEW_FILE"
    echo "_Skipped. Run with \`AI_REVIEW=1 git push\` to enable._" >> "$REVIEW_FILE"
fi

echo ""
echo "Review written to: $REVIEW_FILE"
echo "=================================================="

if [ $EXIT_CODE -ne 0 ]; then
    echo "PUSH BLOCKED — Fix failures before pushing."
    echo "=================================================="
fi

exit $EXIT_CODE
```

### scripts/install-githooks.sh

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/.githooks"

echo "Installing git hooks from $HOOKS_DIR..."

git config core.hooksPath "$HOOKS_DIR"
chmod +x "$HOOKS_DIR"/pre-push

echo "Git hooks installed successfully."
echo "Hooks directory: $(git config core.hooksPath)"
echo ""
echo "To enable AI review on push:"
echo "  AI_REVIEW=1 git push"
```

---

## AI Review Script Interface

`scripts/ai-review.sh` is vendor-neutral. The interface contract:

```bash
#!/usr/bin/env bash
# Input: $1 = path to review file (for context)
# Output: AI review text appended to review file or printed to stdout
# Exit code: 0 = success, non-zero = skip (do not block push)

REVIEW_FILE="${1:-/dev/null}"

# Example: Claude AI integration
if command -v claude &>/dev/null; then
    git diff HEAD~1 HEAD | claude --print \
        "Review this diff for security issues, code quality problems, and bugs. Be concise. Output markdown."
elif [ -n "${OPENAI_API_KEY:-}" ]; then
    # OpenAI integration example
    DIFF=$(git diff HEAD~1 HEAD | head -200)
    # ... curl OpenAI API ...
else
    echo "_No AI provider configured. Set up claude CLI or OPENAI_API_KEY._"
fi
```

**AI review MUST NOT block push.** Only deterministic failures (test failures, SpotBugs, OWASP) block.

---

## reviews/ Directory Rules

```
reviews/
├── .gitkeep           ← commit this
└── local-review.md    ← DO NOT commit (add to .gitignore)
```

`.gitignore` entry:
```
reviews/local-review.md
reviews/*.md
!reviews/.gitkeep
```

---

## mvn clean verify Requirements

The following MUST run during `mvn clean verify`:

| Phase | Plugin | Failure Behavior |
|-------|--------|-----------------|
| `test` | maven-surefire-plugin | Fail on any test failure |
| `integration-test` | maven-failsafe-plugin | Fail on any IT failure |
| `verify` | spotbugs-maven-plugin | Fail on HIGH+ findings |
| `verify` | spotless/checkstyle | Fail on style violations |
| `verify` | dependency-check-maven | Fail on CVSS 7+ |

No plugin may be configured to skip in default builds.
Skipping is only allowed via explicit Maven property with documentation.

---

## Gate Criteria

| Check | Gate Type | Blocks Push |
|-------|-----------|------------|
| Unit tests passing | Deterministic | Yes |
| Integration tests passing | Deterministic | Yes |
| SpotBugs HIGH findings | Deterministic | Yes |
| Code formatting | Deterministic | Yes |
| OWASP CVE HIGH/CRITICAL | Deterministic | Yes |
| AI review | Non-deterministic | Never |

---

## Setup Instructions (Developer Onboarding)

```bash
# Clone repository
git clone <repo-url>
cd <repo>

# Install hooks (one-time setup)
bash scripts/install-githooks.sh

# Verify installation
git config core.hooksPath
# Expected: .githooks

# Test the pre-push hook manually
bash .githooks/pre-push

# Push with AI review enabled
AI_REVIEW=1 git push origin feature/my-branch
```

---

## Definition of Done

- [ ] `.githooks/pre-push` exists and is executable
- [ ] `scripts/install-githooks.sh` exists and configures `core.hooksPath`
- [ ] `reviews/` directory exists with `.gitkeep`
- [ ] `reviews/local-review.md` is in `.gitignore`
- [ ] `mvn clean verify` runs all: tests, SpotBugs, formatting, OWASP
- [ ] Push is blocked on any deterministic failure
- [ ] AI review does NOT block push (exit code always 0 from AI step)
- [ ] `AI_REVIEW=1` flag controls AI step
- [ ] `scripts/ai-review.sh` is vendor-neutral
- [ ] README includes developer onboarding instructions

---

## Common Failure Modes

| Failure | Prevention |
|---------|------------|
| Hooks not executable | `chmod +x .githooks/*` in install script |
| `core.hooksPath` not set | install script sets it; README documents it |
| AI review blocks push on API failure | AI script always exits 0 |
| OWASP check downloads NVD data (slow) | Cache NVD data in CI; use `--noupdate` when cached |
| Review file not generated on failure | Write partial review before gate check |
| Developer bypasses hook with `--no-verify` | CI/CD pipeline catches what was bypassed |
