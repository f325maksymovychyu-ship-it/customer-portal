# Status Vocabularies

This file is the **single authoritative source** for status values used
across the harness. There are **three separate enums**. They describe
different things and MUST NOT be mixed or conflated.

| Enum | Question it answers | Where it is stored |
|---|---|---|
| Artifact lifecycle status | "Is this document current?" | `status:` in each artifact's front matter (`artifact-schema.md`) |
| Review verdict | "What did this stage decide?" | `result.verdict` in the stage result envelope |
| Workflow status | "What state is the delivery workflow in?" | `status:` / `pending_human_gate.status` in `workflow-state.yaml` |

---

## 1. Artifact lifecycle status

Applies to every story-level artifact (front-matter `status:` field).

| Value | Meaning |
|---|---|
| `DRAFT` | Produced by its owner; not yet reviewed or approved. |
| `IN_REVIEW` | A downstream review stage is currently evaluating it. |
| `APPROVED` | Passed its review gate (and human gate where one exists). Safe to consume downstream. |
| `SUPERSEDED` | A newer version exists. The newer artifact's `supersedes:` points here. Never used as a current input. |
| `ARCHIVED` | Belongs to a delivery that has completed archive mode. Retained for history only. |

Rules:
- A revised artifact increments `version:` and sets `supersedes:` to the
  prior path/version. The prior revision becomes `SUPERSEDED`.
- Reviewers MUST check that every input artifact they consumed is the
  current (non-`SUPERSEDED`, non-`ARCHIVED`) version and that its `version:`
  matches what downstream artifacts recorded consuming.
- Stale input (a review or evidence artifact generated from a now-`SUPERSEDED`
  upstream) blocks progression until the dependent stage is re-run.

**Existing documents in this project predate this schema** (everything under
`docs/specifications/` and `docs/reviews/` before this restructure). They do
not carry this front matter yet. Treat them as `APPROVED` if a later revision
exists and superseded otherwise; add the front matter block the next time
each is materially revised rather than retrofitting all of them at once.

---

## 2. Review verdict

The only values a stage may emit in `result.verdict`.

| Value | Meaning | Effect |
|---|---|---|
| `PASS` | Stage goal met. Zero blocking findings. May carry `non_blocking_findings`. | Advance to `stage-map.yaml` `next`. |
| `CHANGES_REQUIRED` | Correctable problem. Set `loop_back_stage` to a key defined under this stage's `loop_back` map. | Route to `loop_back_stage`. |
| `BLOCKED` | Stage cannot be evaluated: missing/stale mandatory input, unresolved blocking Open Decision, environment failure, artifact conflict. | Hold at current stage; surface `blocking_issues`. |
| `NOT_APPLICABLE` | Only for a stage marked `optional: true` whose `optional_when` condition is met and recorded. | Advance to `next`. |

`APPROVED`, `REJECTED`, `FAILED`, and similar ad hoc verdicts are retired —
use the four values above.

Note: `READY_FOR_PR`, `COMPLETED`, and `ARCHIVED` are also **workflow
stages** (`stage-map.yaml`). They are not verdicts.

---

## 3. Workflow status

Stored in `workflow-state.yaml` (`status:` and inside `pending_human_gate`).

| Value | Meaning |
|---|---|
| `NOT_STARTED` | Story activated; no stage has run. |
| `IN_PROGRESS` | An automated stage is the current stage and is runnable. |
| `WAITING_FOR_HUMAN` | Current stage is a `human_gate`; `pending_human_gate.status = PENDING`. |
| `BLOCKED` | Last stage returned `BLOCKED`, or a workflow invariant failed. |
| `COMPLETED` | Reached stage `COMPLETED`. |
| `ARCHIVED` | Reached stage `ARCHIVED` via archive mode. |

`pending_human_gate.status` sub-enum: `PENDING`, `APPROVED`, `REJECTED`.
