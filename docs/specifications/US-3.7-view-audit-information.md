---
story_id: US-3.7
title: "Epic 3 — Administration: View Audit Information"
source: docs/backlog/US-3.7-view-audit-information.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# View Audit Information

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a security auditor, I want to read a chronological, filterable log of what people did,
> So that I can reconstruct an incident and show the sequence to an external reviewer.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**AU-AC1 — Reading the feed**
```gherkin
Given an auditor holding the audit:read scope
When GET /api/v1/admin/audit-events is called with no filters
Then respond 200 with events from the last 7 days, newest first, 50 per page
And each item carries occurrence time, actor, actor type, event, severity, target, outcome, IP and user agent
And occurrence time is returned as an ISO-8601 instant in UTC, which the client renders in the reader's own zone
And a nextCursor is returned while further pages exist
```

**AU-AC2 — Filtering and event detail**
```gherkin
Given the auditor filters by actor, event type, severity and a date range inside 90 days
When the feed is requested
Then only events matching every supplied condition are returned
When GET /api/v1/admin/audit-events/{id} is called
Then the response includes the before and after value of each changed field
And it includes the correlation identifier for cross-referencing application logs
```

**AU-AC3 — No write path exists**
```gherkin
Given any audit event
When PUT, PATCH or DELETE is called against its URI
Then respond 405 with an Allow header listing only GET
And the client renders no edit or delete affordance
And the same holds for a caller holding every scope the system defines
```

**AU-AC4 — Caller without audit:read**
```gherkin
Given an administrator who does not hold the audit:read scope
When GET /api/v1/admin/audit-events is called
Then respond 403 with type ".../errors/insufficient-scope"
And that refused attempt is itself written to the audit log
```

**AU-AC5 — Window too wide, or no matches**
```gherkin
Given a requested range longer than 90 days
When the feed is requested
Then respond 400 with type ".../errors/range-too-wide"
And the detail points the caller at the export in US-3.8
Given the filters match nothing
Then respond 200 with an empty item list and a null cursor
And the client shows "No events match these filters" with a reset action
```

**AU-AC6 — Invalid cursor**
```gherkin
Given a cursor value that is malformed or no longer decodable
When the feed is requested with it
Then respond 400 with type ".../errors/invalid-cursor"
And the client recovers by restarting the query from the first page
```

## 3. Functional Specification

### 3.1 Reading the feed

A feed request from a caller holding the `audit:read` scope, with no filters, responds
`200` with events from the last 7 days, newest first, 50 per page. `[AU-AC1]`

Each item carries its occurrence time, actor, event, target, outcome, IP address and user
agent. `[AU-AC1]`

A `nextCursor` is returned while further pages exist, and is null when the result is empty.
`[AU-AC1]` `[AU-AC5]`

### 3.2 Filtering and detail

Where the caller filters by actor, event type and a date range inside 90 days, only events
matching every supplied condition are returned. `[AU-AC2]`

A request for a single event returns the before and after value of each changed field,
together with the correlation identifier for cross-referencing application logs.
`[AU-AC2]`

Where a requested range is longer than 90 days, the response is `400` with `type`
`.../errors/range-too-wide`, and the detail points the caller at the export of US-3.8.
`[AU-AC5]`

Where a cursor value is malformed or no longer decodable, the response is `400` with
`type` `.../errors/invalid-cursor`, and the client restarts the query from the first page.
`[AU-AC6]`

Where the filters match nothing, the response is `200` with an empty item list and a null
cursor, and the client shows "No events match these filters" with a reset action.
`[AU-AC5]`

### 3.3 Immutability

`PUT`, `PATCH` and `DELETE` against an event's URI respond `405` with an `Allow` header
listing only `GET`. `[AU-AC3]`

The client renders no edit or delete affordance. `[AU-AC3]`

The same holds for a caller holding every scope the system defines. `[AU-AC3]`

### 3.4 Authorisation

