---
name: reconciliation-reviewer
description: >
  Reconciles the implemented result of the active User Story with its original
  intent, Acceptance Criteria, approved Specification, designs, Impact
  Analysis, Implementation Plan, tests, Implementation Report, independent
  verification, Security Review, and actual repository changes. Use after
  Implementation Verification and Security Review are approved, but before
  Pull Request preparation.
---

# Purpose

Determine whether the complete delivery result of the active User Story is
internally consistent, traceable, adequately documented, and ready for Pull
Request preparation.

The Skill compares:

- intended behavior;
- approved delivery artifacts;
- planned changes;
- actual implementation;
- automated test evidence;
- functional verification;
- security verification;
- actual Git change set.

The Skill identifies drift between these elements.

The Skill produces a Reconciliation artifact containing:

- Acceptance Criteria traceability;
- planned-versus-actual comparison;
- predicted-versus-actual impact;
- design-versus-implementation comparison;
- test coverage reconciliation;
- documentation consistency;
- unresolved deviations;
- final readiness recommendation.

The Skill does not fix inconsistencies automatically.

The Skill does not modify source code, tests, requirements, designs, plans, or
review reports.

The Skill does not create a Pull Request or approve merge.

---

# Position in the Workflow

Canonical workflow: `docs/workflow/stage-map.yaml`. Relevant slice:

    IMPLEMENTATION_VERIFICATION
    → SECURITY_REVIEW
    → RECONCILIATION           (this Skill)
    → HUMAN_PR_APPROVAL
    → PR_PREPARATION → READY_FOR_PR → COMPLETED → ARCHIVED

This Skill owns only the `RECONCILIATION` stage.

`stage-map.yaml` loop-backs for `RECONCILIATION`:
`implementation_drift` → `IMPLEMENTATION`,
`test_gap` → `TEST_WRITING`,
`plan_gap` → `IMPLEMENTATION_PLANNING`,
`design_gap` → `API_DESIGN`,
`specification_gap` → `SPECIFICATION`,
`story_source_conflict` → `BACKLOG_SYNC`.
Any other upstream root cause → `verdict: BLOCKED` with the responsible stage in
`blocking_issues`.

---

# Reconciliation Principle

Earlier stages answer different questions.

Impact Analysis answers:

    What do we expect to be affected?

Implementation Plan answers:

    How do we intend to implement the Story?

Implementation Report answers:

    What does the Implementor claim was changed?

Implementation Verification answers:

    What functional and technical behavior can be independently demonstrated?

Security Review answers:

    Which security properties and risks were independently evaluated?

Reconciliation answers:

    Does the complete delivered result remain consistent with the original
    intent and all approved artifacts?

---

# Reconciliation Is Not Another Implementation Review

Implementation Verification focuses on whether the implementation works.

Security Review focuses on whether the implementation is acceptably secure.

Reconciliation focuses on consistency across the complete artifact chain.

A functionally correct and secure implementation may still fail
Reconciliation when:

- approved documentation is stale;
- actual files differ materially from the approved plan;
- the implementation introduced undocumented behavior;
- an Acceptance Criterion has no traceable evidence;
- tests validate behavior not defined by approved requirements;
- OpenAPI differs from runtime behavior;
- database design differs from implemented constraints;
- predicted impact differs materially from actual impact;
- a new dependency was introduced but not documented;
- an implementation decision was made without an approved decision artifact.

---

# When To Use

Use this Skill when:

- an active User Story is configured;
- implementation has completed;
- an Implementation Report exists;
- Implementation Verification has completed;
- Security Review has completed;
- all required implementation corrections have been applied;
- reconciliation is the current workflow stage;
- the Story is being prepared for Pull Request creation;
- a previous Reconciliation rejected the delivery and corrections have been
  completed.

Typical requests:

- Reconcile the active User Story before Pull Request preparation.
- Compare the US-001 implementation with all approved artifacts.
- Check planned versus actual changes for the current Story.
- Verify end-to-end traceability before preparing the Pull Request.
- Re-run Reconciliation after documentation or implementation corrections.

---

# When Not To Use

Do not use this Skill:

- before implementation exists;
- before Implementation Verification;
- before Security Review;
- to clarify requirements;
- to create or rewrite the Specification;
- to design the API or database;
- to create an Implementation Plan;
- to implement missing behavior;
- to fix code or tests;
- to resolve Open Decisions;
- to perform the first functional verification;
- to perform the first Security Review;
- to prepare or create a Pull Request;
- to update GitHub Issue status;
- to mark the Story complete;
- as a replacement for human code review.

---

# Active Scope

Read:

- docs/workflow/active-story.yaml
- docs/workflow/workflow-state.yaml
- docs/workflow/stages.md
- docs/workflow/artifact-lifecycle.md

Determine:

- active Story ID;
- current workflow stage;
- current artifact versions;
- implementation attempt;
- verification attempt;
- Security Review attempt;
- Reconciliation attempt;
- current branch when recorded;
- expected next stage.

