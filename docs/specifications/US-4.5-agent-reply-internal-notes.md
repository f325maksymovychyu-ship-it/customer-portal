---
story_id: US-4.5
title: "Epic 4 — Feedback / Support: Agent Reply and Internal Notes"
source: docs/backlog/US-4.5-agent-reply-internal-notes.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Agent Reply and Internal Notes

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a support agent, I want to answer the customer and separately leave notes only my team
> can read, So that I can pass context to a colleague without showing the customer our working.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**TA-AC5 — Replying to a ticket assigned elsewhere**
```gherkin
Given the ticket is assigned to another agent
And this agent does not hold the tickets:reply:any scope
When they post a message
Then respond 403 with type ".../errors/not-ticket-assignee"
And the detail suggests claiming the ticket or asking a supervisor
But an agent holding tickets:reply:any may post without claiming
```

**TA-AC6 — Canned response**
```gherkin
Given the agent is composing a reply
When they select the template "Request more information"
Then the template text is inserted into the editor with the customer's name and ticket reference substituted
And the agent can edit the text before sending
But no template is ever sent without an explicit send action
```

## 3. Functional Specification

### 3.1 Public reply

A message posted by the assigned agent with visibility `PUBLIC` responds `201` and appears
in the customer's thread. `[TA-AC1]`

An email carrying the reply and a link to the ticket is queued to the customer.
`[TA-AC1]`

The ticket status becomes `WAITING_FOR_CUSTOMER`. `[TA-AC1]`

Where this is the first public agent message, `tickets.first_responded_at` is set.
`[TA-AC1]`

### 3.2 Internal note

A message posted with visibility `INTERNAL` responds `201`, and the note is rendered to
agents with a distinct treatment and a "Team only" label. `[TA-AC2]`

No email is sent to the customer, the ticket status does not change, the SLA timer keeps
running, and `tickets.first_responded_at` is not set by this message. `[TA-AC2]`

### 3.3 Confidentiality

Where a ticket carries both public messages and internal notes, a request by the owning
customer returns no note body, author or timestamp. `[TA-AC3]`

The message count and any sequence numbers show no gap that would reveal hidden entries.
`[TA-AC3]`

No note text appears in any notification email. `[TA-AC3]`

### 3.4 Visibility is mandatory

Where the request body omits `visibility`, the response is `400` with `type`
`.../errors/validation-failed` naming the field, and no message is stored. `[TA-AC4]`

### 3.5 Authorisation

Where the ticket is assigned to another agent and the caller does not hold the
`tickets:reply:any` scope, the response is `403` with `type`
`.../errors/not-ticket-assignee`, and the detail suggests claiming the ticket or asking a
supervisor. `[TA-AC5]`

An agent holding `tickets:reply:any` may post without claiming. `[TA-AC5]`

### 3.6 Canned responses

Where the agent selects a template while composing, its text is inserted into the editor
with the customer's name and the ticket reference substituted. `[TA-AC6]`

