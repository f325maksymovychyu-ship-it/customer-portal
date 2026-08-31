---
name: implementation-verifier
description: >
  Independently verifies a Spring Boot implementation against the active
  User Story, Acceptance Criteria, approved Specification, API and database
  designs, Implementation Plan, tests, architecture rules, and actual
  repository state. Use after implementation and before security review,
  reconciliation, or Pull Request creation.
---

# Purpose

Independently verify that the implementation of the active User Story is:

- functionally correct;
- complete;
- traceable;
- consistent with approved artifacts;
- architecturally compliant;
- buildable;
- covered by appropriate automated tests;
- ready for a dedicated Security Review.

This Skill verifies evidence.

It must not trust completion claims from the Implementor without independently
checking the repository, tests, build results, diagnostics, and changed files.

The Skill does not modify requirements, approve security, reconcile the final
change set, create a Pull Request, or mark the Story as complete.

---

# Position in the Workflow

Canonical workflow: `docs/workflow/stage-map.yaml`. Relevant slice:

    IMPLEMENTATION
    → IMPLEMENTATION_VERIFICATION   (this Skill)
    → SECURITY_REVIEW
    → RECONCILIATION
    → HUMAN_PR_APPROVAL
    → PR_PREPARATION → READY_FOR_PR → COMPLETED → ARCHIVED

This Skill owns only the `IMPLEMENTATION_VERIFICATION` stage. Loop-back
(`stage-map.yaml`): `changes_required` → `IMPLEMENTATION`.

---

# When To Use

Use this Skill when:

- implementation has been produced for the active User Story;
- an Implementation Report exists;
- the Implementor recommends proceeding to Implementation Verification;
- source code and tests are available;
- independent verification is required before Security Review;
- a previous verification attempt failed and the implementation was corrected.

Typical requests:

- Verify the implementation of the active User Story.
- Validate US-001 against its Specification and Acceptance Criteria.
- Check whether the current implementation is ready for Security Review.
- Re-run implementation verification after fixes.
- Verify the implementation independently from the Implementor.

---

# When Not To Use

Do not use this Skill:

- before implementation exists;
- to implement missing behavior;
- to generate an Implementation Plan;
- to create tests that should have existed before implementation;
- to rewrite failed tests so that implementation passes;
- to perform the final Security Review;
- to perform final Reconciliation;
- to create, approve, or merge a Pull Request;
- to resolve Open Decisions;
- to change workflow stage automatically;
- as a replacement for human review.

---

# Independence Principle

The Implementor and Verifier have different responsibilities.

The Implementor answers:

    What was implemented?

The Verifier answers:

    What can be independently demonstrated?

The Implementation Report is evidence input, not authoritative proof.

If the Implementation Report says that a check passed, independently reproduce
that check whenever the environment allows it.

Do not copy the Implementor's conclusions into the Verification Report without
supporting evidence.

---

# Active Scope

Read:

- docs/workflow/active-story.yaml
- docs/workflow/workflow-state.yaml

Determine:

- active Story ID;
- current workflow stage;
- active artifact versions;
- implementation attempt;
- verification attempt;
- current branch when recorded;
- expected next stage.

Work only on the active User Story.

If no active Story is configured, stop and report:

    IMPLEMENTATION_VERIFICATION_BLOCKED:
    No active User Story is configured.

If the workflow stage does not allow verification, stop and report:

    IMPLEMENTATION_VERIFICATION_BLOCKED:
    Current workflow stage does not allow Implementation Verification.

Do not select or activate another Story automatically.

---

# Canonical Sources