Where the caller does not hold the `audit:read` scope, the response is `403` with `type`
`.../errors/insufficient-scope`, and the refused attempt is itself written to the audit
log. `[AU-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/admin/audit-events` | Path and method named by the criteria | `[AU-AC1]` |
| 2 | `GET /api/v1/admin/audit-events/{id}` | Path and method named by the criteria | `[AU-AC2]` |
| 3 | `audit:read` scope | Required for every read | `[AU-AC1]` `[AU-AC4]` |
| 4 | occurrence time | not specified beyond being a time; time zone not stated | `[AU-AC1]` |
| 5 | actor | not specified — no identifier type or representation given | `[AU-AC1]` |
| 6 | event | Filterable by type; the vocabulary is contributed by other stories and is not enumerated here | `[AU-AC1]` `[AU-AC2]` |
| 7 | target | not specified — type and identifier not given | `[AU-AC1]` |
| 8 | outcome | not specified — no value set given | `[AU-AC1]` |
| 9 | IP, user agent | not specified beyond being present | `[AU-AC1]` |
| 10 | changed-field diff | Before and after value per field | `[AU-AC2]` |
| 11 | correlation identifier | Present on the detail; format not specified | `[AU-AC2]` |
| 12 | `nextCursor` | Opaque; null when no further page exists | `[AU-AC1]` `[AU-AC5]` |
| 13 | page size | 50 | `[AU-AC1]` |
| 14 | default and maximum window | 7 days default, 90 days maximum | `[AU-AC1]` `[AU-AC5]` |
| 15 | `Allow` header | Lists only `GET` on the mutating verbs | `[AU-AC3]` |
| 16 | `ProblemDetail.type` | Slugs `insufficient-scope`, `range-too-wide`, `invalid-cursor` | `[AU-AC4]` `[AU-AC5]` `[AU-AC6]` |

## 5. Out of Scope

- Exporting beyond the interactive window — US-3.8.
- Writing audit events. Every producing story owns its own entries; this story only reads.
- Alerting or anomaly detection on the stream — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Append-only is enforced below the API as well, by restricting the application's database grants. | AU-AC3 closes the HTTP path only. Without this the criterion's "no write path exists" is true of the API and false of the system. |
| A-2 | Pagination is cursor-based, per `AGENTS.md` §3.1's allowance for continuous feeds. | AU-AC1 and AU-AC6 name a cursor without stating the strategy. |
| A-3 | Credential material, attachment contents and full document numbers are never written into the diff. | Named in the story's Non-Functional section. No criterion constrains the diff's contents. |
| A-4 | Retention is at least 12 months. | Named in the story's Non-Functional section; no criterion asserts any retention, and AU-AC5's 90-day interactive cap is a separate limit. |
| A-5 | Events are written through a transactional outbox so that a producing operation neither blocks on nor loses its audit entry. | Named in the story's Non-Functional section. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | AU-AC1 requires an "actor", "target" and "outcome" on every item but defines none of them. Epic 2 writes to `audit_events` and Epic 3 to `audit_events`, while this story reads `audit-events`. Are those one stream or three, and if one, how are the differing shapes reconciled? | AU-AC1, AU-AC2 |
| OQ-2 | Retention is stated nowhere in the criteria. Twelve months is a placeholder in the story's Non-Functional section. What does compliance actually require, and does that figure survive the erasure obligation in US-3.4 UD-AC6? | — |
| OQ-3 | AU-AC1 shows occurrence time without stating a time zone or format. The story's Data Model Notes mention both UTC and the reader's zone; the criteria mention neither. | AU-AC1 |
| OQ-4 | AU-AC4 requires the refused attempt to be "written to the audit log" — the same log this story reads. A caller without `audit:read` therefore generates an entry they cannot see, which is intended, but nothing states whether the refusal is retained under the same rules as the events it sits beside. | AU-AC4 |
| OQ-5 | Should an auditor be able to read ticket contents (Epic 4) through the target reference, or only metadata? The answer materially changes the exposure of customer correspondence. | AU-AC2 |
| OQ-6 | The story's Non-Functional section states p95 ≤ 1 s over 10 million rows. No criterion asserts a latency bound. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| AU-AC1 | Default feed of the last 7 days, 50 per page, with a cursor | §3.1, §4 | **Partial** — actor, target and outcome are asserted but undefined, and the stream's identity is unresolved (OQ-1) |
| AU-AC2 | Filters and a before/after diff with a correlation identifier | §3.2, §4 | **Partial** — same unresolved stream identity (OQ-1) |
| AU-AC3 | No mutating verb is accepted, for any caller | §3.3, §4 | Covered — see A-1 |
| AU-AC4 | A caller without the scope is refused and logged | §3.4, §4 | Covered — see OQ-4 |
| AU-AC5 | Over-wide ranges are refused; empty results stay usable | §3.2, §4 | Covered |
| AU-AC6 | A malformed cursor yields 400 and the client recovers | §3.2, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

> OQ-1 is the most consequential finding in this document. Three stories write to what they
> call an audit log under three different names, and this story reads a fourth. Until that
> is settled, AU-AC1 and AU-AC2 cannot be implemented against a known schema.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.7-view-audit-information.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
