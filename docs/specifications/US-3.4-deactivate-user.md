---
story_id: US-3.4
title: "Epic 3 — Administration: Deactivate and Reactivate User"
source: docs/backlog/US-3.4-deactivate-user.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Deactivate and Reactivate User

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to deactivate an account rather than delete it,
> So that a departing person loses access immediately while their history stays intact for audit.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**UD-AC1 — Deactivation with a reason**
```gherkin
Given an administrator holding the customers:deactivate scope
When POST /api/v1/admin/customers/{id}/deactivate is called with a reason
Then the account status becomes DEACTIVATED
And every refresh-token family for that customer is revoked within 60 seconds
And the customer's data and history are retained in full
And an audit_events entry records the reason and the acting administrator
And subsequent sign-in attempts are refused per US-2.1 LI-AC5
```

**UD-AC2 — Reactivation**
```gherkin
Given an account whose status is DEACTIVATED
When POST /api/v1/admin/customers/{id}/activate is called
Then the status returns to ACTIVE with the previously assigned roles intact
And the customer is emailed that access has been restored
And they can sign in with their existing password
```

**UD-AC3 — Deactivating yourself**
```gherkin
Given an administrator is viewing their own record
Then the deactivate control is disabled with the explanation "You cannot deactivate your own account"
When the request is sent directly to the API
Then respond 422 with type ".../errors/self-deactivation"
And the status is unchanged
```

**UD-AC4 — The last active administrator**
```gherkin
Given exactly one active account holds the administrator role
When any caller attempts to deactivate it
Then respond 422 with type ".../errors/last-administrator"
And the detail reads "At least one active administrator must remain"
And the same invariant is enforced by US-3.5 when the last administrative role would be removed
```

**UD-AC5 — Missing reason, or an already-deactivated target**
```gherkin
Given a deactivation request with a blank reason
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
Given the account is already DEACTIVATED
When deactivation is requested again
Then respond 200 without changing anything, because the operation is idempotent
```

**UD-AC6 — Anonymisation on a data-subject request**
```gherkin
Given a verified erasure request and a caller holding the customers:erase scope
When POST /api/v1/admin/customers/{id}/erase is called with the request reference
Then personal fields are overwritten with a tombstone marker
And the account is deactivated if it was not already
But audit_events entries are retained and continue to reference the anonymised identifier
And the erasure itself is recorded in the audit log with its request reference
```

## 3. Functional Specification

### 3.1 Deactivation

A deactivation request from a caller holding the `customers:deactivate` scope, carrying a
reason, moves the account's status to `DEACTIVATED`. `[UD-AC1]`

All of the account's refresh-token families are revoked within 60 seconds. `[UD-AC1]`

The customer's data and history are retained in full. `[UD-AC1]`

An `audit_events` entry records the reason and the acting administrator. `[UD-AC1]`

Subsequent sign-in attempts are refused as described by US-2.1 LI-AC5. `[UD-AC1]`

Where the account is already `DEACTIVATED`, the request responds `200` and changes
nothing. `[UD-AC5]`

### 3.2 Reactivation

Where a deactivated account is activated, its status returns to `ACTIVE` with the
previously assigned roles intact. `[UD-AC2]`

The customer is emailed that access has been restored, and can sign in with their existing
password. `[UD-AC2]`

### 3.3 Invariants

Where an administrator targets their own record, the response is `422` with `type`
`.../errors/self-deactivation`, and the status is unchanged. The client disables the
control with the explanation "You cannot deactivate your own account". `[UD-AC3]`

Where exactly one active account holds the administrator role, any attempt to deactivate
it responds `422` with `type` `.../errors/last-administrator` and the detail "At least one
active administrator must remain". `[UD-AC4]`

The same invariant is enforced by US-3.5 when the last administrative role would be
removed. `[UD-AC4]`

### 3.4 Request validation

Where the reason is blank, the response is `400` with `type`
`.../errors/validation-failed`. `[UD-AC5]`

### 3.5 Erasure

Where a verified erasure request is presented by a caller holding the `customers:erase`
scope, the account's personal fields are overwritten with a tombstone marker, and the
account is deactivated if it was not already. `[UD-AC6]`

`audit_events` entries are retained and continue to reference the anonymised
identifier. `[UD-AC6]`

