---
story_id: US-2.2
title: "Epic 2 — Authentication: Logout"
source: docs/backlog/US-2.2-logout.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Logout

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want to end my session on this device from any page,
> So that nobody who reaches this browser after me inherits my access.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

## 3. Functional Specification

### 3.1 Ending the session

A logout request presenting the current refresh cookie responds `204` and marks that
refresh token revoked. `[LO-AC1]`

The response clears the refresh cookie by setting `Max-Age=0`. `[LO-AC1]`

The operation is reachable from any page of the portal. `[LO-AC1]`

An `audit_events` entry with event `LOGOUT_SUCCEEDED` is written. `[LO-AC1]`

### 3.2 Client-side state

On logout the client clears the in-memory access token, `sessionStorage`, `localStorage` and
any IndexedDB store the portal owns. `[LO-AC1]`

The customer is returned to the login screen carrying the message
"You have been signed out". `[LO-AC1]`

### 3.3 Revoked-token handling

Where the refresh endpoint is called with a token that a logout has revoked, the response
is `401` with `type` `.../errors/refresh-token-invalid`. The logout endpoint itself is
exempt, so a repeated logout still answers `204` per §3.4. `[LO-AC2]`

The revocation extends to the whole token family, not only the presented token.
`[LO-AC2]`

An `audit_events` entry with event `TOKEN_REUSE_AFTER_LOGOUT` is written. `[LO-AC2]`

### 3.4 Idempotency

Where logout is called again after the session has already ended, with the same cookie or
with none, the response is `204` and no error is surfaced to the user. `[LO-AC3]`

### 3.5 Failure of the logout request

Where the logout request fails with a network error, the client still clears local tokens
and session state. `[LO-AC4]`

The customer sees the message "Signed out on this device. If this is a shared computer,
check again once you are back online". `[LO-AC4]`

The server-side token remains valid until its own expiry, at most 30 days per US-2.1
LI-AC8, and the client retries the logout call once on its next successful API request.
`[LO-AC4]`

### 3.6 Cross-site protection

Where a request to end the session arrives from another origin without a valid CSRF token,
the response is `403` with `type` `.../errors/csrf-token-missing` and the session is not
ended. `[LO-AC6]`

`SameSite=Strict` on the refresh cookie is a second layer, not the control this criterion
tests. `[LO-AC6]`

### 3.7 Cached pages and other tabs

After logout, pressing the browser back button restores no protected page from cache; the
customer lands on the login screen. `[LO-AC5]`

Where the portal is open in two tabs and the customer signs out in one, the other reaches
the signed-out state within 5 seconds. `[LO-AC5]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/auth/logout` | Path and method named by the criteria; request body not specified | `[LO-AC1]` |
| 2 | Success response | `204` on both the first and the repeated call | `[LO-AC1]` `[LO-AC3]` |
| 3 | refresh cookie (response) | Cleared with `Max-Age=0` | `[LO-AC1]` |
| 4 | refresh token state | A `revoked` state distinct from absence; the token is not deleted | `[LO-AC1]` `[LO-AC2]` |
| 5 | token family | A grouping over refresh tokens that revocation applies to as a unit; its key is not specified | `[LO-AC2]` |
| 6 | `audit_events.event` | Values `LOGOUT_SUCCEEDED`, `TOKEN_REUSE_AFTER_LOGOUT` | `[LO-AC1]` `[LO-AC2]` |
| 7 | `ProblemDetail.type` | Slug `refresh-token-invalid`, introduced by US-2.4 | `[LO-AC2]` |
| 8 | Cross-tab propagation | Bound of 5 seconds; mechanism not specified | `[LO-AC5]` |
| 9 | Message strings | "You have been signed out"; "Signed out on this device. If this is a shared computer, check again once you are back online" | `[LO-AC1]` `[LO-AC4]` |
| 10 | CSRF token | Required on the logout request; representation not specified | `[LO-AC6]` |
| 11 | `ProblemDetail.type` | Slug `csrf-token-missing` | `[LO-AC6]` |

## 5. Out of Scope

- Ending sessions on other devices — US-2.3.
- Revoking an access token before it expires. The story accepts the 15-minute window.
- Audit log presentation — US-3.7.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-2 | Cross-tab propagation uses `BroadcastChannel` or a storage event rather than polling. | Named in the story's Assumptions table, not in LO-AC5, which constrains only the 5-second bound. |
| A-3 | "No protected page is restored from cache" is achieved by `Cache-Control: no-store` on protected responses. | LO-AC5 states the observable outcome; the mechanism is named only in the story's Non-Functional section. |
| A-4 | Revocation is durable before the response is returned. | Named in the story's Non-Functional section. No criterion distinguishes a durable revocation from an in-memory one. |
| A-5 | The two tabs in LO-AC5 share one browser profile and origin. | LO-AC5 says "two tabs" without qualification; behaviour across profiles is untestable without this reading. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | LO-AC5 assumes the two tabs share an origin and a browser profile. Behaviour across profiles or browser containers is out of scope and untested. | LO-AC5 |
| OQ-2 | LO-AC4 leaves a revoked-but-unexpired token on the server for up to 30 days when the logout call never reaches it. The retry closes the common case; a customer who never returns leaves the token alive until expiry. Is that acceptable, or should the refresh TTL be shortened? | LO-AC4 |
| OQ-3 | Should logout also clear the "remember me" preference, so the next visit starts from a blank form? The story says it does not. | — |
| OQ-4 | The story's Non-Functional section states p95 ≤ 200 ms. No criterion asserts a latency bound. | — |

**Resolved since revision 1.** The LO-AC2 / LO-AC3 contradiction over a repeated logout, the
unenumerable client-state clearing in LO-AC1, the missing status code on the success path,
and the undefined TTL bound in LO-AC4 are all closed. LO-AC6 is new, covering the CSRF gap
raised as OQ-4 in revision 1.

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| LO-AC1 | Logout revokes the token, clears the cookie and audits | §3.1, §3.2, §4 | Covered — the criterion now states `204` and enumerates the client stores |
| LO-AC2 | A revoked token at the refresh endpoint yields 401 | §3.3, §4 | Covered — the criterion now names the endpoint and exempts logout |
| LO-AC3 | Repeated logout answers 204 quietly | §3.4 | Covered — the exemption is now explicit on both sides |
| LO-AC4 | A failed request still clears local state | §3.5 | Covered — the criterion now bounds the token at 30 days and adds a retry |
| LO-AC5 | Back button and second tab reach the signed-out state | §3.7, §4 | Covered |
| LO-AC6 | A cross-origin logout without a CSRF token is refused | §3.6, §4 | Covered |

**Coverage:** 6 Covered, 0 Partial, 0 Not covered.

> Revision 1 of this document reported 4 Partial rows, including a direct contradiction
> between LO-AC2 and LO-AC3 over which governs a repeated logout. All four have been closed
> in the backlog, and LO-AC6 was added to cover the CSRF gap the earlier revision raised as
> OQ-4.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.2-logout.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
