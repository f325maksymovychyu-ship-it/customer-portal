---
story_id: US-2.1
title: "Epic 2 — Authentication: Login"
source: docs/backlog/US-2.1-login.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Login

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant
> from the epic list supplied by the user. No product owner, customer or regulator has
> agreed to them. Until section 2 is signed off, **every row in this document is a
> proposal, not an agreed requirement.** The same caveat already applies to `CP-101`.

## 1. Story

> As a registered customer, I want to exchange my email and password for a session,
> So that I can reach the parts of the portal that are not available anonymously.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**LI-AC1 — Successful login**
```gherkin
Given an active customer with a verified email
When POST /api/v1/auth/login is called with the correct email and password
Then respond 200 with an access token (JWT, 15-minute TTL) in the body
And set the refresh token as an HttpOnly, Secure, SameSite=Strict cookie on Path=/api/v1/auth
And customers.last_login_at is updated
And an audit_events entry is written with event LOGIN_SUCCEEDED
```

**LI-AC2 — Return to the originally requested page**
```gherkin
Given an unauthenticated caller was redirected from "/orders/1042" to the login screen
When the caller authenticates successfully
Then the client returns them to "/orders/1042" rather than the dashboard
But a redirect target pointing at another origin is discarded and the dashboard is used
```

**LI-AC3 — Wrong password**
```gherkin
Given an active customer
When POST /api/v1/auth/login is called with an incorrect password
Then respond 401 with ProblemDetail type ".../errors/invalid-credentials"
And no token of any kind is issued
And an audit_events entry is written with event LOGIN_FAILED and reason BAD_PASSWORD
```

**LI-AC4 — Unknown email, anti-enumeration**
```gherkin
Given an email address that is not registered
When POST /api/v1/auth/login is called with that email and any password
Then respond 401 with the same body and the same status as LI-AC3
And the median response time is within 50 ms of the median for LI-AC3 over 100 samples
Because a dummy Argon2id verification runs so the response time does not reveal account existence
```

**LI-AC5 — Deactivated account**
```gherkin
Given correct credentials are supplied for a customer whose status is DEACTIVATED
When POST /api/v1/auth/login is called
Then respond 403 with type ".../errors/account-deactivated"
And no token is issued
And an audit_events entry is written with event LOGIN_BLOCKED and reason DEACTIVATED
But credential verification runs first, so a caller without the password only ever sees 401
```

**LI-AC6 — Account lockout after repeated failures**
```gherkin
Given 4 failed login attempts have been recorded for one account within 15 minutes
When a fifth attempt fails
Then the account is locked for 15 minutes
And further attempts respond 429 with a Retry-After header and type ".../errors/too-many-attempts"
And a correct password during the lock window receives the same 429, not a session
And the account owner is emailed once per lock window, not once per attempt
And a successful login resets the counter
And an independent limit of 20 attempts per source IP per minute responds 429 the same way
```

**LI-AC7 — Malformed request**
```gherkin
Given a request body missing "password", or carrying an unknown field
When POST /api/v1/auth/login is called
Then respond 400 with type ".../errors/validation-failed"
And the ProblemDetail names the offending fields
And the attempt is not counted against the rate-limit counter
```

**LI-AC8 — Already authenticated, and remember-me**
```gherkin
Given the caller already holds a valid session
When they request the login screen
Then they are redirected to the dashboard instead
When login succeeds with rememberMe set to true
Then the refresh token is issued with a 30-day TTL
But with rememberMe set to false the cookie is a session cookie that dies with the browser
```

**LI-AC9 — Unverified email**
```gherkin
Given correct credentials are supplied for a customer whose email is not yet verified
When POST /api/v1/auth/login is called
Then respond 403 with type ".../errors/email-not-verified"
And no token is issued
And an audit_events entry is written with event LOGIN_BLOCKED and reason EMAIL_NOT_VERIFIED
And the response offers to resend the verification email
But credential verification runs first, so a caller without the password only ever sees 401
```

## 3. Functional Specification

### 3.1 Credential exchange

On a login request carrying an email and a password, the system verifies the credentials
against the named account. `[LI-AC1]`

Where verification succeeds and the account is active with a verified email, the response
is `200` and carries an access token in the body. The access token is a JWT whose time to
live is 15 minutes. `[LI-AC1]`

The same response sets the refresh token as a cookie carrying the `HttpOnly`, `Secure` and
`SameSite=Strict` attributes, scoped to `Path=/api/v1/auth`. `[LI-AC1]`

On success the system updates `customers.last_login_at` and writes an `audit_events`
entry with event `LOGIN_SUCCEEDED`. `[LI-AC1]`

