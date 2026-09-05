---
spec: docs/specifications/US-3.7-view-audit-information.md
spec_revision: 2
story_id: US-3.7
source: docs/backlog/US-3.7-view-audit-information.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — View Audit Information

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Six criteria, verbatim, all covered. The stream-identity problem this spec
raised in revision 1 as its most consequential finding — three stories writing to three
differently named logs while this one read a fourth — was closed in the backlog: every
story now writes `audit_events`, and this story declares it the single stream. F-3 remains
open and is a compliance decision rather than a document defect.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. An audit log's contract
is the artefact an external reviewer reads; unapproved criteria make it evidence of nothing.

**Resolves when:** a compliance owner signs off on the criteria.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D6 Gap handling

**Location:** A-4; OQ-2

**Evidence:**
> | A-4 | Retention is at least 12 months. | Named in the story's Non-Functional section; no
> criterion asserts any retention… |

**Problem:** Retention is load-bearing and lives in an assumption. AU-AC5 caps interactive
queries at 90 days and points at the export for longer ranges, which only works if the data
is still there. Nothing in the criteria says it is.

**Why it matters:** The partitioning and cold-storage design will be built on the figure in
A-4, which nobody agreed. It also collides with US-3.4 UD-AC6's erasure obligation, and no
criterion resolves which wins.

**Resolves when:** a compliance owner states the retention period and how it interacts with
erasure, and a criterion asserts it.

---

### F-4 · Minor · D5 Testability

**Location:** §3.3; AU-AC3

**Evidence:**
> The same holds for a caller holding every scope the system defines. `[AU-AC3]`

**Problem:** The clause is testable only against the scope set as it exists on the day the
test is written. A scope added later is not covered by the assertion, and nothing makes the
test fail when one appears.

**Why it matters:** Low impact — the API-level `405` is unconditional, so the property holds
structurally. It matters because the criterion reads as a stronger guarantee than the test
can deliver.

**Resolves when:** the immutability assertion is made against the HTTP method rather than
the caller, which A-1 already implies through the database-grant restriction.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| AU-AC1 | §3.1, §4 | Seed 120 events over 10 days; `GET` with no filters and `audit:read` → `200`, 50 items from the last 7 days newest first, each carrying nine named fields, `occurredAt` an ISO-8601 UTC instant, `nextCursor` non-null | Yes |
| AU-AC2 | §3.2, §4 | Filter by actor, event, severity and a 30-day range → only matching events; `GET` one event → before and after per changed field plus a correlation id | Yes |
| AU-AC3 | §3.3, §4 | `PUT`, `PATCH` and `DELETE` an event URI → `405` with `Allow: GET`; repeat as a caller holding every scope — see F-4 | Yes |
| AU-AC4 | §3.4, §4 | Caller without `audit:read` → `403` type `insufficient-scope`; assert a row for the refusal exists | Yes |
| AU-AC5 | §3.2, §4 | 91-day range → `400` type `range-too-wide` whose detail points at the export; filters matching nothing → `200`, empty items, null cursor | Yes |
| AU-AC6 | §3.2, §4 | `cursor=notbase64` → `400` type `invalid-cursor`; client restarts from page one | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…occurrence time is returned as an ISO-8601 instant in UTC, which the client renders in the reader's own zone." | §3.1 | `[AU-AC1]` | Yes — added to AU-AC1 in the correction |
| "The client renders no edit or delete affordance." | §3.3 | `[AU-AC3]` | Yes |
| "…the refused attempt is itself written to the audit log." | §3.4 | `[AU-AC4]` | Yes |
| "A `nextCursor` is returned while further pages exist, and is null when the result is empty." | §3.1 | `[AU-AC1]` `[AU-AC5]` | Yes — first half AU-AC1, second half AU-AC5 |

## 5. Ambiguities and Missing Edge Cases

- **Retention is an assumption** — see F-3. `[AU-AC5]`
- **Auditor access to ticket contents** — AU-AC2 returns a target reference. Whether an
  auditor may follow it into customer correspondence in Epic 4 changes the exposure
  materially and is unstated. Recorded as OQ-5. `[AU-AC2]`
- **The refusal in AU-AC4 is itself an audit row a refused caller cannot read** — intended,
  but nothing states whether those rows follow the same retention as the events beside them.
  Recorded as OQ-4. `[AU-AC4]`
- **Severity filter with no declared default** — AU-AC2 adds `severity` as a filter and
  AU-AC1 returns it on every item. Nothing states what the unfiltered feed does with
  severity, which is fine, or what an unknown severity value returns, which is not.
  `[AU-AC2]`
- **Index coverage versus the filter set** — the story indexes time, actor and event; the
  criteria now also filter by severity and target. Two filters have no stated index, which
  is a performance rather than correctness gap. `[AU-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | Revision 1's invented finding about a missing error type on AU-AC4 was itself a transcription defect, corrected before this review |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-4 |
| D6 Gap handling | Yes | Found F-3 |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`AU-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
