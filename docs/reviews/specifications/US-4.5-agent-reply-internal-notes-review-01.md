---
spec: docs/specifications/US-4.5-agent-reply-internal-notes.md
spec_revision: 2
story_id: US-4.5
source: docs/backlog/US-4.5-agent-reply-internal-notes.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Agent Reply and Internal Notes

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. TA-AC3 is the most security-relevant
criterion in Epic 4 and the correction strengthened it materially: sequence numbers are now
removed entirely rather than merely required to have no gaps, and `tickets.updated_at` is
pinned so a note cannot reorder the customer's list. F-3 and F-4 are what remain, and both
are gaps in surrounding stories rather than in this one.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. TA-AC3 defines what a
customer may never see about their own ticket.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D2 Coverage

**Location:** §3.6; §4 row 10; OQ-1

**Evidence:**
> | 10 | template | Named example "Request more information"; substitutes customer name and
> ticket reference. **No criterion says where templates come from** | `[TA-AC6]` |

**Problem:** TA-AC6 consumes canned response templates. No story in the backlog creates,
edits, lists or scopes them.

**Why it matters:** The criterion cannot be tested without seeding a template through an
interface that does not exist. Every implementation will invent one, and templates are text
that reaches customers verbatim.

**Resolves when:** a story owns template management, or TA-AC6 is deferred with it.

---

### F-4 · Major · D5 Testability

**Location:** §3.2; OQ-2

**Evidence:**
> No email is sent to the customer, the ticket status does not change, the SLA timer keeps
> running… `[TA-AC2]`

**Problem:** TA-AC2 says the timer keeps running during an internal note. US-4.6's A-2 and
the SLA section of `docs/backlog/README.md` both pause timers in `WAITING_FOR_CUSTOMER`. An
agent writing a note on a ticket already waiting on the customer meets both rules, which
disagree.

**Why it matters:** A tester cannot decide whether the deadline advanced. The two readings
differ by however long the customer takes to reply, which is exactly the period the pause
exists to exclude.

**Resolves when:** TA-AC2 is scoped to statuses where the timer is running, or the pause
rule states that an internal note does not resume it.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TA-AC1 | §3.1, §4 | Assignee posts with `visibility: PUBLIC` → `201`, message in the customer's thread, one queued mail carrying the reply and a link, status `WAITING_FOR_CUSTOMER`, `first_responded_at` set on the first public message only | Yes |
| TA-AC2 | §3.2, §4 | Post with `visibility: INTERNAL` → `201`, rendered to agents with a "Team only" label, mail port never invoked, status unchanged, `first_responded_at` unset. SLA assertion undecidable — F-4 | Partly |
| TA-AC3 | §3.3, §4 | Seed both kinds; request as the owner → no note body, author or timestamp; message count equals the public count; no sequence numbers present at all; assert `updated_at` did not move on the note | Yes |
| TA-AC4 | §3.4, §4 | Post a body omitting `visibility` → `400` type `validation-failed` naming the field; assert no row was written | Yes |
| TA-AC5 | §3.5, §4 | Non-assignee without `tickets:reply:any` posts → `403` type `not-ticket-assignee`; grant the scope and repeat → accepted | Yes |
| TA-AC6 | §3.6, §4 | Not derivable: no interface creates the template the criterion selects — F-3 | No |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…messages carry no sequence number at all, only their identifier and timestamp, so no gap can exist to reveal." | §3.3 | `[TA-AC3]` | Yes — added to TA-AC3 in the correction |
| "…`tickets.updated_at` is not advanced by an internal note…" | §3.3 | `[TA-AC3]` | Yes — added in the correction; closes the US-4.2 leak |
| "…if this is the first public agent message, `tickets.first_responded_at` is set." | §3.1 | `[TA-AC1]` | Yes |
| "But no template is ever sent without an explicit send action." | §3.6 | `[TA-AC6]` | Yes as a claim; the template source does not exist — F-3 |

## 5. Ambiguities and Missing Edge Cases

- **Template management has no owner** — see F-3. `[TA-AC6]`
- **SLA behaviour on a note during `WAITING_FOR_CUSTOMER`** — see F-4. `[TA-AC2]`
- **Converting a note to a public reply** — a routine request in support tooling and the
  most direct route to disclosing exactly the text TA-AC3 protects. Neither permitted nor
  forbidden. Recorded as OQ-4. `[TA-AC3]`
- **Scope hierarchy** — TA-AC5 names `tickets:reply:any`; US-4.3 TQ-AC4 names
  `tickets:read:any`. Whether one implies the other is unstated. Recorded as OQ-5.
  `[TA-AC5]`
- **No length bound on an agent message** — US-4.4 TR-AC3 bounds the customer's at 5 000
  characters; nothing bounds the agent's. Recorded as OQ-6. `[TA-AC1]`
- **Source status for TA-AC1's transition** — the criterion states the destination
  `WAITING_FOR_CUSTOMER` without naming which statuses it may be entered from. A-4 assumes
  any open status. `[TA-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-3 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TA-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
