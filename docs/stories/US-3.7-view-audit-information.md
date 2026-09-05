# Epic 3 — Administration: View Audit Information

**Story ID:** US-3.7
**Project:** Customer Portal
**AC prefix:** `AU-AC`
**Module:** `shared/`

## User Story
As a security auditor,
I want to read a chronological, filterable log of what people did,
So that I can reconstruct an incident and show the sequence to an external reviewer.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Mutability | Append-only; no update or delete path exists at any layer | A log an administrator can edit proves nothing in an investigation |
| 2 | Pagination | Cursor-based | `AGENTS.md` §3.1 allows cursors for continuous feeds, and an append-only log is the canonical one |
| 3 | Default window | Last 7 days | Almost every query is recent; a wider default makes the common case slow |
| 4 | Maximum interactive window | 90 days | Beyond that the answer belongs in an export (US-3.8), not a page |
| 5 | Retention | At least 12 months, then cold storage | Placeholder until a compliance owner states the real figure |

## In Scope
- `GET /api/v1/admin/audit-events` — cursor-paginated feed with filters
- `GET /api/v1/admin/audit-events/{id}` — one event with its before/after diff
- Enforcement of append-only semantics at the API surface
- Correlation identifiers linking an event to application logs

## Out of Scope
- Exporting beyond the interactive window (US-3.8)
- Writing audit events — every producing story owns its own entries
- Alerting or anomaly detection on the stream

## API Contract
| Method | Path | Auth | Query | Success |
|---|---|---|---|---|
| GET | `/api/v1/admin/audit-events` | Bearer + `audit:read` | `from`, `to`, `actorId`, `event`, `severity`, `targetId`, `cursor`, `size` | `200` `{"items": [...], "nextCursor": str \| null}` |
| GET | `/api/v1/admin/audit-events/{id}` | Bearer + `audit:read` | — | `200` `AuditEventDetail` |

## Data Model Notes
- **`audit_events` is the single audit stream for the whole system.** Every story in Epics 2–5
  writes to it; there is no separate authentication or administration log. `actorType` and
  `event` are what separate the sources.
- `audit_events`: `id` (UUIDv7, so the key is itself chronological and makes a good cursor), `occurredAt`, `actorId`, `actorType`, `event`, `severity`, `targetType`, `targetId`, `outcome`, `ip`, `userAgent`, `correlationId`, `changes` (JSONB)
- `severity` ∈ {`INFO`, `NOTICE`, `SECURITY`}. `SECURITY` marks events that indicate an
  attack or a control failure rather than ordinary activity — token reuse (US-2.4 RT-AC5),
  refused privilege escalation (US-3.2 UC-AC4, US-3.5 RA-AC4), and refused access to audit
  data (AU-AC4). It is set by the producing story, never edited afterwards.
- `actorType` distinguishes `CUSTOMER`, `ADMINISTRATOR` and `SYSTEM`, so scheduled jobs such as US-4.6 auto-closure are attributable
- `changes` holds `field → {before, after}` and must never carry credential material
- Indexes on `(occurredAt DESC)`, `(actorId, occurredAt DESC)` and `(event, occurredAt DESC)` back the three supported filters

## Acceptance Criteria

### Happy path
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

### Immutability
**AU-AC3 — No write path exists**
```gherkin
Given any audit event
When PUT, PATCH or DELETE is called against its URI
Then respond 405 with an Allow header listing only GET
And the client renders no edit or delete affordance
And the same holds for a caller holding every scope the system defines
```

### Authorisation
**AU-AC4 — Caller without audit:read**
```gherkin
Given an administrator who does not hold the audit:read scope
When GET /api/v1/admin/audit-events is called
Then respond 403 with type ".../errors/insufficient-scope"
And that refused attempt is itself written to the audit log
```

### Bounds and empty results
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/range-too-wide",
  "title": "Range Too Wide",
  "status": 400,
  "detail": "Interactive queries are limited to 90 days. Use the export for longer ranges.",
  "instance": "/api/v1/admin/audit-events"
}
```
Error `type` slugs introduced by this story: `range-too-wide`, `invalid-cursor`.

## Non-Functional / Security Requirements
- Append-only is enforced below the API as well: the application's database role holds `INSERT` and `SELECT` on `audit_events` and nothing else.
- Passwords, tokens, attachment contents and full document numbers must never be written into `changes` — only field names and non-sensitive values.
- Writing an event must not block the operation that produced it, but losing one is unacceptable. Use a transactional outbox rather than a fire-and-forget call.
- **Performance:** p95 ≤ 1 s for a filtered query over 10 million rows, which is what the three indexes above exist to guarantee.
- Retention of at least 12 months, with older partitions moved to cold storage while remaining exportable.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| AU-AC1, AU-AC2 | Functional suite (RestAssured + Testcontainers) with a seeded event set | `[gate]` |
| AU-AC3 | Contract test asserting `405` and the `Allow` header for every mutating verb | `[gate]` |
| AU-AC4 | Functional test asserting `403` and that the refusal was itself logged | `[gate]` |
| AU-AC5, AU-AC6 | Functional test on the range bound, the empty result and a corrupt cursor | `[gate]` |
| Grant restriction | Migration test asserting the application role holds no `UPDATE` or `DELETE` on the table | `[gate]` |
| No secrets in `changes` | Unit test over the redaction rules, plus a CI grep on known key names | `[gate]` |
| Latency budget | Performance scenario in `perf/` against a seeded 10 million-row table | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.4.** The retention figure in Decision 5 is a compliance decision. Twelve months is a placeholder and must be confirmed before the partitioning strategy is built on it.
2. Does the auditor role need read access to ticket contents (Epic 4) to make an investigation useful, or is metadata enough? Granting it widens exposure of customer correspondence considerably.
3. Should the feed expose events about a customer to that customer themselves? Nothing in Release 1.0 requires it, but data-subject access requests eventually will.
