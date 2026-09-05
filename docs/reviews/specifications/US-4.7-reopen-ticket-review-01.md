---
spec: docs/specifications/US-4.7-reopen-ticket.md
spec_revision: 2
story_id: US-4.7
source: docs/backlog/US-4.7-reopen-ticket.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Reopen Ticket

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 1 |

No Blockers, and this spec is now the only one in the set with zero Partial rows. Revision 1
found the backlog's clearest case of a value bounded by example but never set — TO-AC1 used
5 days, TO-AC2 used 20, and any boundary between them satisfied both while TO-AC2 required
an error message stating a number that did not exist. The correction fixed the window at 14
days inclusive, gave the error its exact text, added `relatedTicketId` to US-4.1's contract,
and settled the reason/explanation naming.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. The 14-day window is a
customer-facing policy.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Minor · D5 Testability

**Location:** §3.1; A-2

**Evidence:**
> | A-2 | The window is measured from the closure timestamp. | Neither criterion says from
> what. For a ticket auto-closed under US-4.6 TS-AC3, closure and last message are a week
> apart. |

**Problem:** TO-AC2 now states "14 days from `tickets.closed_at`, inclusive", which resolves
the measurement point at the API level. The assumption is stale rather than wrong.

**Why it matters:** Minor. A stale assumption reads as an unresolved choice and sends a
reader looking for a decision that the criterion already made.

**Resolves when:** A-2 is retired or restated as the customer-experience consequence — that
an auto-closed ticket's window starts a week after the customer last heard anything.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TO-AC1 | §3.1, §4 | Own ticket closed 5 days ago, non-empty reason → status `REOPENED`, same reference, full history attached, reason appended as a public message, previous agent assigned, SLA restarted at zero | Yes |
| TO-AC2 | §3.2, §4 | Injected `Clock` at 14 days exactly → reopen accepted; at 14 days plus one second → `422` type `reopen-window-expired` with the exact detail; client offers a create call carrying `relatedTicketId` | Yes |
| TO-AC3 | §3.3, §4 | Reopen a second customer's ticket → `404`, matching US-4.2 TL-AC3 | Yes |
| TO-AC4 | §3.3, §4 | Reopen an `IN_PROGRESS` ticket → `422` type `illegal-transition` listing the permitted next states | Yes |
| TO-AC5 | §3.4, §4 | Reopen with a blank reason → `400` type `validation-failed` with the exact detail | Yes |
| TO-AC6 | §3.1, §4 | Deactivate the previous assignee, then reopen → ticket enters the unassigned queue, shows the exact message, no notification sent to the deactivated account | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…the window boundary is evaluated at exactly 14 days from `tickets.closed_at`, inclusive." | §3.2 | `[TO-AC2]` | Yes — added in the correction |
| "…that action posts to `POST /api/v1/support/tickets` with `relatedTicketId` set to the closed ticket." | §3.2 | `[TO-AC2]` | Yes — the field now exists in US-4.1's contract |
| "The reason is appended to the thread as a public message." | §3.1 | `[TO-AC1]` | Yes — naming settled to "reason" in the correction |
| "…no notification is sent to the deactivated account." | §3.1 | `[TO-AC6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Window origin for auto-closed tickets** — see F-3. `[TO-AC2]`
- **No cap on repeated reopening** — `reopen_count` exists in the data model precisely so an
  escalation rule can be written, and none is. Recorded as OQ-4. `[TO-AC1]`
- **Agent-initiated reopen** — forbidden by omission rather than by a criterion. Support
  teams routinely need it after a phone call. Recorded as OQ-5. `[TO-AC1]`
- **SLA restarts here but continues on reassignment** — TO-AC1 restarts the timer from zero
  while US-4.3 TQ-AC5 deliberately does not. Both are defensible and nothing records why they
  differ. Recorded as OQ-6. `[TO-AC1]`
- **`relatedTicketId` ownership is unvalidated** — TO-AC2 has the client set it to the closed
  ticket. US-4.1 has no criterion covering a value naming a ticket the caller does not own.
  `[TO-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | Zero Partial rows, each supported by the body |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TO-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
