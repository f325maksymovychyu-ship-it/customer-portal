---
story_id: US-5.5
title: "Epic 5 — Notifications: Email Delivery"
source: docs/backlog/US-5.5-email-notifications.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Email Delivery

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 for the module question, and OQ-1 below: the mail provider
> and queue this story requires are unapproved, and every story in the backlog that promises
> an email waits behind them.

## 1. Story

> As a customer, I want the portal to email me about things that matter,
> So that I find out even when I am not signed in.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

## 3. Functional Specification

### 3.1 Queueing and content

Where the email channel is enabled for an event type, creating the notification queues an
email within 10 seconds. `[NE-AC1]`

The subject carries the related object reference, such as `Ticket #10425: new reply`.
`[NE-AC1]`

The body summarises the event and links directly to the relevant page. Following that link
while signed out leads to sign-in and then to the intended page. `[NE-AC1]`

### 3.2 Rendering

Where the customer's profile specifies a language and a time zone, the email's text uses
that language, and every timestamp is shown in that zone with the zone named explicitly.
`[NE-AC2]`

Both a plain-text and an HTML part are present. `[NE-AC2]`

### 3.3 Delivery failure

Where the provider is not responding, three attempts are made at 1, 5 and 15 minute
intervals. `[NE-AC3]`

After the third failure the message moves to the dead-letter queue and raises a monitoring
alert. `[NE-AC3]`

The in-app notification is delivered regardless, because the channels are independent.
`[NE-AC3]`

### 3.4 Permanent failure

Where the provider reports a permanent delivery failure for the address, processing the
webhook disables the email channel for that recipient. `[NE-AC4]`

On their next sign-in the customer sees "We cannot deliver email to your address. Check it
in settings", and an administrator sees the delivery problem on the customer record.
In-app notifications continue unaffected. `[NE-AC4]`

### 3.5 Address resolution at dispatch

Where the customer's address was changed and confirmed between queueing and dispatch, the
message goes to the newly confirmed address. Where the new address is still unconfirmed,
the previous confirmed address is used. `[NE-AC5]`

### 3.6 Runaway protection

Where more than 500 emails have been queued for one recipient within an hour, another is
discarded, an alert is raised naming the originating event type, and the incident is
recorded. `[NE-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | outbound mail port | Required by every criterion; **no interface is named** — see OQ-2 | `[NE-AC1]` |
| 2 | provider webhook | Receives permanent-failure reports; **no path, payload or authentication is named** | `[NE-AC4]` |
| 3 | queue latency bound | 10 seconds from notification creation | `[NE-AC1]` |
| 4 | subject | Carries the related object reference; format shown by example only | `[NE-AC1]` |
| 5 | body | Summary plus a direct link; both plain-text and HTML parts | `[NE-AC1]` `[NE-AC2]` |
| 6 | deep link | Survives a sign-in redirect, per US-2.1 LI-AC2 | `[NE-AC1]` |
| 7 | customer language | **Read from the profile; no story creates this field** — see OQ-3 | `[NE-AC2]` |
| 8 | customer time zone | **Read from the profile; no story creates this field** — see OQ-3 | `[NE-AC2]` |
| 9 | retry schedule | 1, 5 and 15 minutes, three attempts | `[NE-AC3]` |
| 10 | dead-letter queue | Terminal destination after the third failure | `[NE-AC3]` |
| 11 | email-blocked state | Per recipient, set by a hard bounce; visible to administrators | `[NE-AC4]` |
| 12 | confirmed-address rule | Dispatch uses the confirmed address current at send time | `[NE-AC5]` |
| 13 | runaway ceiling | 500 emails per recipient per hour | `[NE-AC6]` |
| 14 | Message strings | "We cannot deliver email to your address. Check it in settings" | `[NE-AC4]` |

## 5. Out of Scope

- Which events qualify — US-5.4; unsubscribing — US-5.6.
- Grouping and digests — US-5.7.
- Inbound email. The portal never receives replies by mail.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Delivery is asynchronous through a queue, written in the same transaction as the notification. | NE-AC1 says "queued" and NE-AC3 retries over 21 minutes, neither of which is possible synchronously. |
| A-2 | The sending domain publishes SPF, DKIM and DMARC records. | Named in the story's Non-Functional section. No criterion asserts it, yet without it NE-AC1's premise — that the customer finds out — fails silently. |
| A-3 | Links in emails carry no authentication token. | Named in the story's Non-Functional section. NE-AC1's sign-in redirect is consistent with this but does not establish it. |
| A-4 | The webhook verifies the provider's signature before parsing. | Named in the story's Non-Functional section. NE-AC4 describes an unauthenticated-looking inbound call. |
| A-5 | "Confirmed" in NE-AC5 refers to the pending-email mechanism of US-3.3 UU-AC2. | NE-AC5 uses the word without defining it; UU-AC2 is the only story that creates the state. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | **Escalation — `AGENTS.md` §7.5.** An outbound mail provider and a queue are both new runtime dependencies and are unapproved. Every story that promises an email — US-2.1 LI-AC6, US-2.5, US-2.6, US-3.2, US-3.4, US-4.1, US-4.6 and US-5.8 — is blocked behind this decision. | all |
| OQ-2 | No criterion names the outbound interface, the webhook path, or the webhook payload. NE-AC4 in particular describes processing a provider callback whose shape differs between every major provider. | NE-AC4 |
| OQ-3 | NE-AC2 reads a language and a time zone from "the customer's profile". No story in this backlog creates either field, and US-3.3 does not list them among the updatable attributes. What happens when they are absent? US-5.7 NG-AC4 has the same dependency for its 09:00 digest. | NE-AC2 |
| OQ-4 | NE-AC6 discards the 501st email. Which one is discarded — the new one, or the oldest queued? And is the customer ever told that notifications were dropped? | NE-AC6 |
| OQ-5 | NE-AC4 disables the channel on a hard bounce. How does the customer re-enable it after correcting the address — automatically on confirmation, or manually through US-5.4? | NE-AC4 |
| OQ-6 | NE-AC1 bounds queueing at 10 seconds but no criterion bounds delivery. NE-AC3's retry schedule spans 21 minutes, so a customer may learn of a password change long after it happened. Is there an end-to-end target? | NE-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NE-AC1 | An eligible event queues an email within 10 seconds | §3.1, §4 | **Partial** — no outbound interface is specified (OQ-2, OQ-6) |
| NE-AC2 | Language and time zone come from the profile | §3.2, §4 | **Partial** — neither profile field exists in any story (OQ-3) |
| NE-AC3 | Three retries, then dead-letter and an alert | §3.3, §4 | Covered |
| NE-AC4 | A hard bounce disables the channel and surfaces the problem | §3.4, §4 | **Partial** — the webhook the criterion processes is unspecified (OQ-2, OQ-5) |
| NE-AC5 | Dispatch uses the address confirmed at send time | §3.5, §4 | Covered |
| NE-AC6 | Above 500 an hour, further mail is dropped and alerted | §3.6, §4 | **Partial** — which message is dropped is undefined (OQ-4) |

**Coverage:** 2 Covered, 4 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.5-email-notifications.md`. |
