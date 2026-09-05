---
story_id: US-4.4
title: "Epic 4 — Feedback / Support: Customer Reply"
source: docs/backlog/US-4.4-customer-reply.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Customer Reply

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want to reply inside my own ticket thread,
> So that clarifications stay with the original problem instead of becoming a second ticket.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**TR-AC4 — Replying to a closed ticket**
```gherkin
Given the ticket status is CLOSED
When the customer opens it
Then no reply field is offered, only a "Reopen this ticket" action
When POST /api/v1/support/tickets/{id}/messages is called directly
Then respond 422 with type ".../errors/ticket-not-repliable"
And the detail points at the reopen operation in US-4.7
```

**TR-AC5 — Replying to someone else's ticket**
```gherkin
Given the ticket belongs to a different customer
When the reply is submitted
Then respond 404 with type ".../errors/ticket-not-found"
And the response is identical to the one for a non-existent ticket, per US-4.2 TL-AC3
```

**TR-AC6 — Simultaneous replies**
```gherkin
Given the customer is composing a reply
And the agent posts a message at the same moment
When the customer submits
Then both messages are stored and ordered by their server timestamps
And the thread updates without a page reload
And neither message is lost, duplicated, or reordered on subsequent loads
```

## 3. Functional Specification

### 3.1 Posting a reply

A reply from the ticket's owner, carrying a non-empty body, responds `201` with the stored
message including its author and time. `[TR-AC1]`

The ticket status becomes `WAITING_FOR_SUPPORT`. `[TR-AC1]`

The assigned agent is notified. `[TR-AC1]`

The SLA timer for a support response resumes. `[TR-AC1]`

### 3.2 Attachments on a reply

Where the reply references an attachment identifier produced by the upload path of
US-4.1, the attachment appears on the message. `[TR-AC2]`

The size, type and scan rules of US-4.1 apply unchanged. `[TR-AC2]`

### 3.3 Body validation

A body that is empty or contains only whitespace is rejected with `400` and `type`
`.../errors/validation-failed`, and the client keeps the send control disabled in that
state. `[TR-AC3]`

A body longer than 5 000 characters is rejected with `400` naming the limit, and the
client shows a character counter that turns to an error state before the limit is passed.
`[TR-AC3]`

### 3.4 State gating

Where the ticket's status is `CLOSED`, the client offers no reply field, only a "Reopen
this ticket" action. `[TR-AC4]`

A direct request to post a message responds `422` with `type`
`.../errors/ticket-not-repliable`, and the detail points at the reopen operation of
US-4.7. `[TR-AC4]`

### 3.5 Authorisation

Where the ticket belongs to a different customer, the response is `404` with `type`
`.../errors/ticket-not-found`, identical to the response for a ticket that never existed.
`[TR-AC5]`

### 3.6 Concurrency

Where the customer and the agent post at the same moment, both messages are stored and
ordered by their server timestamps. `[TR-AC6]`

The thread updates without a page reload, and no message is lost, duplicated or reordered
on subsequent loads. `[TR-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/support/tickets/{id}/messages` | Path and method named by the criteria; responds `201` | `[TR-AC1]` |
| 2 | message body (request) | Non-empty after trimming; maximum 5 000 characters | `[TR-AC1]` `[TR-AC3]` |
| 3 | attachment identifier (request) | Produced by the US-4.1 upload path | `[TR-AC2]` |
| 4 | stored message (response) | Author and time; ordering key is the server timestamp | `[TR-AC1]` `[TR-AC6]` |
| 5 | message visibility | **Not present in this endpoint's contract.** US-4.5 TA-AC4 makes it a required field on the agent's path | — |
| 6 | ticket `status` | `WAITING_FOR_CUSTOMER` → `WAITING_FOR_SUPPORT` on reply; `CLOSED` refuses | `[TR-AC1]` `[TR-AC4]` |
| 7 | SLA timer | Resumes on a customer reply; thresholds undefined — see US-4.3 OQ-1 | `[TR-AC1]` |
| 8 | `ProblemDetail.type` | Slugs `validation-failed`, `ticket-not-repliable`, `ticket-not-found` | `[TR-AC3]` `[TR-AC4]` `[TR-AC5]` |
| 9 | Message strings | "Reopen this ticket" action label | `[TR-AC4]` |
| 10 | live update mechanism | Required by TR-AC6; **no transport is specified** — see OQ-3 | `[TR-AC6]` |

## 5. Out of Scope

- Agent replies and internal notes — US-4.5.
- Reopening a closed ticket — US-4.7.
- Editing or deleting a message — no criterion reaches it; the story treats the thread as
  immutable.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | A customer-authored message is always public; the endpoint does not read a visibility field from the request. | TR-AC3 constrains only the body. Without this, a customer could author an internal note, which US-4.5 TA-AC3 exists to prevent. |
| A-2 | The status transition and the message insert occur in one transaction. | TR-AC1 lists both outcomes without stating atomicity; a stored message with an unchanged status is invisible to the queue. |
| A-3 | Message bodies are sanitised on render, as in US-4.1. | No criterion here constrains rendering. |
| A-4 | TR-AC1's status transition applies from `WAITING_FOR_CUSTOMER`. Replies from other open statuses are governed by the lifecycle table in `docs/backlog/README.md`. | TR-AC1 conditions on one status only; the endpoint is reachable from others. See OQ-1. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TR-AC1 defines the reply path only from `WAITING_FOR_CUSTOMER`. What happens when the customer replies to a ticket that is `NEW`, `IN_PROGRESS` or `RESOLVED`? The lifecycle table permits transitions the criteria never exercise, and a reply to a `RESOLVED` ticket is the common real case. | TR-AC1 |
| OQ-2 | TR-AC1 notifies "the assigned agent". A `NEW` ticket has none (US-4.3). Who is notified then? | TR-AC1 |
| OQ-3 | TR-AC6 requires the thread to update "without a page reload" but names no mechanism. Does this depend on the real-time delivery of US-5.2, which is itself blocked on an unapproved dependency, or is polling acceptable? | TR-AC6 |
| OQ-4 | No criterion limits how often a customer may reply, though US-4.1 TC-AC5 limits ticket creation. A frustrated customer can post without bound. | — |
| OQ-5 | TR-AC3 rejects a body over 5 000 characters. US-4.1 TC-AC3 sets no maximum on the description at all (US-4.1 OQ-6). Should the two limits match? | TR-AC3 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TR-AC1 | A reply moves the ticket to WAITING_FOR_SUPPORT and notifies | §3.1, §4 | **Partial** — the criterion covers one source status and one notification target, both of which have undefined cases (OQ-1, OQ-2) |
| TR-AC2 | An uploaded attachment can be carried on a reply | §3.2, §4 | Covered |
| TR-AC3 | Empty and oversized bodies are rejected at both ends | §3.3, §4 | Covered — see OQ-5 |
| TR-AC4 | A closed ticket offers reopen rather than reply | §3.4, §4 | Covered |
| TR-AC5 | A foreign ticket is indistinguishable from a missing one | §3.5, §4 | Covered |
| TR-AC6 | Concurrent messages are ordered and never lost | §3.6, §4 | **Partial** — the live update the criterion asserts has no specified transport (OQ-3) |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.4-customer-reply.md`. |
