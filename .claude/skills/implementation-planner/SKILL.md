---
name: implementation-planner
description: Produces the implementation plan for a User Story from the approved specification, designs, design review, and impact analysis. Owns the IMPLEMENTATION_PLANNING stage. Does not create or modify the impact analysis.
---

# Purpose

Own the **IMPLEMENTATION_PLANNING** stage. Turn approved requirements, designs,
and the predicted impact into a concrete, ordered, reviewable implementation
plan.

The plan defines exactly what will change and in what order. It does not
introduce new business behavior and does not redesign anything upstream.

# Canonical sources

- Workflow / stage: `docs/workflow/stage-map.yaml` (`IMPLEMENTATION_PLANNING`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` — **authoritative**.
  This Skill's only output is `implementation_plan`.
- Front matter: `docs/workflow/artifact-schema.md`.
- Result vocabulary: `docs/workflow/artifact-lifecycle.md`.

# Inputs (registry keys — resolve paths from artifact-paths.yaml)

- `story`
- `open_decisions`
- `specification`, `specification_review`
- `api_design`, `openapi`  (or their `NOT_APPLICABLE` record)
- `database_design`, `entity_model`  (or their `NOT_APPLICABLE` record)
- `design_review`
- `impact_analysis`  ← **required input; consume it, do not regenerate it**
- Architecture: `docs/architecture/architecture.md`,
  `docs/architecture/package-map.md`,
  `docs/architecture/api-conventions.md`,
  `docs/architecture/persistence-conventions.md`,
  `docs/architecture/security-conventions.md`
- `docs/product/non-functional-requirements.md`,
  `docs/product/business-rules.md`
- `AGENTS.md`

# Preconditions

- `specification_review` verdict `PASS` and `HUMAN_SPEC_APPROVAL` recorded.
- `design_review` verdict `PASS`.
- `impact_analysis` exists with verdict `PASS` (readiness), current version.
- No blocking Open Decision affecting API, persistence, security, architecture,
  Acceptance Criteria, tests, dependencies, or execution order.
- All consumed artifacts are current (non-`SUPERSEDED`); record each version in
  this plan's `inputs` front matter.

If `impact_analysis` is missing, stale, or `BLOCKED` → `verdict: BLOCKED`
(do not proceed without it, and do not create it here).

# Responsibilities

Using the impact analysis as the predicted change surface, and the designs as
the contract:

- confirm affected modules, packages, and classes (reconcile with the impact
  analysis; explain any material difference);
- list files to create and files to modify, each traced to an Acceptance
  Criterion / Specification requirement / design element / impact-analysis
  entry;
- define implementation order respecting dependencies (contract confirmation →
  tests → persistence → service → API → security wiring → validation → build →
  full verification → doc reconciliation);
- define the validation activity / observable completion criterion for each
  significant step;
- define the testing strategy by level (contract, integration, unit, security),
  mapped to Acceptance Criteria — the executable tests are authored later by
  `test-writer`;
- keep scope minimal: no opportunistic refactoring, no unrelated changes, no
  speculative abstractions;
- flag any new dependency (requires explicit human approval) and any needed
  configuration change.

# Output

- `implementation_plan` (`docs/plans/{story_id}-implementation-plan.md`),
  `status: DRAFT`, front matter per `docs/workflow/artifact-schema.md`
  (`artifact_type: implementation_plan`).

  Sections: Goal; Source Artifacts (paths + versions); Architectural Changes;
  Impact-Analysis Reconciliation; Files To Create; Files To Modify; Execution
  Order (numbered, each with validation evidence); Validation Strategy; Testing
  Strategy (by level, mapped to ACs); Risks; New Dependencies (if any);
  Configuration Changes (if any); Open Questions.

**Do not create or modify `impact_analysis` or any other artifact.**

# Result Envelope

Return exactly this; the story-orchestrator records the transition:

```yaml
result:
  verdict: PASS | BLOCKED
  stage: IMPLEMENTATION_PLANNING
  story: <StoryId>
  artifact_status: DRAFT
  artifacts:
    - docs/plans/<StoryId>-implementation-plan.md
  next_stage: PLAN_REVIEW
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings: []
```

- `PASS` — every affected file identified and traced; execution order and
  validation defined; testing strategy covers all Acceptance Criteria; scope is
  minimal.
- `BLOCKED` — `impact_analysis` missing/stale/BLOCKED, blocking Open Decision,
  empty architecture doc, conflicting authoritative artifacts, or an upstream
  artifact internally inconsistent in a way that blocks planning. This stage has
  no `loop_back` map in `stage-map.yaml`; name the offending upstream stage in
  `blocking_issues` for the human to decide. Do not emit `CHANGES_REQUIRED`.

# Prohibited

- Do not generate source code or tests.
- Do not create or edit the impact analysis, designs, or Specification.
- Do not add dependencies (only propose them for human approval).
- Do not resolve Open Decisions.
- Do not update workflow state.
