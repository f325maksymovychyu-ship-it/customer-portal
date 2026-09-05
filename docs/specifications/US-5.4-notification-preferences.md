---
story_id: US-5.4
title: "Epic 5 — Notifications: Preferences"
source: docs/backlog/US-5.4-notification-preferences.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Notification Preferences

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 for the unresolved module question.

## 1. Story

> As a signed-in customer, I want to choose which events reach me and through which channel,
> So that I keep the notifications that matter instead of turning everything off in frustration.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**NP-AC3 — Security notifications cannot be disabled**
```gherkin
Given the event type "password changed" belongs to the security class
When the settings page renders
Then its switches are disabled with the explanation "Security notifications cannot be turned off"
When PUT is called directly attempting to disable it
Then respond 422 with type ".../errors/preference-locked"
And no preference row is written
```

**NP-AC4 — Save fails**
```gherkin
Given the customer toggles a switch
And the request fails
Then the switch returns to its previous position
And the customer sees "We could not save that. Try again"
And no other switch on the page changes state
```

**NP-AC5 — An event type introduced later**
```gherkin
Given a new transactional event type is deployed
When the customer next opens their settings
Then it is enabled by default and flagged as new for 14 days
But a new informational event type is disabled by default
And neither requires a data migration against existing customers
```

**NP-AC6 — Unknown event type or channel**
```gherkin
Given a PUT naming an event type or channel that the code does not declare
When it is submitted
Then respond 400 with type ".../errors/unknown-event-type"
And no part of the request is applied
```

## 3. Functional Specification

### 3.1 Reading preferences

A preferences request returns event types grouped by notification class, each with a short
explanation. `[NP-AC1]`

Each event type shows its current state for the in-app and email channels. `[NP-AC1]`

Types belonging to the security class are marked locked. `[NP-AC1]`

### 3.2 Applying preferences at delivery

Where the customer has disabled the email channel for an event type, an occurrence of that
event still creates the in-app notification but sends no email. `[NP-AC2]`

A saved change is in force within 60 seconds. `[NP-AC2]`

### 3.3 Protected classes

Where an event type belongs to the security class, the settings page renders its switches
disabled with the explanation "Security notifications cannot be turned off". `[NP-AC3]`

A direct request attempting to disable such a type responds `422` with `type`
`.../errors/preference-locked`, and no preference row is written. `[NP-AC3]`

### 3.4 Defaults for later event types

A newly deployed transactional event type is enabled by default and flagged as new for 14
days, while a newly deployed informational event type is disabled by default. Neither
requires a data migration against existing customers. `[NP-AC5]`

### 3.5 Validation

Where a request names an event type or a channel that the code does not declare, the
response is `400` with `type` `.../errors/unknown-event-type`, and no part of the request
is applied. `[NP-AC6]`

### 3.6 Save failure

Where a toggle's request fails, the switch returns to its previous position, the customer
sees "We could not save that. Try again", and no other switch on the page changes state.
`[NP-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/notifications/preferences` | Path and method named by the criteria | `[NP-AC1]` |
| 2 | `PUT` on the same path | Method named by NP-AC3 and NP-AC6; **the request shape is not specified** — see OQ-1 | `[NP-AC3]` `[NP-AC6]` |
| 3 | notification class | Groups the matrix. Values `security`, `transactional` and `informational` are named across the criteria; **the full set is not enumerated** | `[NP-AC1]` `[NP-AC3]` `[NP-AC5]` |
| 4 | event type | Declared in code; examples "ticket reply", "password changed". **The catalogue is not enumerated** | `[NP-AC2]` `[NP-AC3]` `[NP-AC6]` |
| 5 | channel | Values in-app and email; anything else is rejected | `[NP-AC1]` `[NP-AC6]` |
| 6 | locked flag | Derived from the security class; not stored per customer | `[NP-AC1]` `[NP-AC3]` |
| 7 | short explanation | Shown per event type; **source and length not specified** | `[NP-AC1]` |
| 8 | "new" flag | Shown for 14 days after an event type is deployed | `[NP-AC5]` |
| 9 | propagation window | 60 seconds from save | `[NP-AC2]` |
| 10 | `ProblemDetail.type` | Slugs `preference-locked`, `unknown-event-type` | `[NP-AC3]` `[NP-AC6]` |
| 11 | Message strings | Two exact strings, quoted in NP-AC3 and NP-AC4 | `[NP-AC3]` `[NP-AC4]` |

## 5. Out of Scope

- Unsubscribing from an email without signing in — US-5.6.
- Digest and quiet-window scheduling — US-5.7.
- Channels beyond in-app and email — no criterion reaches them.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Absence of a stored preference row means "use the class default", which is what makes NP-AC5's no-migration requirement achievable. | NP-AC5 states the outcome; this is the only shape that delivers it. |
| A-2 | Transactional types are opt-out and informational types opt-in, following the class table in `docs/backlog/README.md`. | NP-AC5 states the defaults for new types only; the same rule is assumed for existing ones. |
| A-3 | Preferences are consulted at delivery time by each channel, with a cache no staler than 60 seconds. | NP-AC2 states the window without saying where the lookup happens. |
| A-4 | Preference changes are recorded in the audit log. | Named in the story's Non-Functional section. No criterion asserts it, though it is what settles a "I never received it" report. |
| A-5 | Administrative-class types exist and are partially lockable. | The class table names four classes; the criteria name three. See OQ-2. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | NP-AC3 and NP-AC6 both describe a `PUT`, but no criterion gives its request shape. Is a preference set replaced wholesale, or are individual triples patched? NP-AC6's "no part of the request is applied" implies the request carries several changes, but nothing describes them. | NP-AC3, NP-AC6 |
| OQ-2 | The criteria name the security, transactional and informational classes. The administrative class from the backlog's class table appears in none of them, though US-5.8 relies on it. Is it in scope here? | NP-AC1, NP-AC5 |
| OQ-3 | NP-AC2 requires the change to be "in force within 60 seconds". For an email already queued when the preference changes, is it cancelled or sent? US-5.5 NE-AC5 answers the analogous question for a changed address but not for a changed preference. | NP-AC2 |
| OQ-4 | Do agents and administrators have a preference matrix? They receive assignment notifications (US-4.3 TQ-AC5) and announcements (US-5.8), and the class model was written around customers. | NP-AC1 |
| OQ-5 | May a customer disable both channels for a transactional type, making themselves unreachable for that event? No criterion forbids it or requires a confirmation. | NP-AC2 |
| OQ-6 | NP-AC1 requires "a short explanation" per event type. Who writes those strings, and are they localised? US-5.5 NE-AC2 assumes a language on the profile that no story creates. | NP-AC1 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NP-AC1 | The matrix groups types by class and marks security types locked | §3.1, §4 | **Partial** — neither the class set nor the event-type catalogue is enumerated (OQ-2, OQ-6) |
| NP-AC2 | A disabled channel is honoured within 60 seconds | §3.2, §4 | **Partial** — the in-flight case the window implies is undefined (OQ-3) |
| NP-AC3 | Security types cannot be disabled from either end | §3.3, §4 | Covered — see OQ-1 |
| NP-AC4 | A failed save reverts one switch and only that switch | §3.6, §4 | Covered |
| NP-AC5 | New types default by class and need no migration | §3.4, §4 | Covered |
| NP-AC6 | Undeclared types and channels are rejected wholesale | §3.5, §4 | **Partial** — "no part of the request" presumes a request shape no criterion gives (OQ-1) |

**Coverage:** 3 Covered, 3 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.4-notification-preferences.md`. |
