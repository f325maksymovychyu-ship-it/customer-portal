# Epic 4 — Feedback / Support: My Tickets

**Story ID:** US-4.2
**Project:** Customer Portal
**AC prefix:** `TL-AC`
**Module:** `support/`

## User Story
As a signed-in customer,
I want to see my own tickets and their current status,
So that I know what is happening with my problem and do not raise it a second time.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Response to someone else's ticket | `404`, never `403` | A `403` confirms the ticket exists, which is itself a disclosure |
| 2 | Ordering | Most recently updated first | The ticket that moved is the one the customer came to look at |
| 3 | Unread marking | Per customer, cleared when the thread is opened | Without it there is no way to tell an answered ticket from a waiting one at a glance |
| 4 | Isolation enforcement | Owner predicate applied in the repository query | A controller-level check is one forgotten annotation away from a leak |

## In Scope
- `GET /api/v1/support/tickets` — the caller's own tickets
- `GET /api/v1/support/tickets/{id}` — one ticket with its message thread
- Unread indicators and the open/closed filter
- Empty state

## Out of Scope
- The agent queue and other customers' tickets (US-4.3)
- Posting replies (US-4.4)
- Status transitions (US-4.6, US-4.7)

## API Contract
| Method | Path | Auth | Query | Success |
|---|---|---|---|---|
| GET | `/api/v1/support/tickets` | Bearer | `state` = `OPEN` \| `CLOSED`, `page`, `size` | `200` `Page<TicketSummary>` |
| GET | `/api/v1/support/tickets/{id}` | Bearer | — | `200` `TicketDetail` including the public message thread |

## Data Model Notes
- `TicketSummary` carries `reference`, `subject`, `status`, `updatedAt`, `hasUnreadReply`
- `ticket_reads` records the last message each participant has seen, which is what `hasUnreadReply` is derived from
- `TicketDetail` returns only messages whose `visibility` is `PUBLIC`; internal notes are excluded at the query level, not filtered afterwards (see US-4.5)

## Acceptance Criteria

### Happy path
**TL-AC1 — Ticket list**
```gherkin
Given a customer with four tickets in different statuses
When GET /api/v1/support/tickets is called
Then respond 200 with one entry per ticket, most recently updated first
And "updated" means the last event the customer can see, so an internal note does not reorder the list
And each entry carries reference, subject, status, creation time and last update time
And tickets carrying an unread agent reply are flagged
When state=OPEN is supplied
Then closed and resolved tickets are excluded
```

**TL-AC2 — Ticket detail**
```gherkin
Given the customer opens their ticket "#10425"
When GET /api/v1/support/tickets/{id} is called
Then respond 200 with the messages in chronological order, each carrying its author and time
And the current status and the assigned agent's display name are included
And every attachment on a public message is retrievable through a link valid for 15 minutes and bound to the calling account
And the unread flag for that ticket is cleared
```

### Isolation
**TL-AC3 — Another customer's ticket**
```gherkin
Given ticket "#10999" belongs to a different customer
When GET /api/v1/support/tickets/{id} is called for it
Then respond 404 with type ".../errors/ticket-not-found"
And the response body is identical to the one returned for an identifier that never existed
And no field of the ticket appears anywhere in the response
```

**TL-AC4 — Internal notes are not reachable**
```gherkin
Given the ticket carries internal notes written by agents
When the owning customer requests the ticket detail
Then no note text appears in the response
And no gap in message numbering or count hints that hidden messages exist
```

### Empty state
**TL-AC5 — No tickets yet**
```gherkin
Given the customer has never raised a ticket
When GET /api/v1/support/tickets is called
Then respond 200 with an empty page
And the client shows an explanation and a "Create a ticket" action rather than an empty table
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/ticket-not-found",
  "title": "Ticket Not Found",
  "status": 404,
  "detail": "No such ticket exists.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000"
}
```
Error `type` slugs introduced by this story: `ticket-not-found`.

## Non-Functional / Security Requirements
- Ownership is enforced by a predicate in the repository query. A build that fetches the ticket and then compares the owner in the service layer fails this story's intent even if TL-AC3 passes, because the next endpoint added will forget the comparison.
- The `404` body for a foreign ticket and for a non-existent identifier must be byte-identical.
- Attachment links are short-lived and bound to the requesting account, matching US-4.1.
- **Performance:** p95 ≤ 500 ms for the list, ≤ 700 ms for a thread of 50 messages.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TL-AC1, TL-AC2 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| TL-AC3 | Functional test comparing the two `404` bodies byte for byte | `[gate]` |
| TL-AC4 | Integration test seeding internal notes and asserting their absence and no count gap | `[gate]` |
| TL-AC5 | Functional test on a customer with no tickets | `[gate]` |
| Repository-level isolation | Repository test asserting the owner predicate is present in the generated query | `[gate]` |

## Open Questions
1. Should a customer see which agent is assigned, or only that someone is working on it? Naming the agent personalises the exchange but exposes staff identity to every customer.
2. How long do closed tickets stay in the list before archiving? Interacts with the reopen window in US-4.7.
3. Does an organisation-level view exist, where a manager sees tickets raised by their colleagues? Nothing in Release 1.0 requires it, and adding it later changes the isolation predicate that TL-AC3 depends on.
