# Epic 2 — Authentication: Password Reset (confirm)

**Story ID:** US-2.6
**Project:** Customer Portal
**AC prefix:** `PN-AC`
**Module:** `customer/`

## User Story
As a customer holding a reset link,
I want to set a new password through it,
So that I can sign in again and know that any access gained with the old password is gone.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Password policy | 12–128 characters, rejected if in a local top-10 000 list, rejected if it contains the email or name | Length carries most of the strength; the deny-list stops the predictable remainder |
| 2 | Effect on sessions | Every session and refresh-token family is revoked | If the reset was recovery from a compromise, leaving sessions alive defeats it |
| 3 | Deny-list checking | Local list, never a third-party API | Sending a candidate password off-box is itself a disclosure |
| 4 | Token consumption | Single use, marked consumed inside the same transaction as the password write | A retry must not be able to set the password twice |
| 5 | Notification | The customer is emailed that the password changed | The one signal that reaches a victim whose account was taken over |

## In Scope
- `POST /api/v1/auth/password-reset/confirm` — validate the token and set the new password
- Password policy enforcement, server-side and mirrored in the client
- Global session revocation on success
- The "password changed" notification

## Out of Scope
- Issuing the token (US-2.5)
- Invitation acceptance for newly created accounts (US-3.2 reuses this endpoint but owns its own token lifetime)
- Password expiry or rotation policy — not in Release 1.0

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/password-reset/confirm` | None (token in body) | `{"token": str, "newPassword": str}` | `204` |

## Data Model Notes
- Writing `customers.password_hash`, setting `password_reset_tokens.consumed_at` and revoking `refresh_tokens` all happen in one `@Transactional` application-service call
- `password_hash` holds an Argon2id digest; no column ever holds the plaintext, which is already asserted by CP-101 AC-5
- `audit_events.event` gains `PASSWORD_CHANGED`

## Acceptance Criteria

### Happy path
**PN-AC1 — Password changed**
```gherkin
Given a valid, unconsumed reset token
When POST /api/v1/auth/password-reset/confirm is called with a password meeting the policy
Then respond 204
And the password hash is replaced and the token is marked consumed
And every session and refresh-token family for that customer is revoked
And an email confirming the change, with time and source IP, is queued
And an audit_events entry is written with event PASSWORD_CHANGED
```

### Token failures
**PN-AC2 — Expired or already consumed token**
```gherkin
Given a reset token issued more than 30 minutes ago
When POST /api/v1/auth/password-reset/confirm is called with it
Then respond 410 with type ".../errors/reset-token-expired"
And the client shows "This link is no longer valid" and offers to send a new one
And the same response is returned for a token that was already consumed
And no password field is presented to the caller
```

**PN-AC3 — Unknown or tampered token**
```gherkin
Given a token that matches no stored hash
When POST /api/v1/auth/password-reset/confirm is called
Then respond 410 with the same body as PN-AC2
And comparison against stored hashes is constant-time
And the response does not reveal whether the token ever existed
```

### Policy failures
**PN-AC4 — Password rejected by policy**
```gherkin
Given a valid reset token
When the new password is shorter than 12 characters
Then respond 400 with type ".../errors/password-rejected" naming the violated rule
When the new password appears in the common-password list
Then respond 400 with detail "That password is too common. Choose another"
When the new password contains the customer's email or name
Then respond 400 with detail "The password must not contain your personal details"
And in every case the token remains unconsumed so the customer can retry
```

**PN-AC5 — Reusing the current password**
```gherkin
Given a valid reset token
When the new password matches the password currently on the account
Then respond 400 with detail "The new password must differ from your previous one"
And the token remains unconsumed
```

### Client behaviour
**PN-AC6 — Strength meter and password managers**
```gherkin
Given the customer is typing into the new-password field
Then a strength indicator updates locally with no request to the server
When the value is pasted from a password manager
Then the paste is accepted and validated exactly as typed input is
And the field carries autocomplete="new-password"
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/password-rejected",
  "title": "Password Rejected",
  "status": 400,
  "detail": "The password must be at least 12 characters long.",
  "instance": "/api/v1/auth/password-reset/confirm",
  "violations": ["MIN_LENGTH"]
}
```
Error `type` slugs introduced by this story: `reset-token-expired`, `password-rejected`.

## Non-Functional / Security Requirements
- Global session revocation is a **requirement**, not an implementation side effect. A build that changes the password while leaving sessions alive fails this story.
- The common-password check runs against a locally shipped list; no candidate password leaves the process.
- The confirm page is served `noindex` and must not leak the token through the `Referer` header.
- Client-side policy checks are a convenience. Every rule is enforced again server-side, and PN-AC4 must pass when the client is bypassed entirely.
- **Performance:** p95 ≤ 500 ms, including Argon2id hashing of the new password.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| PN-AC1 | Functional suite asserting the response, the revocation and the queued mail | `[gate]` |
| PN-AC2, PN-AC3 | Functional test covering expired, consumed and unknown tokens | `[gate]` |
| PN-AC4 | Parameterised slice test, one case per policy rule | `[gate]` |
| PN-AC5 | Integration test comparing against the stored hash | `[gate]` |
| PN-AC6 | Client unit test plus a manual pass on a real password manager | `[manual]` |
| Transactional consumption | Integration test forcing a failure after the password write | `[gate]` |

## Open Questions
1. Which common-password list, at what size, and refreshed how often? The list is a shipped asset with a maintenance owner, and nobody has been named.
2. Should the invitation flow in US-3.2 reuse this endpoint with a 72-hour token, or have its own? Sharing the endpoint means one token lifetime column serving two very different risk profiles.
3. **Escalation — `AGENTS.md` §7.1.** The password policy in Decision 1 is a security-scheme choice and needs human sign-off before it is treated as agreed.
