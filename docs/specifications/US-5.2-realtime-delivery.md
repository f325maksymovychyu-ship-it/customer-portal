---
story_id: US-5.2
title: "Epic 5 — Notifications: Real-Time Delivery"
source: docs/backlog/US-5.2-realtime-delivery.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Real-Time Delivery

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 for the unresolved module question, and OQ-1 below for the
> unapproved fan-out dependency this story additionally requires.

## 1. Story

> As a signed-in customer, I want to see new notifications without reloading the page,
> So that I can react while the work I was doing is still in front of me.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**ND-AC6 — Stream ends with the session**
```gherkin
Given a connected stream
When the customer signs out, or the session is revoked by US-2.3
Then the connection is closed by the server within 5 seconds
And a reconnection attempt with the revoked credentials responds 401
```

## 3. Functional Specification

### 3.1 Delivery while the page is open

Where the stream is connected and a notification is generated for the customer, a toast
carrying the notification title appears within 5 seconds and the unread counter increases.
`[ND-AC1]`

The toast dismisses itself after 6 seconds, or immediately when closed. `[ND-AC1]`

The notification is already present in the centre described by US-5.1. `[ND-AC1]`

### 3.2 Multiple tabs

Where the portal is open in two tabs of the same browser, exactly one stream connection
exists across both. `[ND-AC2]`

The unread counter updates in both tabs, while the toast is shown only in the tab the
customer is looking at. `[ND-AC2]`

Marking a notification read in one tab clears the highlight in the other. `[ND-AC2]`

### 3.3 Reconnection

Where the connection drops, retries follow delays of 1, 2, 4 and 8 seconds and never
exceed one attempt per 30 seconds. `[ND-AC3]`

The reconnection sends `lastEventId` so that missed notifications are replayed, and a
notification delivered twice is rendered once because its identifier de-duplicates it.
`[ND-AC3]`

Where the stream cannot be established for more than 60 seconds, the client polls the
notification endpoint every 60 seconds instead, the customer sees no error, and the client
returns to streaming once a connection succeeds again. `[ND-AC4]`

### 3.4 Bursts

Where 50 notifications are generated for one customer within 10 seconds, a single summary
toast reading "50 new notifications" is shown instead of 50 toasts. `[ND-AC5]`

All 50 are present in the notification centre, and the interface stays responsive
throughout. `[ND-AC5]`

### 3.5 Session binding

Where the customer signs out, or the session is revoked under US-2.3, the connection is
closed by the server within 5 seconds. `[ND-AC6]`

A reconnection attempt carrying the revoked credentials responds `401`. `[ND-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | stream endpoint | Required by every criterion; **no path or transport is named** — see OQ-2 | `[ND-AC1]` |
| 2 | `lastEventId` | Sent on reconnection to replay missed notifications | `[ND-AC3]` |
| 3 | notification identifier | Used to de-duplicate a repeated delivery | `[ND-AC3]` |
| 4 | delivered payload | Carries at least the notification title | `[ND-AC1]` |
| 5 | toast lifetime | 6 seconds, or until dismissed | `[ND-AC1]` |
| 6 | delivery latency bound | 5 seconds from generation | `[ND-AC1]` |
| 7 | reconnection schedule | 1, 2, 4, 8 seconds, capped at one attempt per 30 seconds | `[ND-AC3]` |
| 8 | fallback poll interval | 60 seconds, after 60 seconds of failure | `[ND-AC4]` |
| 9 | burst threshold | 50 notifications in 10 seconds produces one summary toast. **The general rule is not stated** — see OQ-3 | `[ND-AC5]` |
| 10 | connection closure bound | 5 seconds from session end | `[ND-AC6]` |
| 11 | Message strings | "50 new notifications" | `[ND-AC5]` |

## 5. Out of Scope

- The notification list and counter — US-5.1.
- Which events reach the customer at all — US-5.4.
- Browser push to a closed tab — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The transport is Server-Sent Events. | `lastEventId` in ND-AC3 is the SSE reconnection field, which strongly implies it, but no criterion names a transport. See OQ-2. |
| A-2 | "Exactly one stream connection across both tabs" is achieved by a leader election among tabs. | ND-AC2 states the outcome, not the mechanism. |
| A-3 | The stream carries titles and identifiers only, never full message bodies. | Named in the story's Non-Functional section, consistent with US-5.1 A-5. |
| A-4 | The rate limit of 100 events per minute per connection applies. | Named in the story's Non-Functional section. No criterion asserts it, though ND-AC5 shows bursts are expected. |
| A-5 | Notifications are fanned out to whichever application instance holds the customer's connection. | Implicit in every criterion once more than one instance runs. See OQ-1. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | **Escalation — `AGENTS.md` §7.5.** Delivering an event to the instance holding a given customer's connection needs a shared broker or Postgres `LISTEN`/`NOTIFY`. That is a new runtime dependency and is not approved. Without it every criterion here holds only on a single-instance deployment. | all |
| OQ-2 | No criterion names the stream's endpoint or transport. `lastEventId` implies Server-Sent Events, but a WebSocket implementation could satisfy every criterion while requiring different infrastructure and a different security review. | ND-AC1, ND-AC3 |
| OQ-3 | ND-AC5 defines burst behaviour at exactly 50 notifications in 10 seconds. What is the general rule — at what count or rate does coalescing begin, and does the summary toast link anywhere? | ND-AC5 |
| OQ-4 | ND-AC4 falls back to polling "the notification endpoint". Polling US-5.1's list endpoint every 60 seconds for every disconnected client is a materially different load profile, and no criterion bounds it. | ND-AC4 |
| OQ-5 | ND-AC1 requires delivery "within 5 seconds" of an agent's reply. Is that measured from the database commit, or from the notification row being created? US-5.5 NE-AC1 uses a 10-second bound for queueing an email from the same event. | ND-AC1 |
| OQ-6 | ND-AC2 requires read state to propagate between tabs. US-5.3 OQ-1 asks the reciprocal question for two devices and leaves it open. Is the intent that tabs converge instantly but devices do not? | ND-AC2 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| ND-AC1 | A notification appears as a toast within 5 seconds | §3.1, §4 | **Partial** — no transport or endpoint is specified (OQ-2) |
| ND-AC2 | One connection per browser; the counter updates in every tab | §3.2, §4 | Covered — see OQ-6 |
| ND-AC3 | Backoff, catch-up by `lastEventId`, and de-duplication | §3.3, §4 | **Partial** — `lastEventId` presumes a transport no criterion states (OQ-2) |
| ND-AC4 | After 60 seconds of failure, polling replaces streaming | §3.3, §4 | Covered — see OQ-4 |
| ND-AC5 | A burst produces one summary toast, not fifty | §3.4, §4 | Covered — the criterion now states the general coalescing rule |
| ND-AC6 | The stream dies with the session | §3.5, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.2-realtime-delivery.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
