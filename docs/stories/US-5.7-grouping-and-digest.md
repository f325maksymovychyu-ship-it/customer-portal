# Epic 5 — Notifications: Grouping and Digest

**Story ID:** US-5.7
**Project:** Customer Portal
**AC prefix:** `NG-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As a customer with an active ticket,
I want related notifications bundled rather than delivered one by one,
So that a busy exchange does not flood me into turning notifications off altogether.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | In-app grouping | By target object, collapsed in the list | The customer thinks in terms of the ticket, not the individual events on it |
| 2 | Email quiet window | 15 minutes per target after a message is sent | Long enough to absorb a rapid exchange, short enough to stay timely |
| 3 | Security class | Never grouped, never delayed, never digested | Latency on a security notice is the one cost that is never acceptable |
| 4 | Digest schedule | 09:00 in the recipient's own time zone | A digest that arrives at 03:00 local time is not a digest |
| 5 | Empty digest | Not sent | A daily email saying nothing happened trains people to ignore the daily email |

## In Scope
- Collapsing in-app notifications by target object
- The per-target quiet window for email, and the summary that follows it
- The optional daily digest and its scheduling
- Exemption of the security class from all of the above

## Out of Scope
- Which events are eligible at all (US-5.4)
- Email transport and retries (US-5.5)
- Quiet hours by time of day — see Open Questions

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/notifications` | Bearer | — | `200` — entries may be groups; each carries `groupSize` and `targetId` (extends US-5.1) |
| PUT | `/api/v1/notifications/preferences/delivery-mode` | Bearer | `{"mode": "IMMEDIATE" \| "DAILY_DIGEST"}` | `200` `{"mode"}` |

## Data Model Notes
- Grouping is computed on read, over `(recipientId, targetId)` within a time window; nothing denormalised is stored, so a group cannot drift out of step with its members
- `notification_quiet_windows`: `recipientId`, `targetId`, `windowEndsAt` — the row that suppresses immediate email for that target
- `digest_schedules`: `recipientId`, `mode`, `nextRunAt`, computed from the profile time zone
- The digest job resolves `nextRunAt` per recipient rather than running once globally, because 09:00 is local

## Acceptance Criteria

### In-app grouping
**NG-AC1 — Events on one object are collapsed**
```gherkin
Given 5 events occurred on ticket "#10425" within 10 minutes
When the customer opens the notification centre
Then a single collapsed entry reads "5 updates on ticket #10425"
And expanding it lists each event with its own time
And the unread counter treats the group as one entry, not five
And the rule is: 3 or more unread notifications sharing one target within 60 minutes collapse into one group
But 2 notifications on one target, or 3 spread across more than 60 minutes, remain separate entries
```

### Email pacing
**NG-AC2 — Quiet window after a message**
```gherkin
Given an email about ticket "#10425" was sent less than 15 minutes ago
When another event occurs on the same ticket
Then no immediate email is sent
And when the window closes, one summary email covering the accumulated events is sent
And the in-app notifications are delivered immediately throughout, without waiting
```

**NG-AC3 — Security events bypass pacing**
```gherkin
Given a quiet window is open for a target
When an event of the security class occurs
Then its email is sent immediately and separately
And it is never folded into a summary email or a digest
```

### Digest
**NG-AC4 — Daily digest**
```gherkin
Given the customer chose the DAILY_DIGEST delivery mode
When 09:00 arrives in their own time zone
Then a single email is sent covering the previous day, grouped by object
But if nothing happened, no email is sent at all
And security notifications still arrive separately and immediately, whatever the mode
```

**NG-AC5 — Switching modes**
```gherkin
Given the customer switches from DAILY_DIGEST to IMMEDIATE
When events occur afterwards
Then they are delivered immediately, subject only to the quiet window in NG-AC2
And any events already accumulated for the pending digest are included in one final digest rather than discarded
```

### Job behaviour
**NG-AC6 — Digest job is idempotent**
```gherkin
Given the digest job runs twice for the same recipient and the same day
When the second run executes
Then no second email is sent
And running the job on more than one instance concurrently produces exactly one digest per recipient
```

## Error Envelope (RFC 9457 `ProblemDetail`)
Reuses `validation-failed` on an unknown delivery mode. This story introduces no new `type` slug.

## Non-Functional / Security Requirements
- Grouping must never merge notifications belonging to different recipients or different targets. The group key is `(recipientId, targetId)` and both parts are mandatory.
- The digest job must be idempotent and safe under concurrent execution; NG-AC6 is the assertion that makes this testable rather than aspirational.
- Timing rules are tested with an injected `Clock` or Awaitility; `Thread.sleep` is prohibited (`AGENTS.md` §5).
- The digest email is subject to the same subject-line and link rules as US-5.5, including carrying no authentication token.
- **Performance:** the digest run for 100 000 recipients completes within its hourly window without delaying transactional mail, which runs on a higher-priority queue.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NG-AC1 | Integration test seeding 5 events on one target | `[gate]` |
| NG-AC2 | Integration test with an injected `Clock` across the window boundary | `[gate]` |
| NG-AC3 | Integration test emitting a security event inside an open window | `[gate]` |
| NG-AC4 | Parameterised test across time zones, including the empty-day case | `[gate]` |
| NG-AC5 | Integration test switching modes with events pending | `[gate]` |
| NG-AC6 | Integration test running the job twice and concurrently | `[gate]` |
| Group key correctness | Unit test asserting no group spans recipients or targets | `[gate]` |

## Open Questions
1. Should quiet hours by time of day exist, so nothing but security mail arrives overnight? It is the most requested feature of this kind and is deliberately not in this story.
2. Is 15 minutes right for every event type, or should a high-priority ticket use a shorter window? A single global figure is simple but blunt.
3. What happens to the digest when a customer has no time zone on their profile? US-5.5 NE-AC2 has the same gap, and no story populates the field.
