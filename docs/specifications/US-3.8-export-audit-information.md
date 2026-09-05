---
story_id: US-3.8
title: "Epic 3 — Administration: Export Audit Information"
source: docs/backlog/US-3.8-export-audit-information.md
status: draft
revision: 1
last_updated: 2026-08-22
---

# Export Audit Information

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a security auditor, I want to download a filtered set of audit events as a file,
> So that I can hand an external reviewer something they can analyse independently.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**AX-AC6 — Job failure**
```gherkin
Given the export job fails while running
When GET on the job resource is called
Then status is FAILED with a reason the auditor can act on
And the client offers to retry with the same filters
And no partially written file is ever exposed for download
```

## 3. Functional Specification

### 3.1 Requesting an export

An export request from a caller holding the `audit:export` scope, carrying a date range
and the format `CSV`, responds `202` with a `Location` header pointing at the job
resource. `[AX-AC1]`

The client shows "Your file is being prepared. We will send a link when it is ready".
`[AX-AC1]`

An audit event of type `AUDIT_EXPORTED` records who requested the export and over what
range. `[AX-AC1]`

### 3.2 Collecting the result

When the job completes, a read of the job resource returns status `READY` together with a
download link valid for 24 hours. `[AX-AC1]`

The file contains exactly the rows that matched the filters at the moment the job started.
`[AX-AC1]`

### 3.3 Limits and validation

Where the requested filters match more than 50 000 events, the response is `400` with
`type` `.../errors/export-too-large` and the detail "Narrow the range or filters. The limit
is 50 000 events". No job is created. `[AX-AC2]`

Where the request's `to` precedes its `from`, or its format is neither `CSV` nor `JSONL`,
the response is `400` with `type` `.../errors/validation-failed` naming the offending
field. No job is created. `[AX-AC3]`

### 3.4 Access control on the artefact

Where the job resource or its download link belongs to another auditor, the response is
`404`, revealing nothing about the export's existence, filters or size. `[AX-AC4]`

Where more than 24 hours have passed since completion, using the download link responds
`410` with `type` `.../errors/export-expired`, and the stored file has been deleted from
object storage. `[AX-AC5]`

The job record remains after the file is deleted, so the trail of who exported what
survives the artefact. `[AX-AC5]`

### 3.5 Failure

Where the job fails while running, a read of the job resource returns status `FAILED` with
a reason the auditor can act on, and the client offers to retry with the same filters.
`[AX-AC6]`

No partially written file is ever exposed for download. `[AX-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/admin/audit-events/exports` | Path and method named by the criteria; responds `202` | `[AX-AC1]` |
| 2 | job resource | Reached through the `Location` header; read with `GET` | `[AX-AC1]` `[AX-AC4]` `[AX-AC6]` |
| 3 | `audit:export` scope | Required to request an export | `[AX-AC1]` |
| 4 | date range (`from`, `to`) | `to` must not precede `from`; no maximum span stated here | `[AX-AC1]` `[AX-AC3]` |
| 5 | `format` | Values `CSV` and `JSONL`; anything else is rejected | `[AX-AC1]` `[AX-AC3]` |
| 6 | job `status` | Values `READY`, `FAILED`; intermediate values not named by any criterion | `[AX-AC1]` `[AX-AC6]` |
| 7 | failure reason | "a reason the auditor can act on" — no vocabulary or format given | `[AX-AC6]` |
| 8 | download link | Valid 24 hours; bound to the requesting auditor | `[AX-AC1]` `[AX-AC4]` `[AX-AC5]` |
| 9 | row ceiling | 50 000 events | `[AX-AC2]` |
| 10 | audit event type | Value `AUDIT_EXPORTED`, carrying requester and range | `[AX-AC1]` |
| 11 | `ProblemDetail.type` | Slugs `export-too-large`, `validation-failed`, `export-expired`; **AX-AC4's 404 carries no type** | `[AX-AC2]` `[AX-AC3]` `[AX-AC5]` |
| 12 | Message strings | Two exact strings, quoted in AX-AC1 and AX-AC2 | `[AX-AC1]` `[AX-AC2]` |

## 5. Out of Scope

- Interactive browsing — US-3.7.
- Scheduled or recurring exports — no criterion reaches them.
- Streaming into an external SIEM — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The completion link is delivered by email. | AX-AC1 says "we will send a link" without naming a channel. See OQ-1. |
| A-2 | The job passes through intermediate states before `READY` or `FAILED`. | AX-AC1 and AX-AC6 name only the terminal states, but the resource is polled, so something must be returned in between. |
| A-3 | The exported file carries the same redaction rules as the interactive view in US-3.7. | Named in the story's Non-Functional section. No criterion constrains the file's columns, only its rows. |
| A-4 | Export jobs run at a lower queue priority than transactional mail. | Named in the story's Non-Functional section. |
| A-5 | The filters accepted here are those of US-3.7 AU-AC2. | AX-AC1 names only a date range, while AX-AC2 speaks of "filters"; the two are only consistent if the wider set is accepted. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | AX-AC1 says "we will send a link" without naming a channel. Email, an in-app notification (US-5.1), or both? The choice couples this story to Epic 5 and to the unapproved mail dependency. | AX-AC1 |
| OQ-2 | AX-AC1 and AX-AC6 name the `READY` and `FAILED` states. What does the job resource return while the export is still running, and is the client expected to poll or to wait for the notification? | AX-AC1, AX-AC6 |
| OQ-3 | AX-AC5 promises the file "has been deleted from object storage" at the 24-hour mark. Is deletion an active step at expiry, or a storage lifecycle rule that may run later? The criterion asserts the former; only the former is testable at 24 hours. | AX-AC5 |
| OQ-4 | AX-AC1 snapshots the rows "when the job started". Nothing states what the auditor is shown if re-running the same filters now yields a different count — the file and the screen will disagree with no explanation. | AX-AC1 |
| OQ-5 | AX-AC6 requires "a reason the auditor can act on" without giving a vocabulary. Which failures are distinguished, and which are collapsed into a generic message? | AX-AC6 |
| OQ-6 | AX-AC3 rejects a format that is not CSV or JSONL, and AX-AC2 rejects over 50 000 rows, but no criterion caps the date range as US-3.7 AU-AC5 does at 90 days. Is an unbounded range acceptable here provided the row count fits? | AX-AC2, AX-AC3 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| AX-AC1 | An export yields 202, then a READY job with a 24-hour link | §3.1, §3.2, §4 | **Partial** — the delivery channel the criterion promises is unnamed (OQ-1) |
| AX-AC2 | More than 50 000 matching events is refused before a job exists | §3.3, §4 | Covered |
| AX-AC3 | An inverted range or unknown format is refused | §3.3, §4 | Covered |
| AX-AC4 | Another auditor's job or link yields 404 | §3.4, §4 | Covered |
| AX-AC5 | The link expires at 24 hours and the file is deleted | §3.4, §4 | **Partial** — whether deletion is active at expiry or eventual is undefined (OQ-3) |
| AX-AC6 | A failed job reports an actionable reason and offers retry | §3.5, §4 | **Partial** — "a reason the auditor can act on" has no defined vocabulary (OQ-5) |

**Coverage:** 3 Covered, 3 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.8-export-audit-information.md`. |