The agent can edit the text before sending, and no template is ever sent without an
explicit send action. `[TA-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | message endpoint | **Not named by any criterion here.** US-4.4 TR-AC1 gives `POST /api/v1/support/tickets/{id}/messages` for the customer path; whether the agent uses the same one is inferred — see A-1 | `[TA-AC1]` |
| 2 | `visibility` (request) | Required, no default; values `PUBLIC` and `INTERNAL` | `[TA-AC1]` `[TA-AC2]` `[TA-AC4]` |
| 3 | message body (request) | not specified here; US-4.4 TR-AC3 bounds the customer path only | `[TA-AC1]` |
| 4 | `tickets:reply:any` scope | Permits posting to a ticket assigned elsewhere | `[TA-AC5]` |
| 5 | ticket `status` | Becomes `WAITING_FOR_CUSTOMER` on a public reply; unchanged on a note | `[TA-AC1]` `[TA-AC2]` |
| 6 | `tickets.first_responded_at` | Set once, by the first public agent message only | `[TA-AC1]` `[TA-AC2]` |
| 7 | note rendering | Distinct treatment plus a "Team only" label, for agents | `[TA-AC2]` |
| 8 | customer-facing message count | Must contain no gaps attributable to hidden notes | `[TA-AC3]` |
| 9 | reply email | Carries the reply text and a link to the ticket | `[TA-AC1]` |
| 10 | template | Named example "Request more information"; substitutes customer name and ticket reference. **No criterion says where templates come from** — see OQ-1 | `[TA-AC6]` |
| 11 | `ProblemDetail.type` | Slugs `validation-failed`, `not-ticket-assignee` | `[TA-AC4]` `[TA-AC5]` |

## 5. Out of Scope

- Customer replies — US-4.4.
- Status transitions beyond the one a public reply causes — US-4.6.
- Template authoring and management — no criterion reaches it; see OQ-1.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Agents post through the same endpoint as customers, `POST /api/v1/support/tickets/{id}/messages`, with `visibility` added. | No criterion here names an endpoint. TA-AC4 speaks of "a request body", which implies one. |
| A-2 | Visibility is applied as a predicate in the query serving the customer's view, not as a filter after fetching. | Named in the story's Non-Functional section. TA-AC3 is satisfiable either way, but only the former is robust to the next endpoint. |
| A-3 | Internal notes remain visible to auditors holding the relevant scope. | Named in the story's Non-Functional section; TA-AC3 hides them from the customer only. |
| A-4 | TA-AC1's status transition applies whatever the ticket's prior open status. | TA-AC1 states the destination but not the source status. |
| A-5 | The reply email is subject to the notification preferences of US-5.4. | No criterion here says whether it can be switched off, though US-5.4 NP-AC2 uses "ticket reply" as its worked example. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TA-AC6 consumes canned response templates, but no story creates, edits or lists them. Who authors them, through what interface, and are they global or per-agent? | TA-AC6 |
| OQ-2 | TA-AC2 says the SLA timer "keeps running" during an internal note, while US-4.6 pauses it in `WAITING_FOR_CUSTOMER`. If the agent notes on a ticket already waiting on the customer, is the timer running or paused? The two stories disagree. | TA-AC2 |
| OQ-3 | TA-AC3 forbids gaps in "any sequence numbers", which constrains a numbering scheme no criterion defines. Are messages numbered per ticket, and is the number customer-visible? | TA-AC3 |
| OQ-4 | May an agent convert an internal note into a public reply? It is a routine request in support tooling and the most direct route to disclosing exactly the text TA-AC3 protects. | — |
| OQ-5 | TA-AC5 names `tickets:reply:any`, while US-4.3 TQ-AC4 names `tickets:read:any`. Is there a scope hierarchy, and does an agent need both? | TA-AC5 |
| OQ-6 | No criterion bounds an agent message's length, though US-4.4 TR-AC3 bounds the customer's at 5 000 characters. | TA-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TA-AC1 | A public reply emails the customer and sets the response metric | §3.1, §4 | **Partial** — no endpoint is named by any criterion in this story (OQ-6, A-1) |
| TA-AC2 | An internal note changes nothing the customer can observe | §3.2, §4 | **Partial** — the criterion's SLA claim contradicts US-4.6 (OQ-2) |
| TA-AC3 | Notes leave no body, author, timestamp, gap or email trace | §3.3, §4 | Covered — the criterion now removes sequence numbers entirely and pins the list ordering |
| TA-AC4 | An omitted visibility is refused rather than defaulted | §3.4, §4 | Covered |
| TA-AC5 | Only the assignee, or a holder of `tickets:reply:any`, may post | §3.5, §4 | Covered — see OQ-5 |
| TA-AC6 | Templates are inserted for editing, never auto-sent | §3.6, §4 | **Partial** — the templates the criterion selects from exist in no story (OQ-1) |

**Coverage:** 3 Covered, 3 Partial, 0 Not covered.

> TA-AC3 is the most security-relevant criterion in Epic 4 and it is only Partial. The leak
> it protects against has a second route that no criterion closes: US-4.2 OQ-3 notes that
> an internal note may still move the ticket to the top of the customer's list.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.5-agent-reply-internal-notes.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