### 3.2 Post-authentication navigation

Where an unauthenticated caller reached the login screen by redirect from a portal path,
the client returns them to that path after successful authentication rather than to the
dashboard. `[LI-AC2]`

Where the stored redirect target points at an origin other than the portal's, it is
discarded and the dashboard is used instead. `[LI-AC2]`

Where the caller already holds a valid session and requests the login screen, they are
redirected to the dashboard without being asked for credentials. `[LI-AC8]`

### 3.3 Credential failure

Where the password does not match, the response is `401` carrying a `ProblemDetail` whose
`type` is `.../errors/invalid-credentials`. No token of any kind is issued, and an
`audit_events` entry is written with event `LOGIN_FAILED` and reason `BAD_PASSWORD`.
`[LI-AC3]`

Where the supplied email is not registered, the response carries the same body and the same
status as the wrong-password response, and its median response time is within 50 ms of that
path's median over 100 samples. `[LI-AC4]`

The bound is met by performing a dummy Argon2id verification on the unknown-email path, so
that elapsed time does not reveal whether the account exists. `[LI-AC4]`

### 3.4 Account-state gating

Where the credentials are correct but the account's status is `DEACTIVATED`, the response
is `403` with `type` `.../errors/account-deactivated`, no token is issued, and an
`audit_events` entry is written with event `LOGIN_BLOCKED` and reason `DEACTIVATED`.
`[LI-AC5]`

Where the credentials are correct but the email is not yet verified, the response is `403`
with `type` `.../errors/email-not-verified`, no token is issued, an `audit_events` entry is
written with event `LOGIN_BLOCKED` and reason `EMAIL_NOT_VERIFIED`, and the response offers
to resend the verification email. `[LI-AC9]`

Credential verification runs before both state checks. A caller who does not hold the
password therefore never receives either `403`, only the `401` of §3.3. `[LI-AC5]`
`[LI-AC9]`

### 3.5 Throttling

Where four failed attempts have been recorded against one account inside a 15-minute
window, a fifth failure locks that account for 15 minutes. `[LI-AC6]`

While the lock holds, further attempts receive `429` with a `Retry-After` header and
`type` `.../errors/too-many-attempts`. A correct password presented during the lock window
receives the same `429` rather than a session. `[LI-AC6]`

The account owner is emailed once per lock window rather than once per attempt. `[LI-AC6]`

An independent limit of 20 attempts per source IP per minute produces the same `429`.
`[LI-AC6]`

A successful login resets the failure counter. `[LI-AC6]`

### 3.6 Request validation

Where the request body omits `password`, or carries a field the contract does not declare,
the response is `400` with `type` `.../errors/validation-failed`, and the `ProblemDetail`
names the offending fields. `[LI-AC7]`

A request rejected by validation is not counted against the rate-limit counter of §3.5.
`[LI-AC7]`

### 3.7 Session duration

Where login succeeds with `rememberMe` set to `true`, the refresh token is issued with a
time to live of 30 days. `[LI-AC8]`

Where login succeeds with `rememberMe` set to `false`, the refresh cookie is a session
cookie that does not outlive the browser session. `[LI-AC8]`

Assumption 7 of the story now states that these two values are exhaustive: there is no third,
"default TTL" case, which is what the earlier revision of this document flagged as a
contradiction.

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/auth/login` | Path named by the criteria; method named by the criteria | `[LI-AC1]` |
| 2 | `email` (request) | not specified — no criterion constrains its format or length | `[LI-AC1]` `[LI-AC4]` |
| 3 | `password` (request) | not specified — required to be present | `[LI-AC1]` `[LI-AC7]` |
| 4 | `rememberMe` (request) | Boolean; both values defined exhaustively | `[LI-AC8]` |
| 5 | access token (response body) | JWT, 15-minute TTL | `[LI-AC1]` |
| 6 | refresh token (cookie) | `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`; 30-day TTL when `rememberMe` is true, session cookie when false | `[LI-AC1]` `[LI-AC8]` |
| 7 | `customers.last_login_at` | not specified beyond "is updated" on success | `[LI-AC1]` |
| 8 | `customers.status` | Only the value `DEACTIVATED` is named; the rest of the value set is not specified | `[LI-AC5]` |
| 8a | email-verified state | A state distinct from `customers.status`, gating login | `[LI-AC9]` |
| 9 | `audit_events.event` | Values `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `LOGIN_BLOCKED` | `[LI-AC1]` `[LI-AC3]` `[LI-AC5]` `[LI-AC9]` |
| 10 | `audit_events.reason` | Values `BAD_PASSWORD`, `DEACTIVATED`, `EMAIL_NOT_VERIFIED` | `[LI-AC3]` `[LI-AC5]` `[LI-AC9]` |
| 11 | `ProblemDetail.type` | Slugs `invalid-credentials`, `account-deactivated`, `email-not-verified`, `too-many-attempts`, `validation-failed` | `[LI-AC3]` `[LI-AC5]` `[LI-AC6]` `[LI-AC7]` `[LI-AC9]` |
| 12 | `Retry-After` header | Present on the `429`; value not specified | `[LI-AC6]` |

