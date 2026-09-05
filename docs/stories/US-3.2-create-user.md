# Epic 3 — Administration: Create User

**Story ID:** US-3.2
**Project:** Customer Portal
**AC prefix:** `UC-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to create an account and invite the person to set their own password,
So that a new colleague can work on their first day and I never handle their credentials.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Password creation | The administrator never sets one; the invitee does | An administrator who knows a password can impersonate its owner, and the audit trail cannot tell the difference |
| 2 | Invitation lifetime | 72 hours | Long enough to survive a weekend, short enough to bound an unclaimed account |
| 3 | Initial status | `PENDING_INVITATION` | Distinguishes "never signed in" from "active" for reporting and for US-2.1 gating |
| 4 | Role requirement | At least one role at creation | An account with no role can sign in and do nothing, which reads as a broken system |
| 5 | Idempotency | `Idempotency-Key` header on the create call | `AGENTS.md` §3.1 requires retries to evaluate idempotent constraints |

## In Scope
- `POST /api/v1/admin/customers` — create the account and queue the invitation
- Duplicate-address rejection, including against deactivated accounts
- Privilege-ceiling enforcement on the roles being granted
- Resending an invitation when delivery failed

## Out of Scope
- Accepting the invitation and setting the password (US-2.6 owns that endpoint)
- Editing the account afterwards (US-3.3)
- Defining what roles exist (US-3.6)

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/admin/customers` | Bearer + `customers:create` | `{"email", "givenName", "familyName", "roleIds": [uuid]}` | `201` + `Location: /api/v1/admin/customers/{id}` |
| POST | `/api/v1/admin/customers/{id}/invitation` | Bearer + `customers:create` | *(empty)* | `202` |

## Data Model Notes
- `customers.status` starts at `PENDING_INVITATION`
- The invitation token lives in `password_reset_tokens` with a 72-hour `expiresAt`, reusing the US-2.6 confirm endpoint — see Open Questions
- `customers.email` carries a case-insensitive unique constraint, so UC-AC2 is enforced by the database and not only by a pre-check
- Creation and invitation queueing share one `@Transactional` boundary in the application service

## Acceptance Criteria

### Happy path
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

### Conflict and validation
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

### Authorisation
**UC-AC4 — Granting a role above the caller's own ceiling**
```gherkin
Given an administrator who does not hold the roles:grant:admin scope
When POST /api/v1/admin/customers is called with a role carrying administrative permissions
Then respond 403 with type ".../errors/privilege-escalation"
And no account is created — the request fails whole, never partially
And the attempt is recorded in the audit log as a security event
And that role is not offered in the client's role picker either
```

### Retries and delivery
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/email-already-registered",
  "title": "Email Already Registered",
  "status": 409,
  "detail": "An account with this email address already exists.",
  "instance": "/api/v1/admin/customers"
}
```
Error `type` slugs introduced by this story: `email-already-registered`, `privilege-escalation`.

## Non-Functional / Security Requirements
- Scope checks run server-side on every request. Hiding a role from the picker is a usability measure and carries no security weight (`AGENTS.md` §3.4).
- UC-AC4 must fail atomically. A partially created account holding fewer roles than requested is worse than a clean rejection, because it looks successful.
- The invitation token follows the same rules as the reset token in US-2.5: 256-bit random, stored as a SHA-256 hash, single use.
- Creating an account is an administrative action over personal data and is always audited, including the roles granted.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| UC-AC1 | Functional suite asserting `201`, `Location`, status and queued mail | `[gate]` |
| UC-AC2 | Integration test covering active, deactivated and differently-cased duplicates | `[gate]` |
| UC-AC3 | Slice test on the request record's validation constraints | `[gate]` |
| UC-AC4 | Functional test asserting `403` and that no row was written | `[gate]` |
| UC-AC5 | Integration test replaying one `Idempotency-Key` | `[gate]` |
| UC-AC6 | Integration test with the mail port stubbed to fail | `[gate]` |
| No password accepted | Contract test asserting an unknown `password` field is rejected | `[gate]` |

## Open Questions
1. Should the invitation reuse `POST /api/v1/auth/password-reset/confirm` (US-2.6), or have its own endpoint? Sharing it means one token table serving a 30-minute and a 72-hour lifetime, which is workable but easy to misconfigure.
2. What happens to a `PENDING_INVITATION` account whose token expires unclaimed — automatic cleanup after some period, or does it sit in the directory forever? No policy exists.
3. Scope names `customers:create` and `roles:grant:admin` are provisional until US-3.6 fixes the permission vocabulary.
