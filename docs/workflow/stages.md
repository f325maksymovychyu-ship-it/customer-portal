# Workflow Stages (Explanatory)

> **Normative workflow definition:** `docs/workflow/stage-map.yaml`
>
> This document is explanatory and **non-normative**. If it ever disagrees with
> `stage-map.yaml`, `stage-map.yaml` wins. Do not define alternative stage
> identifiers here.

The story-delivery workflow moves one active User Story from backlog through to
an archived, merged delivery. Automated stages are executed by exactly one Skill.
Human gates stop the workflow until a person records a decision with
`/so:approve` or `/so:reject`.

## Stage sequence

| # | Stage | Type | Owner | Produces (registry keys) |
|---|---|---|---|---|
| 1 | `BACKLOG_SYNC` | skill | `backlog-sync` | `story`, `story_catalog` |
| 2 | `CLARIFICATION` | skill | `us-clarifier` | `clarification_report`, `open_decisions` |
| 3 | `SPECIFICATION` | skill | `spec-writer` | `specification` |
| 4 | `SPEC_REVIEW` | skill | `spec-verifier` | `specification_review` |
| 5 | `HUMAN_SPEC_APPROVAL` | human gate | human | — |
| 6 | `API_DESIGN` | skill (optional) | `openapi-designer` | `api_design`, `openapi` |
| 7 | `DB_DESIGN` | skill (optional) | `db-designer` | `database_design`, `entity_model` |
| 8 | `DESIGN_REVIEW` | skill | `design-reviewer` | `design_review` |
| 9 | `IMPACT_ANALYSIS` | skill | `impact-analyzer` | `impact_analysis` |
| 10 | `IMPLEMENTATION_PLANNING` | skill | `implementation-planner` | `implementation_plan` |
| 11 | `PLAN_REVIEW` | skill | `plan-reviewer` | `plan_review` |
| 12 | `HUMAN_PLAN_APPROVAL` | human gate | human | — |
| 13 | `TEST_WRITING` | skill | `test-writer` | `test_strategy`, `ac_test_matrix`, `test_generation_report` |
| 14 | `IMPLEMENTATION` | skill | `springboot-implementor` | `implementation_report` |
| 15 | `IMPLEMENTATION_VERIFICATION` | skill | `implementation-verifier` | `implementation_verification` |
| 16 | `SECURITY_REVIEW` | skill | `security-reviewer` | `security_review` |
| 17 | `RECONCILIATION` | skill | `reconciliation-reviewer` | `reconciliation`, `traceability` |
| 18 | `HUMAN_PR_APPROVAL` | human gate | human | — |
| 19 | `PR_PREPARATION` | skill | `pr-preparer` | `pr_summary` |
| 20 | `READY_FOR_PR` | human gate | human | — |
| 21 | `COMPLETED` | human gate | human | — |
| 22 | `ARCHIVED` | terminal | `story-orchestrator` | `delivery_summary` |

## Terminal-stage meanings

- **`READY_FOR_PR`** — all automated harness checks have passed. A human creates
  or finalizes the Pull Request and records readiness.
- **`COMPLETED`** — a human confirms the Pull Request was merged, or the delivery
  was otherwise explicitly marked complete. Archive mode may only run from here.
- **`ARCHIVED`** — knowledge consolidation and workflow cleanup are done. Story
  artifacts are **not** moved; catalog status and artifact metadata separate
  history from active work.

## Loop-backs

Every review stage can send the Story back to an earlier stage when it finds a
correctable problem (`verdict: CHANGES_REQUIRED`). The allowed targets per stage
are defined in `stage-map.yaml` under each stage's `loop_back:` map. A Skill may
only name a loop-back key that exists there.

## Retired identifiers

`CLARIFY`, `SPEC_WRITE`, `DESIGN`, `PLANNING`, `TESTING`, `VERIFICATION`,
`IMPLEMENTATION_VERIFY`, `HUMAN_REVIEW`, `PULL_REQUEST`, `DONE` are **retired**.
See `stage-map.yaml` `retired_identifiers` for the replacement mapping.