The story's API Contract table also names `tokenType` and `expiresIn` in the response body.
No acceptance criterion mentions either, so they are not recorded here as requirements —
see A-2.

## 5. Out of Scope

- Refresh and rotation mechanics — covered by US-2.4, not by any criterion here.
- Ending a session — US-2.2 and US-2.3.
- Password reset — US-2.5 and US-2.6.
- Multi-factor authentication — no criterion in this story reaches it.
- Registration and email verification — `CP-101`. LI-AC1 says "verified email" but no
  criterion here defines how verification happens.
- Per-IP throttling — see OQ-5.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The request and response payload shapes are as given in the story's API Contract table. | No criterion states the JSON field names beyond `rememberMe`; §4 would otherwise have no interface to describe. |
| A-2 | `tokenType` and `expiresIn` are part of the response body. | Named only in the story's API Contract, never in a criterion. Recorded here so they are not mistaken for agreed requirements. |
| A-3 | The JWT is signed with RS256 and verified through JWKS. | Named only in the story's Assumptions table. No criterion constrains the signing scheme. |
| A-4 | Argon2id is the hashing algorithm for stored passwords, not only for the dummy verification. | LI-AC4 names Argon2id on the unknown-email path. That it is also the real hashing algorithm is inferred, not stated. |
| A-5 | `audit_events` carries actor, IP, user agent and correlation identifiers in addition to `event` and `reason`. | Named in the story's Data Model Notes. No criterion requires any column beyond `event` and `reason`. |
| A-6 | "The account owner is notified" in LI-AC6 means an email to the address on the account. | LI-AC6 asserts a notification without naming a channel; §3.5 cannot be read without some reading of it. See OQ-2. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | LI-AC9 gates login on an email-verified state, but nothing in this backlog produces it. `CP-101` registers the account; Epic 1's "Verify Email" has no story. Until it does, LI-AC9 tests a state that cannot be reached. | LI-AC9 |
| OQ-2 | The 50 ms tolerance in LI-AC4 is an engineering estimate, not a measurement. It must be re-derived once Argon2id is tuned on the target hardware, or the criterion will either pass trivially or fail on unrelated jitter. | LI-AC4 |
| OQ-3 | The story's Non-Functional section states p95 ≤ 500 ms at 200 concurrent requests. No acceptance criterion asserts any latency bound. Is the budget a requirement of this story, or guidance? | — |
| OQ-4 | **Escalation — `AGENTS.md` §7.5.** Both throttling limits in LI-AC6 need a shared TTL store. Until that dependency is approved, the criterion cannot be implemented as written. | LI-AC6 |

**Resolved since revision 1.** The contradiction between LI-AC8 and the story's remember-me
assumption, the undefined timing tolerance in LI-AC4, the unnamed notification channel and
the out-of-scope per-IP limit in LI-AC6, and the unhandled unverified-email path have all
been closed in the backlog. LI-AC9 is new.

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| LI-AC1 | Successful login issues an access token and a refresh cookie | §3.1, §4 | Covered — see OQ-6 |
| LI-AC2 | Caller returns to the page that triggered the redirect | §3.2 | Covered |
| LI-AC3 | Wrong password yields 401 and an audit entry | §3.3, §4 | Covered |
| LI-AC4 | Unknown email is indistinguishable from a wrong password | §3.3 | Covered — the criterion now states a 50 ms median tolerance over 100 samples |
| LI-AC5 | Deactivated account yields 403, after the credential check | §3.4, §4 | Covered |
| LI-AC6 | Fifth failure locks the account for 15 minutes | §3.5, §4 | Covered — the criterion now names the channel, the cadence and the per-IP limit |
| LI-AC7 | Malformed request yields 400 and is not counted | §3.6 | Covered |
| LI-AC8 | Existing session redirects; remember-me extends the refresh TTL | §3.2, §3.7, §4 | Covered |
| LI-AC9 | Unverified email yields 403, after the credential check | §3.4, §4 | **Partial** — the verified state the criterion gates on is produced by no story (OQ-1) |

**Coverage:** 8 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.1-login.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
