---
spec: docs/specifications/US-5.3-read-state.md
spec_revision: 1
story_id: US-5.3
source: docs/backlog/US-5.3-read-state.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Read State

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Six criteria, verbatim, all covered. This is the cleanest spec in Epic 5: four
of six criteria are fully derivable and the two Partial rows are honest about client-side
behaviour the criteria do not fully determine. F-4 is a category question rather than a
defect — NR-AC1 asserts an implementation property that only a query counter can observe.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** Blocked callout

**Problem:** The epic-wide module question in US-5.1 F-3 applies here unchanged. No
criterion in this story can be implemented until `notification/` has a home.

**Resolves when:** an architect rules on the module.

---

### F-4 · Minor · D5 Testability

**Location:** §3.1, second paragraph; OQ-4

**Evidence:**
> Exactly one database statement performs the update, not one per row. `[NR-AC1]`

**Problem:** This is an implementation constraint stated as an acceptance criterion. It is
observable only by counting queries, which couples the test to the persistence layer rather
than to behaviour.

**Why it matters:** Minor, and worth recording because it is the one place in this backlog
where a criterion reaches past observable behaviour. A refactor to a batched-but-multiple
form would fail the test while changing nothing a customer or an operator could detect.

**Resolves when:** the constraint moves to the story's Non-Functional section, or NR-AC1
states the observable property it is protecting — that the bulk call scales with one
round trip rather than with the row count.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NR-AC1 | §3.1, §4 | Recipient with 3 unread; `POST /api/v1/notifications/read` with `all: true` → `200`, `updated: 3`, `unreadCount: 0`, all three still listed; assert one statement — see F-4 | Yes |
| NR-AC2 | §3.2, §4 | Mark one read, then post its id to `/unread` → `200`, `updated: 1`, entry highlighted again, counter incremented | Yes |
| NR-AC3 | §3.1, §4 | Mark all read, repeat the call → `200`, `updated: 0`, `unreadCount: 0`, no error; run both concurrently and assert the counter never goes negative | Yes |
| NR-AC4 | §3.3, §4 | Mix own ids with a second recipient's → the foreign id ignored, `updated` counts only own, no error naming it | Yes |
| NR-AC5 | §3.4, §4 | Client unit test: under the unread filter, mark read → row leaves, undo available 5 s and restores it, no layout jump | Yes |
| NR-AC6 | §3.5, §4 | Body with neither field → `400` type `validation-failed`; 501 ids → `400` naming the limit | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…the counter never becomes negative under any interleaving of these calls." | §3.1 | `[NR-AC3]` | Yes — NR-AC3 final clause |
| "The response counts only the caller's own notifications in `updated`…" | §3.3 | `[NR-AC4]` | Yes |
| "An \"Undo\" action is available for 5 seconds and restores it…" | §3.4 | `[NR-AC5]` | Yes |
| "The notifications remain in the list; only their unread state changes." | §3.1 | `[NR-AC1]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Module placement blocks the epic** — see F-3. All criteria.
- **Cross-device convergence** — NR-AC3 makes the API safe under interleaving and says
  nothing about what the second device displays meanwhile. US-5.2 ND-AC2 requires exactly
  that convergence between tabs, so the two stories set different expectations for tabs and
  devices. Recorded as OQ-1. `[NR-AC3]`
- **`all` on the unread endpoint** — NR-AC1 defines `all: true` for reading. Whether the
  unread endpoint accepts it, and what "mark everything unread" means across 90 days of
  history, is unstated. Recorded as OQ-2. `[NR-AC2]`
- **Scope of "all"** — NR-AC1 implies everything rather than the current filter, which will
  surprise a customer who filtered first. Recorded as OQ-3. `[NR-AC1]`
- **Undo interrupted** — NR-AC5 gives a 5-second window and says nothing about navigating
  away inside it or the undo request failing. Recorded as OQ-5. `[NR-AC5]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NR-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
