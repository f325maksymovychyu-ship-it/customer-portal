# Known Gaps

This project adapted its workflow harness from a reference implementation
(`f325maksymovychyu-ship-it/customer-portal`) that has 21 stage Skills, an
orchestrator, and `/so:*` commands. As of 2026-09-05 this project has ported
16 stage Skills (all except the reference's 3 IDEA-tool skills and its
`skill-creator`, which this project already has its own copy of), the
`story-orchestrator`, and the `/so:*` commands — **every automated stage in
`stage-map.yaml` now has an owning Skill.** This file is the single list of
what's still missing or unverified so nobody has to rediscover it by reading
`stage-map.yaml` line by line. Per the Open Decisions Policy (`AGENTS.md`),
these are recorded as gaps rather than guessed at or silently stubbed.

## Orchestrator + all 16 stage Skills are ported, but none have run end-to-end yet

`story-orchestrator` (`.claude/skills/story-orchestrator/`), the `/so:*`
commands (`.claude/commands/so/`), and the 11 previously-missing stage Skills
— `backlog-sync`, `openapi-designer`, `database-change-planner` (reference's
`db-designer`, renamed to match this project's pre-existing scaffold dir),
`design-reviewer`, `plan-reviewer`, `test-writer`, `springboot-implementor`,
`implementation-verifier`, `security-reviewer`, `reconciliation-reviewer`,
`pr-preparer` — were all ported from the reference repo on 2026-09-05.
Adaptation needed was consistently light because the reference files already
resolve stage ids and artifact paths from `stage-map.yaml` /
`artifact-paths.yaml` rather than hardcoding them. Recurring fixes applied
across the port:

- Genericized `US-001`-style examples — this project's Story ids are not
  zero-padded (`CP-101`, `google-login`, `US-2.1`; see
  `docs/catalog/stories.yaml`).
- Remapped reference-only Skill names to this project's names (`us-clarifier`
  → `story-clarifier`, `spec-writer` → `story-spec-writer`, `spec-verifier` →
  `story-spec-reviewer`, `impact-analyzer` → `file-impact-analyzer`,
  `db-designer` → `database-change-planner`).
- Fixed the Java package base where hardcoded: this project uses
  `org.example.customerportal` (`docs/architecture/package-map.md`), not the
  reference's `com.acme.customerportal`.
- Made Git-dependent steps (branch checks, working-tree diffs, PR creation)
  conditional on this directory actually being a Git repository — it isn't
  yet (`AGENTS.md` Git Policy) — so they degrade gracefully instead of
  blocking.
- Made reads of `docs/product/business-rules.md` / `business-glossary.md` /
  `personas.md` / `non-functional-requirements.md` conditional on those files
  existing — see "Missing product documents" below.
- Called out `security-conventions.md` SC-1 (password hashing algorithm)
  explicitly wherever a Skill touches passwords, instead of letting it assume
  either BCrypt or Argon2id was already decided — see "Open technical
  decision" below.
- `backlog-sync` and `reconciliation-reviewer` both state explicitly that
  this project has no GitHub Issues source configured, so `BACKLOG_SYNC`
  returns `PASS` with a non-blocking note (not `BLOCKED`) and
  `RECONCILIATION`'s Issue-reconciliation step is `N/A`.

**None of these 16 Skills have been exercised against this project's real
Story files yet.** They were adapted by reading the reference text and this
project's docs side by side, not verified by running `/so:next` through a
live Story. Two known open risks going in:

1. **Cross-Skill consistency was not independently re-checked across the
   whole set.** Each Skill was ported by a different parallel task and
   referenced the others by name/loop-back key from its own reading of
   `stage-map.yaml`, not by reading every sibling Skill's finished file. If
   one Skill expects an envelope field, loop-back key, or artifact shape a
   sibling doesn't actually produce, that will only surface the first time
   `/so:next` walks through the affected stage pair. `reconciliation-reviewer`
   is the highest-risk case — it cross-references nearly every other Skill's
   output by name.
2. **The 5 pre-existing Skills still don't speak the Stage Result Envelope**
   (see next section) — the routing chain breaks at `CLARIFICATION`, the very
   first stage, before any of the 11 newly-ported Skills ever get invoked by
   the orchestrator automatically. The 11 new Skills are ready to be invoked
   once that's fixed, or invoked directly by name in the meantime.

