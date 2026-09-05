---
story_id: US-2.5
title: "Epic 2 — Authentication: Password Reset (request)"
source: docs/backlog/US-2.5-password-reset-request.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Password Reset (request)

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a customer who has forgotten their password, I want to request a reset link by email,
> So that I can recover access myself instead of opening a support ticket.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**PR-AC1 — Reset link issued**
```gherkin
Given an active customer with the address "olena@example.com"
When POST /api/v1/auth/password-reset is called with that address
Then respond 202 with the neutral message
And a single-use reset token valid for 30 minutes is created
And an email carrying the reset link is queued for delivery
And an audit_events entry is written with event PASSWORD_RESET_REQUESTED
```

**PR-AC2 — Unknown or deactivated account**
```gherkin
Given the address is not registered
When POST /api/v1/auth/password-reset is called
Then respond 202 with exactly the same body and status as PR-AC1
And no token is created and no email is sent
And the same holds when the address belongs to a DEACTIVATED account
```

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

## 3. Functional Specification

### 3.1 Issuing a reset link

Where the supplied address belongs to an active customer, the response is `202` carrying
the neutral message, and a single-use reset token valid for 30 minutes is created.
`[PR-AC1]`

An email carrying the reset link is queued for delivery. `[PR-AC1]`

An `audit_events` entry with event `PASSWORD_RESET_REQUESTED` is written. `[PR-AC1]`

### 3.2 Uniform response

Where the address is not registered, the response carries exactly the same body and status
as the success path, and no token is created and no email is sent. `[PR-AC2]`

The same holds where the address belongs to a `DEACTIVATED` account. `[PR-AC2]`

PR-AC2 constrains the body and the status. It does not constrain response time — see
OQ-1.

### 3.3 Superseding an earlier link

Where a reset link has already been issued and is still valid, requesting another
invalidates the earlier token, so that only the most recently emailed link can be used.
`[PR-AC4]`

### 3.4 Rate limiting

Where three requests have been made for one address within the last hour, a fourth
receives `429` with a `Retry-After` header and `type` `.../errors/too-many-attempts`, and
no email is sent. `[PR-AC3]`

An independent limit of 10 requests per hour applies per source IP address. `[PR-AC3]`

### 3.5 Request validation

Where the request body's `email` is absent or is not a valid address, the response is
`400` with `type` `.../errors/validation-failed`. `[PR-AC5]`

An attempt rejected by validation is not counted against the per-address rate limit.
`[PR-AC5]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/auth/password-reset` | Path and method named by the criteria | `[PR-AC1]` |
| 2 | `email` (request) | Required, and must be a valid address; no length bound stated | `[PR-AC5]` |
| 3 | Success status | `202` on both the known and unknown paths | `[PR-AC1]` `[PR-AC2]` |
| 4 | "the neutral message" | **not specified** — no criterion gives its text | `[PR-AC1]` `[PR-AC2]` |
| 5 | reset token | Single use, valid 30 minutes; format and storage not specified | `[PR-AC1]` |
| 6 | reset link | Carried in the email; its URL shape and target are not specified | `[PR-AC1]` |
| 7 | token invalidation | An earlier token becomes unusable when a newer one is issued | `[PR-AC4]` |
| 8 | `audit_events.event` | Value `PASSWORD_RESET_REQUESTED` | `[PR-AC1]` |
| 9 | `ProblemDetail.type` | Slugs `too-many-attempts`, `validation-failed` | `[PR-AC3]` `[PR-AC5]` |
| 10 | Rate-limit windows | 3 per address per hour; 10 per source IP per hour | `[PR-AC3]` |
| 11 | `Retry-After` header | Present on the `429`; value not specified | `[PR-AC3]` |

## 5. Out of Scope

- Consuming the token and setting the new password — US-2.6.
- Password policy — US-2.6.
- Changing a password while signed in — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The reset token is a 256-bit random value stored as a SHA-256 hash. | Named in the story's Non-Functional section. No criterion constrains the token's construction. |
| A-2 | Delivery is asynchronous through a queue, so a mail failure does not change the HTTP response. | PR-AC1 says the email is "queued", which implies but does not state the decoupling. |
| A-3 | The neutral message is the one quoted in the story's API Contract: "If that account exists, we have sent instructions." | PR-AC1 and PR-AC2 both refer to "the neutral message" without giving its text; §3 cannot be tested without one. |
| A-4 | The `429` of PR-AC3 does not itself reveal account existence, since the per-address limit applies whether or not the address is registered. | PR-AC3 does not say which addresses are counted. Under the opposite reading, the rate limiter becomes the enumeration oracle PR-AC2 exists to prevent. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | PR-AC2 requires the same body and status for a registered and an unregistered address, but says nothing about timing. The equivalent criterion in US-2.1 (LI-AC4) does require comparable timing. Is the omission here deliberate, or does this endpoint also need to be timing-safe? Issuing a token and queueing mail is measurably slower than doing nothing. | PR-AC2 |
| OQ-2 | Neither PR-AC1 nor PR-AC2 gives the text of "the neutral message", though both depend on it being identical. What is it? | PR-AC1, PR-AC2 |
| OQ-3 | Is the per-address counter in PR-AC3 keyed on addresses that exist, or on every address submitted? A-4 assumes the latter; the opposite reading reintroduces enumeration. | PR-AC3 |
| OQ-4 | No criterion states what the reset link points at, or how the token travels in it — path segment, query parameter, or fragment. A query parameter would place the token in server logs and `Referer` headers. | PR-AC1 |
| OQ-5 | PR-AC1 says the email is "queued". No criterion states what happens when the queue or the mail provider is unavailable, or whether the customer is ever told delivery failed. | PR-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| PR-AC1 | A registered address receives a 30-minute single-use link | §3.1, §4 | **Partial** — the neutral message the criterion asserts has no text (OQ-2) |
| PR-AC2 | An unknown or deactivated address gets an identical response | §3.2, §4 | **Partial** — same missing text, and timing is unconstrained (OQ-1, OQ-2) |
| PR-AC3 | Per-address and per-IP rate limits | §3.4, §4 | Covered — see OQ-3 |
| PR-AC4 | A new request invalidates the previous link | §3.3, §4 | Covered |
| PR-AC5 | A malformed address yields 400 and is not counted | §3.5, §4 | Covered |

**Coverage:** 3 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.5-password-reset-request.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
