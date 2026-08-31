---
name: pr-preparer
description: Assembles the Pull Request summary for a User Story after reconciliation, validating readiness and consuming (not regenerating) reconciliation's traceability and PR candidate file classification. Owns the PR_PREPARATION stage. Does not create a Pull Request.
---

# Purpose

Own the **PR_PREPARATION** stage. Produce the single Pull Request summary
artifact a human uses to create or finalize the Pull Request.

This Skill does **not** create, push, or merge a Pull Request. It runs after
`HUMAN_PR_APPROVAL`.

# Canonical sources

- Workflow / stage / loop-back: `docs/workflow/stage-map.yaml`
  (`PR_PREPARATION`; loop_back key `stale_reconciliation` → `RECONCILIATION`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` — **authoritative**. This
  Skill's only output is `pr_summary`.
- Front matter: `docs/workflow/artifact-schema.md`.
- Result vocabulary: `docs/workflow/artifact-lifecycle.md`.

# Inputs (registry keys)

- `story`, `specification`
- `impact_analysis`, `implementation_plan`
- `implementation_report`, `implementation_verification`, `security_review`
- `reconciliation`  ← contains the authoritative PR candidate file
  classification; **consume it, do not re-derive it**
- `traceability`  ← the authoritative end-to-end AC matrix; **consume it**
- `AGENTS.md`
- current repository / Git state (read only)

# Single-ownership rules

- **`reconciliation-reviewer` owns** the end-to-end traceability matrix
  (`traceability`) and the PR candidate file classification (inside
  `reconciliation`). This Skill reads and presents them; it does not rebuild
  them.
- **`implementation-verifier` owns** the authoritative build/test evidence.
  Reuse it. Re-run validation only if it is missing or if tracked files changed
  after verification (which `reconciliation` will have flagged).

# Preconditions

All required artifacts exist with the required verdicts:

| Artifact | Required |
|---|---|
| `specification_review` | verdict `PASS` |
| `plan_review` | verdict `PASS` |
| `implementation_verification` | verdict `PASS` |
| `security_review` | verdict `PASS` |
| `reconciliation` | verdict `PASS` |
| `traceability` | present, current |

`HUMAN_PR_APPROVAL` recorded. No blocking Open Decision or unresolved marker in
any current Story artifact.

If any required artifact is missing, stale, or not `PASS` → `verdict: BLOCKED`.
If `reconciliation` is stale relative to the current repository state →
`verdict: CHANGES_REQUIRED`, `loop_back_stage: RECONCILIATION` (key
`stale_reconciliation`).

# Responsibilities

1. Confirm the artifact chain is current and consistent (versions in `inputs`
   front matter line up; nothing `SUPERSEDED`).
2. Take the PR candidate file classification from `reconciliation`
   (Include / Exclude-runtime / Exclude-local / Exclude-secret / Exclude-unrelated
   / Human-decision-required). Verify each `Include` file still exists and each
   `Exclude` rationale still holds. Report drift; do not reclassify unilaterally.
3. Take the Acceptance Criteria traceability from `traceability`. Confirm every
   Acceptance Criterion is `RECONCILED`. A non-reconciled criterion →
   `verdict: BLOCKED`.
4. Collect build/test/security evidence references from
   `implementation_verification` and `security_review`.
5. Assemble the `pr_summary`.

# Output

- `pr_summary` (`docs/pr/{story_id}-pr-summary.md`), front matter per
  `docs/workflow/artifact-schema.md` (`artifact_type: pr_summary`).

  One artifact, with these sections (the former separate release-notes and
  PR-preparation-report content is folded in here):

  - Story & Business Goal
  - Scope / Implemented Features
  - API Changes
  - Database Changes
  - Security Changes
  - Tests Executed (evidence references)
  - Acceptance Criteria Coverage (from `traceability`)
  - PR Candidate Files — Include (from `reconciliation`)
  - PR Candidate Files — Exclude (runtime / local / secret / unrelated)
  - Files Needing Human Decision (if any)
  - Risks & Known Limitations
  - Release Notes (user-visible changes, technical changes, security notes)
  - Notes For Reviewers
  - Readiness Result

# Result Envelope

Return exactly this; the story-orchestrator records the transition:

```yaml
result:
  verdict: PASS | CHANGES_REQUIRED | BLOCKED
  stage: PR_PREPARATION
  story: <StoryId>
  artifact_status: APPROVED
  artifacts:
    - docs/pr/<StoryId>-pr-summary.md
  next_stage: READY_FOR_PR
  loop_back_stage: null            # or RECONCILIATION
  blocking_issues: []
  non_blocking_findings: []
```

- `PASS` — chain current and consistent; every Acceptance Criterion reconciled;
  PR candidate scope identified with no runtime artifact, secret, or unrelated
  change in `Include`; `pr_summary` written.
- `CHANGES_REQUIRED` — `reconciliation` is stale relative to the working tree;
  `loop_back_stage: RECONCILIATION`.
- `BLOCKED` — a required artifact missing/stale/not `PASS`, an Open Decision
  unresolved, an Acceptance Criterion not reconciled, or PR scope cannot be
  determined.

The orchestrator then advances the stage to `READY_FOR_PR` (a human gate). A
human creates/finalizes the Pull Request and records readiness with
`/so:approve`.

# Prohibited

- Do not modify source, tests, requirements, designs, plans, or review
  artifacts.
- Do not re-derive the traceability matrix or the PR candidate classification.
- Do not resolve Open Decisions.
- Do not create commits, branches, or Pull Requests; do not merge; do not change
  Issue status.
- Do not update workflow state.
