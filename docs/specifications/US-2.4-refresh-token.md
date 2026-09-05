---
story_id: US-2.4
title: "Epic 2 — Authentication: Refresh Token"
source: docs/backlog/US-2.4-refresh-token.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Refresh Token

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want my session to renew itself quietly in the background,
> So that I am not asked for my password every fifteen minutes and never lose work to an
> expiry.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**RT-AC3 — Parallel requests during a refresh**
```gherkin
Given a page issues five API calls at once
And the access token has expired for all five
When the first call triggers a refresh
Then exactly one call to POST /api/v1/auth/refresh is made
And the remaining four wait for its result and are replayed with the new token
And no call fails because of the rotation race
```

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

**RT-AC7 — Account deactivated mid-session**
```gherkin
Given a customer is working with a currently valid access token
And an administrator deactivates their account
When the next refresh is attempted
Then respond 403 with type ".../errors/account-deactivated"
And the customer is returned to the login screen with that reason
And access therefore survives deactivation by no more than the 15-minute access-token TTL
```

## 3. Functional Specification

### 3.1 Rotation

Where the access token has expired and the presented refresh token is valid and not
revoked, the refresh endpoint responds `200` with a new access token and a rotated refresh
token. `[RT-AC1]`

The previously presented refresh token is retired and can never be used again. `[RT-AC1]`

A successful refresh is invisible to the customer: no sign-in screen is shown and they
remain on the current page. `[RT-AC1]`

### 3.2 Client retry behaviour

Where an API call returns `401` because the access token had expired, the client refreshes
the session and retries the original request once with the new access token. `[RT-AC2]`

The result is returned to the calling code as though no interruption had occurred.
`[RT-AC2]`

The retry happens at most once, so a persistent `401` does not become a loop. `[RT-AC2]`

### 3.3 Concurrent refresh

Where several API calls hold an expired access token at the same time, exactly one refresh
call is made. `[RT-AC3]`

The remaining calls wait for its result and are replayed with the new token. No call fails
because of the rotation race. `[RT-AC3]`

### 3.4 Invalid refresh tokens

Where the refresh token has expired or was revoked, the response is `401` with `type`
`.../errors/refresh-token-invalid`. `[RT-AC4]`

The client clears local state and returns the customer to the login screen, preserving the
page they were on so that US-2.1 LI-AC2 can return them to it. `[RT-AC4]`

Where a token with an invalid signature or an altered payload is presented, the response
is `401` carrying no detail about why validation failed, and revealing nothing about the
algorithm, key or token structure. An `audit_events` entry is written. `[RT-AC6]`

### 3.5 Reuse detection

Where a refresh token that has already been used and rotated is presented again, the
response is `401`. `[RT-AC5]`

Every token in that family is revoked immediately, and all of the customer's sessions end
as potentially compromised. `[RT-AC5]`

The customer is emailed about the suspicious activity, and an `audit_events` entry of
severity `SECURITY` records the IP addresses of both requests. `[RT-AC5]`

### 3.6 Account state

Where the account is deactivated while a session is live, the next refresh responds `403`
with `type` `.../errors/account-deactivated`, and the customer is returned to the login
screen with that reason. `[RT-AC7]`

Access therefore survives deactivation by no more than the access token's 15-minute time
to live. `[RT-AC7]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/auth/refresh` | Path and method named by the criteria; request body not specified | `[RT-AC1]` |
| 2 | access token (response) | Named as "new"; TTL of 15 minutes implied by RT-AC7 | `[RT-AC1]` `[RT-AC7]` |
| 3 | refresh token (response) | Rotated on every success; transport not specified in this story | `[RT-AC1]` |
| 4 | retired token state | A state distinct from revoked and from absent; RT-AC5 requires it to remain recognisable after rotation | `[RT-AC1]` `[RT-AC5]` |
| 5 | token family | The unit that reuse detection revokes; its key is not specified | `[RT-AC5]` |
| 6 | refresh token TTL | As issued by US-2.1 LI-AC8 — 30 days with remember-me, otherwise the browser session | `[RT-AC4]` |
| 7 | `ProblemDetail.type` | Slugs `refresh-token-invalid`, `account-deactivated` | `[RT-AC4]` `[RT-AC7]` |
| 8 | `audit_events` severity | Value `SECURITY`; the severity scale is not otherwise defined | `[RT-AC5]` |
| 9 | recorded IPs | Both the rotating request's and the replaying request's | `[RT-AC5]` |
| 10 | Notification email | Sent on reuse detection; content not specified | `[RT-AC5]` |

