# Epic 5 — Notifications: One-Click Unsubscribe

**Story ID:** US-5.6
**Project:** Customer Portal
**AC prefix:** `NU-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As someone receiving email from the portal,
I want to unsubscribe directly from the message,
So that unwanted mail stops without my having to remember a password first.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Authentication | Not required | Major mailbox providers require one-click unsubscribe to work without a login |
| 2 | Token scope | One recipient and one event type, nothing else | An unauthenticated token must grant the smallest possible thing |
| 3 | Token lifetime | 90 days | Mail sits in inboxes for months; a short-lived token makes an old message's link useless |
| 4 | Security class | No unsubscribe link, and no `List-Unsubscribe` header | These messages are not unsubscribable, so offering the affordance would be a lie |
| 5 | Confirmation | A confirmation page for the link; none for the header-driven flow | The header flow is triggered deliberately from the mail client and must not need a second step |

## In Scope
- `GET` and `POST /api/v1/notifications/unsubscribe` — the token-driven opt-out
- `List-Unsubscribe` and `List-Unsubscribe-Post` headers on eligible mail
- Refusal for the security class
- Handling of invalid, tampered and expired tokens

## Out of Scope
- The authenticated preference matrix (US-5.4)
- Global suppression across every event type — see Open Questions
- Resubscribing without signing in

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/notifications/unsubscribe` | Token in query | — | `200` confirmation page describing what will stop |
| POST | `/api/v1/notifications/unsubscribe` | Token in body or query | `{"token": str}` | `200` `{"eventType", "channel": "EMAIL"}` |

## Data Model Notes
- The token is a signed value carrying `recipientId`, `eventType` and an expiry; nothing is stored server-side, so no table grows with every email sent
- Acting on it writes the same `notification_preferences` row that US-5.4 would write, so the two paths cannot diverge
- Every unsubscribe is recorded in `audit_events` with the method used, which is the evidence a provider or regulator asks for

## Acceptance Criteria

### Happy path
**NU-AC1 — Unsubscribing from the link**
```gherkin
Given an email for a transactional event carrying an unsubscribe link
When the recipient follows it with GET
Then a page names the event type the token carries and states that its emails will stop
And the GET changes no preference, so a mail scanner following the link unsubscribes nobody
When they confirm with POST
Then the email channel for that event type is disabled
And they see confirmation and a link to full settings
But signing in is not required at any point
And the page names no email address, so a forwarded link discloses nothing about the recipient
```

**NU-AC2 — Unsubscribing from the mail client**
```gherkin
Given an email of the transactional, administrative or informational class
Then it carries List-Unsubscribe and List-Unsubscribe-Post headers
When the recipient uses their mail client's own unsubscribe control
Then a POST is received and processed exactly as NU-AC1's confirmation would be
And no confirmation page is required, because the client already confirmed
```

### Protected classes
**NU-AC3 — Security email**
```gherkin
Given an email notifying a password change
Then it contains no unsubscribe link and no List-Unsubscribe header
When a token for a security event type is submitted directly
Then respond 422 with type ".../errors/preference-locked"
And no preference is changed
```

### Invalid tokens
**NU-AC4 — Tampered or expired token**
```gherkin
Given a token whose signature does not verify, or which is older than 90 days
When it is submitted
Then respond 404 with type ".../errors/unsubscribe-token-invalid"
And the page reads "This link is no longer valid" and offers settings after signing in
And no preference is changed
And the page names no address, no account and no event type
Because a token that does not verify proves nothing about who holds it
But a token that does verify proves the holder has the email, so NU-AC1 may name the event type it carries
```

### Idempotency
**NU-AC5 — Unsubscribing twice**
```gherkin
Given the recipient has already unsubscribed from this event type
And the token is still within its 90-day lifetime
When the same link is used again
Then respond 200 with the same confirmation
And nothing changes, because the operation is idempotent
But once the token passes 90 days NU-AC4 governs instead, and the response becomes 404
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/unsubscribe-token-invalid",
  "title": "Unsubscribe Link Invalid",
  "status": 404,
  "detail": "This link is no longer valid. Sign in to manage your notification settings.",
  "instance": "/api/v1/notifications/unsubscribe"
}
```
Error `type` slugs introduced by this story: `unsubscribe-token-invalid`. `preference-locked` is shared with US-5.4.

## Non-Functional / Security Requirements
- The token grants exactly one capability: disabling one channel for one event type for one recipient. It must not be accepted as authentication anywhere else in the system.
- `GET` must not change state. Mail clients and security scanners follow links automatically, and an unsubscribe that fires on prefetch is a bug that looks like a customer decision.
- The response must not disclose the recipient's address or confirm that an account exists, since anyone holding a forwarded email can reach this endpoint.
- Every unsubscribe is logged with time, source address, and whether it came from the link or the header.
- **Performance:** p95 ≤ 300 ms; this page is often the recipient's last interaction with the product and must not feel broken.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NU-AC1 | Functional suite driving the link with no session | `[gate]` |
| NU-AC2 | Contract test asserting both headers, plus a functional test on the POST flow | `[gate]` |
| NU-AC3 | Contract test asserting no header on security mail, plus a `422` test | `[gate]` |
| NU-AC4 | Security test with tampered, foreign and expired tokens | `[gate]` |
| NU-AC5 | Functional test submitting the same token twice | `[gate]` |
| `GET` is safe | Contract test asserting a `GET` changes no preference row | `[gate]` |

## Open Questions
1. Should the page offer "unsubscribe from everything in this class" as well as the single event type? It is friendlier, but it widens what an unauthenticated token can do.
2. How does a recipient resubscribe? Today only by signing in, which is defensible but leaves someone who unsubscribed by mistake with no route back from the page itself.
3. If the address belongs to a deactivated account, should the endpoint still succeed? Answering honestly leaks account state; NU-AC4's uniform response currently hides it.
