# Epic 5 — Notifications: Preferences

**Story ID:** US-5.4
**Project:** Customer Portal
**AC prefix:** `NP-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As a signed-in customer,
I want to choose which events reach me and through which channel,
So that I keep the notifications that matter instead of turning everything off in frustration.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Granularity | Per event type and per channel, never one global switch | A single switch means the customer's only defence against noise is losing everything |
| 2 | Security class | Cannot be disabled on any channel | These are the notifications a compromised account's owner needs most |
| 3 | Transactional class | Opt-out | The customer asked for the underlying thing; telling them about it is expected |
| 4 | Informational class | Opt-in | Consent rules treat these as marketing regardless of what we call them |
| 5 | Propagation | A change takes effect within 60 seconds | Long enough to cache, short enough that the customer believes the switch worked |

## In Scope
- `GET` and `PUT /api/v1/notifications/preferences` — the event-by-channel matrix
- Enforcement of the matrix at delivery time
- Protection of the security class
- Defaults for event types introduced after the customer last visited

## Out of Scope
- Unsubscribing from an email without signing in (US-5.6)
- Digest and quiet-window scheduling (US-5.7)
- Channels beyond in-app and email — see Open Questions

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/notifications/preferences` | Bearer | — | `200` `{"groups": [{"class", "eventTypes": [{"key", "label", "inApp", "email", "locked", "isNew"}]}]}` |
| PUT | `/api/v1/notifications/preferences` | Bearer | `{"preferences": [{"eventType", "channel", "enabled"}]}` | `200` same shape |

## Data Model Notes
- `notification_preferences`: `recipientId`, `eventType`, `channel`, `enabled`, unique on the first three
- Absence of a row means "use the class default", so a new event type needs no backfill across every existing customer
- `locked` is derived from the notification class in code and is never stored per customer
- Preference changes are written to `audit_events`, because they explain why a customer did not receive something

## Acceptance Criteria

### Happy path
**NP-AC1 — The preference matrix**
```gherkin
Given a signed-in customer opens notification settings
When GET /api/v1/notifications/preferences is called
Then event types are returned grouped by notification class, each with a short explanation
And each event type shows its current state for the in-app and email channels
And types belonging to the security class are marked locked
```

**NP-AC2 — A change takes effect**
```gherkin
Given the customer disables the email channel for "ticket reply"
When an agent replies to their ticket
Then the in-app notification is still created
But no email is sent
And the change is in force within 60 seconds of being saved
```

### Protected classes
**NP-AC3 — Security notifications cannot be disabled**
```gherkin
Given the event type "password changed" belongs to the security class
When the settings page renders
Then its switches are disabled with the explanation "Security notifications cannot be turned off"
When PUT is called directly attempting to disable it
Then respond 422 with type ".../errors/preference-locked"
And no preference row is written
```

### Failure handling
**NP-AC4 — Save fails**
```gherkin
Given the customer toggles a switch
And the request fails
Then the switch returns to its previous position
And the customer sees "We could not save that. Try again"
And no other switch on the page changes state
```

### Defaults for new event types
**NP-AC5 — An event type introduced later**
```gherkin
Given a new transactional event type is deployed
When the customer next opens their settings
Then it is enabled by default and flagged as new for 14 days
But a new informational event type is disabled by default
And neither requires a data migration against existing customers
```

### Validation
**NP-AC6 — Unknown event type or channel**
```gherkin
Given a PUT naming an event type or channel that the code does not declare
When it is submitted
Then respond 400 with type ".../errors/unknown-event-type"
And no part of the request is applied
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/preference-locked",
  "title": "Preference Locked",
  "status": 422,
  "detail": "Security notifications cannot be turned off.",
  "instance": "/api/v1/notifications/preferences",
  "eventType": "PASSWORD_CHANGED"
}
```
Error `type` slugs introduced by this story: `preference-locked`, `unknown-event-type`.

## Non-Functional / Security Requirements
- Preferences are consulted at delivery time by every channel. A channel that reads them only at startup will keep sending for the length of its cache, which breaks NP-AC2.
- The distinction between opt-in and opt-out follows the notification class, and the class is fixed in code. Making it configurable would let an informational message be reclassified into an unblockable one.
- Preference changes are audited. When a customer reports never receiving something, the log is what settles it.
- **Performance:** p95 ≤ 300 ms for both operations; the delivery-time lookup must be cached with a maximum staleness of 60 seconds.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NP-AC1 | Functional suite asserting grouping, labels and the locked flags | `[gate]` |
| NP-AC2 | Integration test asserting an in-app row exists and no mail was queued | `[gate]` |
| NP-AC3 | Functional test calling `PUT` directly against a locked type | `[gate]` |
| NP-AC4 | Client unit test with a stubbed failure | `[gate]` |
| NP-AC5 | Integration test adding a new event type without a migration | `[gate]` |
| NP-AC6 | Slice test rejecting an undeclared key | `[gate]` |
| Cache staleness | Integration test asserting a change is honoured within 60 s | `[gate]` |

## Open Questions
1. Should agents and administrators have their own preference matrix? They receive assignment and announcement notifications, and the class model was designed around customers.
2. Where do SMS and browser push sit in this matrix if they are added later? The schema supports another channel value, but the consent rules for SMS are stricter than for email.
3. Does disabling in-app **and** email for a transactional type need a confirmation? The customer is then unreachable for that event, which they may not realise.
