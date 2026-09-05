---
spec: docs/specifications/US-4.4-customer-reply.md
spec_revision: 1
story_id: US-4.4
source: docs/backlog/US-4.4-customer-reply.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Customer Reply

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. This spec was untouched by the backlog
correction and remains at revision 1. F-3 is the substantive finding: TR-AC1 defines the
reply path from one status only, and the status it omits — `RESOLVED` — is the common real
case, since a customer who disagrees with a resolution replies rather than reopens.

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

**Location:** §3.1; A-4; OQ-1

**Evidence:**
> Given a customer viewing their own ticket in status WAITING_FOR_CUSTOMER

**Problem:** TR-AC1 defines the whole reply behaviour — status transition, notification, SLA
resume — from a single source status. The lifecycle table permits replies from `NEW`,
`IN_PROGRESS` and `RESOLVED`, and no criterion says what happens in any of them. A-4 defers
to the lifecycle table, which states permitted transitions but not the notification or timer
consequences.

**Why it matters:** A reply to a `RESOLVED` ticket is how a customer says "this is not
fixed". Whether that reopens the ticket, moves it to `WAITING_FOR_SUPPORT`, or leaves the
resolution standing decides whether the team ever sees it. Nothing specifies it.

**Resolves when:** TR-AC1 covers the other open statuses, or a criterion states that replies
are refused outside `WAITING_FOR_CUSTOMER`.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TR-AC1 | §3.1, §4 | Own ticket in `WAITING_FOR_CUSTOMER`, non-empty body → `201` with author and time, status `WAITING_FOR_SUPPORT`, assignee notified, SLA resumed. Other source statuses undefined — F-3 | Partly |
| TR-AC2 | §3.2, §4 | Upload via US-4.1's endpoint, reference the id on a reply → attachment on the message; oversize and wrong-type rejections behave as in US-4.1 | Yes |
| TR-AC3 | §3.3, §4 | Body of `"   "` → `400` type `validation-failed`, send control disabled; body of 5 001 characters → `400` naming the limit, counter in error state before the limit | Yes |
| TR-AC4 | §3.4, §4 | `CLOSED` ticket → no reply field, reopen action offered; direct post → `422` type `ticket-not-repliable` whose detail points at US-4.7 | Yes |
| TR-AC5 | §3.5, §4 | Post to a second customer's ticket → `404` type `ticket-not-found`, byte-identical to the non-existent case | Yes |
| TR-AC6 | §3.6, §4 | Post customer and agent messages concurrently → both stored, ordered by server timestamp, stable across reloads. The live-update half depends on a transport no criterion names — §5 | Partly |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The SLA timer for a support response resumes." | §3.1 | `[TR-AC1]` | Yes — TR-AC1 final clause |
| "…the client keeps the send control disabled in that state." | §3.3 | `[TR-AC3]` | Yes |
| "…the detail points at the reopen operation of US-4.7." | §3.4 | `[TR-AC4]` | Yes |
| "…no message is lost, duplicated or reordered on subsequent loads." | §3.6 | `[TR-AC6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Reply from statuses other than `WAITING_FOR_CUSTOMER`** — see F-3. `[TR-AC1]`
- **Notification target on an unassigned ticket** — TR-AC1 notifies "the assigned agent". A
  `NEW` ticket has none. Recorded as OQ-2. `[TR-AC1]`
- **Live update has no transport** — TR-AC6 requires the thread to update "without a page
  reload". US-5.2 would provide it and is blocked on an unapproved dependency; polling would
  also satisfy the wording. Recorded as OQ-3. `[TR-AC6]`
- **No reply rate limit** — US-4.1 TC-AC5 caps ticket creation; nothing caps replies.
  Recorded as OQ-4. `[TR-AC1]`
- **`visibility` must not be readable here** — A-1 assumes the endpoint ignores the field on
  the customer path. US-4.5 TA-AC4 makes it required on the agent path, and the two share an
  endpoint. No criterion states the asymmetry, which is the mechanism preventing a customer
  from authoring an internal note. `[TR-AC1]`

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
`TR-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
