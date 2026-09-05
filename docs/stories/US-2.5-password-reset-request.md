# Epic 2 — Authentication: Password Reset (request)

**Story ID:** US-2.5
**Project:** Customer Portal
**AC prefix:** `PR-AC`
**Module:** `customer/`

## User Story
As a customer who has forgotten their password,
I want to request a reset link by email,
So that I can recover access myself instead of opening a support ticket.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Response to an unknown address | Identical to the known-address response | Anything else turns this endpoint into an account-existence oracle |
| 2 | Token lifetime | 30 minutes | Long enough to survive a slow mail hop, short enough to limit an intercepted inbox |
| 3 | Token storage | SHA-256 hash of a 256-bit random value | The database must not hold anything that can be replayed |
| 4 | Repeat requests | The newest token invalidates all earlier ones | Two live links double the attack surface for no user benefit |
| 5 | Delivery | Queued and sent asynchronously | A slow mail provider must not hold the HTTP response open |

## In Scope
- `POST /api/v1/auth/password-reset` — accept an address and, if it resolves, issue a token
- Uniform response and timing regardless of whether the account exists
- Rate limiting per address and per source IP
- Queueing the reset email

## Out of Scope
- Consuming the token and setting the new password (US-2.6)
- Password policy definition (US-2.6)
- Changing a password while signed in — not in Release 1.0

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/password-reset` | None | `{"email": str}` | `202` `{"message": "If that account exists, we have sent instructions."}` |

## Data Model Notes
- `password_reset_tokens`: `tokenHash`, `customerId`, `issuedAt`, `expiresAt`, `consumedAt`, `invalidatedAt`
- Issuing a new token sets `invalidatedAt` on every live token for that customer, satisfying PR-AC4
- No row is created for an unknown address, but the endpoint still performs equivalent work so timing stays flat

## Acceptance Criteria

### Happy path
**PR-AC1 — Reset link issued**
```gherkin
Given an active customer with the address "olena@example.com"
When POST /api/v1/auth/password-reset is called with that address
Then respond 202 with the neutral message
And a single-use reset token valid for 30 minutes is created
And an email carrying the reset link is queued for delivery
And an audit_events entry is written with event PASSWORD_RESET_REQUESTED
```

### Anti-enumeration
**PR-AC2 — Unknown or deactivated account**
```gherkin
Given the address is not registered
When POST /api/v1/auth/password-reset is called
Then respond 202 with exactly the same body and status as PR-AC1
And no token is created and no email is sent
And the same holds when the address belongs to a DEACTIVATED account
```

### Throttling and validation
**PR-AC3 — Rate limiting**
```gherkin
Given three reset requests have been made for one address within the last hour
When a fourth request arrives for that address
Then respond 429 with a Retry-After header and type ".../errors/too-many-attempts"
And no email is sent
And an independent limit of 10 requests per hour applies per source IP
```

**PR-AC4 — Repeat request supersedes the previous link**
```gherkin
Given a reset link has already been issued and is still valid
When the customer requests another one
Then the earlier token is invalidated
And only the most recently emailed link can be used
```

**PR-AC5 — Malformed address**
```gherkin
Given a request body whose "email" is absent or not a valid address
When POST /api/v1/auth/password-reset is called
Then respond 400 with type ".../errors/validation-failed"
And the attempt is not counted against the per-address rate limit
```

## Error Envelope (RFC 9457 `ProblemDetail`)
Reuses `too-many-attempts` and `validation-failed` from US-2.1. This story introduces no new `type` slug.

## Non-Functional / Security Requirements
- Body, status code **and** response time must not vary with account existence. PR-AC2 is the whole point of the story and is easy to break with a naive early return.
- The reset token is a 256-bit cryptographically random value; only its SHA-256 hash is stored.
- Mail delivery failures are logged and monitored but never surfaced to the caller, since the caller may not own the address.
- The reset link carries no session, no role and no personal data beyond the token itself.
- **Performance:** p95 ≤ 300 ms, measured identically for existing and non-existing addresses.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| PR-AC1 | Functional suite (RestAssured + Testcontainers) with a stubbed mail port | `[gate]` |
| PR-AC2 | Functional test comparing body, status and timing across both cases | `[gate]` |
| PR-AC3 | Integration test with a deterministic injected `Clock` | `[gate]` |
| PR-AC4 | Integration test asserting the first token is rejected afterwards | `[gate]` |
| PR-AC5 | Slice test on the request record's validation constraints | `[gate]` |
| Hash-only storage | Persistence test asserting no column holds the raw token | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** Asynchronous delivery needs an outbound mail provider and a queue. Both are new runtime dependencies requiring sign-off; the story cannot ship on a synchronous SMTP call without revisiting Decision 5.
2. Rate limiting shares the store introduced in US-2.1 Open Question 1; if that dependency is refused, PR-AC3 needs a different design.
3. Should a reset request against a deactivated account notify an administrator? It is a weak but real signal that a former user is trying to return.
