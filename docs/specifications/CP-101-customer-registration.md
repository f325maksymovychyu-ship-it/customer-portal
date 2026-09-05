---
story_id: CP-101
title: Customer self-service registration
source: drafted in conversation from AGENTS.md — NOT stakeholder-approved
status: draft
revision: 1
last_updated: 2026-08-17
---

# Customer self-service registration

> **⚠ Provenance warning.** Neither this story nor its acceptance criteria came from a
> product owner, ticket, or customer. They were drafted by an assistant at the user's
> request, inferred from the `customer-portal` technical contract in `AGENTS.md`.
> Until a stakeholder reviews and signs off on section 2, **every row in this document
> is a proposal, not an agreed requirement.** Do not estimate or build from it as-is.

## 1. Story

> As a new customer, I want to register a Customer Portal account with my email and
> password, so that I can sign in and manage my own profile.

## 2. Acceptance Criteria

Drafted, not sourced (see provenance warning). Once approved, these are the only
requirements in this document.

| ID | Acceptance Criterion |
|----|----------------------|
| AC-1 | Given a valid email, password, first name and last name, when I submit the registration request, then my account is created and the response is HTTP 201 Created with a `Location` header pointing to my new customer resource. |
| AC-2 | Given an email that is already registered, when I submit the registration request, then registration is rejected with HTTP 409 Conflict and an RFC 9457 `ProblemDetail` body, and no second account is created. |
| AC-3 | Given a password shorter than 12 characters or longer than 128 characters, when I submit the registration request, then registration is rejected with HTTP 400 Bad Request and the response lists the password rules that were violated. |
| AC-4 | Email addresses are treated case-insensitively: submitting `User@Example.com` when `user@example.com` already exists is treated as an already-registered email. |
| AC-5 | The stored customer record must never contain the password in plaintext. |
| AC-6 | A newly registered customer is assigned the `USER` role. |

## 3. Functional Specification

### 3.1 Submitting a registration

The registration request carries four values: email, password, first name and last
name. `[AC-1]`

When all four are present and the email is not already registered, the system creates
a customer account and responds `201 Created`. `[AC-1]`

The `201 Created` response carries a `Location` header whose value is the URI of the
newly created customer resource. `[AC-1]`

Registration is a single request; the account exists once the `201` is returned. `[AC-1]`

### 3.2 Email uniqueness

An email may identify at most one customer account. A request whose email already
identifies an account is rejected with `409 Conflict`. `[AC-2]`

The `409` response body is an RFC 9457 `ProblemDetail`. `[AC-2]`

A rejected duplicate registration leaves the existing account untouched and creates no
additional account. `[AC-2]`

The uniqueness comparison is case-insensitive on the whole address: `User@Example.com`
and `user@example.com` are the same email for the purposes of AC-2. `[AC-4]`

### 3.3 Password handling

A password shorter than 12 characters is rejected with `400 Bad Request`. `[AC-3]`

A password longer than 128 characters is rejected with `400 Bad Request`. `[AC-3]`

A `400` rejection response enumerates the password rules the submitted password
violated. `[AC-3]`

No representation of the customer record persists the password as plaintext. `[AC-5]`

### 3.4 Role assignment

An account created by this flow holds the role `USER`. `[AC-6]`

## 4. Data and Interfaces

| # | Field | Format | Source |
|---|-------|--------|--------|
| 1 | email | valid email address; compared case-insensitively; maximum length not specified | `[AC-1]` `[AC-4]` |
| 2 | password | minimum 12 characters, maximum 128 characters | `[AC-3]` |
| 3 | firstName | not specified | `[AC-1]` |
| 4 | lastName | not specified | `[AC-1]` |
| 5 | `Location` (response header) | URI of the created customer resource | `[AC-1]` |
| 6 | role | value `USER`; representation not specified | `[AC-6]` |
| 7 | error body | RFC 9457 `ProblemDetail`; member values not specified | `[AC-2]` |
| 8 | stored password | not plaintext; storage form not specified | `[AC-5]` |

## 5. Out of Scope

- **Email verification / account activation** — no AC requires the address to be
  confirmed before the account is usable.
- **Login and token issuance** — the story ends at account creation; authentication,
  access-token lifetime and refresh rotation are not addressed by any AC.
- **Profile viewing and editing** — the benefit clause mentions managing a profile, but
  no AC specifies any profile operation.
- **Rate limiting, CAPTCHA, or abuse protection on the endpoint** — not addressed by
  any AC.
- **Account deletion, data export, or GDPR retention** — not addressed by any AC.
- **Social / SSO registration** — every AC describes email-and-password registration only.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The repository standards in `AGENTS.md` govern the implementation shape — RFC 9457 error bodies, record DTOs, surrogate UUIDv7/TSID keys, Flyway-owned schema. | AC-2 names `ProblemDetail` without defining it; the rest of the ACs name no implementation shape at all. |
| A-2 | The registration endpoint is reachable without authentication. | A customer registering has no account yet, so AC-1 cannot be satisfied by an authenticated-only endpoint — but no AC states this. |
| A-3 | "My new customer resource" in AC-1 refers to a customer resource under the existing `/api/v1/customers` path family. | AC-1 requires a `Location` header without naming the URI space. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | AC-5 forbids plaintext storage but names no storage form. Which password hashing algorithm and parameters are required? | AC-5 |
| OQ-2 | AC-3 refers to "the password rules". Beyond the 12–128 character bound it states, does any other password rule exist (complexity, breach-list check, disallowed values)? | AC-3 |
| OQ-3 | AC-6 assigns the `USER` role but does not say where the assignment is observable — a persisted field, a token claim, or a response body. How is the role verified? | AC-6 |
| OQ-4 | No AC defines the response when the email is malformed, or when a required field is blank. What status and body apply? | AC-1 |
| OQ-5 | When one request both duplicates an existing email and violates a password rule, which outcome wins — the 409 of AC-2 or the 400 of AC-3? | AC-2, AC-3 |
| OQ-6 | No AC constrains the maximum length of email, first name, or last name. What limits apply, and what happens when they are exceeded? | AC-1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| AC-1 | Valid registration returns 201 with `Location` | §3.1, §4 | Covered — see OQ-4, OQ-6 |
| AC-2 | Duplicate email returns 409 `ProblemDetail`, no second account | §3.2, §4 | Covered — see OQ-5 |
| AC-3 | Password outside 12–128 returns 400 listing violated rules | §3.3, §4 | Covered — see OQ-2 |
| AC-4 | Email uniqueness is case-insensitive | §3.2, §4 | Covered |
| AC-5 | No plaintext password is stored | §3.3, §4 | Covered — see OQ-1 |
| AC-6 | New customer holds the `USER` role | §3.4, §4 | Partial — see OQ-3 |

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-17 | Initial specification. Story and acceptance criteria drafted from `AGENTS.md` at user request; not stakeholder-approved. |
