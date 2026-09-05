# Artifact Front-Matter Schema

Authoritative schema for the YAML front matter of every **story-level
Markdown artifact** produced by a workflow stage. New artifacts MUST emit
this block; reviewers MUST validate it.

## Required block

```yaml
---
artifact_type: <canonical-artifact-type>   # a key from docs/workflow/artifact-paths.yaml
story: US-2.1
version: 1                                  # integer, starts at 1, +1 per revision
status: DRAFT                               # artifact lifecycle status (artifact-lifecycle.md §1)
created_at: <ISO-8601 timestamp, runtime>   # never hard-coded
updated_at: <ISO-8601 timestamp, runtime>
produced_by: <skill-name>                   # the owning Skill from artifact-paths.yaml, or "human" if owner is null
inputs:                                     # every artifact this one consumed
  - path: <relative-path>
    version: <integer or null>
supersedes: null                            # or the relative path of the prior version
---
```

## Field rules

| Field | Rule |
|---|---|
| `artifact_type` | Must match a registry key in `artifact-paths.yaml`. |
| `story` | Canonical Story id as used in `docs/catalog/stories.yaml` (e.g. `US-2.1`, `CP-101`). |
| `version` | `1` on first creation. A re-run that changes content increments it. |
| `status` | One of `DRAFT, IN_REVIEW, APPROVED, SUPERSEDED, ARCHIVED`. New artifacts start `DRAFT`. |
| `created_at` / `updated_at` | Generated at runtime from the system clock. |
| `produced_by` | The Skill named as `owner` of this `artifact_type` in `artifact-paths.yaml`, or `human` where `owner: null` (see `docs/workflow/gaps.md`). |
| `inputs[]` | One entry per consumed artifact, with the version that was read. Enables stale-input detection. |
| `supersedes` | `null` unless this revision replaces an earlier one; then the earlier file's path. The earlier file's `status` becomes `SUPERSEDED`. |

## Staleness contract

- A reviewer that consumes artifact X at `version: N` records
  `{path: X, version: N}` in its own `inputs`.
- If X is later revised to `version: N+1` (old becomes `SUPERSEDED`), any
  downstream artifact still recording `version: N` is **stale**.
- Stale review or evidence artifacts **block progression** (`verdict:
  BLOCKED`) until the dependent stage re-runs against the current version.

## OpenAPI YAML exception

`openapi` (`docs/designs/api/{story_id}-openapi.yaml`) is a YAML contract,
not Markdown. It carries traceability via top-level extension keys instead of
a front-matter block:

```yaml
info:
  version: "1"
x-story: US-2.1
x-source-specification: docs/specifications/US-2.1-login.md
x-source-specification-version: 1
x-produced-by: openapi-designer
```

## Story artifact exception

The `story` artifact (`docs/stories/{story_id}-{slug}.md`) is authored by a
human — it is an **input** to the workflow, not a stage output. It does not
carry the produced-artifact block above. Its front matter (once adopted; see
`docs/workflow/gaps.md`) is:

```yaml
---
id: US-2.1
epic: EPIC-2
title: Login
slug: login
priority: HIGH
source:
  type: local_only
  repository: null
  issue_number: null
  issue_url: null
---
```

Story lifecycle status lives in `docs/catalog/stories.yaml`, never in this
file.

## Non-Markdown / generated artifacts

`workflow_history` (JSONL) and `story_catalog` (YAML) are governed by their
own schemas (`state-schema.md`, and the catalog header) and do not use this
front matter.
