---
story_id: US-5.1
title: "Epic 5 — Notifications: Notification Centre"
source: docs/backlog/US-5.1-notification-centre.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Notification Centre

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** Epic 5 has no module in the canonical map in `AGENTS.md` §2.1. Whether
> `notification/` becomes a bounded context is an architect's decision that blocks this
> story and the seven that follow it.

## 1. Story

> As a signed-in customer, I want one place that shows everything the system has told me,
> with an unread count, So that nothing important is missed and I am not searching my inbox
> for it.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**NC-AC1 — Listing notifications**
```gherkin
Given a customer with 12 notifications, 3 of them unread
When GET /api/v1/notifications is called
Then respond 200 with a page of 20, newest first
And each entry carries its event type, title, time and a reference to the related object
And unread entries are distinguishable from read ones
And GET /api/v1/notifications/unread-count returns 3
```

**NC-AC2 — Following a notification to its object**
```gherkin
Given an unread notification about a reply on ticket "#10425"
When the customer follows it
Then they arrive at that ticket
And the notification is marked read
And the unread count drops by one without a page reload
```

**NC-AC3 — The referenced object is gone or no longer permitted**
```gherkin
Given a notification whose target has been deleted, or which the customer may no longer read
When the customer follows it
Then they see "This item is no longer available" rather than an error page
And the notification remains in the list and is marked read
And the message does not disclose why access was lost
```

**NC-AC4 — Another customer's notification**
```gherkin
Given a notification identifier belonging to a different recipient
When GET /api/v1/notifications/{id} is called
Then respond 404 with type ".../errors/notification-not-found"
And no field of that notification appears in the response
```

**NC-AC5 — Empty list and a capped counter**
```gherkin
Given a customer with no notifications
When the list is requested
Then respond 200 with an empty page
And the client explains what will appear there rather than showing an empty table
Given more than 99 unread notifications
Then the counter endpoint returns capped=true
And the UI renders "99+"
```

## 3. Functional Specification

### 3.1 Listing

A list request from a signed-in customer responds `200` with a page of 20 notifications,
newest first. `[NC-AC1]`

Each entry carries its event type, title, time and a reference to the related object.
`[NC-AC1]`

Unread entries are distinguishable from read ones. `[NC-AC1]`

A separate counter endpoint returns the number of unread notifications. `[NC-AC1]`

Where the customer has no notifications, the response is `200` with an empty page and the
client explains what will appear there rather than showing an empty table. `[NC-AC5]`

Where more than 99 notifications are unread, the counter endpoint returns `capped=true`
and the interface renders "99+". `[NC-AC5]`

### 3.2 Following a notification

Where the customer follows a notification, they arrive at the referenced object, the
notification is marked read, and the unread count drops by one without a page reload.
`[NC-AC2]`

Where the target has been deleted, or the customer may no longer read it, they see "This
item is no longer available" rather than an error page. The notification remains in the
list and is marked read, and the message does not disclose why access was lost.
`[NC-AC3]`

### 3.3 Isolation

Where the identifier belongs to a different recipient, the response is `404` with `type`
`.../errors/notification-not-found`, and no field of that notification appears in the
response. `[NC-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/notifications` | Path and method named by the criteria; page size 20 | `[NC-AC1]` `[NC-AC5]` |
| 2 | `GET /api/v1/notifications/unread-count` | Path and method named by the criteria | `[NC-AC1]` `[NC-AC5]` |
| 3 | `GET /api/v1/notifications/{id}` | Path and method named by the criteria | `[NC-AC4]` |
| 4 | event type | Carried on every entry; **the vocabulary is not enumerated** and is contributed by other stories | `[NC-AC1]` |
| 5 | title | Carried on every entry; **length and source not specified** | `[NC-AC1]` |
| 6 | time | Carried on every entry; format and zone not specified | `[NC-AC1]` |
| 7 | target reference | A pointer to the related object; representation not specified | `[NC-AC1]` `[NC-AC2]` |
| 8 | read state | Distinguishable in the list; set by following the notification | `[NC-AC1]` `[NC-AC2]` `[NC-AC3]` |
| 9 | `capped` (counter response) | Boolean, true above 99 unread | `[NC-AC5]` |
| 10 | paging parameters | Implied by "a page of 20"; **not named by any criterion** | `[NC-AC1]` |
| 11 | `ProblemDetail.type` | Slug `notification-not-found` | `[NC-AC4]` |
| 12 | Message strings | "This item is no longer available" | `[NC-AC3]` |

## 5. Out of Scope

- Real-time delivery — US-5.2; bulk read state — US-5.3; preferences — US-5.4.
- Email delivery — US-5.5.
- Producing notifications. Every producing story owns its own events; this story only
  presents them.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Notifications carry one of four classes — security, transactional, administrative, informational — declared in code beside the event type. | Named in the story's Assumptions table and in `docs/backlog/README.md`. NC-AC1 returns an "event type" without reference to a class, but US-5.4 NP-AC1 groups by it. |
| A-2 | A rendered title and body are stored on the row rather than a template reference. | Named in the story's Data Model Notes, so that an old notification still reads correctly after a template changes. No criterion constrains this. |
| A-3 | Retention is 90 days. | Named in the story's Assumptions table. No criterion asserts any retention, and NC-AC1's counts do not reveal it. |
| A-4 | Recipient isolation is enforced by a repository predicate, as in US-4.2. | NC-AC4 is satisfiable either way. |
| A-5 | The notification body carries a summary, never the full contents of the underlying message. | Named in the story's Non-Functional section. NC-AC1 names a "title" only. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | **Escalation.** There is no `notification/` module in `AGENTS.md` §2.1, whose reserved modules are `catalog`, `ordering` and `support`. Until an architect decides where this epic lives, none of its criteria can be implemented. | all |
| OQ-2 | NC-AC1 requires "a reference to the related object" but no criterion states its representation, or how the client turns it into the destination NC-AC2 requires. Is it a URL, a type-plus-identifier pair, or something else? | NC-AC1, NC-AC2 |
| OQ-3 | NC-AC3 marks a notification read when its target is unreachable, and NC-AC2 marks it read on a successful follow. Is a notification ever marked read without being followed — for example by appearing in the list? US-5.3 defines the explicit path but not the implicit one. | NC-AC2, NC-AC3 |
| OQ-4 | Retention is stated nowhere in the criteria. If notifications are deleted at 90 days, the notification is sometimes the only record the customer was told something, while the audit log (US-3.7) records only that the system sent it. | NC-AC1 |
| OQ-5 | Do agents and administrators receive notifications, or do they work from the queue in US-4.3? US-4.3 TQ-AC5 notifies a new assignee, which implies staff recipients this story does not describe. | NC-AC1 |
| OQ-6 | NC-AC1 states a page size of 20 but names no paging parameters, and NC-AC5 speaks of "an empty page". How is the second page requested? | NC-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NC-AC1 | A page of 20, newest first, with a separate unread counter | §3.1, §4 | **Partial** — the object reference the criterion carries has no defined representation (OQ-2, OQ-6) |
| NC-AC2 | Following a notification navigates and marks it read | §3.2, §4 | **Partial** — depends on the same undefined reference (OQ-2) |
| NC-AC3 | A dead or forbidden target degrades without disclosing why | §3.2, §4 | Covered |
| NC-AC4 | Another recipient's notification yields 404 | §3.3, §4 | Covered |
| NC-AC5 | Empty list explains itself; the counter caps at 99+ | §3.1, §4 | Covered |

**Coverage:** 3 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.1-notification-centre.md`. |
