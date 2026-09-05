---
story_id: US-5.3
title: "Epic 5 — Notifications: Read State"
source: docs/backlog/US-5.3-read-state.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Read State

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 for the unresolved module question.

## 1. Story

> As a signed-in customer, I want to mark notifications read individually or all at once,
> and to undo it, So that I can clear the list quickly while keeping the ones I still need
> to act on visible.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**NR-AC1 — Mark all as read**
```gherkin
Given a customer with 3 unread notifications
When POST /api/v1/notifications/read is called with all=true
Then respond 200 with updated=3 and unreadCount=0
And exactly one database statement performs the update, not one per row
And the notifications remain in the list, only their unread state changes
```

**NR-AC2 — Mark one as unread again**
```gherkin
Given a notification that has been read
When POST /api/v1/notifications/unread is called with its identifier
Then respond 200 with updated=1
And the entry is highlighted as unread again
And the unread counter increases by one
```

**NR-AC3 — Repeating the action from another device**
```gherkin
Given the customer already marked everything read on another device
When POST /api/v1/notifications/read with all=true is called again
Then respond 200 with updated=0 and unreadCount=0
And no error is returned
And the counter never becomes negative under any interleaving of these calls
```

**NR-AC4 — Identifiers belonging to someone else**
```gherkin
Given the request names a notification belonging to a different recipient
When it is submitted
Then that identifier is ignored rather than acted upon
And the response counts only the caller's own notifications in updated
And no error reveals whether the foreign identifier exists
```

**NR-AC5 — Acting inside the unread-only filter**
```gherkin
Given the unread-only filter is active
When a notification is marked read
Then it leaves the list
And an "Undo" action is available for 5 seconds and restores it
And the remaining rows do not jump, because the removed row's height is animated out
```

**NR-AC6 — Malformed request**
```gherkin
Given a request that supplies neither notificationIds nor all
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
Given notificationIds contains more than 500 entries
Then respond 400 naming the limit
```

## 3. Functional Specification

### 3.1 Marking read

A read request carrying `all=true` responds `200` with the number of notifications
updated and the resulting unread count. `[NR-AC1]`

Exactly one database statement performs the update, not one per row. `[NR-AC1]`

The notifications remain in the list; only their unread state changes. `[NR-AC1]`

Where everything is already read, the same call responds `200` with `updated=0` and
`unreadCount=0`, returns no error, and the counter never becomes negative under any
interleaving of these calls. `[NR-AC3]`

### 3.2 Marking unread

An unread request naming a notification that has been read responds `200` with
`updated=1`, the entry is highlighted as unread again, and the unread counter increases by
one. `[NR-AC2]`

### 3.3 Foreign identifiers

Where the request names a notification belonging to a different recipient, that identifier
is ignored rather than acted upon. `[NR-AC4]`

The response counts only the caller's own notifications in `updated`, and no error reveals
whether the foreign identifier exists. `[NR-AC4]`

### 3.4 Client behaviour under a filter

Where the unread-only filter is active and a notification is marked read, it leaves the
list. `[NR-AC5]`

An "Undo" action is available for 5 seconds and restores it, and the remaining rows do not
jump because the removed row's height is animated out. `[NR-AC5]`

### 3.5 Request validation

A request supplying neither `notificationIds` nor `all` is rejected with `400` and `type`
`.../errors/validation-failed`. `[NR-AC6]`

A `notificationIds` list containing more than 500 entries is rejected with `400` naming
the limit. `[NR-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/notifications/read` | Path and method named by the criteria | `[NR-AC1]` `[NR-AC3]` |
| 2 | `POST /api/v1/notifications/unread` | Path and method named by the criteria | `[NR-AC2]` |
| 3 | `all` (request) | Boolean; `true` marks every notification of the caller | `[NR-AC1]` `[NR-AC3]` |
| 4 | `notificationIds` (request) | Array, at most 500 entries; foreign identifiers are ignored | `[NR-AC2]` `[NR-AC4]` `[NR-AC6]` |
| 5 | `updated` (response) | Integer; counts only the caller's own notifications | `[NR-AC1]` `[NR-AC2]` `[NR-AC3]` `[NR-AC4]` |
| 6 | `unreadCount` (response) | Integer; never negative | `[NR-AC1]` `[NR-AC3]` |
| 7 | undo window | 5 seconds, client-side | `[NR-AC5]` |
| 8 | `ProblemDetail.type` | Slug `validation-failed` | `[NR-AC6]` |
| 9 | Message strings | "Undo" action label | `[NR-AC5]` |

NR-AC2 is stated only for a list of identifiers. Whether the unread endpoint also accepts
`all=true` is not specified — see OQ-2.

## 5. Out of Scope

- Listing and the counter itself — US-5.1.
- Deleting notifications — no criterion reaches it.
- Read state of the underlying object, such as a ticket thread — US-4.2.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Read state is a nullable timestamp on the notification row, so no second table is needed. | NR-AC1's single-statement requirement is only naturally satisfiable this way. |
| A-2 | `unreadCount` is computed after the update inside the same transaction. | NR-AC3 requires the counter to be correct under interleaving; a read outside the transaction can return a stale value. |
| A-3 | The bulk update is scoped by recipient in the same statement, which is what makes NR-AC4's silent skip fall out naturally. | NR-AC4 states the outcome, not the mechanism. |
| A-4 | "Undo" in NR-AC5 issues the unread call of NR-AC2. | NR-AC5 names no mechanism; without this it is an unimplementable affordance. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | Should read state converge across devices in real time through the US-5.2 stream, or only on the next load? NR-AC3 makes the API safe under interleaving but says nothing about what the other device *displays* in the meantime. US-5.2 ND-AC2 requires exactly this convergence between tabs, so the two stories set different expectations for tabs and devices. | NR-AC3 |
| OQ-2 | NR-AC1 defines `all=true` for the read endpoint. Does the unread endpoint accept it too, and if so what does "mark everything unread" mean for a customer with 90 days of history? | NR-AC2 |
| OQ-3 | Is "mark all as read" scoped to the currently applied filter or to everything? NR-AC1 implies everything, which will surprise a customer who filtered first and then pressed it. | NR-AC1 |
| OQ-4 | NR-AC1 requires "exactly one database statement". That is an implementation constraint rather than observable behaviour, and it is testable only by counting queries. Is it intended as a criterion, or as a note? | NR-AC1 |
| OQ-5 | NR-AC5's undo window is 5 seconds. What happens if the customer navigates away, or the request fails, inside that window? | NR-AC5 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NR-AC1 | One statement marks everything read and returns the new count | §3.1, §4 | Covered — see OQ-3, OQ-4 |
| NR-AC2 | A read notification can be returned to unread | §3.2, §4 | Covered — see OQ-2 |
| NR-AC3 | Repeating the call is safe and the counter never goes negative | §3.1, §4 | **Partial** — the criterion constrains the API but not what a second device shows (OQ-1) |
| NR-AC4 | Foreign identifiers are silently skipped, not reported | §3.3, §4 | Covered |
| NR-AC5 | Under the unread filter, the row leaves with a 5-second undo | §3.4, §4 | **Partial** — the undo's failure and navigation paths are undefined (OQ-5) |
| NR-AC6 | Neither-field and over-500 requests are rejected | §3.5, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.3-read-state.md`. |
