---
story_id: US-4.7
title: "Epic 4 — Feedback / Support: Reopen Ticket"
source: docs/backlog/US-4.7-reopen-ticket.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Reopen Ticket

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want to reopen a closed ticket when the problem comes back,
> So that the conversation continues with its history instead of starting from nothing.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**TO-AC1 — Reopening inside the window**
```gherkin
Given a ticket closed 5 days ago belonging to the calling customer, inside the 14-day reopen window
When POST /api/v1/support/tickets/{id}/reopen is called with a non-empty reason
Then the status becomes REOPENED and the reference is unchanged
And the entire previous message history remains attached
And the reason is appended to the thread as a public message
And the ticket returns to the queue assigned to the agent who handled it
And the SLA timer starts again from zero
```

**TO-AC2 — Reopening outside the window**
```gherkin
Given a ticket closed 20 days ago, outside the 14-day reopen window
When the customer opens it
Then no reopen action is offered, only "Create a new ticket based on this one"
And that action posts to POST /api/v1/support/tickets with relatedTicketId set to the closed ticket
When POST /api/v1/support/tickets/{id}/reopen is called directly
Then respond 422 with type ".../errors/reopen-window-expired"
And the detail reads "This ticket closed more than 14 days ago. Create a new ticket linked to it instead"
And the window boundary is evaluated at exactly 14 days from tickets.closed_at, inclusive
```

**TO-AC3 — Reopening someone else's ticket**
```gherkin
Given the ticket belongs to a different customer
When reopen is requested
Then respond 404, matching US-4.2 TL-AC3
```

**TO-AC4 — Reopening a ticket that is not closed**
```gherkin
Given the ticket status is IN_PROGRESS
When reopen is requested
Then respond 422 with type ".../errors/illegal-transition"
And the allowed transitions from the current status are listed
```

**TO-AC5 — Reopening without an explanation**
```gherkin
Given a reopen request whose reason is absent or blank
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
And the detail reads "Tell us what happened, so the agent knows what changed"
```

**TO-AC6 — The previous agent is no longer available**
```gherkin
Given the ticket is reopened
And the previous assignee's account is deactivated or they no longer hold an agent role
When assignment is resolved
Then the ticket enters the unassigned queue instead
And the ticket shows "The previous agent is unavailable"
And no notification is sent to the deactivated account
```

## 3. Functional Specification

### 3.1 Reopening

A reopen request from the ticket's owner, carrying an explanation, moves a ticket closed
within the window to status `REOPENED`, leaving the reference unchanged. `[TO-AC1]`

The entire previous message history remains attached. `[TO-AC1]`

The explanation is appended to the thread as a public message. `[TO-AC1]`

The ticket returns to the queue assigned to the agent who handled it, and the SLA timer
starts again from zero. `[TO-AC1]`

Where the previous assignee's account is deactivated or they no longer hold an agent role,
the ticket enters the unassigned queue instead, the ticket shows "The previous agent is
unavailable", and no notification is sent to the deactivated account. `[TO-AC6]`

### 3.2 The reopen window

Where the ticket was closed longer ago than the window allows, the client offers no reopen
action, only "Create a new ticket based on this one", which pre-fills a new ticket carrying
a link to the closed one. `[TO-AC2]`

A direct reopen request responds `422` with `type`
`.../errors/reopen-window-expired`, and the detail states the window and points at ticket
creation. `[TO-AC2]`

TO-AC1 exercises 5 days and TO-AC2 exercises 20 days. No criterion states the boundary
itself — see OQ-1.

### 3.3 State and authorisation

Where the ticket is not closed, a reopen request responds `422` with `type`
`.../errors/illegal-transition`, listing the allowed transitions from the current status.
`[TO-AC4]`

Where the ticket belongs to a different customer, the response is `404`, matching US-4.2
TL-AC3. `[TO-AC3]`

### 3.4 Validation

