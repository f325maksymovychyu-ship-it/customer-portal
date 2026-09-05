# Epic 5 — Notifications: Email Delivery

**Story ID:** US-5.5
**Project:** Customer Portal
**AC prefix:** `NE-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As a customer,
I want the portal to email me about things that matter,
So that I find out even when I am not signed in.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Delivery | Asynchronous, through a queue | A slow provider must never hold an HTTP request open or fail the operation that caused it |
| 2 | Retry policy | Three attempts at 1, 5 and 15 minutes, then a dead-letter queue | Covers a transient outage without turning one into an unbounded backlog |
| 3 | Hard bounce | Disables the email channel for that recipient | Continuing to send at a dead address damages the sending domain's reputation |
| 4 | Address at send time | The confirmed address as of dispatch, not as of queueing | A change made in between should be honoured, but only once confirmed |
| 5 | Circuit breaker | 500 emails per recipient per hour | No legitimate flow reaches this; hitting it means a loop |

## In Scope
- Rendering and queueing an email for an eligible notification
- Retry, dead-lettering and bounce handling
- Localisation and time-zone formatting
- Delivery-domain authentication requirements

## Out of Scope
- Which events qualify (US-5.4) and unsubscribing (US-5.6)
- Grouping and digests (US-5.7)
- Inbound email — the portal never receives replies by mail

## API Contract
No public endpoint. This story defines an outbound port consumed by the notification dispatcher.

| Port | Operation | Contract |
|---|---|---|
| `EmailSenderPort` | `send(EmailMessage)` | Idempotent on `notificationId`; returns `Queued`, `Rejected(reason)` or `Failed(retryable)` as a sealed result (`AGENTS.md` §4.2) |

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/webhooks/email-events` | Provider signature | Receives delivery and bounce callbacks |

## Data Model Notes
- `email_deliveries`: `notificationId`, `recipientAddress`, `status`, `attemptCount`, `lastAttemptAt`, `providerMessageId`, `failureReason`
- `status` moves `QUEUED` → `SENT` → `DELIVERED` \| `BOUNCED` \| `FAILED`
- `customers.email_delivery_blocked` records a hard bounce and is what NE-AC4 sets
- The queue row is written in the same transaction as the notification (transactional outbox), so an email is never lost to a crash between the two

## Acceptance Criteria

### Happy path
**NE-AC1 — An email is sent for an eligible event**
```gherkin
Given the email channel is enabled for the event type
When the notification is created
Then an email is queued within 10 seconds
And the subject carries the related object reference, such as "Ticket #10425: new reply"
And the body summarises the event and links directly to the relevant page
And following that link while signed out leads to sign-in and then to the intended page
```

**NE-AC2 — Language and time zone**
```gherkin
Given the customer's profile specifies a language and time zone
When an email is rendered
Then its text uses that language
And every timestamp is shown in that time zone with the zone named explicitly
And both a plain-text and an HTML part are present
```

### Failure handling
**NE-AC3 — Provider unavailable**
```gherkin
Given the mail provider is not responding
When delivery is attempted
Then three attempts are made at 1, 5 and 15 minute intervals
And after the third failure the message moves to the dead-letter queue and raises a monitoring alert
And the in-app notification was delivered regardless, because the channels are independent
```

**NE-AC4 — Hard bounce**
```gherkin
Given the provider reports a permanent delivery failure for the address
When the webhook is processed
Then the email channel is disabled for that recipient
And on their next sign-in they see "We cannot deliver email to your address. Check it in settings"
And an administrator sees the delivery problem on the customer record
But in-app notifications continue unaffected
```

### Edge cases
**NE-AC5 — Address changed while queued**
```gherkin
Given an email is queued
And the customer's address is changed and confirmed before dispatch
When the message is sent
Then it goes to the newly confirmed address
But if the new address is still unconfirmed, the previous confirmed address is used
```

**NE-AC6 — Runaway sending**
```gherkin
Given more than 500 emails have been queued for one recipient within an hour
When another is queued
Then it is discarded and an alert is raised naming the originating event type
And the incident is recorded, because this can only be a defect in a producing story
```

## Error Envelope (RFC 9457 `ProblemDetail`)
The webhook responds `202` on success and `401` to an invalid provider signature, using the standard envelope. No new customer-facing `type` slug is introduced.

## Non-Functional / Security Requirements
- The sending domain must publish SPF, DKIM and DMARC records. Without them the mail lands in spam and the story's premise — that the customer finds out — is false regardless of test results.
- The subject line carries no personal data beyond an object reference. Subjects are visible on lock screens and in notification previews.
- Links in emails contain no authentication token. Access is granted only by signing in, so a forwarded email grants nothing.
- The webhook verifies the provider's signature before parsing the payload; an unsigned callback is rejected without side effects.
- **Performance:** p95 from notification creation to provider acceptance ≤ 30 s.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NE-AC1 | Integration test with a stubbed `EmailSenderPort`, asserting subject and link | `[gate]` |
| NE-AC2 | Parameterised test across locales and time zones | `[gate]` |
| NE-AC3 | Integration test with the port failing, driven by an injected `Clock` | `[gate]` |
| NE-AC4 | Integration test posting a signed bounce callback | `[gate]` |
| NE-AC5 | Integration test changing the address between queue and dispatch | `[gate]` |
| NE-AC6 | Integration test exceeding the hourly ceiling | `[gate]` |
| No token in links | Contract test asserting rendered links carry no credential parameter | `[gate]` |
| SPF, DKIM, DMARC | Deployment checklist verified against the live domain | `[manual]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** An outbound mail provider and a queue are both new runtime dependencies. Every story that promises an email — US-2.1, US-2.5, US-2.6, US-3.2, US-4.1, US-4.6 — is blocked behind this approval.
2. Which provider, and does the choice constrain the webhook contract in NE-AC4? Bounce payloads are not standardised between providers.
3. Is a customer's language stored on their profile at all? NE-AC2 assumes it, and no story in this backlog creates the field.