## 5. Out of Scope

- Initial credential exchange — US-2.1.
- Explicit revocation by the customer — US-2.2 and US-2.3.
- Permission changes taking effect. RT-AC7 states the 15-minute bound for deactivation;
  the equivalent bound for role changes belongs to US-3.5.
- Removal of expired token rows. The story's Data Model Notes describe a daily cleanup
  job; no criterion reaches it — see OQ-5.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Only a SHA-256 hash of the refresh token is persisted, compared in constant time. | Named in the story's Non-Functional section. No criterion constrains storage, so §3 cannot state it normatively. |
| A-2 | The rotated refresh token is returned by the same transport US-2.1 uses — an `HttpOnly` cookie on `Path=/api/v1/auth`. | RT-AC1 says "rotated refresh token" without naming a transport. |
| A-3 | RT-AC2 and RT-AC3 describe client-side behaviour and are verified in the client, not the API. | Both criteria name "the client" and "a page"; neither is observable at the endpoint. |
| A-4 | "All of the customer's sessions end" in RT-AC5 means every family, not only the family containing the reused token. | RT-AC5 says both "every token in the family" and "all of the customer's sessions". The two differ whenever more than one family exists. |
| A-5 | A refresh call presenting no token at all is treated as RT-AC4. | No criterion covers an absent token; §3.4 would otherwise have a hole. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | No criterion in this story states the refresh token's lifetime. US-2.1 LI-AC8 gives 30 days when `rememberMe` is true and a session cookie when false, and US-2.1 OQ-1 already flags the default as contradictory. RT-AC4 depends on an expiry that is defined nowhere. | RT-AC4 |
| OQ-2 | RT-AC5 requires both "every token in the family is revoked" and "all of the customer's sessions end". Where a customer has several families — one per device, per US-2.3 A-1 — do sessions on unrelated devices also end? The two clauses give different answers. | RT-AC5 |
| OQ-3 | RT-AC5 introduces an `audit_events` severity of `SECURITY`. No criterion in any story defines the severity scale or what other values exist. | RT-AC5 |
| OQ-4 | The story's Non-Functional section limits refreshes to 10 per minute per account. No criterion states this, nor what the client should do on a `429` from an endpoint it calls automatically. | — |
| OQ-5 | The Data Model Notes describe a daily job removing expired rows. Nothing states the retention period, or whether removing a row weakens the reuse detection in RT-AC5 — a retired token whose row is gone can no longer be recognised as reused. | RT-AC5 |
| OQ-6 | The story's Non-Functional section states p95 ≤ 200 ms. No criterion asserts a latency bound, although RT-AC2 makes this endpoint's latency payable twice per replayed request. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| RT-AC1 | A valid refresh rotates the pair and retires the old token | §3.1, §4 | Covered |
| RT-AC2 | The original request is replayed once after a refresh | §3.2 | Covered |
| RT-AC3 | Concurrent callers share exactly one refresh | §3.3 | Covered |
| RT-AC4 | An expired or revoked token yields 401 and returns to sign-in | §3.4, §4 | **Partial** — the expiry the criterion asserts is defined nowhere (OQ-1) |
| RT-AC5 | Reuse revokes the family and raises a security event | §3.5, §4 | **Partial** — the criterion contradicts itself on scope (OQ-2) and relies on an undefined severity scale (OQ-3) |
| RT-AC6 | A forged token yields an uninformative 401 | §3.4, §4 | Covered |
| RT-AC7 | Deactivation takes effect at the next refresh | §3.6, §4 | Covered |

**Coverage:** 5 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.4-refresh-token.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
