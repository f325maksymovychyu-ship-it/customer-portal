---
story_id: US-4.6
title: "Epic 4 — Feedback / Support: Ticket Resolution"
source: docs/backlog/US-4.6-ticket-resolution.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Ticket Resolution

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a support agent, I want to mark a ticket resolved with a summary and have it close once
> the customer agrees, So that the queue reflects real outstanding work and the customer knows
> how their problem ended.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**TS-AC1 — Marking a ticket resolved**
```gherkin
Given an agent assigned to a ticket in status IN_PROGRESS
When POST /api/v1/support/tickets/{id}/resolve is called with a summary
Then the status becomes RESOLVED
And the customer receives an email containing the summary and a request to confirm
And tickets.resolved_at is set
And time-to-resolution is recorded as the elapsed time from tickets.created_at to resolved_at, excluding any period spent in WAITING_FOR_CUSTOMER
And the ticket leaves the agent's active queue
```

**TS-AC2 — Customer confirms and rates**
```gherkin
Given the customer's ticket is in status RESOLVED
When POST /api/v1/support/tickets/{id}/close is called
Then the status becomes CLOSED
And the response carries a link to POST /api/v1/support/tickets/{id}/rating
When the customer posts a rating of 1 to 5 with an optional comment to that endpoint
Then respond 201 and the rating is stored against the ticket and the agent who resolved it
When a second rating is submitted for the same ticket
Then respond 409 with type ".../errors/already-rated"
And a rating may be submitted at any time while the ticket remains CLOSED
But declining to rate does not prevent or reverse closure
```

**TS-AC3 — Automatic closure**
```gherkin
Given a ticket has been RESOLVED for 3 days with no customer response
Then a reminder is sent stating that it will close in 4 days
Given 7 days have passed in RESOLVED with no customer response
Then the ticket transitions to CLOSED automatically
And the audit entry names SYSTEM as the actor, not the agent
And the job is idempotent, so a repeated run produces no second transition or second audit entry
```

**TS-AC4 — Disallowed transition**
```gherkin
Given a ticket in status NEW
When a transition directly to CLOSED is requested
Then respond 422 with type ".../errors/illegal-transition"
And the ProblemDetail lists the transitions that are permitted from NEW
And the status is unchanged
And the same rule is enforced regardless of the caller's scopes
```

**TS-AC5 — Resolving without a summary**
```gherkin
Given a resolve request whose summary is absent or blank
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
And the detail reads "Describe how the problem was resolved. The customer will see this"
And the status is unchanged
```

**TS-AC6 — Closing someone else's ticket**
```gherkin
Given the ticket belongs to a different customer
When close is requested by that caller
Then respond 404, matching US-4.2 TL-AC3
Given an agent attempts to close a ticket on the customer's behalf
Then respond 403 with type ".../errors/closure-not-permitted"
Because confirmation is the customer's decision, or the auto-closure job's
```

## 3. Functional Specification

### 3.1 Resolving

A resolve request from the assigned agent, carrying a summary, moves an `IN_PROGRESS`
ticket to `RESOLVED`. `[TS-AC1]`

The customer receives an email containing the summary and a request to confirm.
`[TS-AC1]`

`tickets.resolved_at` is set and the time-to-resolution metric is recorded. `[TS-AC1]`

The ticket leaves the agent's active queue. `[TS-AC1]`

Where the summary is absent or blank, the response is `400` with `type`
`.../errors/validation-failed` and the detail "Describe how the problem was resolved. The
customer will see this", and the status is unchanged. `[TS-AC5]`

### 3.2 Closing and rating

Where the customer closes their own `RESOLVED` ticket, the status becomes `CLOSED`.
`[TS-AC2]`

The customer is offered a 1-to-5 rating with an optional comment, stored against the
ticket and the agent who resolved it. `[TS-AC2]`

A second rating for the same ticket responds `409` with `type`
`.../errors/already-rated`. `[TS-AC2]`

### 3.3 Automatic closure

Where a ticket has been `RESOLVED` for 3 days with no customer response, a reminder is
sent stating that it will close in 4 days. `[TS-AC3]`

Where 7 days have passed in `RESOLVED` with no customer response, the ticket transitions
to `CLOSED` automatically, and the audit entry names `SYSTEM` as the actor rather than the
agent. `[TS-AC3]`

The job is idempotent: a repeated run produces no second transition and no second audit
entry. `[TS-AC3]`

### 3.4 Transition rules

Where a transition is requested that the lifecycle does not permit from the ticket's
current status, the response is `422` with `type` `.../errors/illegal-transition`, the
`ProblemDetail` lists the permitted transitions, and the status is unchanged. `[TS-AC4]`

The rule is enforced regardless of the caller's scopes. `[TS-AC4]`

### 3.5 Authorisation

Where the ticket belongs to a different customer, a close request responds `404`, matching
US-4.2 TL-AC3. `[TS-AC6]`

