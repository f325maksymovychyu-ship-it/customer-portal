# Workflow State Schema

Authoritative schema for the two mutable workflow-state files.

- `docs/workflow/active-story.yaml` — identity + activation metadata of the
  active Story. Owner: `backlog-sync` (directly, or delegated by
  `story-orchestrator` on activation — see `references/start-flow.md`);
  `backlog-sync` itself is still a gap (`docs/workflow/gaps.md`), so
  `story-orchestrator` writes this file directly for now.
- `docs/workflow/workflow-state.yaml` — execution state of the active
  delivery workflow. Owner: `story-orchestrator` only.

---

## `active-story.yaml`

```yaml
version: 1
active_story: US-2.1                                     # canonical Story id, or null when idle
story_path: docs/stories/US-2.1-login.md                 # resolved from artifact-paths.yaml `story`
source:
  type: local_only                                        # this project has no GitHub-issue source configured
  repository: null
  issue_number: null
  issue_url: null
activated_at: null                                        # ISO-8601 or null
activated_by: null                                        # actor string or null
status: NOT_STARTED | IN_PROGRESS | COMPLETED | ARCHIVED
```

Rules:
- Exactly one Story may be active. Multiple `active_story` values, or a value
  that disagrees with `workflow-state.yaml.story`, is an INCONSISTENT state.
- `source.*` stays `null` — this project has no GitHub-issue backlog source
  configured. Do not fabricate repository names or issue numbers.
- `status` here mirrors the Story's participation status in
  `docs/catalog/stories.yaml`.

---

## `workflow-state.yaml`

```yaml
version: 1
story: US-2.1                          # must equal active-story.yaml.active_story
workflow: story-delivery               # matches stage-map.yaml `workflow`
current_stage: SPECIFICATION           # a canonical id from stage-map.yaml stage_order
previous_stage: CLARIFICATION          # canonical id or null
last_completed_stage: CLARIFICATION    # canonical id or null
status: IN_PROGRESS                    # workflow status (artifact-lifecycle.md §3)
attempt: 1                             # attempt counter for current_stage, >=1
last_invoked_skill: null               # skill name or null
last_result:
  verdict: null                        # PASS | CHANGES_REQUIRED | BLOCKED | NOT_APPLICABLE
  stage: null
  recorded_at: null
last_artifacts: []                     # list of {type, path, version} produced by last stage
pending_human_gate: null               # see below; null unless current_stage is a human_gate
blocking_issues: []                    # list of strings; non-empty only when status == BLOCKED
non_blocking_findings: []              # carried-forward advisory findings
started_at: null                       # ISO-8601 when first stage ran
updated_at: null                       # ISO-8601 of last write
completed_at: null                     # ISO-8601 when stage COMPLETED was reached
archived_at: null                      # ISO-8601 when stage ARCHIVED was reached
```

### `pending_human_gate` sub-object

Present (non-null) exactly when `current_stage` is a `human_gate` stage.

```yaml
pending_human_gate:
  stage: HUMAN_SPEC_APPROVAL
  status: PENDING | APPROVED | REJECTED
  required_artifacts:
    - type: specification
      path: docs/specifications/US-2.1-login.md
      version: 1
    - type: specification_review
      path: docs/reviews/specifications/US-2.1-login-review-01.md
      version: 1
  automated_verdict: PASS
  blocking_findings: []
  requested_at: <ISO-8601>
  decided_at: null
  decided_by: null
  comment: null
```

### Transition rules

1. Only `story-orchestrator` mutates this file, and only after a stage
   returns a result or a human gate is decided.
2. Every mutation also appends one event to `docs/workflow/history.jsonl`
   (schema below). History is append-only; never rewritten.
3. `current_stage` must always be a member of `stage-map.yaml` `stage_order`.
4. On `verdict: PASS` → `current_stage := stage.next`, `attempt := 1`. On
   `verdict: CHANGES_REQUIRED` → `current_stage := stage.loop_back[key]`, and
   if the target equals the previous occurrence of that stage,
   `attempt += 1`. On `verdict: BLOCKED` → `current_stage` unchanged,
   `status := BLOCKED`.
5. Entering a `human_gate` stage sets `status := WAITING_FOR_HUMAN` and
   builds `pending_human_gate`. Approval sets it `APPROVED` and advances to
   `on_approve`; rejection sets it `REJECTED` and advances to `on_reject`.
6. Nobody infers human approval from a review stage's `PASS`.

---

## `history.jsonl` event schema

One JSON object per line, append-only.

```json
{
  "timestamp": "<ISO-8601 runtime>",
  "story": "US-2.1",
  "from_stage": "SPECIFICATION",
  "to_stage": "SPEC_REVIEW",
  "skill": "story-spec-writer",
  "verdict": "PASS",
  "artifacts": ["docs/specifications/US-2.1-login.md"],
  "attempt": 1
}
```

`verdict` in a history event is one of the review verdicts (`PASS` /
`CHANGES_REQUIRED` / `BLOCKED` / `NOT_APPLICABLE`) or one of the
lifecycle-event markers: `ACTIVATED`, `HUMAN_APPROVED` / `HUMAN_REJECTED`,
`ARCHIVED`.
