# Epic 2 — Authentication: Login

**Story ID:** US-2.1
**Project:** Customer Portal
**AC prefix:** `LI-AC`
**Module:** `customer/`

## User Story
As a registered customer,
I want to exchange my email and password for a session,
So that I can reach the parts of the portal that are not available anonymously.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Access token TTL | 15 minutes | Upper bound fixed by `AGENTS.md` §3.4; short enough that revocation lag is tolerable |
| 2 | Refresh token transport | `HttpOnly`, `Secure`, `SameSite=Strict` cookie scoped to `Path=/api/v1/auth` | Not reachable from JavaScript; the narrow path keeps it off every other endpoint |
| 3 | JWT signing | RS256, verified through JWKS | Matches the stateless Resource Server baseline; verifiers need no shared secret |
| 4 | Password hashing | Argon2id tuned to ≈100 ms | Balances brute-force resistance against endpoint latency |
| 5 | Unknown-email handling | A dummy Argon2id verification still runs | Response timing must not reveal whether an account exists |
| 6 | Failed-login limits | 5 / account / 15 min (lock), 20 / IP / min | The lock is per account; the IP limit blunts spraying across many accounts |
| 7 | "Remember me" | Refresh TTL 30 days when opted in; a session cookie with no fixed lifetime otherwise | Explicit opt-in. There is no third, "default TTL" case — LI-AC8 defines both values exhaustively |
| 8 | Anti-enumeration timing tolerance | The unknown-email path must land within 50 ms of the wrong-password median | A bound is required, or LI-AC4 cannot be turned into a passing or failing test |
| 9 | Lockout notification | One email per lock window, to the address on the account | Mailing on every attempt during a sustained attack turns the defence into a nuisance vector |

## In Scope
- `POST /api/v1/auth/login` — credential verification and token issuance
- Account-state gating (deactivated) evaluated **after** credential verification
- Brute-force throttling per account and per source IP
- Audit entries for successful, failed and blocked attempts
- Validation of the post-login redirect target

## Out of Scope
- Refresh and rotation mechanics (US-2.4)
- Session termination (US-2.2, US-2.3)
- Password reset (US-2.5, US-2.6)
- MFA — deliberately deferred past Release 1.0; it intercepts this story's success path and must be its own story

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | None | `{"email": str, "password": str, "rememberMe": bool}` | `200` `{"accessToken": str, "tokenType": "Bearer", "expiresIn": 900}` + `Set-Cookie: refreshToken` |

## Data Model Notes
- `customers.last_login_at: Instant` (nullable)
- `customers.status` — an explicit state column with values `ACTIVE`, `PENDING_INVITATION`, `DEACTIVATED`, per the soft-delete rule in `AGENTS.md` §3.2
- `audit_events` — append-only; `event` is one of `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `LOGIN_BLOCKED`, alongside `reason`, `actorId`, `ip`, `userAgent`, `correlationId`, `occurredAt`
- Rate-limit counters are **not** PostgreSQL rows. They need a shared TTL store, which is a new runtime dependency — see Open Questions.

## Acceptance Criteria

### Happy path
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

### Credential failures
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

### Account-state gating
**LI-AC5 — Deactivated account**
```gherkin
Given correct credentials are supplied for a customer whose status is DEACTIVATED
When POST /api/v1/auth/login is called
Then respond 403 with type ".../errors/account-deactivated"
And no token is issued
And an audit_events entry is written with event LOGIN_BLOCKED and reason DEACTIVATED
But credential verification runs first, so a caller without the password only ever sees 401
```

### Throttling and validation
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/invalid-credentials",
  "title": "Invalid Credentials",
  "status": 401,
  "detail": "The email or password is incorrect.",
  "instance": "/api/v1/auth/login"
}
```
Error `type` slugs introduced by this story: `invalid-credentials`, `account-deactivated`, `email-not-verified`, `too-many-attempts`, `validation-failed`.

## Non-Functional / Security Requirements
- The response MUST NOT distinguish "no such account" from "wrong password" in body, status **or** timing.
- Passwords MUST NOT reach logs, traces or APM payloads. Add a scrubbing rule for `password` and `currentPassword` keys; swallowing exceptions and `printStackTrace()` are already prohibited by `AGENTS.md` §5.
- Argon2id verification runs on a virtual thread and must not be pooled (`AGENTS.md` §4.4).
- The login endpoint is CSRF-exempt, but every cookie-authenticated state-changing endpoint requires a CSRF token.
- TLS 1.2+ only, HSTS enabled, and the login response carries `Cache-Control: no-store`.
- **Performance:** p95 ≤ 500 ms at 200 concurrent requests, including the deliberate ≈100 ms hashing cost.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| LI-AC1, LI-AC3 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| LI-AC2 | Unit test on redirect-target validation | `[gate]` |
| LI-AC4 | Functional test asserting identical shape and status, and a median within 50 ms over 100 samples | `[gate]` |
| LI-AC9 | Functional test asserting the credential check precedes the verification check | `[gate]` |
| LI-AC5 | Functional test asserting the credential check precedes the state check | `[gate]` |
| LI-AC6 | Integration test against a deterministic injected `Clock` | `[gate]` |
| LI-AC7 | Slice test on the Jakarta Validation constraints of the request record | `[gate]` |
| LI-AC8 | Functional test covering both `rememberMe` values | `[gate]` |
| No password in logs | CI grep plus a log-scrubbing unit test | `[manual]` until a lint rule exists |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** Per-account and per-IP throttling needs a shared TTL store such as Redis or Valkey. That is a new runtime dependency and requires human sign-off before this story can be estimated as written.
2. **Escalation — `AGENTS.md` §7.1.** Token lifetimes and the RS256/JWKS choice touch the authentication scheme, so Decisions 1–3 need an architect's confirmation rather than only a product owner's.
3. LI-AC9 assumes email verification exists. `CP-101` registers the account but no story in this backlog verifies the address, so the state LI-AC9 gates on is produced by nothing. Epic 1's "Verify Email" needs a story before LI-AC9 can be built.
4. The 50 ms tolerance in Decision 8 is an engineering estimate, not a measured figure. It should be re-derived once Argon2id is tuned on the target hardware.
