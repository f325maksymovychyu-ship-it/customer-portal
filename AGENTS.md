# Customer Portal

A training project demonstrating an artifact-driven agentic SDLC. A minimal
Spring Boot application evolves through User Stories, each delivered by the
workflow defined in `docs/workflow/`.

All changes must be traceable to documented requirements and workflow artifacts.

---

# Canonical Sources (authoritative — do not duplicate their content elsewhere)

| Concern | File |
|---|---|
| Workflow: stages, order, ownership, transitions, loop-backs, human gates | `docs/workflow/stage-map.yaml` |
| Where every artifact lives and who owns it | `docs/workflow/artifact-paths.yaml` |
| Status vocabularies (artifact status / review verdict / workflow status) | `docs/workflow/artifact-lifecycle.md` |
| Workflow-state & active-story schema, history event schema | `docs/workflow/state-schema.md` |
| Artifact front-matter schema | `docs/workflow/artifact-schema.md` |
| Human-readable workflow overview (non-normative) | `docs/workflow/stages.md` |
| Architecture decisions | `docs/architecture/architecture.md` |
| Package ownership & dependency rules | `docs/architecture/package-map.md` |
| API conventions | `docs/architecture/api-conventions.md` |
| Persistence conventions | `docs/architecture/persistence-conventions.md` |
| Security conventions & training-project security policy | `docs/architecture/security-conventions.md` |
| Product context | `docs/product/` (vision, epic-map, business-glossary, business-rules, personas, non-functional-requirements) |

No Skill, command, or document may define an alternative stage list, alternative
stage identifiers, or an alternative artifact-path convention. If two documents
disagree, the canonical file above wins.

---

# Technology Stack

- Java 21
- Spring Boot 4.x — Spring Web MVC, Spring Data JPA, Spring Security
- Gradle (Kotlin DSL)
- H2 (file-based locally; isolated in-memory for tests — see
  `docs/architecture/persistence-conventions.md`)
- JUnit 5, Spring Boot Test
- Lombok where project conventions allow

Always verify actual project dependencies before relying on a library.

---

# Active Scope

The active Story is defined by `docs/workflow/active-story.yaml` and its
execution state by `docs/workflow/workflow-state.yaml`. Story lifecycle status
is owned by `docs/catalog/stories.yaml`. Work only on the active Story unless
explicitly instructed otherwise.

Only `story-orchestrator` writes `workflow-state.yaml`. Only `backlog-sync`
(directly or orchestrator-delegated) writes `active-story.yaml` and
`docs/catalog/stories.yaml`. Stage Skills never write workflow state — they
return a result envelope and the orchestrator records the transition.

---

# Artifact-Driven Development

Code generation is always driven by approved artifacts. The delivery flow is
`docs/workflow/stage-map.yaml`. Do not start implementation directly from a
User Story. Do not invent endpoints, schema, security behavior, or business
rules during coding — if something is undefined, record an Open Decision.

Order of authority when artifacts conflict:

1. User Story & Acceptance Criteria
2. Approved Specification
3. Resolved Open Decisions
4. Approved API & database designs
5. Approved Implementation Plan

Implementation never overrides a documented requirement.

---

# Human Gates

`stage-map.yaml` defines gates (`HUMAN_SPEC_APPROVAL`, `HUMAN_PLAN_APPROVAL`,
`HUMAN_PR_APPROVAL`, `READY_FOR_PR`, `COMPLETED`) where the workflow stops for a
person. A review Skill returning `PASS` is **not** human approval. Approval is
recorded only via `/so:approve` (or `/so:reject`). Auto Mode never passes a
human gate.

---

# Open Decisions Policy

Open Decisions are blockers. If an approved artifact contains `TODO`, `TBD`,
`FIXME`, `???`, or an unresolved Open Decision that affects the next stage, do
not proceed. Instead: document the gap, request clarification, update the
Specification. Clarification is always preferred over guessing.

---

# Security Policy

Security-first defaults are mandatory. The full policy (password rules,
authentication model, CSRF, `ddl-auto` restrictions, H2 console, secrets) lives
in `docs/architecture/security-conventions.md`. Do not weaken it without a
human-approved Open Decision. Never commit secrets. Passwords are stored only as
a BCrypt hash and never returned by any API.

---

# Git Policy

- Generated changes stay scoped to the active Story. No opportunistic
  refactoring, no unrelated file edits.
- Branch before committing when on the default branch.
- Skills do not commit, push, create, or merge Pull Requests. `pr-preparer`
  assembles the PR summary; a human creates the PR.
- Generated database files, IDE-local config, and secrets never enter a
  commit.

---

# Observability

- `docs/workflow/history.jsonl` — the single append-only workflow transition
  log (owned by `story-orchestrator`).
- `docs/hooks/tool-usage.jsonl` — tool-usage telemetry (separate; metadata
  only, never full sensitive payloads).

Telemetry is execution evidence, never requirement authority. Do not disable or
bypass configured hooks.

---

# Agent Behavior

When information is missing: do not assume, do not invent requirements,
security rules, or business rules. Record an Open Decision, explain the
uncertainty, and request clarification.
