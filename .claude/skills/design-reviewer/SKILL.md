---
name: design-reviewer
description: >
  Independently reviews the API design (OpenAPI contract + API design notes) and
  the database design (DB design + entity model) for a User Story against the
  approved Specification, architecture conventions, security conventions, and
  cross-model consistency. Owns the DESIGN_REVIEW stage. Use after API_DESIGN and
  DB_DESIGN, before IMPACT_ANALYSIS.
---

# Purpose

Own the **DESIGN_REVIEW** stage. Provide a quality gate on the API and database
designs before the Story commits to impact analysis and planning.

Review both designs together in one pass. Do not split into separate API and DB
review stages in this version of the harness.

The Skill does not edit designs. It records findings and, on
`CHANGES_REQUIRED`, names the loop-back target.

# Canonical sources

- Workflow / stage / loop-back keys: `docs/workflow/stage-map.yaml`
  (`DESIGN_REVIEW`; loop_back keys `changes_required_api`,
  `changes_required_database`, `changes_required_both`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` (authoritative; resolve
  every path from a registry key).
- Status vocabulary: `docs/workflow/artifact-lifecycle.md`.
- Front matter: `docs/workflow/artifact-schema.md`.

# Inputs (registry keys — resolve paths from artifact-paths.yaml)

- `story`
- `specification`, `specification_review`
- `api_design`, `openapi`  (may be marked NOT_APPLICABLE upstream)
- `database_design`, `entity_model`  (may be marked NOT_APPLICABLE upstream)
- `open_decisions`
- Architecture references: `docs/architecture/architecture.md`,
  `docs/architecture/api-conventions.md`,
  `docs/architecture/persistence-conventions.md`,
  `docs/architecture/security-conventions.md`,
  `docs/architecture/package-map.md`
- `docs/product/business-rules.md`, `docs/product/non-functional-requirements.md`
- `AGENTS.md`

# Preconditions

- `specification_review` verdict is `PASS`, and `HUMAN_SPEC_APPROVAL` was
  recorded (the orchestrator will only route here after that gate).
- For each design area not explicitly marked `NOT_APPLICABLE` by the approved
  Specification, the corresponding design artifacts exist and are `DRAFT` or
  `APPROVED`, not `SUPERSEDED`.
- Architecture convention documents exist and contain real guidance. An empty or
  placeholder-only convention document that the review depends on →
  `verdict: BLOCKED`.
- No blocking Open Decision affecting API or persistence design.

If a design area is `NOT_APPLICABLE`, confirm the Specification actually says so
and review only the other area.

If **both** `API_DESIGN` and `DB_DESIGN` recorded `NOT_APPLICABLE`, there is no
design to review. Still produce a `design_review` artifact that records both
areas as out of scope (citing the Specification), and return
`verdict: NOT_APPLICABLE` — the orchestrator advances to `IMPACT_ANALYSIS`
(`DESIGN_REVIEW` is `optional: true` for exactly this case).

# Review checklist

## API design (when applicable)
- every Acceptance Criterion with externally observable behavior maps to an
  operation / status code in the OpenAPI contract;
- paths, methods, media type, versioning, and error model follow
  `api-conventions.md`;
- request and response schemas use DTOs, never entities;
- no response field exposes a credential or internal-only value;
- validation constraints from the Specification are reflected in the contract;
- error responses cover the documented failure cases (400/401/403/404/409 as
  applicable) and the structured error body from `api-conventions.md`;
- authentication / authorization per operation is stated and matches
  `security-conventions.md`;
- backward compatibility: note any breaking change to an existing contract.

## Database design (when applicable)
- entities trace to business concepts in `business-glossary.md` /
  `business-rules.md`;
- explicit column length, nullability, uniqueness, indexes — no reliance on JPA
  defaults;
- identifier type and generation follow `persistence-conventions.md`;
- sensitive fields (password hash, tokens, PII) identified with storage rules;
- schema-initialization strategy is consistent with
  `persistence-conventions.md` (no `ddl-auto` shortcut in place of explicit
  design);
- relationships and cardinality are explicit.

## Cross-model consistency
- every resource in the API maps to a coherent persistence model;
- field names, types, and constraints agree between DTO schemas and entities
  where they represent the same data;
- uniqueness / validation enforced consistently (e.g. email uniqueness at both
  request-validation and DB-constraint level);
- no business decision introduced by a design that is absent from the
  Specification or an approved decision → that is a finding, not something to
  accept.

# Findings

Classify each: `Critical` (blocks), `Major` (must fix before proceeding),
`Minor` (advisory). For every `Critical`/`Major` finding, record which design
area it belongs to (API / database / both) and the required correction.

# Output

Create `design_review` at its registry path
(`docs/reviews/designs/{story_id}-design-review.md`) with front matter per
`docs/workflow/artifact-schema.md` (`artifact_type: design_review`).

Sections: Summary; Reviewed Artifacts (paths + versions); API Design Review;
Database Design Review; Cross-Model Consistency; Security Review of Designs;
Findings (id, severity, area, evidence, required correction); Open Decisions;
Limitations; Verdict.

# Result Envelope

Return exactly this; the story-orchestrator records the transition (this Skill
does not touch `workflow-state.yaml`):

```yaml
result:
  verdict: PASS | CHANGES_REQUIRED | BLOCKED | NOT_APPLICABLE
  stage: DESIGN_REVIEW
  story: <StoryId>
  artifact_status: APPROVED        # of the design_review artifact itself
  artifacts:
    - docs/reviews/designs/<StoryId>-design-review.md
  next_stage: IMPACT_ANALYSIS
  loop_back_stage: null            # or API_DESIGN / DB_DESIGN
  blocking_issues: []
  non_blocking_findings: []
```

Loop-back selection (keys must match `stage-map.yaml` `DESIGN_REVIEW.loop_back`):

| Situation | loop_back_stage | key |
|---|---|---|
| API contract wrong / incomplete | `API_DESIGN` | `changes_required_api` |
| DB design wrong / incomplete | `DB_DESIGN` | `changes_required_database` |
| Both need changes | `API_DESIGN` | `changes_required_both` |

- `PASS` — no `Critical`/`Major` findings; both designs are sound and
  consistent.
- `CHANGES_REQUIRED` — `Critical` or `Major` findings; set `loop_back_stage`.
- `BLOCKED` — missing/stale mandatory input, empty architecture doc, or a
  blocking Open Decision.
- `NOT_APPLICABLE` — both design areas recorded `NOT_APPLICABLE` upstream; the
  `design_review` artifact documents that and `next_stage` is `IMPACT_ANALYSIS`.

# Prohibited

- Do not edit designs, the OpenAPI file, the Specification, or architecture docs.
- Do not resolve Open Decisions.
- Do not update workflow state.
- Do not create commits or Pull Requests.
