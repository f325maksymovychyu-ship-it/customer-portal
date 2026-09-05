---
story_id: US-4.3
title: "Epic 4 — Feedback / Support: Queue and Assignment"
source: docs/backlog/US-4.3-ticket-queue-assignment.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Queue and Assignment

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a support agent, I want to see the unassigned queue and claim tickets from it,
> So that the most urgent problems are worked first and nothing sits without an owner.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**TQ-AC3 — Two agents claim at once**
```gherkin
Given another agent claimed ticket "#10425" moments earlier
When this agent's claim request arrives with the now-stale If-Match value
Then respond 409 with type ".../errors/ticket-already-assigned"
And the detail names the agent who holds it
And the client refreshes the queue so the row disappears
And the existing assignment is unchanged
```

**TQ-AC4 — Caller without agent scope**
```gherkin
Given a customer without the tickets:read:any scope
When GET /api/v1/support/queue is called directly
Then respond 403 with type ".../errors/insufficient-scope"
And no ticket belonging to any other customer appears in any response to that caller
```

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

## 3. Functional Specification

### 3.1 The queue

A queue request from an agent holding the `tickets:read:any` scope, with `assigned=NONE`,
responds `200` with unassigned tickets ordered by priority and then by nearest SLA
deadline. `[TQ-AC1]`

Each entry carries its SLA state, so that breached and at-risk tickets are
distinguishable. `[TQ-AC1]`

Filters on category and priority narrow the result. `[TQ-AC1]`

### 3.2 Claiming

Where a ticket has no assignee, a claim makes the calling agent the assignee and moves the
status from `NEW` to `IN_PROGRESS`. `[TQ-AC2]`

The customer is notified that their ticket is being worked on. `[TQ-AC2]`

An audit entry records the claim. `[TQ-AC2]`

Where another agent claimed the ticket moments earlier and the request carries a stale
`If-Match` value, the response is `409` with `type`
`.../errors/ticket-already-assigned`, and the detail names the agent who holds it. The
existing assignment is unchanged, and the client refreshes the queue so the row
disappears. `[TQ-AC3]`

### 3.3 Reassignment

Where the ticket is assigned to the calling agent, an assignment naming another agent and
giving a reason records and notifies the new assignee. `[TQ-AC5]`

The reason is stored as an internal note, invisible to the customer, who sees only that
the agent's name changed. `[TQ-AC5]`

The SLA timer continues rather than restarting. `[TQ-AC5]`

Where the named assignee is deactivated or holds no agent role, the response is `422` with
`type` `.../errors/ineligible-assignee`, and the current assignment is unchanged.
`[TQ-AC6]`

### 3.4 Authorisation

Where a caller does not hold the `tickets:read:any` scope, a queue request responds `403`
with `type` `.../errors/insufficient-scope`. `[TQ-AC4]`

No ticket belonging to any other customer appears in any response to that caller.
`[TQ-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/support/queue` | Path and method named by the criteria | `[TQ-AC1]` |
| 2 | `POST /api/v1/support/tickets/{id}/claim` | Path and method named by the criteria; success status not specified | `[TQ-AC2]` `[TQ-AC3]` |
| 3 | `POST /api/v1/support/tickets/{id}/assign` | Path and method named by the criteria; success status not specified | `[TQ-AC5]` `[TQ-AC6]` |
| 4 | `tickets:read:any` scope | Gates the queue | `[TQ-AC1]` `[TQ-AC4]` |
| 5 | `assigned` (query) | Value `NONE` named; the rest of the set is not specified | `[TQ-AC1]` |
| 6 | `category`, `priority` (query) | Narrow the queue; value sets not specified — see US-4.1 OQ-3 | `[TQ-AC1]` |
| 7 | SLA state | Distinguishes at least "breached" and "at risk"; **the full value set and the thresholds are not specified** | `[TQ-AC1]` |
| 8 | SLA deadline | Derived from the priority thresholds in `docs/backlog/README.md` | `[TQ-AC1]` `[TQ-AC5]` |
| 9 | `If-Match` (request header) | Required on the claim; carries the ticket's current version | `[TQ-AC3]` |
| 10 | assignee | An agent account; must be active and hold an agent role | `[TQ-AC2]` `[TQ-AC5]` `[TQ-AC6]` |
| 11 | reassignment reason | Required; stored as an internal note per US-4.5 | `[TQ-AC5]` |
| 12 | ticket `status` | `NEW` → `IN_PROGRESS` on claim | `[TQ-AC2]` |
| 13 | `ProblemDetail.type` | Slugs `ticket-already-assigned`, `insufficient-scope`, `ineligible-assignee` | `[TQ-AC3]` `[TQ-AC4]` `[TQ-AC6]` |

