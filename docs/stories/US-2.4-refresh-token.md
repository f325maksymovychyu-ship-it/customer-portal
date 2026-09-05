# Epic 2 — Authentication: Refresh Token

**Story ID:** US-2.4
**Project:** Customer Portal
**AC prefix:** `RT-AC`
**Module:** `customer/`

## User Story
As a signed-in customer,
I want my session to renew itself quietly in the background,
So that I am not asked for my password every fifteen minutes and never lose work to an expiry.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Rotation | Every refresh issues a new refresh token and retires the old one | Mandated by `AGENTS.md` §3.4; a static refresh token is a long-lived bearer secret |
| 2 | Reuse detection | Presenting a retired token revokes the entire family | The only honest reading of a replayed token is that it leaked |
| 3 | Storage | Only a SHA-256 hash of the token is persisted | A database dump must not yield usable sessions |
| 4 | Client concurrency | One in-flight refresh per client; other calls await its result | Parallel refreshes race the rotation and would revoke each other |
| 5 | Retry policy | A failed call is retried exactly once after a refresh | More than once turns an outage into a request storm |
| 6 | Refresh token lifetime | As issued by US-2.1 LI-AC8 — 30 days with "remember me", otherwise the browser session | This story rotates the token but does not set its lifetime; RT-AC4 tests against whatever US-2.1 issued |
| 7 | Blast radius of reuse detection | Every family for the customer, not only the implicated one | A replayed token means the secret leaked; which device it leaked from is unknowable at detection time |

## In Scope
- `POST /api/v1/auth/refresh` — rotate the pair and reissue an access token
- Reuse detection and family-wide revocation
- Single-flight coordination on the client, with queued callers replayed after success
- Re-evaluation of account state on every refresh

## Out of Scope
- Initial credential exchange (US-2.1)
- Explicit revocation by the customer (US-2.2, US-2.3)
- Permission changes taking effect — the propagation bound is stated here but owned by US-3.5

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/refresh` | Refresh cookie | *(empty)* | `200` `{"accessToken": str, "tokenType": "Bearer", "expiresIn": 900}` + rotated `Set-Cookie: refreshToken` |

## Data Model Notes
- `refresh_tokens`: `tokenHash`, `familyId`, `previousTokenId`, `issuedAt`, `expiresAt`, `revokedAt`, `usedAt`
- A token is retired by setting `usedAt`, not by deletion — reuse detection needs the row to still exist
- `familyId` links every descendant of one login, which is what family-wide revocation acts on
- A scheduled job removes rows past `expiresAt` daily, so the table does not grow without bound

## Acceptance Criteria

### Happy path
**RT-AC1 — Transparent rotation**
```gherkin
Given the access token has expired
And the refresh token is valid and not revoked
When POST /api/v1/auth/refresh is called
Then respond 200 with a new access token and a rotated refresh token
And the previous refresh token is retired and can never be used again
And the customer sees no sign-in screen and stays on the current page
```

**RT-AC2 — Replaying the original request**
```gherkin
Given an API call returned 401 because the access token had expired
When the client refreshes the session
Then the original request is retried once with the new access token
And the result is returned to the calling code as though no interruption occurred
But the retry happens at most once, so a persistent 401 never becomes a loop
```

### Concurrency
**RT-AC3 — Parallel requests during a refresh**
```gherkin
Given a page issues five API calls at once
And the access token has expired for all five
When the first call triggers a refresh
Then exactly one call to POST /api/v1/auth/refresh is made
And the remaining four wait for its result and are replayed with the new token
And no call fails because of the rotation race
```

### Token failures
**RT-AC4 — Expired or revoked refresh token**
```gherkin
Given the refresh token has expired or was revoked
When POST /api/v1/auth/refresh is called
Then respond 401 with type ".../errors/refresh-token-invalid"
And the client clears local state and returns the customer to the login screen
And the page the customer was on is preserved so US-2.1 LI-AC2 can return them to it
```

**RT-AC5 — Reuse of a retired token**
```gherkin
Given a refresh token was already used and rotated
When that same token is presented again
Then respond 401
And every refresh-token family belonging to that customer is revoked, not only the family containing the replayed token
And the customer is therefore signed out on every device, because a leaked token implicates the account rather than one session
And the customer is emailed about the suspicious activity
And an audit_events entry of severity SECURITY records the IP of the rotating request and of the replaying request
```

**RT-AC6 — Forged or tampered token**
```gherkin
Given a token with an invalid signature or an altered payload is presented
When the server validates it
Then respond 401 with no detail about why validation failed
And the response reveals nothing about the algorithm, key or token structure
And an audit_events entry is written
```

### Account state
**RT-AC7 — Account deactivated mid-session**
```gherkin
Given a customer is working with a currently valid access token
And an administrator deactivates their account
When the next refresh is attempted
Then respond 403 with type ".../errors/account-deactivated"
And the customer is returned to the login screen with that reason
And access therefore survives deactivation by no more than the 15-minute access-token TTL
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/refresh-token-invalid",
  "title": "Refresh Token Invalid",
  "status": 401,
  "detail": "The session could not be renewed. Sign in again.",
  "instance": "/api/v1/auth/refresh"
}
```
Error `type` slugs introduced by this story: `refresh-token-invalid`.

## Non-Functional / Security Requirements
- Rotation is mandatory. A deployment that reissues the same refresh token fails this story regardless of test results.
- Token comparison is constant-time against the stored hash.
- Rate limit: at most 10 refreshes per minute per account; exceeding it means a client defect or an attack and must alert.
- **Performance:** p95 ≤ 200 ms. This endpoint sits in the critical path of every replayed request, so its latency is paid twice by the user.
- The cleanup job must be idempotent and safe to run concurrently with live traffic.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| RT-AC1 | Integration test asserting the old token is rejected after rotation | `[gate]` |
| RT-AC2 | Client unit test on the retry interceptor, asserting exactly one retry | `[gate]` |
| RT-AC3 | Client unit test issuing five concurrent calls against a counting stub | `[gate]` |
| RT-AC4, RT-AC6 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| RT-AC5 | Integration test asserting family-wide revocation and the SECURITY audit entry | `[gate]` |
| RT-AC7 | Integration test deactivating the account between two refreshes | `[gate]` |
| Hash-only storage | Persistence test asserting no column holds the raw token | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.1.** Rotation, reuse detection and family revocation are authentication-scheme changes and need architect sign-off before implementation.
2. RT-AC5 now ends every session on reuse. For a customer whose browser extension or aggressive prefetcher replays requests, that is a hard lockout with no self-service recovery beyond signing in again. Should the first occurrence in a rolling window warn instead of revoking?
3. Should the 15-minute deactivation window in RT-AC7 be tightened for privileged roles by checking account state on every request? That trades latency for revocation speed and belongs to the architect.
4. The daily cleanup job in the Data Model Notes removes expired rows. A retired token whose row is gone can no longer be recognised as reused, so retention must exceed the refresh TTL. What is the retention, and does it survive the 30-day case?
