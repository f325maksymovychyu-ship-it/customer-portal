---
story_id: US-4.2
title: "Epic 4 — Feedback / Support: My Tickets"
source: docs/backlog/US-4.2-my-tickets.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# My Tickets

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want to see my own tickets and their current status,
> So that I know what is happening with my problem and do not raise it a second time.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**TL-AC5 — No tickets yet**
```gherkin
Given the customer has never raised a ticket
When GET /api/v1/support/tickets is called
Then respond 200 with an empty page
And the client shows an explanation and a "Create a ticket" action rather than an empty table
```

## 3. Functional Specification

### 3.1 The ticket list

A list request from a signed-in customer responds `200` with one entry per ticket they
own, most recently updated first. `[TL-AC1]`

Each entry carries the reference, subject, status, creation time and last update time.
`[TL-AC1]`

Tickets carrying an unread agent reply are flagged. `[TL-AC1]`

Where `state=OPEN` is supplied, closed and resolved tickets are excluded. `[TL-AC1]`

Where the customer has never raised a ticket, the response is `200` with an empty page,
and the client shows an explanation and a "Create a ticket" action rather than an empty
table. `[TL-AC5]`

### 3.2 The ticket detail

A detail request for a ticket the customer owns responds `200` with the messages in
chronological order, each carrying its author and time. `[TL-AC2]`

The current status and the assigned agent's display name are included. `[TL-AC2]`

Every attachment the customer uploaded is retrievable through a short-lived link.
`[TL-AC2]`

The unread flag for that ticket is cleared. `[TL-AC2]`

### 3.3 Isolation

Where the ticket belongs to a different customer, the response is `404` with `type`
`.../errors/ticket-not-found`. `[TL-AC3]`

The response body is identical to the one returned for an identifier that never existed,
and no field of the ticket appears anywhere in the response. `[TL-AC3]`

### 3.4 Internal notes

Where the ticket carries internal notes written by agents, no note text appears in the
customer's detail response. `[TL-AC4]`

No gap in message numbering or count hints that hidden messages exist. `[TL-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/support/tickets` | Path and method named by the criteria | `[TL-AC1]` |
| 2 | `GET /api/v1/support/tickets/{id}` | Path and method named by the criteria | `[TL-AC2]` |
| 3 | `state` (query) | Value `OPEN` named; the rest of the set is not specified | `[TL-AC1]` |
| 4 | list entry | reference, subject, status, creation time, last update time, unread flag | `[TL-AC1]` |
| 5 | unread flag | Per ticket, per customer; cleared on opening the detail | `[TL-AC1]` `[TL-AC2]` |
| 6 | `{id}` path segment | TL-AC2 and TL-AC3 quote references of the form `#10425`; whether the path takes the reference or an internal identifier is not specified | `[TL-AC2]` `[TL-AC3]` |
| 7 | message | Author and time; ordering is chronological. Body format not specified | `[TL-AC2]` |
| 8 | message numbering | Must show no gaps to the customer | `[TL-AC4]` |
| 9 | assigned agent | Display name; behaviour when unassigned is not specified — see OQ-2 | `[TL-AC2]` |
| 10 | attachment link | "short-lived"; **no lifetime is stated** | `[TL-AC2]` |
| 11 | paging | TL-AC5 names "an empty page"; page size and parameters are not specified | `[TL-AC1]` `[TL-AC5]` |
| 12 | `ProblemDetail.type` | Slug `ticket-not-found` | `[TL-AC3]` |
| 13 | Message strings | "Create a ticket" action label | `[TL-AC5]` |

## 5. Out of Scope

- The agent queue and other customers' tickets — US-4.3.
- Posting replies — US-4.4.
- Status transitions — US-4.6 and US-4.7.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Ownership is enforced by a predicate in the repository query rather than a check after fetching. | Named in the story's Non-Functional section. TL-AC3 is satisfiable either way, but only the former survives the next endpoint being added. |
| A-2 | "Closed and resolved" in TL-AC1 map to the `CLOSED` and `RESOLVED` statuses of the lifecycle owned by US-4.6. | TL-AC1 uses lowercase prose; no criterion here enumerates statuses. |
| A-3 | Attachments uploaded by agents, if any, are also visible to the customer. | TL-AC2 says "every attachment the customer uploaded", which is silent on the agent's. US-4.5 permits agents to attach files. |
| A-4 | The short-lived link is bound to the requesting account, matching US-4.1 A-2. | TL-AC2 constrains only its lifetime, and only qualitatively. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TL-AC2 requires attachments to be retrievable "through a short-lived link" without stating a lifetime, and without saying whether the link is bound to the requester. A link that is merely short-lived but bearer-usable is a different security property from one that is account-bound. | TL-AC2 |
| OQ-2 | TL-AC2 includes "the assigned agent's display name". What is returned before a ticket is claimed (US-4.3 leaves `NEW` tickets unassigned), and is exposing staff names to every customer intended? | TL-AC2 |
| OQ-3 | TL-AC1 lists tickets "most recently updated first" but no criterion says what counts as an update. Does an internal note (US-4.5 TA-AC2), which the customer cannot see, move their ticket to the top of their list? That would leak the existence of hidden activity, which TL-AC4 exists to prevent. | TL-AC1, TL-AC4 |
| OQ-4 | Neither TL-AC1 nor TL-AC5 states a page size or paging parameters, though both speak of "a page". | TL-AC1, TL-AC5 |
| OQ-5 | TL-AC3 quotes ticket references such as `#10999` in a path position. Is the API addressed by the human-readable reference or by an internal identifier? Referencing by a guessable sequence makes TL-AC3's isolation the only defence. | TL-AC3 |
| OQ-6 | How long do closed tickets remain in the list? This interacts with the 14-day reopen window in US-4.7. | TL-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TL-AC1 | Own tickets, newest activity first, with an unread flag | §3.1, §4 | Covered — the criterion now defines "updated" as the last customer-visible event |
| TL-AC2 | Detail shows the thread, status, agent and attachments | §3.2, §4 | **Partial** — the attachment link's lifetime and binding are unstated (OQ-1) |
| TL-AC3 | A foreign ticket is indistinguishable from a missing one | §3.3, §4 | Covered — see OQ-5 |
| TL-AC4 | Internal notes leave no trace in the customer's view | §3.4, §4 | Covered — but see OQ-3, which is the same leak by another route |
| TL-AC5 | An empty list explains itself | §3.1, §4 | Covered — see OQ-4 |

**Coverage:** 4 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.2-my-tickets.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
