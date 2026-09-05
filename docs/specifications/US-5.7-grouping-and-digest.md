---
story_id: US-5.7
title: "Epic 5 — Notifications: Grouping and Digest"
source: docs/backlog/US-5.7-grouping-and-digest.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Grouping and Digest

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 and US-5.5 OQ-1.

## 1. Story

> As a customer with an active ticket, I want related notifications bundled rather than
> delivered one by one, So that a busy exchange does not flood me into turning notifications
> off altogether.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**NG-AC6 — Digest job is idempotent**
```gherkin
Given the digest job runs twice for the same recipient and the same day
When the second run executes
Then no second email is sent
And running the job on more than one instance concurrently produces exactly one digest per recipient
```

## 3. Functional Specification

### 3.1 In-app grouping

Where several events occurred on one object within a short period, the notification centre
shows a single collapsed entry naming the count and the object. `[NG-AC1]`

Expanding it lists each event with its own time. `[NG-AC1]`

The unread counter treats the group as one entry rather than as its members. `[NG-AC1]`

The window over which events are collapsed is exercised at 10 minutes but is not stated as
a rule — see OQ-1.

### 3.2 Email pacing

Where an email about an object was sent less than 15 minutes ago, another event on the same
object sends no immediate email. `[NG-AC2]`

When the window closes, one summary email covering the accumulated events is sent.
`[NG-AC2]`

In-app notifications are delivered immediately throughout, without waiting. `[NG-AC2]`

Where an event of the security class occurs while a quiet window is open, its email is sent
immediately and separately, and is never folded into a summary email or a digest.
`[NG-AC3]`

### 3.3 The daily digest

Where the customer chose the `DAILY_DIGEST` delivery mode, a single email covering the
previous day, grouped by object, is sent when 09:00 arrives in their own time zone.
`[NG-AC4]`

Where nothing happened, no email is sent at all. `[NG-AC4]`

Security notifications arrive separately and immediately whatever the mode. `[NG-AC4]`

Where the customer switches from `DAILY_DIGEST` to `IMMEDIATE`, later events are delivered
immediately, subject only to the quiet window of §3.2, and any events already accumulated
for the pending digest are included in one final digest rather than discarded. `[NG-AC5]`

### 3.4 Job behaviour

Where the digest job runs twice for the same recipient and the same day, the second run
sends no second email. `[NG-AC6]`

Running the job on more than one instance concurrently produces exactly one digest per
recipient. `[NG-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | delivery mode | Values `DAILY_DIGEST` and `IMMEDIATE`; **no endpoint for changing it is named** — see OQ-2 | `[NG-AC4]` `[NG-AC5]` |
| 2 | collapsed group entry | Count plus object reference; expandable to its members | `[NG-AC1]` |
| 3 | group key | Implied to be the target object; **not stated by any criterion** | `[NG-AC1]` |
| 4 | grouping window | Exercised at 10 minutes; **not stated as a rule** | `[NG-AC1]` |
| 5 | quiet window | 15 minutes per target, from the last email sent | `[NG-AC2]` |
| 6 | summary email | Sent at window close, covering the accumulated events | `[NG-AC2]` |
| 7 | digest send time | 09:00 in the recipient's time zone | `[NG-AC4]` |
| 8 | customer time zone | **Read from the profile; no story creates this field** — see US-5.5 OQ-3 | `[NG-AC4]` |
| 9 | security-class exemption | Never paced, never grouped, never digested | `[NG-AC3]` `[NG-AC4]` |
| 10 | digest idempotency key | Recipient plus day; representation not specified | `[NG-AC6]` |
| 11 | Message strings | "5 updates on ticket #10425" shown by example | `[NG-AC1]` |

## 5. Out of Scope

- Which events are eligible at all — US-5.4.
- Email transport and retries — US-5.5.
- Quiet hours by time of day — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Grouping is computed on read over recipient and target, so a group cannot drift out of step with its members. | Named in the story's Data Model Notes. NG-AC1 states the display, not the storage. |
| A-2 | The group key is recipient plus target object. | NG-AC1 groups by ticket. Without both parts a group could span recipients. |
| A-3 | The digest job resolves its run time per recipient rather than firing once globally. | NG-AC4's 09:00 is local to each recipient, which a single global run cannot satisfy. |
| A-4 | Timing rules are tested with an injected `Clock` or Awaitility, not `Thread.sleep`, per `AGENTS.md` §5. | The windows in NG-AC2 and NG-AC4 are otherwise untestable in reasonable time. |
| A-5 | The digest email obeys the same subject and link rules as US-5.5. | No criterion here constrains the digest's content beyond its grouping. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | NG-AC1 collapses 5 events occurring within 10 minutes. Is 10 minutes the rule, or an example? At what count does collapsing begin — two events, or five? Two implementations could differ on every case except the one the criterion states. | NG-AC1 |
| OQ-2 | NG-AC4 and NG-AC5 depend on a delivery mode the customer "chose", but no criterion describes where it is set. US-5.4's preference matrix covers event types and channels, not cadence. | NG-AC4, NG-AC5 |
| OQ-3 | Is the 15-minute quiet window right for every event type, or should a high-priority ticket use a shorter one? A single global figure is simple but blunt, and no criterion permits variation. | NG-AC2 |
| OQ-4 | NG-AC5 sends "one final digest" on switching to `IMMEDIATE`. When — immediately on the switch, or at the next 09:00? The criterion does not say, and the two differ by up to a day. | NG-AC5 |
| OQ-5 | NG-AC4 sends the digest at 09:00 in the recipient's time zone, but no story creates a time-zone field. US-5.5 OQ-3 records the same dependency. What happens for a customer without one? | NG-AC4 |
| OQ-6 | NG-AC1 makes the unread counter treat a group as one entry. US-5.3 NR-AC1 marks notifications read individually and returns `updated=3` for three rows. Does marking a group read count as one update or as its members? The two stories disagree about what a unit is. | NG-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NG-AC1 | Events on one object collapse into a single counted entry | §3.1, §4 | Covered — the criterion now states the 3-in-60-minutes rule |
| NG-AC2 | A 15-minute quiet window replaces per-event email with a summary | §3.2, §4 | Covered — see OQ-3 |
| NG-AC3 | Security events ignore pacing entirely | §3.2, §4 | Covered |
| NG-AC4 | A daily digest arrives at 09:00 local, or not at all | §3.3, §4 | **Partial** — the mode has no interface and the time zone no source (OQ-2, OQ-5) |
| NG-AC5 | Switching to immediate flushes the pending digest | §3.3, §4 | **Partial** — when the final digest is sent is undefined (OQ-4) |
| NG-AC6 | The digest job is idempotent and concurrency-safe | §3.4, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.7-grouping-and-digest.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
