# Epic 4 — Feedback / Support: Ticket Resolution

**Story ID:** US-4.6
**Project:** Customer Portal
**AC prefix:** `TS-AC`
**Module:** `support/`

## User Story
As a support agent,
I want to mark a ticket resolved with a summary and have it close once the customer agrees,
So that the queue reflects real outstanding work and the customer knows how their problem ended.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Transition control | A state machine on the server; the client only renders what it permits | Two implementations of the rules will diverge, and the client's copy is the one attackers skip |
| 2 | Resolution summary | Mandatory, and shown to the customer | "Resolved" with no explanation is the most common complaint about ticket systems |
| 3 | Closure | The customer confirms; otherwise auto-close after 7 days in `RESOLVED` | Waiting forever inflates the queue; closing immediately denies the customer a say |
| 4 | Reminder | Sent on day 3 of the resolved window | One reminder is a courtesy; more is nagging |
| 5 | Satisfaction rating | 1–5 with an optional comment, once per ticket | Repeat ratings turn a metric into a poll |

## In Scope
- `POST /api/v1/support/tickets/{id}/resolve` and `/close`
- Server-side validation of every status transition
- The auto-closure job and its reminder
- Customer satisfaction capture on closure

## Out of Scope
- Reopening (US-4.7)
- Reporting on satisfaction scores or resolution times — captured here, analysed elsewhere
- SLA threshold values

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/support/tickets/{id}/resolve` | Bearer + `tickets:reply`, `If-Match` | `{"summary": str}` | `200` `TicketDetail` |
| POST | `/api/v1/support/tickets/{id}/close` | Bearer (owner), `If-Match` | *(empty)* | `200` `TicketDetail` + `ratingUrl` |
| POST | `/api/v1/support/tickets/{id}/rating` | Bearer (owner) | `{"rating": 1..5, "comment"?: str}` | `201` |

## Data Model Notes
- Permitted transitions are declared as a sealed type in the domain layer (`AGENTS.md` §4.2), so a switch over them cannot silently accept an unhandled case
- `tickets.resolved_at`, `tickets.closed_at`, `tickets.resolution_summary`
- `ticket_ratings`: one row per ticket, with a unique constraint on `ticketId` enforcing TS-AC2's "once only"
- Auto-closure runs as `actorType = SYSTEM` in the audit log, so it is never mistaken for an agent's action

## Acceptance Criteria

### Happy path
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

### Transition rules
**TS-AC4 — Disallowed transition**
```gherkin
Given a ticket in status NEW
When a transition directly to CLOSED is requested
Then respond 422 with type ".../errors/illegal-transition"
And the ProblemDetail lists the transitions that are permitted from NEW
And the status is unchanged
And the same rule is enforced regardless of the caller's scopes
```

### Validation and authorisation
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/illegal-transition",
  "title": "Illegal Transition",
  "status": 422,
  "detail": "A ticket in status NEW cannot move directly to CLOSED.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000/close",
  "allowedTransitions": ["IN_PROGRESS", "WAITING_FOR_CUSTOMER"]
}
```
Error `type` slugs introduced by this story: `illegal-transition`, `already-rated`, `closure-not-permitted`.

## Non-Functional / Security Requirements
- The state machine is the single source of truth and lives in the domain layer with no Spring or Jakarta imports (`AGENTS.md` §2.1). The client mirrors it for rendering only.
- Every status change is audited with actor, previous status and new status.
- SLA timers pause in `WAITING_FOR_CUSTOMER` and `RESOLVED` and resume on return to active work, so a customer's own delay never counts as a breach.
- The auto-closure job must be idempotent and safe to run concurrently on more than one instance.
- Use Awaitility or an injected `Clock` in tests for the timing rules; `Thread.sleep` is prohibited (`AGENTS.md` §5).

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TS-AC1 | Functional suite asserting status, summary, mail and the metric | `[gate]` |
| TS-AC2 | Functional test plus an integration test on the unique rating constraint | `[gate]` |
| TS-AC3 | Integration test with an injected `Clock`, run twice to prove idempotency | `[gate]` |
| TS-AC4 | Parameterised test covering every illegal pair in the transition table | `[gate]` |
| TS-AC5 | Slice test on the resolve request record | `[gate]` |
| TS-AC6 | Functional test for the foreign customer and for an agent caller | `[gate]` |
| Sealed transitions | ArchUnit test asserting no `default` case exists in the transition switch | `[gate]` |

## Open Questions
1. May an agent resolve a ticket that is in `WAITING_FOR_CUSTOMER`, effectively giving up on a reply? The transition table permits it, but nobody has confirmed that is intended.
2. Is a satisfaction rating attributed to the resolving agent fair when a ticket passed through several? US-4.3 TQ-AC5 allows reassignment, so the last agent inherits the whole exchange's score.
3. Should the 7-day auto-closure window vary by priority? A critical ticket sitting resolved and unconfirmed for a week is a different situation from a low-priority one.
