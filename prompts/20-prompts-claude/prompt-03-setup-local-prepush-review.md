# Claude Prompt 03: Setup Local Pre-Push AI Review

**Layer:** 20-prompts-claude
**Task:** TASK 3 – Local Pre-Push AI Review
**AI Target:** Claude AI (claude-sonnet-4-6 or claude-opus-4-6)
**Skills:** skill-global-output-contract, skill-local-prepush-reviewer
**Prerequisite:** Prompt 01 (Base Architecture) must be completed first.

---

## How to Use This Prompt

1. Complete Prompt 01 first — this prompt adds tooling to an existing project.
2. Copy everything in the "PROMPT START" section below.
3. Paste as your first message in a new Claude conversation.
4. After Claude generates output, run `bash scripts/install-githooks.sh` to activate.

---

## PROMPT START

---

You are a Senior DevOps and Platform Engineer setting up a local pre-push quality review
pipeline for a Java Spring Boot microservice project named `patient-service`.

## ABSOLUTE RULES

1. Every script must be output completely. Zero truncation.
2. "Omitted for brevity" and all equivalents are FORBIDDEN.
3. Every script must use `set -euo pipefail` for safety.
4. No hardcoded credentials or API keys in any script.
5. All credentials must come from environment variables.
6. Output the file tree first, then each file in full order.
7. All bash scripts must include shebang: `#!/usr/bin/env bash`

---

## Task: Local Pre-Push Review Pipeline

Generate a complete local pre-push review pipeline for the `patient-service` project.

---

## Required Files

Generate exactly these files:

```
project-root/
├── .githooks/
│   └── pre-push
├── scripts/
│   ├── install-githooks.sh
│   └── ai-review.sh
├── reviews/
│   └── .gitkeep
└── .gitignore             ← ADD entries (do not replace existing content)
    └── (add reviews/*.md, !reviews/.gitkeep)
```

---

## Detailed Requirements

### .githooks/pre-push

This is the main gate script. Must implement:

**Header:**
- `#!/usr/bin/env bash`
- `set -euo pipefail`
- `IFS=$'\n\t'`
- Variable declarations: `SCRIPT_DIR`, `PROJECT_ROOT`, `REVIEW_FILE`, `EXIT_CODE=0`
- Banner print: script name, timestamp, branch name, commit SHA

**Review File Initialization:**
- `mkdir -p "$PROJECT_ROOT/reviews"`
- Create `reviews/local-review.md` with header:
  - Title: "# Local Pre-Push Review Report"
  - Metadata: date, time, branch, commit SHA, author

**Gate 1: Maven Build & Verify**

```
echo "[1/4] Running mvn clean verify..."
```

- Run: `mvn clean verify --batch-mode --no-transfer-progress`
- Capture stdout+stderr to `/tmp/mvn-output.txt` AND display in terminal
- On SUCCESS:
  - Write `## Build & Verify — PASSED` to review file
  - Print `[PASS] mvn clean verify`
- On FAILURE:
  - Write `## Build & Verify — FAILED` to review file
  - Append last 100 lines of Maven output to review file in fenced code block
  - Print `[FAIL] mvn clean verify`
  - Set `EXIT_CODE=1`
  - Do NOT exit early — continue generating the review report

**Gate 2: Test Results Summary**

```
echo "[2/4] Extracting test results..."
```

- Parse Maven Surefire XML reports in `target/surefire-reports/` (if they exist)
- Count: tests run, failures, errors, skipped
- Write `## Test Results` section to review file
- Format: table or summary line

If Surefire reports not present (build failed before tests):
- Write "Test results not available — build failed before test phase"

**Gate 3: Changed Files Summary**

```
echo "[3/4] Generating changed files summary..."
```

- List files changed since last commit:
  - Try: `git diff --name-only HEAD~1 HEAD 2>/dev/null`
  - Fallback: `git status --porcelain 2>/dev/null | awk '{print $2}'`
  - Fallback: "No previous commit or diff unavailable"
- Write `## Changed Files` section to review file
- Count total changed files

**Gate 4: AI Review (Optional)**

```
echo "[4/4] AI Review..."
```

- Check: `if [ "${AI_REVIEW:-0}" = "1" ]`
- If enabled:
  - Check if `scripts/ai-review.sh` exists
  - If exists: run `bash "$PROJECT_ROOT/scripts/ai-review.sh" "$REVIEW_FILE"`; capture exit code (but never fail on it)
  - If not exists: write notice to review file
- If disabled:
  - Write `## AI Review — Skipped` section
  - Include: "Run with `AI_REVIEW=1 git push` to enable"

**Footer:**
- Print review file path
- Print total duration: calculate from start time
- Print gate summary table:
  ```
  Gate 1: mvn clean verify    [PASS/FAIL]
  Gate 2: Test results        [PARSED/UNAVAILABLE]
  Gate 3: Changed files       [N files]
  Gate 4: AI Review           [ENABLED/SKIPPED]
  ```
- If EXIT_CODE=1: print "PUSH BLOCKED. Fix failures above."
- If EXIT_CODE=0: print "All gates passed. Push proceeding."
- `exit $EXIT_CODE`

---

### scripts/install-githooks.sh

