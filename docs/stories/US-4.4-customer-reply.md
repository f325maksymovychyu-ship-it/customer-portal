# Epic 4 — Feedback / Support: Customer Reply

**Story ID:** US-4.4
**Project:** Customer Portal
**AC prefix:** `TR-AC`
**Module:** `support/`

## User Story
As a signed-in customer,
I want to reply inside my own ticket thread,
So that clarifications stay with the original problem instead of becoming a second ticket.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Effect on status | A customer reply moves the ticket to `WAITING_FOR_SUPPORT` | The ball is visibly back with the team, and the SLA clock restarts on the right side |
| 2 | Editing | Messages are immutable once posted | An editable thread is worthless as a record of what was actually said |
| 3 | Closed tickets | Not repliable; the customer is offered reopen instead | Replying to a closed ticket silently reopens it without anyone deciding to |
| 4 | Length | 1–5 000 characters | Matches the description bound in US-4.1 |
| 5 | Ordering under concurrency | Server timestamp decides, not arrival order at the client | Two clients cannot agree on order without a server authority |

## In Scope
- `POST /api/v1/support/tickets/{id}/messages` — post a public reply as the ticket owner
- The resulting status transition and agent notification
- Thread refresh when both sides post at once
- Attachment reuse from US-4.1 on replies

## Out of Scope
- Agent replies and internal notes (US-4.5)
- Reopening a closed ticket (US-4.7)
- Message editing or deletion — deliberately unsupported

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/support/tickets/{id}/messages` | Bearer (owner) | `{"body": str, "attachmentIds": [uuid]}` | `201` `TicketMessage` |

## Data Model Notes
- `ticket_messages`: `id` (UUIDv7, so insertion order and chronological order agree), `ticketId`, `authorId`, `authorRole`, `visibility`, `body`, `postedAt`
- A customer-authored message always carries `visibility = PUBLIC`; the field is not accepted from the request body on this endpoint
- Posting updates `tickets.status` and `tickets.updated_at` in the same `@Transactional` boundary as the insert

## Acceptance Criteria

### Happy path
**TR-AC1 — Posting a reply**
```gherkin
Given a customer viewing their own ticket in status WAITING_FOR_CUSTOMER
When POST /api/v1/support/tickets/{id}/messages is called with a non-empty body
Then respond 201 with the stored message including its author and time
And the ticket status becomes WAITING_FOR_SUPPORT
And the assigned agent is notified
And the SLA timer for a support response resumes
```

**TR-AC2 — Replying with an attachment**
```gherkin
Given the customer has uploaded a file through the attachment endpoint in US-4.1
When the reply references that attachment identifier
Then the attachment appears on the message
And the same size, type and scan rules from US-4.1 apply unchanged
```

### Validation
**TR-AC3 — Empty or oversized body**
```gherkin
Given a body that is empty or contains only whitespace
When the reply is submitted
Then respond 400 with type ".../errors/validation-failed"
And the client keeps the send control disabled in that state
Given a body longer than 5 000 characters
Then respond 400 naming the limit
And the client shows a character counter that turns to an error state before the limit is passed
```

### State gating
**TR-AC4 — Replying to a closed ticket**
```gherkin
Given the ticket status is CLOSED
When the customer opens it
Then no reply field is offered, only a "Reopen this ticket" action
When POST /api/v1/support/tickets/{id}/messages is called directly
Then respond 422 with type ".../errors/ticket-not-repliable"
And the detail points at the reopen operation in US-4.7
```

### Authorisation
**TR-AC5 — Replying to someone else's ticket**
```gherkin
Given the ticket belongs to a different customer
When the reply is submitted
Then respond 404 with type ".../errors/ticket-not-found"
And the response is identical to the one for a non-existent ticket, per US-4.2 TL-AC3
```

### Concurrency
**TR-AC6 — Simultaneous replies**
```gherkin
Given the customer is composing a reply
And the agent posts a message at the same moment
When the customer submits
Then both messages are stored and ordered by their server timestamps
And the thread updates without a page reload
And neither message is lost, duplicated, or reordered on subsequent loads
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/ticket-not-repliable",
  "title": "Ticket Not Repliable",
  "status": 422,
  "detail": "This ticket is closed. Reopen it to continue the conversation.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000/messages"
}
```
Error `type` slugs introduced by this story: `ticket-not-repliable`.

## Non-Functional / Security Requirements
- `visibility` must not be readable from the request body on this endpoint. A customer must have no path, however malformed, to authoring an internal note.
- Message bodies are sanitised on render exactly as ticket descriptions are in US-4.1.
- The status transition is part of the same transaction as the insert; a stored message with an unchanged status leaves the ticket invisible to the queue.
- **Performance:** p95 ≤ 400 ms.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TR-AC1 | Functional suite asserting the message, the status change and the notification | `[gate]` |
| TR-AC2 | Integration test reusing the US-4.1 attachment flow | `[gate]` |
| TR-AC3 | Slice test on the request record | `[gate]` |
| TR-AC4 | Functional test asserting `422` against a closed ticket | `[gate]` |
| TR-AC5 | Functional test comparing the `404` body with the non-existent case | `[gate]` |
| TR-AC6 | Integration test posting two messages concurrently and asserting the order | `[gate]` |
| `visibility` not accepted | Contract test asserting the field is rejected as unknown | `[gate]` |

## Open Questions
1. Should a reply to a `RESOLVED` ticket reopen it automatically, or move it to `WAITING_FOR_SUPPORT` while leaving resolution intact? The state table in `README.md` currently allows the transition but does not say which the customer intends.
2. Is there a cooling-off limit on replies, to stop a frustrated customer posting twenty messages in a minute? None is specified, and the US-4.1 rate limit does not cover this endpoint.
3. Should the customer see typing or read indicators from the agent? It sets an expectation of immediacy the team may not want to make.
