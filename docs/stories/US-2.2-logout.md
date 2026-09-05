# Epic 2 — Authentication: Logout

**Story ID:** US-2.2
**Project:** Customer Portal
**AC prefix:** `LO-AC`
**Module:** `customer/`

## User Story
As a signed-in customer,
I want to end my session on this device from any page,
So that nobody who reaches this browser after me inherits my access.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Scope of the operation | Current session only | Ending every session is a different intent with a different risk profile — US-2.3 |
| 2 | Idempotency | A repeated logout answers `204`, never an error | Retries and double-clicks are normal; a second call has nothing left to undo |
| 3 | Cross-tab propagation | `BroadcastChannel`, not server polling | The other tabs already know; asking the server would be pure noise |
| 4 | Client behaviour on network failure | Clear local state anyway | The user asked to be signed out; refusing to do so locally is the worse failure |

## In Scope
- `POST /api/v1/auth/logout` — revoke the presented refresh token
- Clearing the refresh cookie and every client-held fragment of session state
- Propagating the signed-out state to other open tabs
- Preventing cached protected pages from being restored by the back button

## Out of Scope
- Ending sessions on other devices (US-2.3)
- Access-token revocation before its natural expiry — the 15-minute TTL is the accepted bound (US-2.4)
- Audit log presentation (US-3.7)

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/logout` | Refresh cookie | *(empty)* | `204` + `Set-Cookie: refreshToken=; Max-Age=0` |

## Data Model Notes
- `refresh_tokens.revoked_at: Instant` (nullable) — revocation is a state transition, not a delete, so reuse detection in US-2.4 still has a row to recognise
- `refresh_tokens.family_id` — shared by every token descended from one login; revoking the family is what makes LO-AC2 possible
- `audit_events.event` gains `LOGOUT_SUCCEEDED` and `TOKEN_REUSE_AFTER_LOGOUT`

## Acceptance Criteria

### Happy path
**LO-AC1 — Successful logout**
```gherkin
Given a signed-in customer on any page of the portal
When POST /api/v1/auth/logout is called with the current refresh cookie
Then respond 204
And the refresh token is marked revoked
And the refresh cookie is cleared with Max-Age=0
And the client clears the in-memory access token, sessionStorage, localStorage and any IndexedDB store the portal owns
And the customer is returned to the login screen with the message "You have been signed out"
And an audit_events entry is written with event LOGOUT_SUCCEEDED
```

### Token invalidation
**LO-AC2 — Presenting a revoked token to the refresh endpoint**
```gherkin
Given a customer has signed out
When POST /api/v1/auth/refresh is called with the token that logout revoked
Then respond 401 with type ".../errors/refresh-token-invalid"
And the whole token family is revoked, not just the presented token
And an audit_events entry is written with event TOKEN_REUSE_AFTER_LOGOUT
But the logout endpoint itself is exempt from this rule, per LO-AC3
```

**LO-AC3 — Repeated logout is idempotent**
```gherkin
Given a customer has already signed out
When POST /api/v1/auth/logout is called again with the same or an absent cookie
Then respond 204
And no error is surfaced to the user
And no TOKEN_REUSE_AFTER_LOGOUT entry is written, because ending an ended session is not an attack
```

### Failure and client-side behaviour
**LO-AC4 — Logout while the network is unavailable**
```gherkin
Given the customer pressed "Sign out"
And the request failed with a network error
Then the client still clears local tokens and session state
And the customer sees "Signed out on this device. If this is a shared computer, check again once you are back online"
And the server-side token remains valid until its own expiry, which is at most 30 days per US-2.1 LI-AC8
And the client retries the logout call once on its next successful request to the API
```

**LO-AC5 — Back button and multiple tabs**
```gherkin
Given the customer has signed out
When they press the browser back button
Then no protected page is restored from cache and they land on the login screen
Given the portal is open in two tabs
When the customer signs out in one of them
Then the other tab reaches the signed-out state within 5 seconds
```

**LO-AC6 — Cross-site logout is refused**
```gherkin
Given a form on another origin submits POST /api/v1/auth/logout with the customer's cookie
When the request arrives without a valid CSRF token
Then respond 403 with type ".../errors/csrf-token-missing"
And the session is not ended
Because SameSite=Strict is a defence in depth, not the only control
```

## Error Envelope (RFC 9457 `ProblemDetail`)
Reuses `refresh-token-invalid`, introduced by US-2.4.
Error `type` slugs introduced by this story: `csrf-token-missing`.

## Non-Functional / Security Requirements
- Protected responses carry `Cache-Control: no-store, no-cache` and `Pragma: no-cache`, otherwise LO-AC5 cannot hold.
- Revocation must be durable before the `204` is returned; a logout that is lost on restart is a security defect, not a performance trade-off.
- Cross-tab propagation uses `BroadcastChannel` or a storage event, never a polling loop.
- **Performance:** p95 ≤ 200 ms — logout must never feel like it might not have worked.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| LO-AC1 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| LO-AC2 | Integration test asserting family-wide revocation | `[gate]` |
| LO-AC3 | Functional test calling the endpoint twice | `[gate]` |
| LO-AC4 | Client unit test with a stubbed network failure | `[gate]` |
| LO-AC5 | End-to-end test driving the back button and a second tab | `[gate]` |
| LO-AC6 | Security test posting from a foreign origin without a CSRF token | `[gate]` |
| Cache headers | Contract test on the response headers of protected endpoints | `[gate]` |

## Open Questions
1. Should logout also clear the "remember me" preference, so the next visit starts from a blank login form? Current default: it does not.
2. LO-AC5 assumes the two tabs share an origin and a browser profile. Behaviour across profiles or containers is out of scope and untested.
3. LO-AC4 leaves a revoked-but-unexpired token on the server for up to 30 days when the logout call never reaches it. The retry closes the common case; a customer who never returns leaves the token alive until expiry. Is that acceptable, or should the refresh TTL be shortened?
