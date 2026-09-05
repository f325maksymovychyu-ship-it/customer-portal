---
story_id: US-3.2
title: "Epic 3 — Administration: Create User"
source: docs/backlog/US-3.2-create-user.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Create User

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to create an account and invite the person to set their own
> password, So that a new colleague can work on their first day and I never handle their
> credentials.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**UC-AC1 — Account created and invitation sent**
```gherkin
Given an administrator holding the customers:create scope
When POST /api/v1/admin/customers is called with an email, both names and at least one role
Then respond 201 with a Location header pointing at the new resource
And the account is created with status PENDING_INVITATION
And an invitation email carrying a link valid for 72 hours is queued
And an audit entry with event CUSTOMER_CREATED records the acting administrator
But no password is set, and none is accepted in the request body
```

**UC-AC2 — Email already registered**
```gherkin
Given an account already exists with the address "olena@example.com"
When POST /api/v1/admin/customers is called with that address
Then respond 409 with type ".../errors/email-already-registered"
And no second account is created
And the same holds when the existing account is DEACTIVATED
And matching is case-insensitive, so "Olena@Example.com" collides too
```

**UC-AC3 — Missing or invalid fields**
```gherkin
Given a request missing a required field, or carrying a malformed email
When POST /api/v1/admin/customers is called
Then respond 400 with type ".../errors/validation-failed"
And the ProblemDetail names every offending field, not only the first
When roleIds is empty
Then respond 400 with detail "Assign at least one role"
```

**UC-AC4 — Granting a role above the caller's own ceiling**
```gherkin
Given an administrator who does not hold the roles:grant:admin scope
When POST /api/v1/admin/customers is called with a role carrying administrative permissions
Then respond 403 with type ".../errors/privilege-escalation"
And no account is created — the request fails whole, never partially
And the attempt is recorded in the audit log as a security event
And that role is not offered in the client's role picker either
```

**UC-AC5 — Duplicate submission**
```gherkin
Given the create request is submitted twice with the same Idempotency-Key
When both requests reach the server
Then exactly one account is created
And the second response repeats the first, including the Location header
And the client disables the submit control while a request is in flight
```

**UC-AC6 — Invitation delivery failure**
```gherkin
Given the account was created successfully
And the mail provider rejected the invitation
Then the account remains in PENDING_INVITATION
And the administrator sees "The invitation could not be sent" with a resend action
And POST /api/v1/admin/customers/{id}/invitation issues a fresh token and invalidates the previous one
```

## 3. Functional Specification

### 3.1 Creating the account

A create request from a caller holding the `customers:create` scope, carrying an email,
both names and at least one role, responds `201` with a `Location` header pointing at the
new resource. `[UC-AC1]`

The account is created with status `PENDING_INVITATION`. `[UC-AC1]`

No password is set, and none is accepted in the request body. `[UC-AC1]`

An audit entry with event `CUSTOMER_CREATED` records the acting administrator.
`[UC-AC1]`

### 3.2 Invitation

An invitation email carrying a link valid for 72 hours is queued. `[UC-AC1]`

Where the mail provider rejects the invitation, the account remains in
`PENDING_INVITATION` and the administrator sees "The invitation could not be sent"
together with a resend action. `[UC-AC6]`

Re-issuing the invitation creates a fresh token and invalidates the previous one.
`[UC-AC6]`

### 3.3 Address uniqueness

Where an account already exists with the supplied address, the response is `409` with
`type` `.../errors/email-already-registered`, and no second account is created.
`[UC-AC2]`

The same holds where the existing account is `DEACTIVATED`. `[UC-AC2]`

Matching is case-insensitive. `[UC-AC2]`

### 3.4 Request validation

Where a required field is missing or the email is malformed, the response is `400` with
`type` `.../errors/validation-failed`, and the `ProblemDetail` names every offending
field rather than only the first. `[UC-AC3]`

Where the role list is empty, the response is `400` with the detail "Assign at least one
role". `[UC-AC3]`

### 3.5 Privilege ceiling

Where the caller does not hold the `roles:grant:admin` scope and the request names a role
carrying administrative permissions, the response is `403` with `type`
`.../errors/privilege-escalation`. `[UC-AC4]`

No account is created; the request fails whole rather than partially. `[UC-AC4]`

The attempt is recorded in the audit log as a security event. `[UC-AC4]`

Such a role is not offered in the client's role picker. `[UC-AC4]`

### 3.6 Duplicate submission

Where the same request is submitted twice carrying the same `Idempotency-Key`, exactly one
account is created and the second response repeats the first, including the `Location`
header. `[UC-AC5]`