The erasure is itself recorded in the audit log together with its request reference.
`[UD-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/admin/customers/{id}/deactivate` | Path and method named by the criteria | `[UD-AC1]` |
| 2 | `POST /api/v1/admin/customers/{id}/activate` | Path and method named by the criteria; status not specified | `[UD-AC2]` |
| 3 | `POST /api/v1/admin/customers/{id}/erase` | Path and method named by the criteria; status not specified | `[UD-AC6]` |
| 4 | `customers:deactivate`, `customers:erase` scopes | Gate the respective operations | `[UD-AC1]` `[UD-AC6]` |
| 5 | `reason` (request) | Required, non-blank; no length bound or vocabulary stated | `[UD-AC1]` `[UD-AC5]` |
| 6 | request reference (erasure) | Required; format and verification not specified | `[UD-AC6]` |
| 7 | `customers.status` | Values `ACTIVE`, `DEACTIVATED` | `[UD-AC1]` `[UD-AC2]` |
| 8 | revocation window | 60 seconds from deactivation | `[UD-AC1]` |
| 9 | tombstone marker | **not specified** — no criterion states its value or which fields it covers | `[UD-AC6]` |
| 10 | `audit_events` entry | Carries the reason and the actor; retained after erasure | `[UD-AC1]` `[UD-AC6]` |
| 11 | `ProblemDetail.type` | Slugs `self-deactivation`, `last-administrator`, `validation-failed` | `[UD-AC3]` `[UD-AC4]` `[UD-AC5]` |
| 12 | Message strings | Two exact strings, quoted in UD-AC3 and UD-AC4 | `[UD-AC3]` `[UD-AC4]` |
| 13 | Reactivation email | Content not specified | `[UD-AC2]` |

## 5. Out of Scope

- Profile edits — US-3.3; role changes — US-3.5.
- A customer ending their own sessions — US-2.3.
- Retention or eventual deletion of anonymised records — see OQ-3.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Deactivation is a state transition; no row is ever physically deleted, per `AGENTS.md` §3.2. | UD-AC1 requires history retained but does not forbid deletion in terms a test could check. |
| A-2 | The 60-second revocation window in UD-AC1 bounds the write, while access itself may persist up to the 15-minute access-token TTL of US-2.4 RT-AC7. | The two windows differ; without this reading UD-AC1 and RT-AC7 appear to contradict each other. |
| A-3 | UD-AC4 is evaluated inside the deactivating transaction, not as a pre-check. | Named in the story's Non-Functional section. UD-AC4 says "any caller attempts", which under a pre-check allows two concurrent deactivations to both pass. |
| A-4 | The deactivation reason is visible only to callers holding `customers:read` and is never shown to the customer. | Named in the story's Non-Functional section; no criterion constrains who may read it. |
| A-5 | "Verified erasure request" in UD-AC6 means verification performed outside this system. | No criterion describes any verification step inside it. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | UD-AC6 overwrites "personal fields" with "a tombstone marker" but names neither the field set nor the marker. Does it cover the deactivation reason from UD-AC1, which is free text an administrator wrote about the person? | UD-AC6 |
| OQ-2 | UD-AC5 makes repeated deactivation idempotent with `200`, but UD-AC1 states no status code for the first, successful call. Are they the same? | UD-AC1, UD-AC5 |
| OQ-3 | How long are anonymised rows retained, and does any regulation require their eventual deletion? UD-AC6 keeps audit entries indefinitely by implication, which may itself conflict with an erasure obligation. | UD-AC6 |
| OQ-4 | UD-AC2 restores "the previously assigned roles". If a role was deleted or changed while the account was inactive (US-3.6 MR-AC5 forbids deleting an assigned role, but a deactivated holder may not count), what is restored? | UD-AC2 |
| OQ-5 | Is the customer notified on deactivation? UD-AC2 emails on reactivation; nothing states the intent for the other direction. | — |
| OQ-6 | UD-AC4 protects "the administrator role". US-3.6 leaves open which roles are system-defined. Does this invariant track a specific role, or any role carrying a particular permission? | UD-AC4 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| UD-AC1 | Deactivation with a reason revokes sessions and retains data | §3.1, §4 | **Partial** — no status code is stated for the successful call (OQ-2) |
| UD-AC2 | Reactivation restores status, roles and access | §3.2, §4 | Covered — see OQ-4 |
| UD-AC3 | Self-deactivation is refused with 422 | §3.3, §4 | Covered |
| UD-AC4 | The last active administrator cannot be deactivated | §3.3, §4 | Covered — see OQ-6 |
| UD-AC5 | Blank reason yields 400; repeat deactivation is idempotent | §3.1, §3.4, §4 | Covered |
| UD-AC6 | Erasure anonymises personal fields and keeps the audit trail | §3.5, §4 | **Partial** — neither the field set nor the marker the criterion asserts is defined (OQ-1) |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.4-deactivate-user.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
