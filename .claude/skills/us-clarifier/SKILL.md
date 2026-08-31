---
name: us-clarifier
description: Clarifies a User Story, identifies ambiguities, missing requirements, and Open Decisions before specification writing. Owns the CLARIFICATION stage.
---

# Purpose

Own the **CLARIFICATION** stage. Analyze the active User Story and prepare it for
Specification creation by removing ambiguity and surfacing every decision that a
human must make.

# Canonical sources

- Workflow / stage: `docs/workflow/stage-map.yaml` (`CLARIFICATION`).
- Artifact paths: `docs/workflow/artifact-paths.yaml` — **authoritative**.
  Resolve `story`, `clarification_report`, `open_decisions`. Paths shown below
  are illustrative.
- Front matter: `docs/workflow/artifact-schema.md`.
- Result vocabulary: `docs/workflow/artifact-lifecycle.md`.

# Inputs (registry keys)

- `story`
- `docs/workflow/active-story.yaml` (to confirm the active Story id)
- `docs/product/product-vision.md`, `docs/product/personas.md`,
  `docs/product/business-rules.md`, `docs/product/business-glossary.md`
- `AGENTS.md`

Load only the product context needed for this Story.

# Responsibilities

Analyze: business intent, actor, business value, acceptance criteria, security
expectations, validation expectations, dependencies, assumptions.

Identify: ambiguities, contradictions, missing acceptance criteria, missing
validation rules, missing security requirements, missing non-functional
expectations.

# Open Decision detection

When information cannot be reliably derived from the Story or product docs, do
**not** invent a requirement. Record an Open Decision instead. Typical areas:
uniqueness rules, password policy, authorization rules, validation constraints,
duplicate handling, error handling, account state.

# Outputs

Both artifacts carry front matter per `docs/workflow/artifact-schema.md`.

- `open_decisions` (`docs/decisions/{story_id}-open-decisions.md`,
  `artifact_type: open_decisions`): one entry per decision with
  `id`, `question`, `context`, `affects` (stages/areas), `status: OPEN`,
  `options` (if known), `recommended` (optional, non-binding).
- `clarification_report` (`docs/evidence/{story_id}-clarification-report.md`,
  `artifact_type: clarification_report`): scope understanding, ambiguities
  found, contradictions, a checklist of what the Specification must cover, and a
  reference to every Open Decision. This report is a required **input** to
  `spec-writer`.

Do not write a Specification.

# Result Envelope

Return exactly this; the story-orchestrator records the transition (this Skill
does not update `workflow-state.yaml`):

```yaml
result:
  verdict: PASS | BLOCKED
  stage: CLARIFICATION
  story: <StoryId>
  artifact_status: DRAFT
  artifacts:
    - docs/decisions/<StoryId>-open-decisions.md
    - docs/evidence/<StoryId>-clarification-report.md
  next_stage: SPECIFICATION
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings: []
```

- `PASS` — scope is understood; ambiguities and Open Decisions are documented;
  the clarification report exists. Open Decisions may still be `OPEN`: they are
  resolved at `HUMAN_SPEC_APPROVAL`, not here.
- `BLOCKED` — the Story is missing or unintelligible, or `active-story.yaml`
  and `workflow-state.yaml` disagree.

# Prohibited

- Do not invent requirements, security rules, or business rules.
- Do not resolve Open Decisions.
- Do not write specifications, designs, or code.
- Do not update workflow state.
