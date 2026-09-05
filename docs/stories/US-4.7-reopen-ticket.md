# Epic 4 — Feedback / Support: Reopen Ticket

**Story ID:** US-4.7
**Project:** Customer Portal
**AC prefix:** `TO-AC`
**Module:** `support/`

## User Story
As a signed-in customer,
I want to reopen a closed ticket when the problem comes back,
So that the conversation continues with its history instead of starting from nothing.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Reopen window | 14 days after closure | Long enough for an intermittent fault to recur, short enough that the context is still current |
| 2 | Beyond the window | A new ticket that links to the old one | A months-old thread is history, not live context, and reopening it hides a new problem inside an old record |
| 3 | Identity | The same ticket, the same reference, the full history | A new reference would make the customer re-explain and break their own records |
| 4 | Assignment | Returns to the previous agent when they are still active | They hold the context; routing elsewhere wastes it |
| 5 | SLA | Restarts on reopen | The team is being asked for new work, not resuming interrupted work |

## In Scope
- `POST /api/v1/support/tickets/{id}/reopen` — return a closed ticket to active work
- Window enforcement and the fallback to a linked new ticket
- Reassignment when the previous agent is unavailable
- Explanation required from the customer

## Out of Scope
- Closing (US-4.6)
- An agent reopening a ticket on the customer's behalf — see Open Questions
- Merging duplicate tickets

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/support/tickets/{id}/reopen` | Bearer (owner), `If-Match` | `{"reason": str}` | `200` `TicketDetail` |
| POST | `/api/v1/support/tickets` | Bearer | `{..., "relatedTicketId": uuid}` | `201` — the US-4.1 endpoint, with a link to the closed ticket |

## Data Model Notes
- `tickets.reopened_at` and `tickets.reopen_count`, so a ticket reopened repeatedly is visible as a recurring failure rather than one long thread
- `tickets.related_ticket_id` carries the link created by the out-of-window path
- The reopen reason is stored as a normal `PUBLIC` message, so the thread reads continuously
- `REOPENED` is a distinct status in the state machine owned by US-4.6, not an alias for `IN_PROGRESS`

## Acceptance Criteria

### Happy path
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

### Window enforcement
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

### Authorisation
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

### Validation and edge cases
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/reopen-window-expired",
  "title": "Reopen Window Expired",
  "status": 422,
  "detail": "This ticket closed more than 14 days ago. Create a new ticket linked to it instead.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000/reopen",
  "closedAt": "2026-08-02T09:14:00Z"
}
```
Error `type` slugs introduced by this story: `reopen-window-expired`. `illegal-transition` is shared with US-4.6.

## Non-Functional / Security Requirements
- The window is evaluated against an injected `Clock`, so the boundary is testable without waiting 14 days.
- Reopening is a customer decision. The endpoint must not be reachable by an agent scope, or the closure metric in US-4.6 becomes editable by the team it measures.
- A reopened ticket must not clear `first_responded_at` or the original resolution summary; both remain part of the record.
- **Performance:** p95 ≤ 400 ms.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TO-AC1 | Functional suite asserting status, reference, history and assignment | `[gate]` |
| TO-AC2 | Integration test at 13 and 15 days against an injected `Clock` | `[gate]` |
| TO-AC3 | Functional test asserting the shared `404` body | `[gate]` |
| TO-AC4 | Parameterised test over non-closed statuses | `[gate]` |
| TO-AC5 | Slice test on the reopen request record | `[gate]` |
| TO-AC6 | Integration test with a deactivated previous assignee | `[gate]` |
| Metric preservation | Integration test asserting the original resolution fields survive | `[gate]` |

## Open Questions
1. Should an agent be able to reopen on the customer's behalf, for instance after a phone call? Currently forbidden by design, but support teams routinely ask for it.
2. Does a repeatedly reopened ticket need escalation? `reopen_count` exists precisely so that rule can be written, but no rule has been agreed.
3. Should the 14-day window be measured from closure or from the last message? For an auto-closed ticket (US-4.6 TS-AC3) those differ by a week.
