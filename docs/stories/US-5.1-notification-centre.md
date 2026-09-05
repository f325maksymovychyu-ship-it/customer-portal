# Epic 5 — Notifications: Notification Centre

**Story ID:** US-5.1
**Project:** Customer Portal
**AC prefix:** `NC-AC`
**Module:** `notification/` — **proposed; not in the canonical map in `AGENTS.md` §2.1**

## User Story
As a signed-in customer,
I want one place that shows everything the system has told me, with an unread count,
So that nothing important is missed and I am not searching my inbox for it.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Notification classes | `SECURITY`, `TRANSACTIONAL`, `ADMINISTRATIVE`, `INFORMATIONAL` | The class decides opt-out and grouping rights; see the table in `README.md` |
| 2 | Class assignment | Declared in code beside the event type, not editable at runtime | An editable class is how a security notification becomes unsubscribable |
| 3 | Retention | 90 days | The audit log (US-3.7) is the long-term record; this is a working inbox |
| 4 | Unread count | Capped at "99+" in the UI | An exact count above that informs nobody and costs a full scan |
| 5 | Dead references | Kept in the list, marked unavailable | Deleting them silently makes the customer doubt what they remember reading |

## In Scope
- `GET /api/v1/notifications` — the caller's own notifications, paginated
- The unread counter as a separate, cheap endpoint
- Navigation to the referenced object, with read-marking on follow
- Graceful handling of targets that no longer exist or are no longer permitted

## Out of Scope
- Real-time delivery (US-5.2), read-state bulk actions (US-5.3), preferences (US-5.4)
- Email delivery (US-5.5)
- Producing notifications — every producing story owns its own events

## API Contract
| Method | Path | Auth | Query | Success |
|---|---|---|---|---|
| GET | `/api/v1/notifications` | Bearer | `unreadOnly`, `page`, `size` | `200` `Page<NotificationSummary>` |
| GET | `/api/v1/notifications/unread-count` | Bearer | — | `200` `{"count": int, "capped": bool}` |
| GET | `/api/v1/notifications/{id}` | Bearer | — | `200` `NotificationDetail` |

## Data Model Notes
- `notifications`: `id` (UUIDv7), `recipientId`, `eventType`, `notificationClass`, `title`, `body`, `targetType`, `targetId`, `readAt`, `createdAt`
- The row stores a rendered title and body, not a template reference — a notification must still read correctly after the template changes
- Index on `(recipientId, createdAt DESC)` and a partial index on unread rows, which is what makes the counter cheap
- A daily job removes rows older than 90 days; the corresponding audit events (US-3.7) are unaffected

## Acceptance Criteria

### Happy path
**NC-AC1 — Listing notifications**
```gherkin
Given a customer with 12 notifications, 3 of them unread
When GET /api/v1/notifications is called
Then respond 200 with a page of 20, newest first
And each entry carries its event type, title, time and a reference to the related object
And unread entries are distinguishable from read ones
And GET /api/v1/notifications/unread-count returns 3
```

**NC-AC2 — Following a notification to its object**
```gherkin
Given an unread notification about a reply on ticket "#10425"
When the customer follows it
Then they arrive at that ticket
And the notification is marked read
And the unread count drops by one without a page reload
```

### Degraded targets
**NC-AC3 — The referenced object is gone or no longer permitted**
```gherkin
Given a notification whose target has been deleted, or which the customer may no longer read
When the customer follows it
Then they see "This item is no longer available" rather than an error page
And the notification remains in the list and is marked read
And the message does not disclose why access was lost
```

### Isolation
**NC-AC4 — Another customer's notification**
```gherkin
Given a notification identifier belonging to a different recipient
When GET /api/v1/notifications/{id} is called
Then respond 404 with type ".../errors/notification-not-found"
And no field of that notification appears in the response
```

### Bounds and empty state
**NC-AC5 — Empty list and a capped counter**
```gherkin
Given a customer with no notifications
When the list is requested
Then respond 200 with an empty page
And the client explains what will appear there rather than showing an empty table
Given more than 99 unread notifications
Then the counter endpoint returns capped=true
And the UI renders "99+"
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/notification-not-found",
  "title": "Notification Not Found",
  "status": 404,
  "detail": "No such notification exists.",
  "instance": "/api/v1/notifications/0193f2c1-0000-7000-8000-000000000000"
}
```
Error `type` slugs introduced by this story: `notification-not-found`.

## Non-Functional / Security Requirements
- **Performance:** p95 ≤ 400 ms for the list and ≤ 100 ms for the counter. The counter is requested on every page load and must not touch the main table without its partial index.
- The notification body carries a summary, never the full contents of the underlying message. A notification is a pointer, and it travels through channels the object itself does not.
- Recipient isolation is enforced by a repository predicate, matching US-4.2 TL-AC3.
- Retention deletion must not cascade into `audit_events`.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NC-AC1 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| NC-AC2 | Functional test asserting the read transition and the counter change | `[gate]` |
| NC-AC3 | Integration test with a deleted target and with a revoked permission | `[gate]` |
| NC-AC4 | Functional test with a second recipient | `[gate]` |
| NC-AC5 | Functional test on the empty case and above the cap | `[gate]` |
| Counter latency | Performance scenario against a seeded recipient with 100 000 notifications | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §2.1 and §7.5.** There is no `notification/` module in the canonical layer map, and the reserved modules are `catalog`, `ordering` and `support`. Whether this becomes a new bounded context or lives in `shared/` is the architect's decision and blocks the whole epic.
2. Is 90 days the right retention when the notification is sometimes the only evidence a customer was told something? The audit log holds the event, but not what the customer saw.
3. Should notifications exist for agents and administrators as well as customers, or does staff work from the queue in US-4.3? The data model supports both; the product decision has not been made.
