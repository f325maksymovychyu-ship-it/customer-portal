# Workflow Stages (Explanatory)

> **Normative workflow definition:** `docs/workflow/stage-map.yaml`
>
> This document is explanatory and **non-normative**. If it ever disagrees
> with `stage-map.yaml`, `stage-map.yaml` wins.

The story-delivery workflow moves one active User Story from backlog through
to an archived, merged delivery. Each automated stage names exactly one
owning Skill. Where that Skill does not exist yet in this project, the stage
is still defined — a human does the work by hand until the Skill is built
(see `docs/workflow/gaps.md`). Human gates stop the workflow until a person
records a decision.

## Stage sequence

| # | Stage | Type | Owner | Produces (registry keys) |
|---|---|---|---|---|
| 1 | `BACKLOG_SYNC` | skill | *(gap)* | `story`, `story_catalog` |
| 2 | `CLARIFICATION` | skill | `story-clarifier` | `clarification_report`, `open_decisions` |
| 3 | `SPECIFICATION` | skill | `story-spec-writer` | `specification` |
| 4 | `SPEC_REVIEW` | skill | `story-spec-reviewer` | `specification_review` |
| 5 | `HUMAN_SPEC_APPROVAL` | human gate | human | — |
| 6 | `API_DESIGN` | skill (optional) | *(gap)* | `api_design`, `openapi` |
| 7 | `DB_DESIGN` | skill (optional) | *(gap — scaffold only)* | `database_design`, `entity_model` |
| 8 | `DESIGN_REVIEW` | skill | *(gap)* | `design_review` |
| 9 | `IMPACT_ANALYSIS` | skill | `file-impact-analyzer` | `impact_analysis` |
| 10 | `IMPLEMENTATION_PLANNING` | skill | `implementation-planner` | `implementation_plan` |
| 11 | `PLAN_REVIEW` | skill | *(gap)* | `plan_review` |
| 12 | `HUMAN_PLAN_APPROVAL` | human gate | human | — |
| 13 | `TEST_WRITING` | skill | *(gap)* | `test_strategy`, `ac_test_matrix`, `test_generation_report` |
| 14 | `IMPLEMENTATION` | skill | *(gap)* | `implementation_report` |
| 15 | `IMPLEMENTATION_VERIFICATION` | skill | *(gap)* | `implementation_verification` |
| 16 | `SECURITY_REVIEW` | skill | *(gap — `/security-review` command is a manual substitute)* | `security_review` |
| 17 | `RECONCILIATION` | skill | *(gap)* | `reconciliation`, `traceability` |
| 18 | `HUMAN_PR_APPROVAL` | human gate | human | — |
| 19 | `PR_PREPARATION` | skill | *(gap)* | `pr_summary` |
| 20 | `READY_FOR_PR` | human gate | human | — |
| 21 | `COMPLETED` | human gate | human | — |
| 22 | `ARCHIVED` | terminal | *(gap — `story-orchestrator`)* | `delivery_summary` |

See `docs/workflow/gaps.md` for what each gap needs before it can run
automatically.

## Terminal-stage meanings

- **`READY_FOR_PR`** — all automated harness checks have passed. A human
  creates or finalizes the Pull Request and records readiness.
- **`COMPLETED`** — a human confirms the Pull Request was merged, or the
  delivery was otherwise explicitly marked complete. Archive mode may only
  run from here.
- **`ARCHIVED`** — knowledge consolidation and workflow cleanup are done.
  Story artifacts are **not** moved; catalog status and artifact metadata
  separate history from active work.

## Loop-backs

Every review stage can send the Story back to an earlier stage when it finds
a correctable problem (`verdict: CHANGES_REQUIRED`). The allowed targets per
stage are defined in `stage-map.yaml` under each stage's `loop_back:` map.