## 5. Out of Scope

- Replying — US-4.5; resolution — US-4.6.
- Defining SLA thresholds — consumed here, set by no story.
- Agent scheduling, shifts or capacity planning — no criterion reaches them.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Agents pull from the queue; nothing is assigned automatically. | Named in the story's Assumptions table. No criterion describes an automatic route. |
| A-2 | The `409` in TQ-AC3 is decided by the optimistic-locking version, not by a read-then-write check. | Named in the story's Non-Functional section. TQ-AC3 names `If-Match`, which implies but does not state the mechanism. |
| A-3 | The internal note created by TQ-AC5 inherits the visibility guarantees of US-4.5 TA-AC3. | TQ-AC5 says "invisible to the customer" without stating how. |
| A-4 | The customer notification in TQ-AC2 is subject to US-5.4 preferences. | No criterion here says whether it can be switched off. |
| A-5 | An agent may claim only an unassigned ticket; taking one from a colleague requires the assign endpoint. | TQ-AC2 conditions on "no assignee" and TQ-AC3 rejects the conflict; neither describes claiming an assigned ticket. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TQ-AC1 orders the queue by "nearest SLA deadline" and requires an SLA state, but no criterion in any story defines a threshold for any priority. The queue's primary ordering rule cannot be implemented, and US-4.1 OQ-1 records the same gap from the creation side. | TQ-AC1 |
| OQ-2 | TQ-AC1 distinguishes "breached and at-risk" tickets. What is the full state set, and at what fraction of the deadline does a ticket become at risk? | TQ-AC1 |
| OQ-3 | TQ-AC5 lets the assignee hand the ticket on. May a supervisor reassign a ticket they do not hold, and does any scope permit it? No criterion covers a third party. | TQ-AC5 |
| OQ-4 | What happens to a ticket already `IN_PROGRESS` when its assignee is deactivated under US-3.4? TQ-AC6 blocks assigning *to* such an account, and US-4.7 TO-AC3 handles the reopen case, but nothing handles a live ticket. | TQ-AC6 |
| OQ-5 | TQ-AC2 requires "an audit entry", without naming the stream or the event. US-3.7 OQ-1 records the same ambiguity across the backlog. | TQ-AC2 |
| OQ-6 | TQ-AC3 requires the detail to name "the agent who holds it". That discloses one staff member's identity to another, which is likely fine, but the same field is not constrained for the customer-facing path in US-4.2 TL-AC2 OQ-2. | TQ-AC3 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TQ-AC1 | The queue is ordered by priority then SLA deadline | §3.1, §4 | **Partial** — the SLA thresholds and state set the criterion asserts do not exist (OQ-1, OQ-2) |
| TQ-AC2 | Claiming assigns the agent and moves NEW to IN_PROGRESS | §3.2, §4 | Covered — see OQ-5 |
| TQ-AC3 | A stale claim yields 409 and leaves the assignment alone | §3.2, §4 | Covered |
| TQ-AC4 | Without the scope, no other customer's ticket is ever visible | §3.4, §4 | Covered |
| TQ-AC5 | Reassignment notifies, notes the reason, and keeps the timer | §3.3, §4 | **Partial** — depends on the same undefined SLA timer (OQ-1) |
| TQ-AC6 | An ineligible assignee yields 422 | §3.3, §4 | Covered — see OQ-4 |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.3-ticket-queue-assignment.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
