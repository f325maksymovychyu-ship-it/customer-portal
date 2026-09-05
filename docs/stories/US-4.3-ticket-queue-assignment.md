# Epic 4 — Feedback / Support: Queue and Assignment

**Story ID:** US-4.3
**Project:** Customer Portal
**AC prefix:** `TQ-AC`
**Module:** `support/`

## User Story
As a support agent,
I want to see the unassigned queue and claim tickets from it,
So that the most urgent problems are worked first and nothing sits without an owner.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Assignment model | Agents pull from a queue; nothing is auto-assigned | Auto-assignment needs a workload model the team does not have yet |
| 2 | Queue ordering | Priority first, then nearest SLA deadline | Ordering by age alone lets a low-priority ticket outrank a breaching one |
| 3 | Claim conflict | Optimistic; the loser gets `409` and a refreshed queue | Locking a row while an agent reads it stalls the whole queue |
| 4 | Reassignment | Allowed with a reason, recorded as an internal note | The reason matters to the next agent and never to the customer |
| 5 | SLA on reassignment | Timer continues; it does not restart | Restarting it would let a breach be hidden by passing the ticket around |

## In Scope
- `GET /api/v1/support/queue` — unassigned and assigned views for agents
- `POST /api/v1/support/tickets/{id}/claim` — take a ticket
- `POST /api/v1/support/tickets/{id}/assign` — hand a ticket to another agent
- SLA-deadline highlighting in the queue

## Out of Scope
- Replying (US-4.5) and resolution (US-4.6)
- Defining SLA thresholds — consumed here, owned elsewhere
- Agent scheduling, shifts or capacity planning

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/support/queue` | Bearer + `tickets:read:any` | `assigned` = `NONE` \| `ME` \| `ANY`, `category`, `priority`, `page`, `size` | `200` `Page<QueueEntry>` |
| POST | `/api/v1/support/tickets/{id}/claim` | Bearer + `tickets:assign`, `If-Match` | *(empty)* | `200` `TicketDetail` |
| POST | `/api/v1/support/tickets/{id}/assign` | Bearer + `tickets:assign`, `If-Match` | `{"assigneeId": uuid, "reason": str}` | `200` `TicketDetail` |

## Data Model Notes
- `tickets.assignee_id` is null while unclaimed; `tickets.version` backs the `If-Match` that makes TQ-AC3 detectable
- `QueueEntry` adds `slaDueAt` and `slaState` ∈ {`OK`, `AT_RISK`, `BREACHED`} to the summary
- A reassignment writes a message row with `visibility = INTERNAL` carrying the reason, so US-4.5's visibility rule covers it automatically

## Acceptance Criteria

### Happy path
**TQ-AC1 — Viewing the queue**
```gherkin
Given an agent holding the tickets:read:any scope
When GET /api/v1/support/queue is called with assigned=NONE
Then respond 200 with unassigned tickets ordered by priority and then by nearest SLA deadline
And each entry carries an slaState of OK, AT_RISK or BREACHED, computed from the thresholds in docs/backlog/README.md
And a ticket is AT_RISK once 75% of its first-response deadline has elapsed, and BREACHED once the deadline has passed
And filters on category and priority narrow the result
```

**TQ-AC2 — Claiming a ticket**
```gherkin
Given ticket "#10425" has no assignee
When POST /api/v1/support/tickets/{id}/claim is called
Then the calling agent becomes the assignee and the status moves from NEW to IN_PROGRESS
And the customer is notified that their ticket is being worked on
And an audit entry records the claim
```

### Concurrency
**TQ-AC3 — Two agents claim at once**
```gherkin
Given another agent claimed ticket "#10425" moments earlier
When this agent's claim request arrives with the now-stale If-Match value
Then respond 409 with type ".../errors/ticket-already-assigned"
And the detail names the agent who holds it
And the client refreshes the queue so the row disappears
And the existing assignment is unchanged
```

### Authorisation
**TQ-AC4 — Caller without agent scope**
```gherkin
Given a customer without the tickets:read:any scope
When GET /api/v1/support/queue is called directly
Then respond 403 with type ".../errors/insufficient-scope"
And no ticket belonging to any other customer appears in any response to that caller
```

### Reassignment
**TQ-AC5 — Handing a ticket to a colleague**
```gherkin
Given the ticket is assigned to this agent
When POST /api/v1/support/tickets/{id}/assign names another agent and gives a reason
Then the new assignee is recorded and notified
And the reason is stored as an internal note, invisible to the customer
And the customer sees only that the agent's name changed
And the SLA timer continues rather than restarting
```

**TQ-AC6 — Assigning to an ineligible account**
```gherkin
Given the named assignee is deactivated, or holds no agent role
When the assignment is attempted
Then respond 422 with type ".../errors/ineligible-assignee"
And the current assignment is unchanged
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/ticket-already-assigned",
  "title": "Ticket Already Assigned",
  "status": 409,
  "detail": "This ticket is already assigned to Ivan Petrenko.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000/claim"
}
```
Error `type` slugs introduced by this story: `ticket-already-assigned`, `ineligible-assignee`.

## Non-Functional / Security Requirements
- The queue is the one place where an agent sees tickets belonging to customers other than themselves. The `tickets:read:any` scope must be checked on every queue and ticket read, not only on the queue listing.
- TQ-AC3 must be resolved by the optimistic-locking version, not by a read-then-write check, which would still allow both claims under load.
- Reassignment reasons are internal notes and inherit US-4.5's visibility guarantees, including its API-level exclusion.
- **Performance:** p95 ≤ 600 ms for the queue at 10 000 open tickets, with an index on `(status, priority, sla_due_at)`.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TQ-AC1 | Functional suite asserting the ordering across mixed priorities and deadlines | `[gate]` |
| TQ-AC2 | Functional suite asserting assignment, status change and notification | `[gate]` |
| TQ-AC3 | Integration test issuing two concurrent claims against one ticket | `[gate]` |
| TQ-AC4 | Functional test asserting `403` for a customer account | `[gate]` |
| TQ-AC5 | Integration test asserting the note is internal and the SLA due time is unchanged | `[gate]` |
| TQ-AC6 | Functional test with a deactivated and a non-agent target | `[gate]` |

## Open Questions
1. Should an agent be able to claim a ticket already assigned to someone else, or must it be released first? Current default: only the assignee or a supervisor may reassign, and TQ-AC3 refuses everyone else.
2. What happens to tickets assigned to an agent who is deactivated (US-3.4)? US-4.7 TO-AC3 handles it on reopen, but nothing handles it for a ticket already in progress.
3. SLA thresholds per priority are consumed by TQ-AC1 but defined nowhere. Until a product owner sets them, `slaState` cannot be computed.