The first real run through this workflow (once the envelope gap below is
closed, or by invoking Skills directly and updating `workflow-state.yaml` by
hand) should be treated as an integration test of the whole port, not just of
`story-orchestrator`.

## The 5 pre-existing Skills don't emit the Stage Result Envelope

`story-clarifier`, `story-spec-writer`, `story-spec-reviewer`,
`file-impact-analyzer`, `implementation-planner` predate this harness and do
**not** emit the Stage Result Envelope (`result: {verdict, stage, story,
artifact_status, artifacts, next_stage, loop_back_stage, blocking_issues,
non_blocking_findings}`) that `story-orchestrator` needs to route
automatically:

- `story-clarifier`, `file-impact-analyzer`, `implementation-planner` produce
  a report/plan with no verdict at all.
- `story-spec-reviewer` emits `Pass` / `Needs Changes` / `Fail` prose, not
  `PASS` / `CHANGES_REQUIRED` / `BLOCKED`.

Per `story-orchestrator`'s `SKILL.md` "Failure Handling", it treats an
unparseable result as `BLOCKED` rather than guessing — so `/so:next` will
invoke the correct Skill, the Skill will produce its artifact, and the
orchestrator will then correctly stop with `BLOCKED` at the **first** stage it
processes (`CLARIFICATION`). This is the single biggest blocker to running the
workflow end-to-end via `/so:*` commands right now — every other stage Skill
is in place and envelope-compliant, but the chain can't get past stage one
automatically.

Until these 5 Skills are updated to return the envelope (deliberately not
done here — it risks invalidating their existing eval suites, see
`.claude/skills/file-impact-analyzer-workspace/`), treat every stage as
manual: run the Skill yourself, read its artifact, and update
`workflow-state.yaml` by hand (or re-run `/so:next`, note the `BLOCKED`, and
resolve it the same way) — the same posture `AGENTS.md` already describes for
Skills with no owner.

## Missing product documents

`docs/product/` has `product-vision.md` and `epic-map.md`. The reference
project's product folder also has:

- `business-glossary.md`
- `business-rules.md`
- `personas.md`
- `non-functional-requirements.md`

These were not fabricated during this restructure — writing them accurately
requires product decisions this session doesn't have authority to make.
Several of the newly-ported Skills (`openapi-designer`, `test-writer`,
`plan-reviewer`, `security-reviewer`, `springboot-implementor`) read these
files when they exist and treat their absence as a non-blocking gap rather
than a failure — but the resulting artifacts will be less grounded than they
would be with real business rules and NFRs behind them. Draft them from
`docs/stories/README.md`'s existing "Standing constraints" and
ticket-lifecycle content (a good starting point already exists there) the
next time someone works on Epic-level product docs.

## Open technical decision blocking security work

`security-conventions.md` SC-1 — password hashing algorithm (BCrypt vs.
Argon2id) is unresolved. `docs/stories/README.md`'s existing convention notes
assume Argon2id was already decided somewhere; no canonical document actually
says so. `security-reviewer`, `springboot-implementor`, and `plan-reviewer`
all now explicitly flag implementing either algorithm without a recorded
resolution as a blocking finding, rather than silently picking one. Resolve
before any Story touching password storage passes `SPEC_REVIEW`.

## Legacy artifacts predate the new front-matter schema

Everything under `docs/stories/`, `docs/specifications/`, and
`docs/reviews/specifications/` was written before `artifact-schema.md`
existed and does not carry the front-matter block it defines. Don't
mass-retrofit it; add the block the next time each file is materially
revised (see `artifact-lifecycle.md`'s note on legacy documents).

The same ~60 legacy files also contain internal cross-references written
before this restructure: many cite the old `docs/backlog/` path (confirmed
by `grep -rl "docs/backlog" docs/`), and `docs/stories/README.md` cites
`AGENTS.md §2.1` / `§7` / `§7.5` — section numbers that don't exist in the
new canonical-sources-index `AGENTS.md`. Fix a file's stale references when
you next materially revise it, same policy as the front-matter gap above —
don't bulk-edit ~60 files' prose to chase a renumbered AGENTS.md.
