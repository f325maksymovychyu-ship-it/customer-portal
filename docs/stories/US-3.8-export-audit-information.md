# Epic 3 — Administration: Export Audit Information

**Story ID:** US-3.8
**Project:** Customer Portal
**AC prefix:** `AX-AC`
**Module:** `shared/`

## User Story
As a security auditor,
I want to download a filtered set of audit events as a file,
So that I can hand an external reviewer something they can analyse independently.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Execution | Asynchronous job, with a link delivered on completion | A synchronous export of tens of thousands of rows holds a connection open and times out |
| 2 | Row ceiling | 50 000 per export | Above that the request is really a data-warehouse question, not a download |
| 3 | Formats | CSV and JSON Lines | CSV for spreadsheet review, JSONL because `changes` is nested and CSV flattens it badly |
| 4 | Link lifetime | 24 hours, bound to the requesting account | An audit extract is among the most sensitive artefacts the system produces |
| 5 | Snapshot semantics | The export reflects the filters as evaluated when the job started | Otherwise two downloads of "the same" export differ |

## In Scope
- `POST /api/v1/admin/audit-events/exports` — request an export
- `GET /api/v1/admin/audit-events/exports/{id}` — poll status and retrieve the link
- Row-ceiling enforcement before the job is created
- Access control and expiry on the produced file

## Out of Scope
- Interactive browsing (US-3.7)
- Scheduled or recurring exports
- Streaming into an external SIEM — see Open Questions

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/admin/audit-events/exports` | Bearer + `audit:export` | `{"from", "to", "actorId"?, "event"?, "format": "CSV" \| "JSONL"}` | `202` + `Location: /api/v1/admin/audit-events/exports/{id}` |
| GET | `/api/v1/admin/audit-events/exports/{id}` | Bearer + `audit:export` | — | `200` `{"status", "rowCount"?, "downloadUrl"?, "expiresAt"?}` |

## Data Model Notes
- `audit_exports`: `id`, `requestedBy`, `filters` (JSONB), `format`, `status`, `rowCount`, `storageKey`, `expiresAt`, `createdAt`
- `status` moves `PENDING` → `RUNNING` → `READY` \| `FAILED`
- The produced file lives in object storage, never on a local disk, so any instance can serve the link — a new runtime dependency, see Open Questions
- Requesting an export writes an `AUDIT_EXPORTED` event into `audit_events`, which means the export log is itself auditable

## Acceptance Criteria

### Happy path
**AX-AC1 — Requesting and collecting an export**
```gherkin
Given an auditor holding the audit:export scope
When POST /api/v1/admin/audit-events/exports is called with a date range and format CSV
Then respond 202 with a Location header pointing at the job resource
And the client shows "Your file is being prepared. We will send a link when it is ready"
When the job completes
Then GET on the job resource returns status READY with a download link valid for 24 hours
And the file contains exactly the rows that matched the filters when the job started
And an audit event with type AUDIT_EXPORTED records who requested it and over what range
```

### Limits and validation
**AX-AC2 — Selection exceeds the row ceiling**
```gherkin
Given the requested filters match more than 50 000 events
When the export is requested
Then respond 400 with type ".../errors/export-too-large"
And the detail reads "Narrow the range or filters. The limit is 50 000 events"
And no job is created
```

**AX-AC3 — Invalid request**
```gherkin
Given a request whose "to" precedes its "from", or whose format is not CSV or JSONL
When the export is requested
Then respond 400 with type ".../errors/validation-failed" naming the offending field
And no job is created
```

### Access control on the artefact
**AX-AC4 — Someone else's export**
```gherkin
Given an export requested by another auditor
When GET on that job resource, or on its download link, is attempted
Then respond 404
And the response reveals nothing about the export's existence, filters or size
```

**AX-AC5 — Expired link**
```gherkin
Given an export completed more than 24 hours ago
When the download link is used
Then respond 410 with type ".../errors/export-expired"
And the stored file has been deleted from object storage
And the job record remains, so the audit trail of who exported what survives the file
```

### Failure
**AX-AC6 — Job failure**
```gherkin
Given the export job fails while running
When GET on the job resource is called
Then status is FAILED with a reason the auditor can act on
And the client offers to retry with the same filters
And no partially written file is ever exposed for download
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/export-too-large",
  "title": "Export Too Large",
  "status": 400,
  "detail": "Narrow the range or filters. The limit is 50 000 events.",
  "instance": "/api/v1/admin/audit-events/exports",
  "matchedRows": 184203
}
```
Error `type` slugs introduced by this story: `export-too-large`, `export-expired`.

## Non-Functional / Security Requirements
- The download link is bound to the requesting account. A pre-signed URL that anyone holding the string can fetch does not satisfy AX-AC4.
- Files are deleted from object storage when they expire; a lifecycle rule is not sufficient on its own, because the story promises deletion at 24 hours, not "eventually".
- Export jobs run on a lower-priority queue than transactional mail (US-5.5), so a large extract cannot delay a password-change notification.
- The exported file carries the same redaction rules as US-3.7. An export must never contain material the interactive view hides.
- **Performance:** a 50 000-row export completes within 5 minutes at p95.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| AX-AC1 | Functional suite driving the job to completion with Awaitility, never `Thread.sleep` | `[gate]` |
| AX-AC2 | Integration test with a seeded set above the ceiling | `[gate]` |
| AX-AC3 | Slice test on the request record | `[gate]` |
| AX-AC4 | Functional test with a second auditor account | `[gate]` |
| AX-AC5 | Integration test against a deterministic injected `Clock` | `[gate]` |
| AX-AC6 | Integration test with the export port stubbed to fail | `[gate]` |
| Redaction parity | Test asserting the exported columns match the interactive projection | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** Object storage and a job queue are both new runtime dependencies. Without them this story cannot be built as specified, and AX-AC1 would have to fall back to a synchronous, much smaller export.
2. Should exports be streamed to a SIEM instead of downloaded by a person? That is a different story with different security properties, and nobody has asked for it yet.
3. Does an export need approval from a second auditor, as US-5.8 requires for large announcements? An audit extract is arguably the more sensitive of the two.
