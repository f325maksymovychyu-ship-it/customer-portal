---
spec: docs/specifications/US-4.6-ticket-resolution.md
spec_revision: 2
story_id: US-4.6
source: docs/backlog/US-4.6-ticket-resolution.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Ticket Resolution

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 2 |

No Blockers. Six criteria, verbatim, all covered. The correction closed both of revision 1's
Partial rows: the rating endpoint now exists in TS-AC2 and time-to-resolution is defined in
TS-AC1 with its exclusion. What remains is one attribution question that is a product
decision, and one gap around agent-initiated closure.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. TS-AC3's seven-day
auto-closure decides when a customer's silence counts as agreement.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Minor · D5 Testability

**Location:** §3.2; OQ-3

**Evidence:**
> …the rating is stored against the ticket and the agent who resolved it

**Problem:** US-4.3 TQ-AC5 permits reassignment, so a ticket may pass through several agents
and the rating attaches to whoever happened to resolve it. The criterion is testable; what
it measures is arguably not what it appears to measure.

**Why it matters:** Minor as a specification defect and material as a metric. A score
attributed to the last agent for an exchange others handled will be used in performance
conversations.

**Resolves when:** the product owner confirms the attribution, or the rating records the
handling agents rather than only the resolver.

---

### F-4 · Minor · D2 Coverage

**Location:** §3.5; OQ-5

**Evidence:**
> Where an agent attempts to close a ticket on the customer's behalf, the response is `403`
> with `type` `.../errors/closure-not-permitted`… `[TS-AC6]`

**Problem:** TS-AC6 forbids agent-initiated closure and TS-AC3 auto-closes after seven days
in `RESOLVED`. There is no path for a ticket that will demonstrably never be confirmed —
raised in error, duplicate, or by a customer who has left — except waiting out the window.

**Why it matters:** Minor because the seven-day path always terminates. It matters because a
support team will ask for the operation and, finding none, will resolve-and-wait on tickets
that should be closed immediately, which distorts both the queue and the metric.

**Resolves when:** a criterion covers administrative closure, or the omission is confirmed
deliberate.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TS-AC1 | §3.1, §4 | Assignee resolves an `IN_PROGRESS` ticket with a summary → status `RESOLVED`, one queued mail carrying the summary, `resolved_at` set, time-to-resolution equal to created-to-resolved minus time in `WAITING_FOR_CUSTOMER`, ticket out of the active queue | Yes |
| TS-AC2 | §3.2, §4 | Owner closes a `RESOLVED` ticket → `CLOSED`, response carries a rating URL; post a rating of 4 → `201`; post a second → `409` type `already-rated`; assert closure stands without a rating | Yes — see F-3 |
| TS-AC3 | §3.3, §4 | Injected `Clock`: at day 3 a reminder is queued; at day 7 status becomes `CLOSED` with `actorType` `SYSTEM`; run the job twice and assert one transition and one audit row | Yes |
| TS-AC4 | §3.4, §4 | Request `NEW` → `CLOSED` → `422` type `illegal-transition` listing the permitted next states; repeat as a caller holding every scope | Yes |
| TS-AC5 | §3.1, §4 | Resolve with a blank summary → `400` type `validation-failed` with the exact detail, status unchanged | Yes |
| TS-AC6 | §3.5, §4 | Second customer closes → `404`; agent closes → `403` type `closure-not-permitted` — see F-4 | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…time-to-resolution is recorded as the elapsed time from `created_at` to `resolved_at`, excluding any period spent in `WAITING_FOR_CUSTOMER`." | §3.1 | `[TS-AC1]` | Yes — defined in TS-AC1 since the correction |
| "…the response carries a link to `POST /api/v1/support/tickets/{id}/rating`." | §3.2 | `[TS-AC2]` | Yes — added in the correction |
| "…the audit entry names `SYSTEM` as the actor rather than the agent." | §3.3 | `[TS-AC3]` | Yes |
| "The rule is enforced regardless of the caller's scopes." | §3.4 | `[TS-AC4]` | Yes — TS-AC4 final clause |

## 5. Ambiguities and Missing Edge Cases

- **Rating attribution across reassignment** — see F-3. `[TS-AC2]`
- **No administrative closure** — see F-4. `[TS-AC6]`
- **Resolving from `WAITING_FOR_CUSTOMER`** — the lifecycle permits it; TS-AC1 conditions on
  `IN_PROGRESS`. Whether an agent may resolve a ticket that is waiting on the customer, in
  effect giving up on a reply, is unstated. Recorded as OQ-2. `[TS-AC1]`
- **One auto-closure window for every priority** — TS-AC3 uses seven days regardless. A
  `CRITICAL` ticket sitting unconfirmed for a week is a different situation from a `LOW` one.
  Recorded as OQ-4. `[TS-AC3]`
- **Rating window** — TS-AC2 permits a rating "at any time while the ticket remains
  `CLOSED`". US-4.7 lets a ticket leave `CLOSED` within 14 days, so the window closes
  silently on reopen and nothing says what happens to a rating in flight. `[TS-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TS-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