Work only on the active User Story.

Do not include unrelated Stories merely because their artifacts exist in the
repository.

If no active Story is configured, stop and report:

    RECONCILIATION_BLOCKED:
    No active User Story is configured.

If the workflow stage does not permit Reconciliation, stop and report:

    RECONCILIATION_BLOCKED:
    Current workflow stage does not allow Reconciliation.

Do not select another Story automatically.

---

# Canonical Sources

- Workflow / stage / loop-back: `docs/workflow/stage-map.yaml` (`RECONCILIATION`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` — **authoritative**.
  Resolve every path from its registry key. Paths shown are illustrative.
- Status vocabulary: `docs/workflow/artifact-lifecycle.md`.
- Front matter: `docs/workflow/artifact-schema.md`.

# Required Context

Read AGENTS.md first. Read `docs/workflow/active-story.yaml`,
`docs/workflow/workflow-state.yaml`, `docs/workflow/stage-map.yaml` (read only).

Read (registry keys, resolved via `artifact-paths.yaml`):

- `story`
- `specification`, `specification_review`
- `api_design`, `openapi`, `database_design`, `entity_model`
  (or their `NOT_APPLICABLE` record)
- `design_review`
- `impact_analysis`
- `implementation_plan`, `plan_review`
- `test_strategy`, `ac_test_matrix`, `test_generation_report`
  (+ executable tests under `src/test/`)
- `implementation_report`
- `implementation_verification`
- `security_review`
- `open_decisions`

Read repository state and relevant source code. Read tool telemetry
(`docs/hooks/tool-usage.jsonl`, `docs/evidence/`) only to verify execution
claims.

Read architecture references:

- docs/architecture/architecture.md
- docs/architecture/package-map.md
- docs/architecture/api-conventions.md
- docs/architecture/persistence-conventions.md
- docs/architecture/security-conventions.md

Read product context:

- docs/product/product-vision.md
- docs/product/business-glossary.md
- docs/product/business-rules.md
- docs/product/non-functional-requirements.md

(`open_decisions` is listed in Required Context above.)

Do not load artifacts from unrelated or completed Stories unless a concrete
dependency requires them.

---

# Canonical Output

This Skill produces two artifacts (both with front matter per
`docs/workflow/artifact-schema.md`):

- `reconciliation`
  (`docs/reviews/reconciliation/{story_id}-reconciliation.md`,
  `artifact_type: reconciliation`) — the reconciliation verdict, drift register,
  findings, and the **PR candidate file classification** (this Skill owns that
  classification; `pr-preparer` consumes it).
- `traceability`
  (`docs/reconciliation/{story_id}-traceability.md`,
  `artifact_type: traceability`) — the **authoritative end-to-end
  Acceptance-Criteria → artifact/code/test matrix** (this Skill owns it;
  downstream Skills reference it, they do not rebuild it).

Do not create any other reconciliation artifact under another directory.

---

# Artifact Authority

Use the following authority order when evaluating consistency:

1. Active User Story and Acceptance Criteria
2. Approved and resolved Story decisions
3. Approved Specification
4. Approved API and database designs
5. Approved architecture and product rules
6. Approved Impact Analysis
7. Approved Implementation Plan
8. Approved test artifacts
9. Actual source code and configuration
10. Independently observed build and test evidence
11. Implementation Report
12. Tool telemetry

This order does not mean that earlier artifacts are automatically correct.

It means that implementation cannot silently redefine approved intent.

When repository evidence demonstrates that an approved artifact is infeasible
or inaccurate, record the conflict and return to the earliest responsible
stage.

Do not rewrite the earlier artifact during Reconciliation.

---

# Preconditions

All of the following review artifacts must exist, be current
(non-`SUPERSEDED`), and carry verdict `PASS`:

- `specification_review`  (+ `HUMAN_SPEC_APPROVAL` recorded)
- `design_review`
- `impact_analysis`
- `plan_review`  (+ `HUMAN_PLAN_APPROVAL` recorded)
- `implementation_verification`
- `security_review`

Do not issue a positive Reconciliation result when any is
`CHANGES_REQUIRED` / `BLOCKED` / missing — return `verdict: BLOCKED`.

Record every consumed artifact version in this Skill's `inputs` front matter.
Any `SUPERSEDED` mandatory input → `verdict: BLOCKED`.

## Open Decisions

Search current Story artifacts for unresolved markers:

- Open Decision
- OPEN
- TODO
- TBD
- FIXME
- ???
- unresolved
- to be decided

An unresolved decision is blocking when it affects:

- an Acceptance Criterion;
- observable behavior;
- API contract;
- persistence;
- security;
- validation;
- architecture;
- testing;
- configuration;
- dependency selection;
- Pull Request scope.

Do not resolve Open Decisions during Reconciliation.

## Repository State

Inspect:

- current branch;
- modified files;
- untracked files;
- deleted files;
- generated runtime files;
- unrelated changes;
- ignored files;
- staged files when applicable.

If unrelated changes prevent reliable scope analysis, set Reconciliation to
BLOCKED and request human action.

---

# Reconciliation Dimensions

The Skill must examine all of the following dimensions:

- Story versus Specification;
- Acceptance Criteria versus implementation;
- Acceptance Criteria versus tests;
- Specification versus API design;
- Specification versus database design;
- design versus implementation;
- Impact Analysis versus actual file changes;
- Implementation Plan versus actual implementation;
- test plan versus actual tests;
- Implementation Report versus repository evidence;
- Implementation Verification versus current repository state;
- Security Review versus current repository state;
- architecture documentation versus implementation;
- product rules versus implementation;
- documentation versus actual behavior;
- expected dependencies versus actual dependencies;
- expected configuration versus actual configuration;
- predicted scope versus Pull Request candidate scope.

---

# Tooling Strategy

Prefer repository artifacts and Git evidence for change-set reconciliation.

Use IDEA MCP when available for semantic and project-aware inspection.

## Repository and Project Inspection

Preferred capabilities:

- mcp__idea__git_status
- mcp__idea__get_repositories
- mcp__idea__get_project_modules
- mcp__idea__get_project_dependencies
- mcp__idea__list_directory_tree
- mcp__idea__read_file

## Semantic Analysis

Preferred capabilities:

- mcp__idea__search_symbol
- mcp__idea__get_symbol_info
- mcp__idea__analyze_calls
- mcp__idea__generate_psi_tree

Use semantic analysis to verify:

- actual component ownership;
- class and interface relationships;
- actual call paths;
- architecture boundaries;
- claimed file responsibilities;
- renamed or duplicated abstractions.

## Build and Diagnostics

Use when current evidence must be reconfirmed:

- mcp__idea__build_project
- mcp__idea__get_file_problems
- mcp__idea__lint_files
- mcp__ide__getDiagnostics

## GitHub MCP

Use GitHub MCP only when remote repository data is necessary.

Possible read-only uses include:

- reading the source Issue;
- reading branch information;
- reading existing Pull Requests;
- comparing current Issue content with the local Story artifact.

Do not create or update the Pull Request in this Skill.

Do not change Issue status.

Do not merge.

## Database Inspection

When a configured IDEA database connection exists and runtime schema evidence
is relevant, use read-only capabilities:

- mcp__idea__list_database_connections
- mcp__idea__test_database_connection
- mcp__idea__list_database_schemas
- mcp__idea__list_schema_objects
- mcp__idea__get_database_object_description
- mcp__idea__introspect_schema

Do not execute destructive SQL.

Do not create database connections automatically.

---

# Built-In Tool Fallback

If IDEA MCP is unavailable:

1. Use built-in file discovery and reading.
2. Use approved Git commands through the shell.
3. Use approved Gradle commands when evidence must be refreshed.
4. Record missing semantic capabilities.
5. Avoid claiming semantic certainty based only on text matching.

Suggested Git evidence may include:

    git status --short
    git diff --name-status
    git diff --stat
    git diff
    git ls-files

Suggested Gradle evidence may include:

Windows:

    gradlew.bat test
    gradlew.bat check

Unix-compatible environment:

    ./gradlew test
    ./gradlew check

Use only commands appropriate to the current environment and repository.

---

# Reconciliation Workflow

## Step 1: Resolve Active Story

Read workflow state.

Record:

- Story ID;
- current stage;
- current artifact versions;
- implementation attempt;
- Verification attempt;
- Security Review attempt;
- Reconciliation attempt.

Confirm that RECONCILIATION is the permitted stage.

---

## Step 2: Build the Artifact Inventory

Create an inventory of all artifacts belonging to the active Story.

For each artifact record:

- path;
- artifact type;
- version;
- status;
- superseded artifact when applicable;
- producing Skill or stage when known;
- whether the artifact is current;
- whether the artifact is mandatory.

Identify:

- missing artifacts;
- duplicate current artifacts;
- stale artifacts;
- inconsistent paths;
- incorrect Story identifiers;
- references to superseded versions.

A mandatory stale or missing artifact is blocking.

---

## Step 3: Validate the Artifact Chain

Verify that each downstream artifact references the current upstream versions.

Expected dependency chain:

    User Story
    ↓
    Specification
    ↓
    Specification Review
    ↓
    API and Database Designs
    ↓
    Impact Analysis
    ↓
    Implementation Plan
    ↓
    Plan Review
    ↓
    Test Artifacts
    ↓
    Implementation Report
    ↓
    Implementation Verification
    ↓
    Security Review
    ↓
    Reconciliation

Flag an artifact when it was generated from an outdated predecessor.

Do not accept a later approval when its input artifact was superseded after the
review.

---

## Step 4: Reconcile Source Issue and Local Story

When the User Story originated from GitHub:

Compare the relevant GitHub Issue with the local Story artifact.

Check:

- Story identifier;
- title;
- actor;
- intent;
- business value;
- Acceptance Criteria;
- Definition of Done;
- labels or Epic association when relevant.

Classify differences as:

- formatting-only;
- approved clarification;
- unapproved requirement change;
- missing synchronization;
- remote-source drift.

Do not update either source automatically.

Use the source-of-truth policy defined by `backlog-sync` / AGENTS.md.

If the GitHub Issue and local Story materially disagree, return
`verdict: CHANGES_REQUIRED`, `loop_back_stage: BACKLOG_SYNC` (key
`story_source_conflict`). If no source-of-truth policy exists at all, return
`verdict: BLOCKED` and say a human must decide.

---

## Step 5: Reconcile Story and Specification

Verify that:

- every Acceptance Criterion is represented;
- no approved requirement was omitted;
- no unsupported business requirement was added;
- Out of Scope boundaries are preserved;
- resolved decisions are reflected;
- unresolved decisions were not materialized as implementation assumptions.

Create a Story-to-Specification mapping.

---

## Step 6: Reconcile Specification and Designs

Compare the Specification with:

- OpenAPI contract;
- API design;
- database design;
- entity model.

Verify:

- API operations represent approved behavior;
- request and response schemas remain consistent;
- status codes represent Acceptance Criteria;
- validation constraints align;
- persistence constraints align;
- security-sensitive fields are treated consistently;
- error behavior aligns;
- design does not introduce unsupported scope.

Record every material design deviation.

---

## Step 7: Reconcile Impact Analysis and Actual Impact

Compare predicted impact with actual repository changes.

For every predicted item classify it as:

- Confirmed;
- Not Needed;
- Replaced by Alternative;
- Missing from Implementation;
- Unable to Verify.

For every actual changed item classify it as:

- Predicted;
- Required Supporting Change;
- Unexpected but Justified;
- Unexpected and Unapproved;
- Unrelated;
- Generated Runtime Artifact.

Do not treat differences as failures automatically.

Evaluate whether each difference is documented and justified.

Update is not performed in this Skill.

The result is recorded for the future history of prediction accuracy.

---

## Step 8: Reconcile Plan and Actual Implementation

For every approved plan step determine:

- Completed;
- Partially Completed;
- Not Completed;
- Replaced by Approved Alternative;
- No Longer Required;
- Unable to Verify.

For every actual implementation change determine which plan step authorized it.

Flag:

- unimplemented plan steps;
- undocumented implementation steps;
- changed execution strategy;
- unapproved dependency changes;
- unapproved configuration changes;
- hidden refactoring;
- modified unrelated behavior.

---

## Step 9: Reconcile Acceptance Criteria and Tests

For every Acceptance Criterion identify:

- planned test;
- implemented test;
- test type;
- execution evidence;
- current result;
- functional Verification evidence;
- Security Review evidence when relevant.

Assign one traceability status:

- FULLY_TRACED;
- PARTIALLY_TRACED;
- NOT_TRACED;
- BLOCKED.

A passing test does not establish traceability if the test does not represent
the Acceptance Criterion.

A traced Acceptance Criterion should have:

- approved requirement;
- implementation location;
- test or deterministic evidence;
- successful result.

---

## Step 10: Reconcile OpenAPI and Runtime Implementation

When API behavior is present, compare:

- approved OpenAPI;
- Controller behavior;
- DTOs;
- validation;
- status codes;
- error responses;
- authentication and authorization;
- tests.

Identify:

- implementation missing from OpenAPI;
- OpenAPI behavior missing from implementation;
- undocumented fields;
- response field exposure;
- incorrect status codes;
- inconsistent validation;
- endpoint path or HTTP method drift.

Do not update OpenAPI or implementation automatically.

---

## Step 11: Reconcile Database Design and Persistence

Compare:

- approved DB design;
- entity model;
- JPA entities;
- repository behavior;
- explicit constraints;
- runtime schema evidence when available;
- H2 configuration;
- persistence tests.

Verify alignment for:

- table names;
- field names;
- lengths;
- nullability;
- uniqueness;
- indexes;
- identifiers;
- relationships;
- sensitive fields;
- database location;
- schema initialization strategy.

For the training project, confirm that file-based H2 behavior matches approved
documentation.

Generated H2 database files must be classified as runtime artifacts and must
not enter the Pull Request candidate set.

---

## Step 12: Reconcile Architecture and Actual Dependencies

Verify that the final implementation follows:

- architecture.md;
- package-map.md;
- API conventions;
- persistence conventions;
- security conventions.

Use semantic analysis when available.

Check:

- Controller-to-Service relationships;
- Service-to-Repository relationships;
- absence of forbidden Controller-to-Repository access;
- DTO and entity separation;
- validation ownership;
- exception handling;
- security configuration placement;
- package responsibilities;
- module boundaries.

Architecture drift must be documented even when tests pass.

---

## Step 13: Reconcile Security Review and Current State

Confirm that the repository has not materially changed after the approved
Security Review.

Compare current relevant files with the scope reviewed by
`security-reviewer`.

If security-sensitive files changed after Security Review:

1. identify the changed files;
2. mark the `security_review` evidence stale;
3. return `verdict: BLOCKED`; name `SECURITY_REVIEW` in `blocking_issues`;
4. do not approve Reconciliation.

Security-sensitive files may include:

- Spring Security configuration;
- password handling;
- authentication or authorization components;
- entities containing sensitive data;
- response DTOs;
- logging;
- application configuration;
- dependency configuration.

---

## Step 14: Reconcile Implementation Verification and Current State

Confirm that implementation and tests have not materially changed after the
approved Implementation Verification.

If production code, tests, build configuration, or application configuration
changed after verification:

1. identify the changed files;
2. determine whether existing evidence remains valid;
3. when it does not, return `verdict: BLOCKED`; name
   `IMPLEMENTATION_VERIFICATION` in `blocking_issues`;
4. do not treat stale verification as current evidence.

---

## Step 15: Reconcile Documentation

Verify that required documentation reflects actual behavior.

Review:

- Specification;
- OpenAPI;
- API design;
- database design;
- architecture references;
- testing artifacts;
- implementation report;
- configuration guidance.

Classify every inconsistency by authority direction:

- implementation must change to match approved documentation;
- documentation must be updated to reflect an approved implementation
  decision;
- an upstream decision is missing;
- human decision is required.

Do not update documentation automatically.

---

## Step 16: Reconcile Configuration and Dependencies

Compare actual changes with approved plans and reports.

Inspect:

- build.gradle.kts;
- application configuration;
- profile-specific configuration;
- Git ignore rules;
- dependencies;
- H2 settings;
- Spring Security settings;
- test settings.

Flag:

- added but undocumented dependencies;
- obsolete planned dependencies;
- unapproved configuration;
- insecure defaults;
- generated database files;
- local-only paths;
- hidden environment assumptions.

---

## Step 17: Inspect Pull Request Candidate Scope

Determine which working-tree changes belong to the prospective Pull Request.

Classify every changed or untracked file as:

- INCLUDE;
- EXCLUDE_RUNTIME_ARTIFACT;
- EXCLUDE_LOCAL_CONFIGURATION;
- EXCLUDE_SECRET;
- EXCLUDE_UNRELATED;
- NEEDS_HUMAN_DECISION.

Examples of likely exclusions:

- generated H2 database files;
- IDE-local configuration;
- secret-bearing configuration;
- unrelated edits;
- temporary logs;
- transient debug output.

Do not stage or commit files.

---

## Step 18: Build End-to-End Traceability

Create a complete traceability matrix.

For every Acceptance Criterion map:

- User Story section;
- Specification section;
- resolved decision;
- API design section;
- database design section;
- Impact Analysis entry;
- Implementation Plan step;
- production file or symbol;
- test;
- Implementation Verification evidence;
- Security Review evidence;
- final status.

Each Acceptance Criterion must have one final status:

- RECONCILED;
- PARTIALLY_RECONCILED;
- NOT_RECONCILED;
- BLOCKED.

---

## Step 19: Detect Drift

Classify drift as:

### Requirement Drift

Implementation behavior differs from approved requirements.

### Design Drift

Implementation differs from approved API, database, or architecture design.

### Plan Drift

Actual changes differ from the approved execution plan.

### Test Drift

Tests validate behavior different from approved requirements.

### Documentation Drift

Documentation no longer represents actual approved behavior.

### Security Drift

Security-sensitive implementation changed after Security Review or differs
from approved controls.

### Scope Drift

The change set contains behavior or files outside the active Story.

### Artifact Drift

Artifacts reference stale, superseded, duplicate, or inconsistent versions.

For every drift item identify:

- origin;
- affected artifacts;
- risk;
- required correction;
- loop-back target.

---

## Step 20: Classify Findings

Classify each finding as:

### Critical

Blocks Pull Request preparation.

Examples:

- Acceptance Criterion not implemented;
- unresolved requirement materialized as code;
- approved verification is stale;
- approved Security Review is stale;
- implementation contradicts approved Specification;
- undocumented sensitive behavior;
- secret or generated database file in candidate scope;
- source Issue and local Story materially disagree without source-of-truth
  policy.

### Major

Requires correction before Pull Request preparation.

Examples:

- missing or stale documentation;
- material Plan deviation without approval;
- incomplete traceability;
- undocumented dependency or configuration change;
- design drift;
- unexpected file without justification;
- missing test evidence.

### Minor

Should be addressed or documented but does not materially block Pull Request
preparation.

Examples:

- non-functional documentation inconsistency;
- low-risk naming drift;
- minor predicted-versus-actual difference;
- optional clarification in implementation report.

### Informational

Provides historical or process insight without requiring correction.

Examples:

- predicted file was not needed;
- an alternative existing component was reused;
- implementation required fewer changes than expected.

---

## Step 21: Determine Loop-Back Target

For every Critical or Major finding, identify the earliest responsible stage,
then map it to a `stage-map.yaml` `RECONCILIATION.loop_back` key:

| Root cause | loop_back_stage | key |
|---|---|---|
| Correct artifacts but incorrect code / drift in implementation | `IMPLEMENTATION` | `implementation_drift` |
| Missing or wrong Acceptance-Criterion test | `TEST_WRITING` | `test_gap` |
| Undocumented implementation action / plan not followed | `IMPLEMENTATION_PLANNING` | `plan_gap` |
| API/DB mismatch caused by the approved design | `API_DESIGN` | `design_gap` |
| Specification omission / requirement drift | `SPECIFICATION` | `specification_gap` |
| GitHub Issue and local Story materially disagree | `BACKLOG_SYNC` | `story_source_conflict` |

If the finding is a **report-only / traceability-only** issue this Skill can fix
by re-running: return `verdict: CHANGES_REQUIRED`, `loop_back_stage: RECONCILIATION`
(the orchestrator re-invokes this stage; use only when nothing else needs to
change).

Any other root cause — e.g. code changed after verification, security-sensitive
code changed after security review, a missing business decision — return
`verdict: BLOCKED` and name the responsible stage
(`IMPLEMENTATION_VERIFICATION`, `SECURITY_REVIEW`, `CLARIFICATION`, …) in
`blocking_issues` for the orchestrator / a human to route.

Do not return every inconsistency to `IMPLEMENTATION`.

---

## Step 22: Determine Readiness

Reconciliation may recommend progression only when:

- every Acceptance Criterion is RECONCILED;
- Implementation Verification is current and approved;
- Security Review is current and approved;
- no Critical findings exist;
- no Major findings exist;
- artifact chain is current;
- actual and planned scope differences are justified;
- candidate Pull Request files are identified;
- no secret or runtime artifact is included;
- traceability is complete;
- documentation is consistent or has only approved Minor comments.

---

## Step 23: Create Reconciliation Artifacts

Create both artifacts at their registry paths (see Canonical Output):

- `reconciliation` → `docs/reviews/reconciliation/{story_id}-reconciliation.md`
- `traceability`   → `docs/reconciliation/{story_id}-traceability.md`

Do not modify any reviewed artifact.

Do not update workflow state automatically.

Do not stage files.

Do not create a commit.

Do not create or modify a Pull Request.

---

# Reconciliation Artifact Format

Applies to the `reconciliation` artifact. The `traceability` artifact uses the
same shared front matter with `artifact_type: traceability` and just the matrix
body.

## Front Matter

Shared block from `docs/workflow/artifact-schema.md`
(`artifact_type: reconciliation`), plus: `reconciled_acceptance_criteria`,
`total_acceptance_criteria`, `critical_findings`, `major_findings`,
`minor_findings`, `informational_findings`, `candidate_files`, `excluded_files`.
`created_at` / `updated_at` are runtime timestamps. `inputs` records every
consumed artifact path + version.

Illustrative (dates are examples only):

    ---
    artifact_type: reconciliation
    story: US-001
    version: 1
    status: DRAFT
    created_at: <runtime>
    updated_at: <runtime>
    produced_by: reconciliation-reviewer
    inputs:
      - path: docs/evidence/US-001-implementation-report.md
        version: 1
      - path: docs/verification/US-001-implementation-verification.md
        version: 1
      - path: docs/reviews/security/US-001-security-review.md
        version: 1
    supersedes: null
    reconciled_acceptance_criteria: 4
    total_acceptance_criteria: 5
    critical_findings: 1
    major_findings: 1
    minor_findings: 0
    informational_findings: 2
    candidate_files: 14
    excluded_files: 3
    ---

## 1. Executive Summary

Summarize:

- Reconciliation result;
- Acceptance Criteria reconciliation;
- artifact consistency;
- planned-versus-actual alignment;
- principal drift;
- candidate Pull Request scope;
- recommended next action.

## 2. Artifact Inventory

For every Story artifact list:

- path;
- artifact type;
- version;
- status;
- current or stale;
- mandatory or optional;
- producing stage.

## 3. Source-of-Truth Review

Record:

- remote Issue status when inspected;
- local Story status;
- source-of-truth policy;
- synchronization differences;
- required action.

## 4. Acceptance Criteria Traceability Matrix

For every Acceptance Criterion record:

- ID;
- Story text;
- Specification reference;
- design reference;
- Plan step;
- implementation location;
- test reference;
- Verification evidence;
- Security Review evidence;
- final reconciliation status.

## 5. Specification and Design Alignment

Record:

- Specification-to-API consistency;
- Specification-to-database consistency;
- design-to-implementation consistency;
- deviations.

## 6. Predicted Versus Actual Impact

For predicted items record:

- predicted component;
- confidence;
- actual result;
- explanation.

For actual unpredicted items record:

- file or component;
- classification;
- justification;
- approval status.

## 7. Plan Versus Implementation

For every plan step record:

- completion state;
- implementation evidence;
- deviation;
- required action.

## 8. Test Reconciliation

Record:

- planned tests;
- actual tests;
- executed tests;
- Acceptance Criteria coverage;
- missing or extra test behavior;
- stale evidence.

## 9. API Reconciliation

Record:

- OpenAPI operations;
- runtime implementation;
- DTOs;
- validation;
- status codes;
- errors;
- deviations.

## 10. Persistence Reconciliation

Record:

- database design;
- entities;
- repositories;
- constraints;
- H2 configuration;
- runtime schema evidence;
- generated database files;
- deviations.

## 11. Architecture Reconciliation

Record:

- package ownership;
- dependency direction;
- component responsibilities;
- semantic evidence;
- architecture drift.

## 12. Security Reconciliation

Record:

- Security Review version;
- security-sensitive files reviewed;
- changes after review;
- current evidence status;
- security drift.

## 13. Configuration and Dependency Reconciliation

Record:

- planned configuration;
- actual configuration;
- planned dependencies;
- actual dependencies;
- undocumented changes;
- local-environment assumptions.

## 14. Documentation Reconciliation

List:

- current documents;
- stale documents;
- missing updates;
- conflicting descriptions;
- required corrections.

## 15. Pull Request Candidate Scope

Create separate lists:

### Include

Files that belong to the active Story.

### Exclude Runtime Artifacts

Generated files such as H2 database files or temporary reports.

### Exclude Local Configuration

IDE-local or developer-local configuration.

### Exclude Sensitive Files

Files containing or potentially containing secrets.

### Exclude Unrelated Changes

Changes outside the active Story.

### Human Decision Required

Files whose inclusion cannot be determined safely.

## 16. Drift Register

For every drift item provide:

- ID;
- drift type;
- severity;
- affected artifact or file;
- expected state;
- actual state;
- risk;
- required correction;
- loop-back target.

## 17. Findings

For every finding provide:

- ID;
- severity;
- category;
- evidence;
- impact;
- required correction;
- responsible stage;
- loop-back target.

## 18. Positive Alignment

List areas where intent, design, plan, implementation, tests, Verification, and
Security Review align correctly.

## 19. Open Decisions

List unresolved decisions.

If none exist, state:

    No blocking Open Decisions were identified.

## 20. Reconciliation Limitations

List:

- unavailable tools;
- unavailable remote evidence;
- unavailable runtime checks;
- low-confidence conclusions;
- human checks still required.

## 21. Verdict Rationale

Explain the verdict (see Result Envelope). Do not use `PROCEED_TO_*` /
`RETURN_TO_*` / `REPEAT_RECONCILIATION` labels — they are retired.

---

# Result Envelope

Return exactly this; the story-orchestrator records the transition — this Skill
does not update `workflow-state.yaml`:

```yaml
result:
  verdict: PASS | CHANGES_REQUIRED | BLOCKED
  stage: RECONCILIATION
  story: <StoryId>
  artifact_status: APPROVED        # of the reconciliation artifact itself
  artifacts:
    - docs/reviews/reconciliation/<StoryId>-reconciliation.md
    - docs/reconciliation/<StoryId>-traceability.md
  next_stage: HUMAN_PR_APPROVAL
  loop_back_stage: null            # or a stage-map.yaml RECONCILIATION.loop_back target
  blocking_issues: []
  non_blocking_findings: []
```

## PASS

Use only when: every Acceptance Criterion is `RECONCILED` in the `traceability`
matrix; Specification and designs align with the implementation; differences
from the impact analysis are justified; the approved plan is materially
implemented; `implementation_verification` and `security_review` are current and
`PASS`; the artifact chain is current; no Critical or Major findings; the PR
candidate scope is identified with no secret, runtime artifact, or unrelated
change in `Include`. Minor / Informational findings go in
`non_blocking_findings`. The orchestrator advances to `HUMAN_PR_APPROVAL`.

## CHANGES_REQUIRED

Use when a Critical/Major finding maps to a `stage-map.yaml`
`RECONCILIATION.loop_back` key (Step 21). Set `loop_back_stage` accordingly.

## BLOCKED

Use when: the active Story cannot be determined; a mandatory artifact is
missing/stale; artifact versions cannot be established; a source-of-truth
conflict prevents evaluation; repository state prevents reliable scope analysis;
an Open Decision affects completion; `implementation_verification` or
`security_review` does not exist or is not `PASS`; a change was made after
verification / security review (name `IMPLEMENTATION_VERIFICATION` /
`SECURITY_REVIEW` in `blocking_issues`); or evidence is insufficient.

---

# Prohibited Actions

This Skill must not:

- edit source code;
- edit tests;
- edit User Story or Acceptance Criteria;
- edit Specification;
- edit API or database design;
- edit Impact Analysis;
- edit Implementation Plan;
- edit Verification or Security Review;
- resolve Open Decisions;
- silently accept drift;
- redefine source of truth;
- stage files;
- commit files;
- push changes;
- create or update a Pull Request;
- change GitHub Issue status;
- merge a Pull Request;
- archive artifacts;
- mark the Story `COMPLETED`;
- update workflow state (the orchestrator does that);
- include secrets in reports;
- include generated H2 database files in the candidate scope;
- treat tool logs as stronger authority than approved requirements.

---

# Failure Handling

If a mandatory artifact is missing:

1. Create the `reconciliation` artifact with the review context.
2. List the missing artifact in `blocking_issues`; name the producing stage.
3. Return `verdict: BLOCKED`. Stop before approval.

If artifacts reference different versions (staleness):

1. identify the stale dependency and the earliest stage that must re-run;
2. return `verdict: BLOCKED` (or `CHANGES_REQUIRED` when the fix maps to a
   `RECONCILIATION.loop_back` key);
3. do not reconcile incompatible versions as if they were current.

If current code changed after `implementation_verification`:

1. identify affected files; mark verification evidence stale;
2. return `verdict: BLOCKED`; name `IMPLEMENTATION_VERIFICATION` in
   `blocking_issues`;
3. continue only enough analysis to document impact.

If security-sensitive code changed after `security_review`:

1. identify affected files; mark security evidence stale;
2. return `verdict: BLOCKED`; name `SECURITY_REVIEW` in `blocking_issues`;
3. do not approve Reconciliation.

If the remote GitHub Issue cannot be accessed:

1. continue local reconciliation when the local Story is the documented source
   of truth;
2. record the remote comparison limitation;
3. do not claim remote synchronization;
4. use BLOCKED when remote Issue authority is mandatory.

If IDEA MCP is unavailable:

1. use built-in tools and Git evidence;
2. record unavailable semantic analysis;
3. avoid unsupported architecture claims;
4. lower confidence where necessary.

If Git status contains unrelated changes:

1. classify the changes in the PR candidate classification;
2. do not modify or discard them;
3. exclude clearly unrelated files;
4. when ownership is ambiguous, put the file under
   `Human Decision Required` and return `verdict: BLOCKED` if it prevents a
   reliable scope determination.

---

# Observability

Do not disable or bypass configured telemetry hooks.

Use telemetry only to confirm execution history or investigate discrepancies.

Relevant telemetry may include:

- session identifier;
- tool name;
- timestamp;
- success or failure;
- input size;
- response size;
- execution duration when available.

Do not copy full sensitive tool payloads into the Reconciliation artifact.

Do not include:

- tokens;
- authorization headers;
- passwords;
- password hashes;
- database credentials;
- private keys;
- secret environment variables;
- unnecessary personal data.

Tool telemetry can demonstrate that a tool was invoked.

Tool telemetry cannot by itself demonstrate that the resulting implementation
is correct.

---

# Human Review Boundary

The Skill may recommend Pull Request preparation.

The Skill cannot:

- replace human diff review;
- accept scope drift;
- accept business risk;
- accept security risk;
- approve merge;
- waive missing Acceptance Criteria;
- decide between conflicting sources of truth;
- approve inclusion of ambiguous files;
- override organizational Git or security policy.

Return `verdict: BLOCKED` with an explicit "human decision required" note in
`blocking_issues` when:

- an intentional design deviation lacks recorded approval;
- PR scope cannot be determined safely;
- risk acceptance is required;
- a security exception is requested;
- unrelated changes cannot be separated reliably.

(A remote-Issue vs local-Story conflict uses `verdict: CHANGES_REQUIRED`,
`loop_back_stage: BACKLOG_SYNC` instead.)

---

# Completion Criteria

Reconciliation is complete only when:

- active Story and workflow stage are resolved;
- the current artifact chain is identified;
- mandatory artifact versions are validated;
- source Issue and local Story are compared when applicable;
- Story and Specification are reconciled;
- Specification and designs are reconciled;
- predicted and actual impact are compared;
- Plan and actual implementation are compared;
- Acceptance Criteria and tests are reconciled;
- OpenAPI and runtime implementation are reconciled;
- database design and persistence are reconciled;
- architecture and actual dependencies are reconciled;
- Implementation Verification remains current;
- Security Review remains current;
- documentation is reconciled;
- configuration and dependencies are reconciled;
- Pull Request candidate scope is classified;
- end-to-end traceability is created;
- drift is classified;
- findings are assigned loop-back targets;
- limitations are explicit;
- Reconciliation artifact is created;
- result is explicit;
- recommended next stage is explicit.

Finish with a concise summary containing:

- Reconciliation result;
- reconciled Acceptance Criteria count;
- Critical and Major finding counts;
- stale artifact count;
- planned-versus-actual deviation summary;
- Pull Request candidate file count;
- excluded file count;
- Reconciliation artifact path;
- recommended next stage.
