---
story_id: US-5.8
title: "Epic 5 — Notifications: System Announcements"
source: docs/backlog/US-5.8-system-announcements.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# System Announcements

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 for the unresolved module question.

## 1. Story

> As an administrator, I want to send an announcement to everyone or to selected roles,
> So that I can warn people about maintenance without resorting to a mailing list outside
> the system.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**NA-AC1 — Composing and sending**
```gherkin
Given an administrator holding the announcements:send scope
When they compose an announcement, select the audience "all active customers" and request the preview
Then the preview shows the rendered announcement and the exact recipient count
When they confirm dispatch
Then respond 202 and a notification appears in each recipient's notification centre
And an audit event records the author, the audience definition and the recipient count
```

**NA-AC2 — Scheduled sending**
```gherkin
Given an announcement about scheduled maintenance
When a future send time is chosen
Then the announcement is stored with status SCHEDULED
And it can be edited or deleted at any point before that time
And at the scheduled moment it dispatches automatically
```

**NA-AC3 — Without the scope**
```gherkin
Given an administrator without the announcements:send scope
When they attempt to create or dispatch an announcement
Then respond 403 with type ".../errors/insufficient-scope"
And the announcements section is not rendered for that caller
And the attempt is recorded in the audit log
```

**NA-AC4 — Large audience needs a second approver**
```gherkin
Given the previewed audience exceeds 10 000 recipients
When dispatch is requested without an approvedBy value
Then respond 422 with type ".../errors/approval-required"
When approvedBy names the requesting administrator themselves
Then respond 422 as well, because approval must come from a second person
```

**NA-AC5 — Empty audience or body**
```gherkin
Given no role or status is selected
When dispatch is requested
Then respond 400 with detail "Select at least one audience"
Given a body shorter than 10 characters
Then respond 400 with detail "The announcement must be at least 10 characters"
Given the selected audience currently resolves to no accounts
Then respond 422 with type ".../errors/empty-audience"
And the client keeps the dispatch control disabled
```

**NA-AC6 — Cannot recall a dispatched announcement**
```gherkin
Given an announcement whose status is DISPATCHING or SENT
When its detail is opened
Then no edit, delete or recall action is offered
And only "Send a correction" is available, which links the new announcement to the original
When DELETE is called against it directly
Then respond 422 with type ".../errors/announcement-dispatched"
```

## 3. Functional Specification

### 3.1 Composing and previewing

An administrator holding the `announcements:send` scope composes an announcement and
selects an audience. `[NA-AC1]`

Requesting the preview shows the rendered announcement and the exact recipient count.
`[NA-AC1]`

### 3.2 Dispatch

On confirmation the response is `202`, and a notification appears in each recipient's
notification centre. `[NA-AC1]`

An audit event records the author, the audience definition and the recipient count.
`[NA-AC1]`

Where the previewed audience exceeds 10 000 recipients and dispatch is requested without
an `approvedBy` value, the response is `422` with `type` `.../errors/approval-required`.
Naming the requesting administrator themselves responds `422` as well, because approval
must come from a second person. `[NA-AC4]`

### 3.3 Scheduling

Where a future send time is chosen, the announcement is stored with status `SCHEDULED`,
can be edited or deleted at any point before that time, and dispatches automatically at
the scheduled moment. `[NA-AC2]`

### 3.4 Validation

Where no role or status is selected, dispatch responds `400` with the detail "Select at
least one audience". `[NA-AC5]`

Where the body is shorter than 10 characters, dispatch responds `400` with the detail "The
announcement must be at least 10 characters". `[NA-AC5]`

Where the selected audience currently resolves to no accounts, the response is `422` with
`type` `.../errors/empty-audience`, and the client keeps the dispatch control disabled.
`[NA-AC5]`

### 3.5 Authorisation

Where the caller lacks the `announcements:send` scope, creating or dispatching responds
`403` with `type` `.../errors/insufficient-scope`, the announcements section is not
rendered for that caller, and the attempt is recorded in the audit log. `[NA-AC3]`

### 3.6 Irreversibility

Where an announcement's status is `DISPATCHING` or `SENT`, no edit, delete or recall
action is offered; only "Send a correction" is available, which links the new announcement
to the original. `[NA-AC6]`