Where an agent attempts to close a ticket on the customer's behalf, the response is `403`
with `type` `.../errors/closure-not-permitted`, because confirmation is the customer's
decision or the auto-closure job's. `[TS-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/support/tickets/{id}/resolve` | Path and method named by the criteria; success status not specified | `[TS-AC1]` `[TS-AC5]` |
| 2 | `POST /api/v1/support/tickets/{id}/close` | Path and method named by the criteria; success status not specified | `[TS-AC2]` `[TS-AC6]` |
| 3 | rating submission | Offered on close; **no endpoint or request shape is named** — see OQ-1 | `[TS-AC2]` |
| 4 | `summary` (request) | Required, non-blank; shown to the customer. No length bound stated | `[TS-AC1]` `[TS-AC5]` |
| 5 | rating | Integer 1–5, with an optional comment; one per ticket | `[TS-AC2]` |
| 6 | ticket `status` | `IN_PROGRESS` → `RESOLVED` → `CLOSED`; `NEW` → `CLOSED` refused | `[TS-AC1]` `[TS-AC2]` `[TS-AC4]` |
| 7 | permitted-transition list | Returned in the `422` body | `[TS-AC4]` |
| 8 | `tickets.resolved_at` | Set on resolution | `[TS-AC1]` |
| 9 | time-to-resolution metric | Recorded on resolution; definition and storage not specified | `[TS-AC1]` |
| 10 | audit actor | Value `SYSTEM` for the automatic closure | `[TS-AC3]` |
| 11 | reminder and closure windows | 3 days to reminder, 7 days to closure, from entering `RESOLVED` | `[TS-AC3]` |
| 12 | resolution email | Carries the summary and a confirmation request | `[TS-AC1]` |
| 13 | `ProblemDetail.type` | Slugs `already-rated`, `illegal-transition`, `validation-failed`, `closure-not-permitted` | `[TS-AC2]` `[TS-AC4]` `[TS-AC5]` `[TS-AC6]` |
| 14 | Message strings | One exact string, quoted in TS-AC5 | `[TS-AC5]` |

## 5. Out of Scope

- Reopening — US-4.7.
- Reporting on satisfaction scores or resolution times — captured here, analysed nowhere.
- SLA threshold values — see US-4.3 OQ-1.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The permitted transitions are those in the lifecycle table in `docs/backlog/README.md`. | TS-AC4 requires the response to list them but the criteria enumerate only the `NEW` → `CLOSED` case. |
| A-2 | SLA timers pause in `WAITING_FOR_CUSTOMER` and `RESOLVED`. | Named in the story's Non-Functional section. It also contradicts US-4.5 TA-AC2 — see US-4.5 OQ-2. |
| A-3 | The rating is optional; declining it does not prevent closure. | TS-AC2 says the customer "is offered" a rating, and states no consequence for declining. |
| A-4 | The auto-closure job runs as an identifiable `SYSTEM` actor in the same audit stream as agent actions. | TS-AC3 names the actor without naming the stream. See US-3.7 OQ-1. |
| A-5 | The reminder in TS-AC3 is an email, subject to US-5.4 preferences. | TS-AC3 says "a reminder is sent" without naming a channel. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TS-AC2 offers a rating and stores it, but no criterion names an endpoint, a request shape, or when the rating may be submitted relative to the close call. The `409` for a second rating implies a separate submission the contract does not describe. | TS-AC2 |
| OQ-2 | TS-AC1 conditions on a ticket "in status IN_PROGRESS". The lifecycle also permits `WAITING_FOR_CUSTOMER` → `RESOLVED`. May an agent resolve a ticket that is waiting on the customer, effectively giving up on a reply? | TS-AC1 |
| OQ-3 | TS-AC2 attributes the rating to "the agent who resolved it". US-4.3 TQ-AC5 permits reassignment, so the last agent inherits a score for an exchange others handled. Is that intended? | TS-AC2 |
| OQ-4 | TS-AC3 fixes a 7-day auto-closure window for every priority. Should a critical ticket sitting unconfirmed for a week be treated the same as a low-priority one? | TS-AC3 |
| OQ-5 | TS-AC6 refuses agent-initiated closure, but TS-AC3 lets the system close automatically after 7 days. Is there any path for an agent to close a ticket the customer will demonstrably never confirm, short of waiting? | TS-AC6 |
| OQ-6 | TS-AC1 records "the time-to-resolution metric" without defining it — from creation, from first response, or from claim? Each yields a different number and a different team incentive. | TS-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TS-AC1 | Resolving sets the status, emails the summary and records a metric | §3.1, §4 | Covered — the criterion now defines time-to-resolution and its exclusions |
| TS-AC2 | The customer closes and rates once | §3.2, §4 | **Partial** — no interface exists for the rating the criterion stores (OQ-1) |
| TS-AC3 | Reminder at 3 days, automatic closure at 7, idempotently | §3.3, §4 | Covered — see OQ-4 |
| TS-AC4 | Illegal transitions are refused and the legal ones listed | §3.4, §4 | Covered — see A-1 |
| TS-AC5 | Resolving without a summary is refused | §3.1, §4 | Covered |
| TS-AC6 | Only the customer or the job may close | §3.5, §4 | Covered — see OQ-5 |

**Coverage:** 5 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.6-ticket-resolution.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