Where the reason is absent or blank, the response is `400` with `type`
`.../errors/validation-failed` and the detail "Tell us what happened, so the agent knows
what changed". `[TO-AC5]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/support/tickets/{id}/reopen` | Path and method named by the criteria; success status not specified | `[TO-AC1]` |
| 2 | "Create a new ticket based on this one" | A client action pre-filling the US-4.1 creation form with a link to the closed ticket; **no field or endpoint carries the link** — see OQ-2 | `[TO-AC2]` |
| 3 | explanation / reason (request) | Required, non-blank; appended to the thread as a public message. TO-AC1 calls it an explanation, TO-AC5 calls it a reason — see OQ-3 | `[TO-AC1]` `[TO-AC5]` |
| 4 | ticket `status` | Becomes `REOPENED`; only a closed ticket may transition | `[TO-AC1]` `[TO-AC4]` |
| 5 | ticket `reference` | Unchanged by a reopen | `[TO-AC1]` |
| 6 | reopen window | Bounded somewhere between 5 and 20 days; **the boundary is not stated** | `[TO-AC1]` `[TO-AC2]` |
| 7 | assignment on reopen | Previous assignee when eligible, otherwise the unassigned queue | `[TO-AC1]` `[TO-AC6]` |
| 8 | SLA timer | Restarts from zero; thresholds undefined — see US-4.3 OQ-1 | `[TO-AC1]` |
| 9 | `ProblemDetail.type` | Slugs `reopen-window-expired`, `illegal-transition`, `validation-failed` | `[TO-AC2]` `[TO-AC4]` `[TO-AC5]` |
| 10 | Message strings | Three exact strings, quoted in TO-AC2, TO-AC5 and TO-AC6 | `[TO-AC2]` `[TO-AC5]` `[TO-AC6]` |

## 5. Out of Scope

- Closing — US-4.6.
- An agent reopening on the customer's behalf — no criterion permits it; see OQ-5.
- Merging duplicate tickets — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The window is 14 days, as the story's Assumptions table states. | TO-AC1 and TO-AC2 exercise 5 and 20 days, which bound the boundary without fixing it. §3.2 cannot be tested at the edge without a value. See OQ-1. |
| A-2 | The window is measured from the closure timestamp. | Neither criterion says from what. For a ticket auto-closed under US-4.6 TS-AC3, closure and last message are a week apart. |
| A-3 | Reopening preserves `first_responded_at` and the original resolution summary. | Named in the story's Non-Functional section; no criterion asserts it, and TO-AC1's "entire previous message history" speaks only of messages. |
| A-4 | The window is evaluated against an injected clock so the boundary is testable. | Named in the story's Non-Functional section. |
| A-5 | Reopen is reachable only by the ticket owner, not by any agent scope. | TO-AC3 covers another customer; nothing covers an agent. The story's Non-Functional section states the intent. See OQ-5. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | No criterion states the reopen window. TO-AC1 uses 5 days and TO-AC2 uses 20, so any boundary between 6 and 20 satisfies both. TO-AC2 additionally requires the detail to "state the window", which cannot be written without the number. | TO-AC1, TO-AC2 |
| OQ-2 | TO-AC2's "Create a new ticket based on this one" must carry "a link to the closed one", but US-4.1's creation contract has no field for it, and no criterion there mentions a related ticket. | TO-AC2 |
| OQ-3 | TO-AC1 requires "an explanation" and TO-AC5 validates "reason". Are these the same field? A single request cannot satisfy both names. | TO-AC1, TO-AC5 |
| OQ-4 | Is there a limit on how often one ticket may be reopened? Nothing caps it, and a repeatedly reopened ticket is a signal worth escalating. | — |
| OQ-5 | May an agent reopen on the customer's behalf, for instance after a phone call? Support teams routinely need this, and no criterion either permits or forbids it. | — |
| OQ-6 | TO-AC1 restarts the SLA timer from zero while US-4.3 TQ-AC5 deliberately does not restart it on reassignment. Both are defensible; nothing records why they differ. | TO-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TO-AC1 | Reopening keeps the reference, history and previous agent | §3.1, §4 | Covered — the criterion now states the 14-day window and uses one field name |
| TO-AC2 | Outside the window, only a linked new ticket is offered | §3.2, §4 | Covered — the window, the error text and the `relatedTicketId` field are all now stated |
| TO-AC3 | A foreign ticket yields 404 | §3.3, §4 | Covered |
| TO-AC4 | A ticket that is not closed cannot be reopened | §3.3, §4 | Covered |
| TO-AC5 | A blank reason is refused | §3.4, §4 | Covered |
| TO-AC6 | An unavailable previous agent sends the ticket to the queue | §3.1, §4 | Covered |

**Coverage:** 6 Covered, 0 Partial, 0 Not covered.

> OQ-1 is the clearest example in the backlog of criteria that bound a value without
> setting it. Two criteria exercise 5 and 20 days; every implementation choosing a boundary
> in between passes both, and TO-AC2's required error text cannot be written at all.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.7-reopen-ticket.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
