---
story_id: US-2.3
title: "Epic 2 — Authentication: Active Session Management"
source: docs/backlog/US-2.3-active-sessions.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Active Session Management

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a customer who suspects their account has been compromised, I want to see my active
> sessions and end all of them at once, So that I can cut off an intruder myself instead of
> waiting on support.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**SM-AC1 — Listing active sessions**
```gherkin
Given the customer holds three active sessions on different devices
When GET /api/v1/auth/sessions is called
Then respond 200 with one entry per session
And each entry carries device, browser, approximate city and last-activity time
And exactly one entry is flagged as the current session
```

**SM-AC2 — Ending every session**
```gherkin
Given the customer holds three active sessions
When POST /api/v1/auth/sessions/revoke-all is called with keepCurrent=true
Then every refresh-token family except the current one is revoked
And respond 200 with the number of sessions ended
And the other devices lose access at their next refresh, and no later than 15 minutes
And the customer receives an email listing what was ended and when
```

**SM-AC3 — Ending one session**
```gherkin
Given the customer is viewing their session list
When DELETE /api/v1/auth/sessions/{sessionId} is called for another device
Then that session is revoked and respond 204
And the current session is unaffected
```

**SM-AC4 — Ending someone else's session**
```gherkin
Given a session identifier that belongs to a different customer
When DELETE /api/v1/auth/sessions/{sessionId} is called
Then respond 404, not 403
And the target session remains active
And an audit_events entry records the unauthorised attempt
```

**SM-AC5 — Revoking with no other sessions**
```gherkin
Given the current session is the only active one
When POST /api/v1/auth/sessions/revoke-all is called with keepCurrent=true
Then respond 200 with revokedCount=0
And no email is sent, because nothing changed
```

## 3. Functional Specification

### 3.1 Listing sessions

A request for the session list returns `200` with one entry per active session belonging
to the caller. `[SM-AC1]`

Each entry carries the device, the browser, an approximate city and the time of last
activity. `[SM-AC1]`

Exactly one entry is flagged as the current session. `[SM-AC1]`

### 3.2 Ending every session

Where the bulk revoke is called with `keepCurrent=true`, every refresh-token family except
the current one is revoked. `[SM-AC2]`

The response is `200` and carries the number of sessions ended. `[SM-AC2]`

Devices whose sessions were ended lose access at their next refresh, and no later than 15
minutes after the revocation. `[SM-AC2]`

The customer receives an email listing what was ended and when. `[SM-AC2]`

Where the current session is the only active session, the response is `200` with a count
of zero and no email is sent. `[SM-AC5]`

The behaviour when `keepCurrent` is `false`, or absent, is not stated by any criterion —
see OQ-1.

### 3.3 Ending one session

Where a single session belonging to the caller is deleted, that session is revoked and the
response is `204`. The caller's current session is unaffected. `[SM-AC3]`

### 3.4 Sessions belonging to another customer

Where the named session belongs to a different customer, the response is `404` rather than
`403`. The target session remains active, and an `audit_events` entry records the
unauthorised attempt. `[SM-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/auth/sessions` | Path and method named by the criteria | `[SM-AC1]` |
| 2 | `DELETE /api/v1/auth/sessions/{sessionId}` | Path and method named by the criteria | `[SM-AC3]` `[SM-AC4]` |
| 3 | `POST /api/v1/auth/sessions/revoke-all` | Path and method named by the criteria | `[SM-AC2]` `[SM-AC5]` |
| 4 | `keepCurrent` (request) | Boolean; only the value `true` is exercised by any criterion | `[SM-AC2]` `[SM-AC5]` |
| 5 | session entry: `device` | not specified — no format or source stated | `[SM-AC1]` |
| 6 | session entry: `browser` | not specified | `[SM-AC1]` |
| 7 | session entry: approximate city | not specified — resolution method and precision not stated | `[SM-AC1]` |
| 8 | session entry: last activity | not specified beyond being a time | `[SM-AC1]` |
| 9 | session entry: current flag | Exactly one entry carries it | `[SM-AC1]` |
| 10 | `sessionId` | Opaque identifier; format not specified | `[SM-AC3]` `[SM-AC4]` |
| 11 | revoked count (response) | Integer; zero when nothing was ended | `[SM-AC2]` `[SM-AC5]` |
| 12 | refresh-token family | The unit that revocation applies to; its key is not specified | `[SM-AC2]` |
| 13 | Notification email | Lists what was ended and when; template not specified | `[SM-AC2]` |

## 5. Out of Scope

- Ending the current session — US-2.2.
- Administrative termination of another customer's sessions — US-3.4.
- Alerting on sign-in from a new device — no criterion reaches it.
- Re-authentication before the bulk revoke — see A-2.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | A "session" in the list is one refresh-token family, so rotation (US-2.4) does not create a second entry. | SM-AC1 counts three sessions on three devices and SM-AC2 revokes families. The two are only consistent under this reading. |
| A-2 | The bulk revoke requires a confirmation dialog but not a password. | Named in the story's Assumptions table. No criterion states any confirmation step. |
| A-3 | The approximate city is resolved from a local database rather than a third-party service. | Named in the story's Non-Functional section. SM-AC1 requires only that a city be shown. |
| A-4 | Ended sessions disappear from the list immediately. | No criterion states what the list contains after a revocation; §3.1 would otherwise be ambiguous. |
| A-5 | The 15-minute bound in SM-AC2 follows from the access-token TTL defined in US-2.1. | SM-AC2 states the bound without deriving it. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | Every criterion exercising the bulk revoke passes `keepCurrent=true`. What happens when it is `false`, or omitted — is the caller's own session ended, and what does the client do next? The parameter exists in the story's API Contract but no criterion defines its other value. | SM-AC2, SM-AC5 |
| OQ-2 | SM-AC1 requires an "approximate city" but states no precision, no source, and no behaviour when the address cannot be resolved. What is displayed for an unresolvable or private-range address? | SM-AC1 |
| OQ-3 | SM-AC3 ends one session but no criterion says whether the customer may end their *own* current session through that endpoint, which would duplicate US-2.2 with a different status code. | SM-AC3 |
| OQ-4 | SM-AC2 promises an email. No criterion states what happens when that email cannot be delivered — is the revocation still considered complete? | SM-AC2 |
| OQ-5 | The story's Non-Functional section states p95 ≤ 400 ms for the listing and permits the bulk revoke to be asynchronous. No criterion asserts either. If the revoke is asynchronous, is the count in SM-AC2 the number attempted or the number completed? | SM-AC2 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| SM-AC1 | Session list shows device, browser, city and last activity | §3.1, §4 | **Partial** — the "approximate city" the criterion asserts has no defined source or fallback (OQ-2) |
| SM-AC2 | Bulk revoke ends every other session and emails the customer | §3.2, §4 | Covered — see OQ-1, OQ-4 |
| SM-AC3 | A single session can be ended without affecting the current one | §3.3, §4 | Covered — see OQ-3 |
| SM-AC4 | A foreign session identifier yields 404 and is audited | §3.4, §4 | Covered |
| SM-AC5 | Revoking with nothing to revoke returns zero and sends no email | §3.2, §4 | Covered |

**Coverage:** 4 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.3-active-sessions.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
