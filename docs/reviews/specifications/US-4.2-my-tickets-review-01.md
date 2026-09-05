---
spec: docs/specifications/US-4.2-my-tickets.md
spec_revision: 2
story_id: US-4.2
source: docs/backlog/US-4.2-my-tickets.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — My Tickets

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 2 |

No Blockers. Five criteria, verbatim, all covered. The revision-2 correction closed the
confidentiality leak this spec raised in revision 1 — an internal note could reorder the
customer's list and reveal hidden activity — by defining "updated" as the last
customer-visible event and pinning `tickets.updated_at` in US-4.5 TA-AC3. The attachment
link is now bounded at 15 minutes and account-bound.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. TL-AC3 and TL-AC4 are
data-isolation controls.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Minor · D5 Testability

**Location:** §4 row 6; OQ-5

**Evidence:**
> | 6 | `{id}` path segment | TL-AC2 and TL-AC3 quote references of the form `#10425`;
> whether the path takes the reference or an internal identifier is not specified |

**Problem:** The criteria address tickets by a human-readable reference in prose and by
`{id}` in the path. If the path takes the sequential reference, TL-AC3's isolation is the
only defence against enumeration by increment.

**Why it matters:** Minor because TL-AC3 does hold that line. It matters because the choice
determines whether an attacker needs to guess a UUID or count upward.

**Resolves when:** a criterion states which identifier the path carries.

---

### F-4 · Minor · D2 Coverage

**Location:** §3.1; §4 row 11; OQ-4

**Evidence:**
> | 11 | paging | TL-AC5 names "an empty page"; page size and parameters are not specified |

**Problem:** Both TL-AC1 and TL-AC5 speak of a page and neither states a size or how the
second one is requested.

**Why it matters:** Minor — the criteria are testable for a customer with few tickets, which
is the common case. A customer with two hundred is untested.

**Resolves when:** a criterion states the page size and parameters, as US-3.1 UL-AC1 does.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TL-AC1 | §3.1, §4 | Four tickets in different statuses → `200`, ordered by last customer-visible event; each entry carries five named fields; a ticket with an unread agent reply is flagged; `state=OPEN` excludes closed and resolved. Add an internal note and assert the order does not move | Yes |
| TL-AC2 | §3.2, §4 | `GET` own ticket → messages in chronological order with author and time, current status, assignee display name, attachment links valid 15 minutes and bound to the caller, unread flag cleared | Yes |
| TL-AC3 | §3.3, §4 | `GET` a second customer's ticket and a never-existing id → byte-identical `404` type `ticket-not-found` | Yes |
| TL-AC4 | §3.4, §4 | Seed public messages and internal notes; `GET` as the owner → no note text, author or timestamp; the message count equals the public count only | Yes |
| TL-AC5 | §3.1, §4 | Customer with no tickets → `200`, empty page; client shows the explanation and the create action — see F-4 | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…\"updated\" means the last event the customer can see, so an internal note does not reorder the list." | §3.1 | `[TL-AC1]` | Yes — added to TL-AC1 in the correction |
| "Every attachment on a public message is retrievable through a link valid for 15 minutes and bound to the calling account." | §3.2 | `[TL-AC2]` | Yes — bounded in TL-AC2 since the correction |
| "The response body is identical to the one returned for an identifier that never existed…" | §3.3 | `[TL-AC3]` | Yes |
| "No gap in message numbering or count hints that hidden messages exist." | §3.4 | `[TL-AC4]` | Yes; US-4.5 TA-AC3 now removes numbering entirely, which is the stronger form |

## 5. Ambiguities and Missing Edge Cases

- **Identifier in the path** — see F-3. `[TL-AC3]`
- **Paging unspecified** — see F-4. `[TL-AC1]` `[TL-AC5]`
- **Assignee display name before a claim** — TL-AC2 includes "the assigned agent's display
  name". A `NEW` ticket has no assignee and nothing states what is returned. Recorded as
  OQ-2, which also asks whether exposing staff names to every customer is intended.
  `[TL-AC2]`
- **Agent-uploaded attachments** — TL-AC2 speaks of attachments "the customer uploaded".
  US-4.5 permits agents to attach files and nothing says whether the customer sees them.
  A-3 assumes yes. `[TL-AC2]`
- **Closed-ticket retention in the list** — nothing states how long a closed ticket stays
  visible, which interacts with the 14-day reopen window in US-4.7. Recorded as OQ-6.
  `[TL-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Five criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TL-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