A direct `DELETE` responds `422` with `type` `.../errors/announcement-dispatched`.
`[NA-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | create, preview and dispatch operations | Named by behaviour; **no paths are given by any criterion** — see OQ-1 | `[NA-AC1]` |
| 2 | `DELETE` on an announcement | Method named by NA-AC2 and NA-AC6; path not given | `[NA-AC2]` `[NA-AC6]` |
| 3 | `announcements:send` scope | Gates creation and dispatch | `[NA-AC1]` `[NA-AC3]` |
| 4 | audience | Selected by role and status; example "all active customers" | `[NA-AC1]` `[NA-AC5]` |
| 5 | recipient count | Exact, shown in the preview and recorded at dispatch | `[NA-AC1]` `[NA-AC4]` |
| 6 | body | Minimum 10 characters; **no maximum stated**, and no format (plain text, markup) | `[NA-AC5]` |
| 7 | `approvedBy` | Required above 10 000 recipients; must differ from the requester | `[NA-AC4]` |
| 8 | approval threshold | 10 000 recipients | `[NA-AC4]` |
| 9 | status | Values `SCHEDULED`, `DISPATCHING`, `SENT`; the pre-scheduled state is not named | `[NA-AC2]` `[NA-AC6]` |
| 10 | scheduled send time | A future instant; time zone not specified | `[NA-AC2]` |
| 11 | correction link | Relates a later announcement to the original; field not named | `[NA-AC6]` |
| 12 | `ProblemDetail.type` | Slugs `insufficient-scope`, `approval-required`, `empty-audience`, `announcement-dispatched` | `[NA-AC3]` `[NA-AC4]` `[NA-AC5]` `[NA-AC6]` |
| 13 | Message strings | Three exact strings, quoted in NA-AC5 and NA-AC6 | `[NA-AC5]` `[NA-AC6]` |

No criterion states whether announcements are also emailed. NA-AC1 places them only in the
notification centre — see OQ-3.

## 5. Out of Scope

- Per-customer targeting beyond role and status — no criterion reaches it.
- Rich templating or campaign analytics — no criterion reaches them.
- Customer-facing announcement history beyond the notification centre — US-5.1.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Announcements respect the preference matrix of US-5.4, except for a critical-maintenance class that is always delivered in-app. | Named in the story's Non-Functional section. No criterion mentions preferences at all, and US-5.4's criteria do not name the administrative class — see US-5.4 OQ-2. |
| A-2 | Fan-out runs asynchronously in batches, which is why NA-AC1 responds `202` rather than `200`. | NA-AC1 gives the status without explaining it. |
| A-3 | Fan-out is resumable after a restart without duplicating notifications. | Named in the story's Non-Functional section. NA-AC6's `DISPATCHING` state implies a partial condition no criterion describes. |
| A-4 | The second-approver check compares account identifiers rather than scopes. | NA-AC4 forbids self-approval, which is only enforceable by identity. |
| A-5 | The audience is re-resolved at dispatch, so the recorded count may differ from the previewed one. | NA-AC1 records a count "at dispatch" and shows one "in the preview"; the two are separate moments. See OQ-4. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | No criterion names an endpoint for creating, previewing, dispatching or scheduling an announcement, though NA-AC2 and NA-AC6 both refer to `DELETE`. The whole interface is described only in the story's API Contract, which is not part of the agreed requirement set. | NA-AC1, NA-AC2 |
| OQ-2 | Who may set the critical-maintenance class that bypasses customer preferences? A-1 assumes it exists; nothing gates it, so any holder of `announcements:send` could make every announcement unblockable. | — |
| OQ-3 | Are announcements emailed as well as shown in the notification centre? NA-AC1 mentions only the centre. Emailing them multiplies the blast radius of a mistake and interacts directly with the unsubscribe rules of US-5.6 OQ-3. | NA-AC1 |
| OQ-4 | NA-AC1 shows an "exact recipient count" in the preview and records a count at dispatch. Between the two, accounts may be created or deactivated. Which count does NA-AC4's 10 000 threshold test, and what happens if the audience crosses the threshold after approval? | NA-AC1, NA-AC4 |
| OQ-5 | NA-AC2 permits editing a `SCHEDULED` announcement. If an edit takes the audience above 10 000, is a second approver required again? | NA-AC2, NA-AC4 |
| OQ-6 | NA-AC5 sets a 10-character minimum for the body but no maximum and no format. Is markup permitted, and if so how is it sanitised before reaching every customer in the system? | NA-AC5 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NA-AC1 | Preview shows an exact count; dispatch fans out and audits | §3.1, §3.2, §4 | **Partial** — no interface is specified and the count's moment is ambiguous (OQ-1, OQ-4) |
| NA-AC2 | A scheduled announcement is editable until it sends | §3.3, §4 | **Partial** — no interface, and the re-approval case is undefined (OQ-1, OQ-5) |
| NA-AC3 | Without the scope, nothing is visible or dispatchable | §3.5, §4 | Covered |
| NA-AC4 | Above 10 000 recipients a second administrator must approve | §3.2, §4 | Covered — see OQ-4 |
| NA-AC5 | Empty audience, short body and empty resolution are refused | §3.4, §4 | **Partial** — the body has no maximum and no format rule (OQ-6) |
| NA-AC6 | A dispatched announcement can only be corrected, never recalled | §3.6, §4 | Covered |

**Coverage:** 3 Covered, 3 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.8-system-announcements.md`. |