- Workflow / stage / loop-back: `docs/workflow/stage-map.yaml`
  (`IMPLEMENTATION_VERIFICATION`; loop_back `changes_required` → `IMPLEMENTATION`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` — **authoritative**.
  Resolve every path from its registry key. Paths shown are illustrative.
- Status vocabulary: `docs/workflow/artifact-lifecycle.md`.
- Front matter: `docs/workflow/artifact-schema.md`.

# Required Context

Read AGENTS.md first. Read `docs/workflow/active-story.yaml` and
`docs/workflow/workflow-state.yaml` (read only).

Read (registry keys, resolved via `artifact-paths.yaml`):

- `story`
- `specification`, `specification_review`
- `impact_analysis`
- `implementation_plan`, `plan_review`
- `implementation_report`
- `api_design`, `openapi`, `database_design`, `entity_model`
  (or their `NOT_APPLICABLE` record)
- `design_review`
- `test_strategy`, `ac_test_matrix`  (+ the executable tests under `src/test/`)
- `open_decisions`

Read relevant telemetry only when needed: `docs/hooks/tool-usage.jsonl`,
`docs/evidence/`.

Read architecture references:

- docs/architecture/architecture.md
- docs/architecture/package-map.md
- docs/architecture/api-conventions.md
- docs/architecture/persistence-conventions.md
- docs/architecture/security-conventions.md

Read product rules:

- docs/product/business-rules.md
- docs/product/business-glossary.md
- docs/product/non-functional-requirements.md

(`open_decisions` is listed in Required Context above.)

Do not load unrelated completed Stories, historical Specifications, or archived
artifacts unless a concrete dependency requires them.

---

# Artifact Authority

Use the following authority order:

1. Active User Story and Acceptance Criteria
2. Approved Specification
3. Resolved Story decisions
4. Approved API and persistence designs
5. Approved Implementation Plan
6. Architecture and product rules
7. Test artifacts
8. Implementation Report
9. Current implementation

Source code cannot redefine approved requirements.

Tests cannot redefine approved requirements.

The Implementation Report cannot override repository evidence.

If authoritative artifacts conflict, stop and report the conflict.

Do not choose a preferred interpretation silently.

---

# Preconditions

## User Story

The active User Story must exist.

Acceptance Criteria must be identifiable.

## Specification

`specification_review` verdict is `PASS`; `HUMAN_SPEC_APPROVAL` recorded.

## Designs

`design_review` verdict is `PASS`. Required `api_design` / `openapi` /
`database_design` / `entity_model` exist or are recorded `NOT_APPLICABLE`.

## Impact Analysis

`impact_analysis` verdict is `PASS`.

## Plan Review

`plan_review` verdict is `PASS`; `HUMAN_PLAN_APPROVAL` recorded.

## Implementation Report

`implementation_report` must exist, current version. The `IMPLEMENTATION` stage
returned `verdict: PASS` (implementation candidate ready) or
`CHANGES_REQUIRED` with `loop_back_stage: IMPLEMENTATION` (still partial). A
still-partial implementation may be inspected but cannot yield a `PASS`
verification.

## Staleness

Record each consumed artifact version in the verification report `inputs`. Any
`SUPERSEDED` mandatory input → `verdict: BLOCKED`.

## Working Tree

Inspect the current Git state.

Identify:

- modified files;
- untracked files;
- deleted files;
- unrelated changes;
- generated database files;
- current branch.

Do not overwrite current changes.

## Open Decisions

Search required artifacts for unresolved markers:

- Open Decision
- OPEN
- TODO
- TBD
- FIXME
- ???
- unresolved
- to be decided

An unresolved decision is blocking when it affects observable behavior,
security, API, persistence, validation, architecture, or testing.

---

# Verification Principles

## Evidence Over Claims

A statement such as:

    Everything works.

is not evidence.

Acceptable evidence includes:

- successful build result;
- observed test execution;
- test result;
- IDEA diagnostics;
- contract comparison;
- persisted database constraints;
- semantic architecture inspection;
- traceability from Acceptance Criteria to code and tests.

## Independent Execution

Run verification independently when possible.

Do not rely only on logs copied from an earlier implementation run.

## Requirements-Based Verification

Verify observable behavior against approved requirements.

Do not verify only that code looks plausible.

## Negative-Path Verification

A successful happy path is insufficient when Acceptance Criteria define error
or rejection behavior.

## No Self-Healing During Verification

Do not modify production code or tests while verifying.

If verification finds a defect:

1. Record the finding.
2. Identify the correct loop-back stage.
3. Stop or continue collecting evidence as appropriate.
4. Do not fix the defect inside this Skill.

## Deterministic Results

Prefer deterministic build, test, diagnostics, schema, and contract evidence
over model judgment.

---

# IDEA MCP Tooling Strategy

Prefer IDEA MCP capabilities when available.

## Project and Repository Inspection

Use when appropriate:

- mcp__idea__get_project_modules
- mcp__idea__get_project_dependencies
- mcp__idea__list_directory_tree
- mcp__idea__git_status
- mcp__idea__get_repositories

## Semantic Verification

Use when appropriate:

- mcp__idea__search_symbol
- mcp__idea__get_symbol_info
- mcp__idea__analyze_calls
- mcp__idea__generate_psi_tree

Semantic analysis should be used to verify:

- package ownership;
- dependency direction;
- symbol usage;
- Controller-to-Service calls;
- forbidden Controller-to-Repository calls;
- duplicated components;
- actual implementation locations.

## Build and Diagnostics

Use when appropriate:

- mcp__idea__build_project
- mcp__idea__get_file_problems
- mcp__idea__lint_files
- mcp__ide__getDiagnostics

## Test and Runtime Execution

Use when appropriate:

- mcp__idea__get_run_configurations
- mcp__idea__execute_run_configuration
- mcp__idea__execute_terminal_command

Use an existing approved run configuration when available.

Do not create or alter run configurations unless explicitly permitted.

## Database Verification

When an IDEA database connection is already configured, use read-only
capabilities when relevant:

- mcp__idea__list_database_connections
- mcp__idea__test_database_connection
- mcp__idea__list_database_schemas
- mcp__idea__list_schema_objects
- mcp__idea__get_database_object_description
- mcp__idea__introspect_schema
- mcp__idea__execute_sql_query
- mcp__idea__preview_table_data

Do not mutate database state outside approved automated tests.

Do not create a connection automatically without human approval.

---

# Built-In Tool Fallback

If IDEA MCP is unavailable:

1. Use built-in file inspection and search.
2. Use approved Gradle commands.
3. Inspect Git status using an approved shell command.
4. Record unavailable semantic checks.
5. Avoid claiming semantic certainty where only text matching was used.

Suggested commands:

Windows:

    gradlew.bat clean test
    gradlew.bat check

Unix-compatible environment:

    ./gradlew clean test
    ./gradlew check

Use only commands supported by the current project.

Record actual exit codes and relevant output.

---

# Verification Workflow

## Step 1: Resolve Active Story

Read workflow state.

Record:

- active Story ID;
- current stage;
- Specification version;
- Plan version;
- Implementation Report version;
- implementation attempt;
- verification attempt.

Confirm that Implementation Verification is allowed.

---

## Step 2: Validate Artifact Chain

Verify that all dependent artifacts reference current versions.

Check for:

- stale Specification;
- superseded design;
- outdated Impact Analysis;
- old Implementation Plan;
- review of a different plan version;
- Implementation Report based on outdated artifacts.

If a material version mismatch exists:

1. Set verification status to BLOCKED.
2. identify stale artifacts;
3. recommend regeneration of dependent artifacts;
4. stop before functional verification.

---

## Step 3: Establish Repository Baseline

Inspect:

- current branch;
- Git status;
- changed files;
- untracked files;
- generated H2 files;
- build configuration;
- application configuration;
- existing baseline failures when evidence exists.

Record unrelated changes separately.

Do not include generated H2 database files in the intended Pull Request.

---

## Step 4: Reconstruct Required Behavior

From the User Story, Specification, designs, and Acceptance Criteria, produce a
verification checklist.

For every Acceptance Criterion identify:

- required observable behavior;
- expected success result;
- expected failure result;
- relevant API contract;
- relevant persistence effect;
- relevant security condition;
- expected automated test.

This checklist drives all subsequent verification.

---

## Step 5: Inspect Implementation Scope

Compare the actual changed files with:

- Impact Analysis;
- Implementation Plan;
- Implementation Report.

Classify changed files as:

- Planned;
- Required Supporting Change;
- Unexpected;
- Unrelated;
- Generated Runtime Artifact.

Unexpected changes require explanation.

Unrelated changes are a verification finding.

Generated runtime artifacts must not be treated as implementation deliverables.

---

## Step 6: Verify Compilation and Build

Run the approved build or check operation.

Collect:

- command or IDEA operation;
- start and completion status;
- exit status;
- compilation errors;
- warnings relevant to the Story;
- generated reports.

A failed build produces a Critical finding.

Do not continue to a PASS verdict after a failed build.

Other evidence collection may continue when useful for diagnosis.

---

## Step 7: Run Automated Tests

Run Story-relevant tests first.

Then run the required project test suite.

Record:

- test command;
- number or names of relevant test groups when available;
- passed tests;
- failed tests;
- skipped tests;
- aborted tests;
- execution limitations.

A test reported as existing but not executed is not verified.

A test passing only because it was disabled is not evidence.

---

## Step 8: Verify Acceptance Criteria

For every Acceptance Criterion, assign one status:

- VERIFIED;
- PARTIALLY_VERIFIED;
- NOT_VERIFIED;
- FAILED;
- BLOCKED.

Provide:

- implementation location;
- test evidence;
- runtime or contract evidence;
- outstanding gap.

An Acceptance Criterion cannot be marked VERIFIED solely because matching code
exists.

---

# Customer Registration Verification Profile

When the active Story is User Registration, verify at minimum the following.

## Successful Registration

Verify:

- valid request is accepted;
- expected HTTP status is returned;
- account is persisted;
- response matches the approved contract.

## Duplicate Email

Verify:

- duplicate email is rejected;
- expected HTTP status is returned;
- no second account is created;
- case sensitivity follows the approved business rule.

## Invalid Email

Verify:

- invalid format is rejected;
- expected HTTP status is returned;
- persistence is not performed.

## Password Policy

Verify:

- approved length and structure rules are enforced;
- invalid password is rejected;
- password policy is not invented by implementation.

## Secure Response

Verify:

- plaintext password is absent;
- password hash is absent;
- internal credential fields are not serialized.

---

## Step 9: Verify API Contract

When the Story changes API behavior, compare implementation with the approved
OpenAPI artifact.

Verify:

- path;
- HTTP method;
- request schema;
- response schema;
- status codes;
- content types;
- validation errors;
- conflict errors;
- authentication requirements;
- authorization requirements.

Record every contract deviation.

Do not update OpenAPI to match an incorrect implementation.

---

## Step 10: Verify Persistence Behavior

Compare implementation and actual schema evidence with the approved DB design.

Verify:

- table or entity representation;
- primary key;
- expected columns;
- explicit lengths;
- nullability;
- uniqueness;
- indexes where required;
- repository behavior;
- file-based H2 configuration;
- database file exclusion from Git.

Do not treat JPA annotations alone as proof of actual runtime schema behavior
when schema inspection is available.

For this training project, verify that the application does not silently use
an in-memory H2 database when file persistence is required.

---

## Step 11: Verify Architecture

Use semantic analysis when available.

Verify:

- Controller delegates to Service;
- Service owns business behavior;
- Repository owns persistence access;
- Controller does not access Repository directly;
- HTTP-specific types do not leak into Service logic unless explicitly
  permitted;
- persistence entities are not exposed as public API models;
- package structure follows package-map.md;
- no unnecessary architectural layer was introduced;
- no approved boundary was bypassed.

Architecture violations are Major or Critical findings depending on impact.

---

## Step 12: Verify Validation and Error Handling

Verify:

- request validation is active;
- invalid input produces the approved response;
- uniqueness conflict is mapped correctly;
- internal exceptions are not exposed;
- error representation follows API conventions;
- error behavior is consistent across relevant endpoints.

Check actual framework wiring.

The presence of validation annotations is insufficient if validation is not
activated at runtime.

---

## Step 13: Verify Basic Security-Relevant Behavior

This stage performs only the security checks required to establish functional
readiness.

The dedicated `security-reviewer` performs the broader adversarial review.

Verify at minimum:

- plaintext passwords are not persisted;
- password hashes are not returned;
- passwords are not logged;
- H2 console is not exposed without approval;
- sensitive fields are excluded from API responses;
- required authentication or authorization is wired;
- no obvious permissive security configuration bypasses the Story constraints.

Any suspected vulnerability must be forwarded to Security Review even when it
does not block functional verification.

---

## Step 14: Verify Configuration

Inspect relevant configuration files.

Verify:

- H2 uses the approved file-based location;
- generated database files are ignored by Git;
- unsafe H2 console exposure is absent;
- schema behavior matches approved persistence conventions;
- secrets are not committed;
- active profiles do not invalidate test evidence;
- configuration changes match the approved plan.

Flag undocumented configuration changes.

---

## Step 15: Inspect Diagnostics and Warnings

Collect IDEA and compiler diagnostics.

Classify findings as:

- Error;
- Relevant Warning;
- Unrelated Warning;
- Informational.

A clean build does not automatically mean that IDEA diagnostics are clean.

Record relevant warnings that may affect reliability, nullability, security, or
future maintenance.

---

## Step 16: Verify Test Quality

Review tests, not only test results.

Check whether tests:

- map to Acceptance Criteria;
- verify observable behavior;
- include negative scenarios;
- assert persistence effects when relevant;
- assert sensitive data is not exposed;
- avoid depending on execution order;
- avoid false-positive assertions;
- do not bypass Spring Security unintentionally;
- do not mock away the behavior they claim to verify.

Do not rewrite tests during verification.

---

## Step 17: Check Scope and Regression Risk

Check for:

- unrelated refactoring;
- unnecessary dependency additions;
- configuration changes outside the Story;
- public API changes not described by Specification;
- modified behavior in unrelated modules;
- broad security configuration changes;
- removed or weakened tests.

Run the broader project test suite when feasible.

If full regression testing is not possible, explicitly report the limitation.

---

## Step 18: Evaluate Implementation Report Accuracy

Compare the Implementation Report with actual repository evidence.

Identify:

- omitted files;
- incorrect status claims;
- tests claimed but not run;
- validation claimed but not observed;
- undocumented plan deviations;
- security-sensitive changes not disclosed.

Implementation Report inaccuracies must be recorded as findings.

---

## Step 19: Classify Findings

Classify each finding as:

### Critical

Blocks progression.

Examples:

- build failure;
- failed Acceptance Criterion;
- plaintext password storage;
- password hash exposure;
- missing required behavior;
- implementation contradicts approved Specification;
- test manipulation that hides incorrect behavior;
- unresolved decision materialized as code.

### Major

Requires correction before Security Review or Reconciliation.

Examples:

- missing negative test;
- architecture violation;
- undocumented configuration change;
- contract mismatch;
- missing persistence constraint;
- relevant diagnostics;
- unexplained scope expansion.

### Minor

Does not block progression but should be corrected or documented.

Examples:

- documentation inconsistency;
- non-blocking warning;
- naming issue;
- low-risk maintainability improvement.

---

## Step 20: Determine Loop-Back Target

`stage-map.yaml` defines exactly one loop-back for
`IMPLEMENTATION_VERIFICATION`: `changes_required` → `IMPLEMENTATION`.

- A **code defect with correct upstream artifacts** → `verdict:
  CHANGES_REQUIRED`, `loop_back_stage: IMPLEMENTATION`.
- A defect caused by an **upstream artifact** (Specification / design /
  impact-analysis / plan / a missing test) → `verdict: BLOCKED`; name the
  responsible upstream stage in `blocking_issues` so a human can route it. Do
  not silently send an upstream defect to `IMPLEMENTATION`.

---

## Step 21: Create Verification Report

Create the `implementation_verification` artifact at its registry path
(`docs/verification/{story_id}-implementation-verification.md`), front matter per
`docs/workflow/artifact-schema.md`
(`artifact_type: implementation_verification`).

Do not modify production code or tests. Do not update workflow state. Do not
create a commit or Pull Request.

**This Skill owns the authoritative build and test evidence for the Story.**
Downstream stages reuse it and only re-run validation if tracked files changed
after this verification.

---

# Verification Report Format

## Front Matter

Shared block from `docs/workflow/artifact-schema.md`
(`artifact_type: implementation_verification`), plus:
`build_status`, `tests_status` (`PASS` / `FAIL` / `NOT_RUN`),
`acceptance_criteria_verified`, `acceptance_criteria_total`,
`critical_findings`, `major_findings`, `minor_findings`,
`semantic_analysis` (`IDEA_MCP` / `TEXT_FALLBACK` / `UNAVAILABLE`).
`created_at` / `updated_at` are runtime timestamps.

Illustrative (dates are examples only):

    ---
    artifact_type: implementation_verification
    story: US-001
    version: 1
    status: DRAFT
    created_at: <runtime>
    updated_at: <runtime>
    produced_by: implementation-verifier
    inputs:
      - path: docs/evidence/US-001-implementation-report.md
        version: 1
      - path: docs/specifications/US-001-spec.md
        version: 1
      - path: docs/plans/US-001-implementation-plan.md
        version: 1
    supersedes: null
    build_status: PASS
    tests_status: FAIL
    acceptance_criteria_verified: 4
    acceptance_criteria_total: 5
    critical_findings: 1
    major_findings: 1
    minor_findings: 0
    semantic_analysis: IDEA_MCP
    ---

## 1. Executive Summary

Summarize:

- verification result;
- build status;
- test status;
- Acceptance Criteria coverage;
- critical risks;
- recommended next action.

## 2. Verified Artifacts

List exact paths and versions of all reviewed artifacts.

## 3. Environment

Record:

- Java version;
- Spring Boot version;
- Gradle environment;
- active profile;
- database mode;
- verification tools;
- unavailable capabilities.

Do not record secrets.

## 4. Repository State

Record:

- branch;
- modified files;
- untracked files;
- deleted files;
- unrelated changes;
- generated runtime artifacts.

## 5. Build Evidence

Record:

- command or tool;
- result;
- exit status when available;
- errors;
- relevant warnings.

## 6. Test Evidence

Record:

- commands or run configurations;
- test groups;
- results;
- failures;
- skipped tests;
- limitations.

## 7. Acceptance Criteria Matrix

For every Acceptance Criterion record:

- ID;
- required behavior;
- implementation evidence;
- test evidence;
- status;
- findings.

## 8. API Contract Verification

Record:

- matched operations;
- mismatches;
- missing behavior;
- extra undocumented behavior;
- status code verification;
- response data exposure.

## 9. Persistence Verification

Record:

- expected design;
- implementation evidence;
- runtime schema evidence when available;
- constraint mismatches;
- H2 persistence behavior;
- generated file handling.

## 10. Architecture Verification

Record:

- package compliance;
- dependency direction;
- component responsibilities;
- semantic evidence;
- violations.

## 11. Validation and Error Handling

Record:

- input validation;
- error mapping;
- runtime activation;
- negative-path evidence.

## 12. Basic Security Readiness

Record:

- password handling;
- sensitive response fields;
- H2 console state;
- authentication and authorization wiring;
- concerns forwarded to Security Review.

## 13. Configuration Verification

Record:

- relevant settings;
- plan alignment;
- unsafe defaults;
- undocumented changes.

## 14. Test Quality Review

Record:

- AC coverage;
- positive scenarios;
- negative scenarios;
- false-positive risks;
- missing tests.

## 15. Scope Verification

Compare:

- planned files;
- actual files;
- required supporting files;
- unexpected files;
- unrelated files.

## 16. Implementation Report Accuracy

List discrepancies between the report and repository evidence.

If none exist, state:

    The Implementation Report is materially consistent with observed evidence.

## 17. Findings

For every finding provide:

- ID;
- severity;
- category;
- affected artifact or file;
- observed evidence;
- expected behavior;
- why it matters;
- required correction;
- loop-back target.

## 18. Verification Limitations

List:

- checks not executed;
- unavailable tools;
- environment restrictions;
- low-confidence conclusions;
- remaining manual checks.

## 19. Verdict Rationale

Explain the verdict (see Result Envelope below). Do not use `PROCEED_TO_*` /
`RETURN_TO_*` labels — they are retired.

---

# Result Envelope

Return exactly this; the story-orchestrator records the transition — this Skill
does not update `workflow-state.yaml`:

```yaml
result:
  verdict: PASS | CHANGES_REQUIRED | BLOCKED
  stage: IMPLEMENTATION_VERIFICATION
  story: <StoryId>
  artifact_status: APPROVED        # of the verification artifact itself
  artifacts:
    - docs/verification/<StoryId>-implementation-verification.md
  next_stage: SECURITY_REVIEW
  loop_back_stage: null            # or IMPLEMENTATION
  blocking_issues: []
  non_blocking_findings: []
```

- `PASS` — build passes; all required tests pass with observed evidence; every
  Acceptance Criterion is `VERIFIED`; no Critical or Major findings. Minor
  findings go in `non_blocking_findings`. The orchestrator advances to
  `SECURITY_REVIEW`.
- `CHANGES_REQUIRED` — build fails, a required test fails, an Acceptance
  Criterion is not verified, or a Critical/Major code defect exists **with
  correct upstream artifacts** → `loop_back_stage: IMPLEMENTATION`.
- `BLOCKED` — a mandatory input is missing/stale; the environment prevents
  meaningful verification; the active Story cannot be determined; unrelated
  repository state prevents reliable evidence; an unresolved decision invalidates
  expected behavior; or the defect originates in an **upstream artifact** (name
  the responsible stage in `blocking_issues`).

---

# Prohibited Actions

This Skill must not:

- edit production code or tests;
- alter the User Story, Acceptance Criteria, Specification, designs, or plan;
- resolve Open Decisions;
- suppress diagnostics; disable tests; change assertions to make tests pass;
- update OpenAPI or database constraints to match incorrect code;
- approve security posture (that is `security-reviewer`'s stage);
- update workflow state (the orchestrator does that);
- commit, push, or create/merge a Pull Request;
- mark the Story `COMPLETED`;
- claim verification without observed evidence.

---

# Failure Handling

If build execution fails because of the implementation:

1. Create the verification artifact.
2. Record a Critical finding.
3. Return `verdict: CHANGES_REQUIRED`, `loop_back_stage: IMPLEMENTATION`.

If build execution cannot start because of the environment:

1. Record the environment blocker.
2. Return `verdict: BLOCKED`; explain what evidence is unavailable.

If an automated test fails:

1. Preserve failure output.
2. Map the failure to an Acceptance Criterion or implementation area.
3. Do not change the test.
4. If the cause is a code defect with correct artifacts →
   `verdict: CHANGES_REQUIRED`, `loop_back_stage: IMPLEMENTATION`. If the cause
   is a missing/incorrect test or an upstream artifact → `verdict: BLOCKED`,
   name the responsible stage (`TEST_WRITING` / `SPECIFICATION` / ...) in
   `blocking_issues`.

If IDEA MCP is unavailable:

1. Use Gradle and built-in tools.
2. Record unavailable semantic checks.
3. mark semantic_analysis as TEXT_FALLBACK or UNAVAILABLE.
4. avoid unsupported claims.

If database inspection is unavailable:

1. verify persistence artifacts and tests;
2. record missing runtime schema evidence;
3. do not claim schema compliance solely from assumptions.

---

# Observability

Do not disable or bypass configured telemetry hooks.

Reference available tool usage logs when relevant.

Verification evidence may include:

- Claude Code tool calls;
- IDEA MCP operations;
- build invocations;
- test invocations;
- tool response sizes;
- session identifiers;
- success or failure status.

Do not duplicate full telemetry logs in the Verification Report.

Do not record:

- authorization headers;
- tokens;
- passwords;
- database credentials;
- secret environment values;
- unnecessary personal data.

---

# Human Review Boundary

This Skill may recommend progression to Security Review.

It cannot:

- provide final Pull Request approval;
- replace human code review;
- approve merge;
- accept an unresolved product decision;
- waive a Critical or Major finding.

Human reviewers retain responsibility for reviewing the diff and approving the
Pull Request.

---

# Completion Criteria

Implementation Verification is complete only when:

- the active Story is resolved;
- input artifact versions are validated;
- repository state is captured;
- build is executed or a blocker is documented;
- relevant tests are executed or limitations are documented;
- every Acceptance Criterion receives an explicit status;
- API behavior is verified when relevant;
- persistence behavior is verified when relevant;
- architecture is checked;
- validation and error handling are checked;
- basic security readiness is checked;
- configuration is checked;
- actual change scope is compared with the plan;
- Implementation Report accuracy is evaluated;
- findings are classified;
- loop-back targets are assigned;
- Verification Report is saved;
- the result and recommended next stage are explicit.

Finish with a concise summary containing:

- verification result;
- build status;
- test status;
- Acceptance Criteria coverage;
- Critical and Major finding counts;
- Verification Report path;
- recommended next stage.
- 