# Epic 4 — Feedback / Support: Agent Reply and Internal Notes

**Story ID:** US-4.5
**Project:** Customer Portal
**AC prefix:** `TA-AC`
**Module:** `support/`

## User Story
As a support agent,
I want to answer the customer and separately leave notes only my team can read,
So that I can pass context to a colleague without showing the customer our working.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Message visibility | An explicit, required field with no default | A default is the mechanism by which an internal note eventually becomes public |
| 2 | Internal notes | Never emailed, never change status, never stop the SLA clock | A note is thinking, not work delivered to the customer |
| 3 | First response metric | Recorded on the first **public** agent message | A note is not a response, however substantial |
| 4 | Reply authority | Assignee, or a holder of `tickets:reply:any` | Otherwise a busy team cannot cover for an absent colleague |
| 5 | Canned responses | Inserted into the editor, never sent automatically | An unreviewed template reaches the customer with the wrong name in it |

## In Scope
- `POST /api/v1/support/tickets/{id}/messages` as an agent, with an explicit visibility
- Internal notes and their exclusion from every customer-facing surface
- First-response-time capture
- Canned response templates with placeholder substitution

## Out of Scope
- Customer replies (US-4.4)
- Status transitions beyond the one implied by a public reply (US-4.6)
- Template authoring and management — see Open Questions

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/support/tickets/{id}/messages` | Bearer + `tickets:reply` | `{"body", "visibility": "PUBLIC" \| "INTERNAL", "attachmentIds": [uuid]}` | `201` `TicketMessage` |
| GET | `/api/v1/support/templates` | Bearer + `tickets:reply` | — | `200` `{"templates": [{"id", "name", "body"}]}` |

## Data Model Notes
- `ticket_messages.visibility` ∈ {`PUBLIC`, `INTERNAL`}; the column is `NOT NULL` with no database default, so an omission fails loudly rather than quietly publishing
- `tickets.first_responded_at` is set once, on the first `PUBLIC` agent message, and never overwritten
- Customer-facing reads apply `visibility = 'PUBLIC'` inside the query (US-4.2), so an internal note is not merely hidden from the response but never selected

## Acceptance Criteria

### Happy path
**TA-AC1 — Public reply to the customer**
```gherkin
Given an agent assigned to the ticket
When POST /api/v1/support/tickets/{id}/messages is called with visibility PUBLIC
Then respond 201 and the message appears in the customer's thread
And an email carrying the reply and a link to the ticket is queued to the customer
And the ticket status becomes WAITING_FOR_CUSTOMER
And if this is the first public agent message, tickets.first_responded_at is set
```

**TA-AC2 — Internal note**
```gherkin
Given an agent working the ticket
When a message is posted with visibility INTERNAL
Then respond 201 and the note is rendered to agents with a distinct treatment and a "Team only" label
And no email is sent to the customer
And the ticket status does not change
And the SLA timer keeps running
And tickets.first_responded_at is not set by this message
```

### Confidentiality
**TA-AC3 — Notes are unreachable by the customer**
```gherkin
Given the ticket carries both public messages and internal notes
When the owning customer requests the ticket through the API
Then no note body, author or timestamp appears in the response
And messages carry no sequence number at all, only their identifier and timestamp, so no gap can exist to reveal
And the message count returned to the customer counts public messages only
And no note text appears in any notification email
And tickets.updated_at is not advanced by an internal note, so the customer's list order in US-4.2 TL-AC1 does not shift
```

**TA-AC4 — Visibility must be stated**
```gherkin
Given a request body omitting "visibility"
When the message is posted
Then respond 400 with type ".../errors/validation-failed" naming the field
And no message is stored
Because a defaulted visibility is how an internal note becomes public
```

### Authorisation
**TA-AC5 — Replying to a ticket assigned elsewhere**
```gherkin
Given the ticket is assigned to another agent
And this agent does not hold the tickets:reply:any scope
When they post a message
Then respond 403 with type ".../errors/not-ticket-assignee"
And the detail suggests claiming the ticket or asking a supervisor
But an agent holding tickets:reply:any may post without claiming
```

### Templates
**TA-AC6 — Canned response**
```gherkin
Given the agent is composing a reply
When they select the template "Request more information"
Then the template text is inserted into the editor with the customer's name and ticket reference substituted
And the agent can edit the text before sending
But no template is ever sent without an explicit send action
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/not-ticket-assignee",
  "title": "Not The Assignee",
  "status": 403,
  "detail": "This ticket is assigned to another agent. Claim it or ask a supervisor.",
  "instance": "/api/v1/support/tickets/0193f2c1-0000-7000-8000-000000000000/messages"
}
```
Error `type` slugs introduced by this story: `not-ticket-assignee`.

## Non-Functional / Security Requirements
- Visibility is decided in the query, not in the serializer. Filtering notes out of a fetched list is one refactor away from a leak, and TA-AC3 is written to fail if that shortcut is taken.
- The composer must show which mode it is in **before** the message is sent, not after. A confirmation that arrives afterwards does not prevent the disclosure.
- Internal notes are still audit-logged and still discoverable by an auditor holding the relevant scope — they are hidden from the customer, not from the organisation.
- **Performance:** p95 ≤ 400 ms.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TA-AC1 | Functional suite asserting the mail, the status change and `first_responded_at` | `[gate]` |
| TA-AC2 | Functional test asserting no mail, no status change and no metric write | `[gate]` |
| TA-AC3 | Security test seeding notes and asserting absence in API, mail and message counts | `[gate]` |
| TA-AC4 | Slice test asserting the omitted field is rejected | `[gate]` |
| TA-AC5 | Functional test with and without `tickets:reply:any` | `[gate]` |
| TA-AC6 | Client unit test on substitution, plus a manual review of the template copy | `[manual]` |
| Query-level exclusion | Repository test asserting the visibility predicate is in the generated SQL | `[gate]` |

## Open Questions
1. Who authors and maintains canned response templates, and through what interface? TA-AC6 consumes them but no story creates them.
2. Should an agent be able to convert a note into a public reply? It is a common request and a direct route to disclosing exactly the text that was meant to stay internal.
3. Do internal notes need their own retention rule, separate from the ticket? They often contain franker assessments of a customer than the thread does.