Must implement:
- `#!/usr/bin/env bash`
- `set -euo pipefail`
- Detect project root (directory of the script's parent)
- Verify `.githooks/` directory exists
- Run: `git config core.hooksPath .githooks`
- Run: `chmod +x .githooks/pre-push`
- Verify: read back `git config core.hooksPath` and compare to `.githooks`
- Print success message with:
  - Hooks path
  - How to test: `bash .githooks/pre-push`
  - How to enable AI review: `AI_REVIEW=1 git push`
  - Note: run this once per clone

---

### scripts/ai-review.sh

This script is vendor-neutral. Must implement:

**Interface contract:**
- Accept `$1` as the path to the review file (context, optional read)
- ALL output goes to stdout (caller appends to review file)
- ALWAYS exit 0 — never block push
- On error: print notice and exit 0

**Provider detection (in priority order):**

1. **Claude CLI** (if `claude` command available on PATH):
```bash
if command -v claude &>/dev/null; then
    DIFF_CONTENT=$(git diff HEAD~1 HEAD 2>/dev/null | head -300 || echo "No diff available")
    claude --print "You are a senior code reviewer. Review this git diff for:
    1. Security vulnerabilities
    2. Java best practices violations
    3. Missing error handling
    4. Performance issues
    5. Test coverage gaps

    Be concise. Output in markdown with sections. Max 500 words.

    Diff:
    $DIFF_CONTENT" 2>/dev/null || echo "_Claude CLI review failed — continuing_"
fi
```

2. **OpenAI API** (if `OPENAI_API_KEY` is set):
```bash
elif [ -n "${OPENAI_API_KEY:-}" ]; then
    DIFF_CONTENT=$(git diff HEAD~1 HEAD 2>/dev/null | head -200 || echo "No diff")
    # curl to OpenAI completions API with bounded diff content
    # ... full implementation ...
fi
```

3. **Fallback:**
```bash
else
    echo "_No AI provider configured._"
    echo "_Available providers: Claude CLI (install claude), OpenAI (set OPENAI_API_KEY)_"
fi
```

Include proper error handling around each provider (try/catch equivalent in bash).

---

## Additional Files to Update

### .gitignore additions

Add to the existing `.gitignore`:

```
# Pre-push review reports
reviews/local-review.md
reviews/*.md
!reviews/.gitkeep

# Maven wrapper (optional)
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.DS_Store
```

### reviews/.gitkeep

Empty file. Just `touch reviews/.gitkeep`.

---

## README Section to Generate

Generate a complete section to be added to the project README:

```markdown
## Development Quality Gate

### Setup (Required — one time per clone)

\`\`\`bash
bash scripts/install-githooks.sh
\`\`\`

Verify:
\`\`\`bash
git config core.hooksPath
# → .githooks
\`\`\`

### What Happens on Push

Every \`git push\` runs:
1. \`mvn clean verify\` (tests + SpotBugs + formatting + OWASP)
2. Test results extracted
3. Changed files listed
4. (Optional) AI code review

Push is **blocked** on any build failure.
Push is **never blocked** by AI review.

### Enable AI Review

\`\`\`bash
AI_REVIEW=1 git push origin my-branch
\`\`\`

### View Last Review Report

\`\`\`bash
cat reviews/local-review.md
\`\`\`

### Bypass Hook (Emergency Only)

\`\`\`bash
git push --no-verify
\`\`\`

> **Warning:** Bypassing the hook skips all quality gates. CI/CD will still catch failures.
```

---

## Reasoning Instructions

Before generating each script, explain:
1. Why each gate is ordered as it is.
2. Why AI review is gate 4 (last) and non-blocking.
3. How bash error handling with `set -euo pipefail` affects the script logic.
4. How `EXIT_CODE` accumulation differs from early-exit strategy.

Label decisions with `[DECISION]: explanation`.

---

## Anti-Hallucination Rules

- `git config core.hooksPath` is the correct command (not `git hooks.path`).
- `set -e` means any command failing exits the script — use `|| true` or capture exit codes explicitly.
- `AI_REVIEW=1 git push` passes env var to the current shell — this works in bash.
- `command -v claude` is the correct way to check for a command on PATH.
- Maven `--batch-mode --no-transfer-progress` suppresses progress bars in CI/scripts.
- Surefire XML reports are in `target/surefire-reports/*.xml` — parse with `grep` or `xmllint`.
- `tee /tmp/file` writes to both stdout and file simultaneously.
- `2>&1` redirects stderr to stdout — needed to capture Maven errors.

---

## Output Format

Start with complete file tree (numbered). Then each file in full. End with verification:

```
## Verification

Setup steps:
1. bash scripts/install-githooks.sh
2. git config core.hooksPath   → should be .githooks
3. bash .githooks/pre-push      → manual test run
4. cat reviews/local-review.md → review report

Gates:
- [ ] pre-push script uses set -euo pipefail
- [ ] install-githooks.sh sets core.hooksPath
- [ ] ai-review.sh always exits 0
- [ ] reviews/local-review.md in .gitignore
- [ ] reviews/.gitkeep committed
- [ ] All scripts have #!/usr/bin/env bash
- [ ] No hardcoded credentials in scripts
```

---

## PROMPT END
