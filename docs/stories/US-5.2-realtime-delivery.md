# Epic 5 — Notifications: Real-Time Delivery

**Story ID:** US-5.2
**Project:** Customer Portal
**AC prefix:** `ND-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As a signed-in customer,
I want new notifications to appear without reloading the page,
So that I can react while the work I was doing is still in front of me.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Transport | Server-Sent Events | Delivery is one-directional; a full duplex socket buys nothing and costs infrastructure |
| 2 | Connections | One per browser, shared across tabs by a leader election | Per-tab connections multiply server load by however many tabs a user keeps open |
| 3 | Reconnection | Exponential backoff 1, 2, 4, 8 s, capped at 30 s | Without a cap, a brief outage becomes a synchronised retry storm on recovery |
| 4 | Fallback | Polling every 60 s after 60 s of failed reconnection | The feature degrades in latency, never into an error message |
| 5 | Burst handling | Above 10 events in 10 s, show one summary toast | Fifty stacked toasts are an outage of the interface, not a notification |

## In Scope
- `GET /api/v1/notifications/stream` — the event stream
- Leader election and cross-tab fan-out on the client
- Reconnection, catch-up and de-duplication
- Burst coalescing and the polling fallback

## Out of Scope
- The notification list and counter (US-5.1)
- Which events reach the customer at all (US-5.4)
- Browser push to a closed tab — out of Release 1.0, see Open Questions

## API Contract
| Method | Path | Auth | Query | Success |
|---|---|---|---|---|
| GET | `/api/v1/notifications/stream` | Bearer | `lastEventId` | `200` `text/event-stream` |

Each event carries the notification identifier as its SSE `id`, which is what `lastEventId` replays from.

## Data Model Notes
- No new tables. The stream reads the same rows US-5.1 writes.
- Catch-up after a reconnection is a query for rows newer than `lastEventId`, which works because the identifier is a UUIDv7 and therefore ordered
- The stream is bound to the session; ending it (US-2.2, US-2.3) must close the connection

## Acceptance Criteria

### Happy path
**ND-AC1 — A notification arrives while the page is open**
```gherkin
Given the portal is open and the stream is connected
When an agent replies to the customer's ticket
Then within 5 seconds a toast appears carrying the notification title
And the unread counter increases
And the toast dismisses itself after 6 seconds, or immediately when closed
And the notification is already present in the centre from US-5.1
```

**ND-AC2 — Several tabs open**
```gherkin
Given the portal is open in two tabs of the same browser
When a notification arrives
Then exactly one stream connection exists across both tabs
And the unread counter updates in both
But the toast is shown only in the tab the customer is looking at
And marking it read in one tab clears the highlight in the other
```

### Connection failure
**ND-AC3 — Reconnection and catch-up**
```gherkin
Given the stream connection drops
When the client reconnects
Then retries follow 1, 2, 4 and 8 second delays and never exceed one attempt per 30 seconds
And the reconnection sends lastEventId so missed notifications are replayed
And a notification delivered twice is rendered once, because the identifier de-duplicates it
```

**ND-AC4 — Prolonged unavailability**
```gherkin
Given the stream cannot be established for more than 60 seconds
When the client gives up on reconnecting
Then it polls the notification endpoint every 60 seconds instead
And the customer sees no error, only a longer delay
And the client returns to streaming once a connection succeeds again
```

### Load
**ND-AC5 — Burst of events**
```gherkin
Given 50 notifications are generated for one customer within 10 seconds
When they are delivered
Then a single summary toast reading "50 new notifications" is shown instead of 50 toasts
And all 50 are present in the notification centre
And the interface stays responsive throughout
And the rule is: more than 3 notifications arriving within any 10-second window are coalesced into one counted toast
And the summary toast opens the notification centre when clicked
```

### Session binding
**ND-AC6 — Stream ends with the session**
```gherkin
Given a connected stream
When the customer signs out, or the session is revoked by US-2.3
Then the connection is closed by the server within 5 seconds
And a reconnection attempt with the revoked credentials responds 401
```

## Error Envelope (RFC 9457 `ProblemDetail`)
Stream establishment failures use the standard envelope; no new `type` slug is introduced. Mid-stream errors are reported as an SSE `error` event, not as an HTTP status, because the response has already begun.

## Non-Functional / Security Requirements
- One connection per browser is a requirement, not an optimisation. Ten tabs must not produce ten connections, or a modest user population exhausts the server's connection budget.
- The stream carries titles and identifiers only, never full message bodies, matching US-5.1.
- Rate limit of 100 events per minute per connection. Exceeding it indicates a defect in a producing story and must raise an alert rather than being silently dropped.
- Virtual threads make a connection-per-client model affordable (`AGENTS.md` §4.4), but the connections must not be pooled.
- **Performance:** p95 end-to-end latency from event creation to client render ≤ 2 s.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| ND-AC1 | Integration test consuming the stream and asserting delivery latency | `[gate]` |
| ND-AC2 | Client unit test on leader election, plus a two-tab end-to-end test | `[gate]` |
| ND-AC3 | Client unit test on the backoff schedule and on de-duplication by identifier | `[gate]` |
| ND-AC4 | Client unit test asserting the switch to polling and back | `[gate]` |
| ND-AC5 | Integration test emitting 50 events, asserting one toast and 50 rows | `[gate]` |
| ND-AC6 | Integration test revoking the session and asserting closure within 5 s | `[gate]` |
| Connection count | Load scenario in `perf/` asserting connections scale with browsers, not tabs | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** Fanning events out across more than one application instance needs a shared broker or Postgres `LISTEN/NOTIFY`. Either is a new runtime dependency and must be approved before this story is estimated.
2. Should browser push notifications reach a customer whose tab is closed? That adds a service worker, a push provider and a permission prompt, and belongs in its own story.
3. Is 2 s end-to-end the right target, or would 10 s be acceptable and much cheaper? The number drives the transport decision more than any other requirement here.