The client disables the submit control while a request is in flight. `[UC-AC5]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/admin/customers` | Path and method named by the criteria | `[UC-AC1]` |
| 2 | `POST /api/v1/admin/customers/{id}/invitation` | Path and method named by the criteria; response status not specified | `[UC-AC6]` |
| 3 | `customers:create` scope | Required for creation | `[UC-AC1]` |
| 4 | `roles:grant:admin` scope | Gates roles carrying administrative permissions | `[UC-AC4]` |
| 5 | `email` (request) | Required, validated, unique case-insensitively; no length bound stated | `[UC-AC1]` `[UC-AC2]` `[UC-AC3]` |
| 6 | given and family name (request) | Both required; formats not specified | `[UC-AC1]` `[UC-AC3]` |
| 7 | `roleIds` (request) | At least one entry; element format not specified | `[UC-AC1]` `[UC-AC3]` |
| 8 | password | Explicitly absent from the contract; a supplied value is rejected | `[UC-AC1]` |
| 9 | `Idempotency-Key` (header) | Replays return the first response; format and retention not specified | `[UC-AC5]` |
| 10 | `Location` (response header) | Points at the new resource | `[UC-AC1]` `[UC-AC5]` |
| 11 | `customers.status` | Value `PENDING_INVITATION` | `[UC-AC1]` `[UC-AC6]` |
| 12 | invitation token | Valid 72 hours; single use implied by UC-AC6's invalidation | `[UC-AC1]` `[UC-AC6]` |
| 13 | audit event | Value `CUSTOMER_CREATED`; the security-event category of UC-AC4 is not named | `[UC-AC1]` `[UC-AC4]` |
| 14 | `ProblemDetail.type` | Slugs `email-already-registered`, `validation-failed`, `privilege-escalation` | `[UC-AC2]` `[UC-AC3]` `[UC-AC4]` |

## 5. Out of Scope

- Accepting the invitation and setting the password — US-2.6 owns that endpoint.
- Editing the account afterwards — US-3.3.
- Defining what roles exist — US-3.6.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Account creation and invitation queueing share one transaction. | Named in the story's Non-Functional section. UC-AC6 shows the two can diverge, so the boundary matters. |
| A-2 | The invitation link targets the US-2.6 confirm endpoint. | UC-AC1 says "a link" without a destination; without this the invitation leads nowhere. See OQ-2. |
| A-3 | The invitation token follows the construction rules of US-2.5 — random, stored hashed, single use. | No criterion here constrains it. |
| A-4 | "A role carrying administrative permissions" in UC-AC4 means one whose permission set is not a subset of the caller's. | UC-AC4 names a scope and a role category but defines neither. See OQ-1. |
| A-5 | The `Idempotency-Key` is supplied by the client. | UC-AC5 requires the same key on both submissions without saying who generates it. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | UC-AC4 gates "a role carrying administrative permissions" behind the `roles:grant:admin` scope, but no criterion defines which roles those are, or how the ceiling is computed when a role carries a mix of permissions. Is the check per-role or per-permission? | UC-AC4 |
| OQ-2 | The invitation token lives for 72 hours, while US-2.6 PN-AC2 hard-codes 30 minutes as the expiry test on the endpoint that consumes it. Does the confirm endpoint accept both lifetimes, and how does it distinguish them? US-2.6 OQ-4 records the same conflict from the other side. | UC-AC1 |
| OQ-3 | UC-AC5 requires idempotent replay but states no retention for the key. For how long is a replayed key honoured, and what happens when the same key arrives with a different body? | UC-AC5 |
| OQ-4 | What becomes of a `PENDING_INVITATION` account whose token expires unclaimed? No criterion covers cleanup, and US-3.1 OQ-3 asks whether such accounts appear in the directory at all. | UC-AC1 |
| OQ-5 | UC-AC4 requires the attempt to be recorded "as a security event". No criterion defines that category, which stream it lands in, or how it differs from a normal audit entry. US-2.4 OQ-3 records the same gap for `SECURITY` severity. | UC-AC4 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| UC-AC1 | Creation yields 201, PENDING_INVITATION and a 72-hour invitation | §3.1, §3.2, §4 | **Partial** — the invitation lifetime conflicts with the endpoint that consumes it (OQ-2) |
| UC-AC2 | A duplicate address yields 409, case-insensitively | §3.3, §4 | Covered |
| UC-AC3 | Missing or malformed fields yield 400 naming all of them | §3.4, §4 | Covered |
| UC-AC4 | Granting above the caller's ceiling fails whole | §3.5, §4 | **Partial** — "administrative permissions" is undefined (OQ-1, OQ-5) |
| UC-AC5 | A replayed Idempotency-Key creates one account | §3.6, §4 | Covered — see OQ-3 |
| UC-AC6 | A mail failure leaves the account and offers a resend | §3.2, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.2-create-user.md`. |
