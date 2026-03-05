# Copilot Prompt 03: Local Pre-Push Review Pipeline

**Layer:** 21-prompts-copilot
**Task:** TASK 3 – Local Pre-Push AI Review
**AI Target:** GitHub Copilot Chat (IDE-inline)
**Quality bar:** Identical to Claude prompt 03
**Prerequisite:** Copilot Prompt 01 completed (base project exists)

---

## Usage

Paste into GitHub Copilot Chat with the project open in the IDE.

---

## PROMPT

Add a local pre-push quality gate pipeline to the existing `patient-service` project.

**Rules:**
- Every script complete — no truncation, no "omitted for brevity"
- Every bash script uses `set -euo pipefail` and `#!/usr/bin/env bash`
- No hardcoded credentials — env vars only
- Output file tree first, then each file numbered

---

### Files to Generate

```
.githooks/pre-push
scripts/install-githooks.sh
scripts/ai-review.sh
reviews/.gitkeep
(update .gitignore)
```

---

### .githooks/pre-push

Bash script that:

1. **Init:** Set `EXIT_CODE=0`. Record start time. Print banner with timestamp, branch, commit.

2. **Create review file:** `reviews/local-review.md`
   - Header: title, date, branch, commit SHA, author
   - Sections appended by each gate

3. **Gate 1 — `mvn clean verify`:**
   - Run: `mvn clean verify --batch-mode --no-transfer-progress 2>&1 | tee /tmp/mvn-output.txt`
   - Pass: append `## Build & Verify — PASSED` to review file; print `[PASS]`
   - Fail: append `## Build & Verify — FAILED` + last 100 lines of `/tmp/mvn-output.txt`; print `[FAIL]`; set `EXIT_CODE=1`
   - Do NOT exit early on failure — continue to generate full report

4. **Gate 2 — Test results:**
   - Parse `target/surefire-reports/*.xml` if they exist
   - Count tests/failures/errors/skipped
   - Append `## Test Results` table to review file
   - If no reports: note "build failed before test phase"

5. **Gate 3 — Changed files:**
   - `git diff --name-only HEAD~1 HEAD 2>/dev/null` (fallback to `git status --porcelain`)
   - Append `## Changed Files` list and count to review file

6. **Gate 4 — AI review (optional):**
   - Check `if [ "${AI_REVIEW:-0}" = "1" ]`
   - If yes: run `bash scripts/ai-review.sh "$REVIEW_FILE"` (always exit 0 from AI step)
   - If no: append `## AI Review — Skipped (run with AI_REVIEW=1 git push to enable)`

7. **Footer:**
   - Print total duration
   - Print gate summary table
   - If `EXIT_CODE=1`: print "PUSH BLOCKED"
   - `exit $EXIT_CODE`

---

### scripts/install-githooks.sh

- Detect project root
- `git config core.hooksPath .githooks`
- `chmod +x .githooks/pre-push`
- Verify: `git config core.hooksPath` matches `.githooks`
- Print success + usage instructions:
  - How to test manually: `bash .githooks/pre-push`
  - How to enable AI: `AI_REVIEW=1 git push`

---

### scripts/ai-review.sh

Vendor-neutral AI review interface:
- Accept `$1` = review file path
- ALWAYS `exit 0`
- Provider detection (in order):
  1. `claude` CLI on PATH → pipe bounded git diff (max 300 lines) to `claude --print "code review prompt"`
  2. `OPENAI_API_KEY` set → curl OpenAI chat completions API with bounded diff
  3. Fallback → print "No AI provider configured"
- Error in any provider: print notice, continue, exit 0
- Output: markdown with sections for Security, Quality, Tests, Performance

---

### .gitignore additions

```
# Review reports
reviews/local-review.md
reviews/*.md
!reviews/.gitkeep

# IDE
.idea/
*.iml
.vscode/
.DS_Store
```

---

### reviews/.gitkeep

Empty file (tracked to ensure `reviews/` directory is committed).

---

### README section

Generate a "Development Quality Gate" section with:
- Setup: `bash scripts/install-githooks.sh`
- Verify: `git config core.hooksPath` → `.githooks`
- What runs on push (4 gates)
- Enable AI: `AI_REVIEW=1 git push origin branch`
- View report: `cat reviews/local-review.md`
- Emergency bypass: `git push --no-verify` (with warning)

---

### Key bash patterns to use

```bash
#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# Non-blocking gate (capture exit code without set -e killing script)
if mvn clean verify ... ; then
    # pass
else
    EXIT_CODE=1
fi

# Timing
START_TIME=$(date +%s)
# ... do work ...
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
echo "Duration: ${DURATION}s"

# AI step — always exit 0
if [ "${AI_REVIEW:-0}" = "1" ]; then
    bash scripts/ai-review.sh "$REVIEW_FILE" || true
fi
```

---

### Output Format

File tree → each file complete → verification:
```
## Verification
1. bash scripts/install-githooks.sh
2. git config core.hooksPath → .githooks
3. bash .githooks/pre-push (manual test)
4. cat reviews/local-review.md

Checklist:
- [ ] pre-push uses set -euo pipefail
- [ ] install-githooks.sh sets core.hooksPath
- [ ] ai-review.sh always exits 0
- [ ] reviews/local-review.md in .gitignore
- [ ] reviews/.gitkeep committed
- [ ] Gate 1 blocks push on mvn failure
- [ ] Gate 4 never blocks push
```
