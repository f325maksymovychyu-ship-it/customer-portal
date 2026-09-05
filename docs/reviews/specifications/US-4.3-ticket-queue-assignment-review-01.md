---
spec: docs/specifications/US-4.3-ticket-queue-assignment.md
spec_revision: 2
story_id: US-4.3
source: docs/backlog/US-4.3-ticket-queue-assignment.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Queue and Assignment

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. TQ-AC1 gained a definition of `slaState`
and its at-risk point in the correction, so the queue's ordering rule is now stated rather
than referenced. F-3 is what remains: three of the four SLA rows it depends on are expressed
in business days that nothing defines.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. TQ-AC1's ordering rule
decides which customer waits longest.

**Resolves when:** the criteria and the SLA table carry a product owner's sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.1; §4 rows 7 and 8

**Evidence:**
> And a ticket is AT_RISK once 75% of its first-response deadline has elapsed, and BREACHED
> once the deadline has passed

**Problem:** The state machine is now precise and the deadline it measures is not. The SLA
table gives `CRITICAL` in hours and the other three priorities in business days, with no
calendar defined. A queue containing mixed priorities cannot be ordered deterministically.

**Why it matters:** TQ-AC1 is the criterion the whole story exists for — that the most
urgent problems are worked first. Three quarters of the priority set currently yield no
computable deadline, so the ordering assertion holds only for a queue of `CRITICAL` tickets.

**Resolves when:** the business calendar is defined, or the thresholds are restated in
elapsed hours. Recorded against US-4.1 as F-3 there.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TQ-AC1 | §3.1, §4 | Agent with `tickets:read:any` requests `assigned=NONE` → `200`, unassigned only, ordered by priority then nearest deadline, each carrying `slaState`; a ticket at 76% of its deadline is `AT_RISK`. Deterministic only for `CRITICAL` — F-3 | Partly |
| TQ-AC2 | §3.2, §4 | Claim an unassigned ticket → assignee set, status `NEW` → `IN_PROGRESS`, customer notified, audit row written | Yes |
| TQ-AC3 | §3.2, §4 | Two agents claim concurrently → the second gets `409` type `ticket-already-assigned` whose detail names the holder; assignment unchanged | Yes |
| TQ-AC4 | §3.4, §4 | Customer without `tickets:read:any` requests the queue → `403` type `insufficient-scope`; assert no other customer's ticket appears in any response to that caller | Yes |
| TQ-AC5 | §3.3, §4 | Assignee reassigns with a reason → new assignee recorded and notified, reason stored as an internal note invisible to the customer, `sla_due_at` unchanged | Yes |
| TQ-AC6 | §3.3, §4 | Assign to a deactivated account, and to one holding no agent role → `422` type `ineligible-assignee` both times, assignment unchanged | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…a ticket is AT_RISK once 75% of its first-response deadline has elapsed, and BREACHED once the deadline has passed." | §3.1 | `[TQ-AC1]` | Yes — added to TQ-AC1 in the correction |
| "The SLA timer continues rather than restarting." | §3.3 | `[TQ-AC5]` | Yes — TQ-AC5 final clause |
| "The reason is stored as an internal note, invisible to the customer…" | §3.3 | `[TQ-AC5]` | Yes |
| "…and the client refreshes the queue so the row disappears." | §3.2 | `[TQ-AC3]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Business days undefined** — see F-3. `[TQ-AC1]`
- **Supervisor reassignment** — TQ-AC5 lets the assignee hand a ticket on. Whether anyone
  else may, and under what scope, is unstated. Recorded as OQ-3. `[TQ-AC5]`
- **Assignee deactivated while a ticket is in progress** — TQ-AC6 blocks assigning *to* a
  deactivated account and US-4.7 TO-AC6 handles the reopen case. A live `IN_PROGRESS` ticket
  whose agent is deactivated by US-3.4 falls between them. Recorded as OQ-4. `[TQ-AC6]`
- **`assigned` value set** — TQ-AC1 exercises `NONE`. The contract implies `ME` and `ANY`,
  and no criterion defines them. `[TQ-AC1]`
- **Claiming an already-assigned ticket** — TQ-AC2 conditions on "no assignee" and TQ-AC3
  covers the race. A deliberate claim of a ticket someone already holds is neither.
  `[TQ-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TQ-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
