---
spec: docs/specifications/US-5.1-notification-centre.md
spec_revision: 1
story_id: US-5.1
source: docs/backlog/US-5.1-notification-centre.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Notification Centre

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Five criteria, verbatim, all covered. F-3 is the finding that governs the whole
epic: there is no `notification/` module in the canonical map, and until an architect rules,
none of the eight Epic 5 stories can be built. The spec surfaces this correctly as an
escalation rather than assuming a placement.

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

**Location:** Blocked callout; OQ-1

**Evidence:**
> **⚠ Blocked.** Epic 5 has no module in the canonical map in `AGENTS.md` §2.1.

**Problem:** `AGENTS.md` §2.1 lists `shared`, `customer`, `catalog`, `ordering` and
`support`. Nothing here fits, and the ArchUnit rules that enforce the layer boundaries will
reject code that has no declared home.

**Why it matters:** Every criterion in Epic 5 is unbuildable until this is decided, and the
decision shapes whether notifications are a bounded context with its own persistence or a
`shared` concern every module calls.

**Resolves when:** an architect rules on the module, and `AGENTS.md` §2.1 records it.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NC-AC1 | §3.1, §4 | Seed 12 notifications, 3 unread → `GET /api/v1/notifications` returns 20-per-page ordering newest first, each carrying event type, title, time and a target reference; the counter endpoint returns 3 | Yes |
| NC-AC2 | §3.2, §4 | Follow an unread notification about a ticket → land on that ticket, `read_at` set, counter decremented without a reload. The navigation half depends on an unspecified reference format — §5 | Partly |
| NC-AC3 | §3.2, §4 | Delete the target, then follow → the exact message, notification still listed and marked read, no reason disclosed; repeat with the permission revoked | Yes |
| NC-AC4 | §3.3, §4 | `GET` a second recipient's notification id → `404` type `notification-not-found`, no field present | Yes |
| NC-AC5 | §3.1, §4 | Recipient with none → `200`, empty page, client explanation; seed 120 unread → counter returns `capped: true` and the UI renders "99+" | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…the notification remains in the list and is marked read, and the message does not disclose why access was lost." | §3.2 | `[NC-AC3]` | Yes |
| "Where more than 99 notifications are unread, the counter endpoint returns `capped=true`…" | §3.1 | `[NC-AC5]` | Yes |
| "A separate counter endpoint returns the number of unread notifications." | §3.1 | `[NC-AC1]` | Yes — NC-AC1 final clause names the path |
| "…no field of that notification appears in the response." | §3.3 | `[NC-AC4]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Module placement blocks the epic** — see F-3. All criteria.
- **Target reference has no format** — NC-AC1 requires "a reference to the related object"
  and NC-AC2 requires following it to arrive somewhere. Whether it is a URL, a
  type-plus-identifier pair or something else is unstated, so the client's navigation cannot
  be specified. Recorded as OQ-2. `[NC-AC1]` `[NC-AC2]`
- **Implicit read-marking** — NC-AC2 and NC-AC3 mark read on following. Whether merely
  appearing in the list ever marks read is unstated, and US-5.3 defines only the explicit
  path. Recorded as OQ-3. `[NC-AC2]`
- **Retention is 90 days by assumption** — A-3 carries it and no criterion asserts it. For a
  customer, the notification is sometimes the only record they were told something; the audit
  log records that the system sent it, not what they saw. Recorded as OQ-4. `[NC-AC1]`
- **Staff recipients** — US-4.3 TQ-AC5 notifies a new assignee, which implies agents receive
  notifications. Nothing in this story describes that case. Recorded as OQ-5. `[NC-AC1]`
- **Paging parameters** — NC-AC1 states a page size of 20 and names no way to request the
  second page. Recorded as OQ-6. `[NC-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Five criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | The module question is correctly routed as an escalation |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NC-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
