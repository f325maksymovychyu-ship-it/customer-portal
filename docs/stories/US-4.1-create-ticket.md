# Epic 4 — Feedback / Support: Create Ticket

**Story ID:** US-4.1
**Project:** Customer Portal
**AC prefix:** `TC-AC`
**Module:** `support/`

## User Story
As a signed-in customer,
I want to raise a support ticket with a description and screenshots,
So that I can get help without hunting for a contact address outside the portal.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Field bounds | Subject 5–120 characters, description 20–5 000 | A one-word ticket costs an agent a round trip before work can start |
| 2 | Attachments | Up to 5 files, 10 MB each | Enough for a set of screenshots and a log, small enough to bound storage and scanning |
| 3 | Accepted types | PNG, JPEG, PDF, TXT, LOG, ZIP, checked by content signature | Extension checks are trivially bypassed |
| 4 | Anti-spam | 10 tickets per customer per hour | High enough that no honest user meets it |
| 5 | Draft safety | The client keeps a local draft until submission succeeds | Losing a long description to a dropped connection is the most common complaint about ticket forms |
| 6 | Category vocabulary | `ACCOUNT`, `BILLING`, `TECHNICAL`, `OTHER`, fixed in code | An editable list would make the routing in US-4.3 editable too, without a release or a review |
| 7 | Priority vocabulary | `CRITICAL`, `HIGH`, `NORMAL`, `LOW`, chosen by the customer | Matches the SLA table in `docs/backlog/README.md`. Whether customers may set `CRITICAL` themselves is Open Question 3 |

## In Scope
- `POST /api/v1/support/tickets` — create the ticket
- Attachment upload, type and size validation, and antivirus scanning
- Per-customer rate limiting
- Local draft recovery in the client, including across a session expiry

## Out of Scope
- Viewing tickets (US-4.2), replying (US-4.4), assignment (US-4.3), resolution (US-4.6)
- Ticket creation by an agent on a customer's behalf
- Setting the SLA thresholds themselves. TC-AC1 references the table in `docs/backlog/README.md`; agreeing its values is a product decision, not this story's

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/support/attachments` | Bearer | `multipart/form-data` | `201` `{"attachmentId", "filename", "size", "scanStatus"}` |
| POST | `/api/v1/support/tickets` | Bearer | `{"subject", "category", "priority", "description", "attachmentIds": [uuid], "relatedTicketId"?: uuid}` | `201` + `Location: /api/v1/support/tickets/{id}` |

## Data Model Notes
- `tickets`: `id`, `reference` (human-facing, e.g. `#10425`), `customerId`, `subject`, `category`, `priority`, `status`, `assigneeId`, `createdAt`, `firstRespondedAt`, `resolvedAt`, `version`
- `status` starts at `NEW`; the full state machine is defined in `README.md` and enforced by US-4.6
- `ticket_attachments` references object storage by key and carries `scanStatus` — an attachment is only linkable once its scan has passed
- `reference` is generated from a sequence separate from the UUID key, because customers must be able to quote it over the phone

## Acceptance Criteria

### Happy path
**TC-AC1 — Ticket created**
```gherkin
Given a signed-in customer
When POST /api/v1/support/tickets is called with a valid subject, category, priority and description
Then respond 201 with a Location header and a human-readable reference such as "#10425"
And the ticket is created with status NEW
And a confirmation email is queued to the customer
And the ticket appears at the top of their own ticket list
And the SLA timer starts against the thresholds for the chosen priority in docs/backlog/README.md
And the resulting first-response and resolution deadlines are stored on the ticket
```

**TC-AC2 — Attaching files**
```gherkin
Given the customer posts three PNG files totalling 6 MB to POST /api/v1/support/attachments
When each upload completes
Then respond 201 with an attachment identifier and a scan status per file
And each file reports progress and then a thumbnail
And any file can be removed before the ticket is submitted
When the ticket is created referencing those attachment identifiers
Then the attachments are visible on the ticket
But an attachment whose scan has not passed cannot be linked to a ticket
```

### Validation
**TC-AC3 — Field bounds**
```gherkin
Given a subject of 3 characters
When the ticket is submitted
Then respond 400 with type ".../errors/validation-failed" and detail naming the minimum of 5
Given a subject longer than 120 characters
Then respond 400 with detail naming the maximum of 120
Given a description shorter than 20 characters
Then respond 400 with detail "Describe the problem in at least 20 characters"
Given a description longer than 5 000 characters
Then respond 400 with detail naming the maximum of 5 000
And the client shows a live character counter for both fields and keeps the submit control disabled until all four bounds are satisfied
```

**TC-AC4 — Rejected attachments**
```gherkin
Given a file larger than 10 MB
When it is uploaded
Then respond 413 with type ".../errors/attachment-too-large"
And previously accepted files in the same form are unaffected
Given a file whose content signature is an executable, whatever its extension
Then respond 415 with type ".../errors/attachment-type-rejected"
And the detail lists exactly PNG, JPEG, PDF, TXT, LOG and ZIP
Given a sixth file
Then respond 400 with detail "You can attach at most 5 files"
```

### Abuse prevention
**TC-AC5 — Rate limiting**
```gherkin
Given the customer has created 10 tickets in the last hour
When another creation is attempted
Then respond 429 with type ".../errors/too-many-attempts" and a Retry-After header
And the detail suggests adding to an existing ticket instead
```

### Resilience
**TC-AC6 — Lost connection and expired session**
```gherkin
Given the customer has typed a description
And the submission fails with a network error
Then the client retains the draft locally
And on returning to the form the draft is restored with a notice
Given the customer's session expired while the form was open
When they submit
Then they are sent to sign in and returned to the form afterwards with the draft intact
```

**TC-AC7 — Attachment scanning**
```gherkin
Given a file has been uploaded but its scan has not finished
When the customer submits the ticket referencing it
Then respond 409 with type ".../errors/attachment-scan-pending"
And the client retries automatically for up to 30 seconds before surfacing the error
Given the scan reports the file as malicious
Then the attachment is deleted and its identifier becomes permanently unusable
And the customer sees "This file was rejected by our security scan" against that file only
And the ticket can still be submitted without it
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/attachment-type-rejected",
  "title": "Attachment Type Rejected",
  "status": 415,
  "detail": "Accepted formats are PNG, JPEG, PDF, TXT, LOG and ZIP.",
  "instance": "/api/v1/support/attachments",
  "rejectedFilename": "setup.exe"
}
```
Error `type` slugs introduced by this story: `attachment-too-large`, `attachment-type-rejected`, `attachment-scan-pending`.

## Non-Functional / Security Requirements
- File type is determined by content signature, never by the supplied extension or `Content-Type` header.
- Every attachment is scanned for malware before it becomes linkable. An unscanned file must not be reachable by any reader, including the uploading customer.
- Attachments live in private object storage and are served only through short-lived, account-bound links; the bucket is never public.
- Description and subject are sanitised on render. If any markup is supported, it renders through an allow-list, never by trusting stored content.
- **Performance:** a 10 MB upload completes within 10 s at a 10 Mbit/s client link; ticket creation itself is p95 ≤ 500 ms excluding uploads.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| TC-AC1 | Functional suite asserting `201`, reference, status and queued mail | `[gate]` |
| TC-AC2 | Integration test asserting an unscanned attachment cannot be linked | `[gate]` |
| TC-AC3 | Slice test on the request record's validation constraints | `[gate]` |
| TC-AC4 | Integration test with a renamed executable and an oversized file | `[gate]` |
| TC-AC5 | Integration test against a deterministic injected `Clock` | `[gate]` |
| TC-AC6 | Client unit test on draft persistence and restoration | `[gate]` |
| TC-AC7 | Integration test with the scanner stubbed pending, then clean, then malicious | `[gate]` |
| XSS on render | Security test submitting a script payload and asserting it renders inert | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.5.** Object storage and an antivirus scanner are both new runtime dependencies. Until they are approved this story is blocked, and TC-AC2, TC-AC4 and TC-AC7 cannot be built as written.
2. The SLA thresholds TC-AC1 now references are a proposal in `docs/backlog/README.md` and need a product owner's sign-off. "Business day" is undefined there — no calendar, working hours or holiday set exists — so only the `CRITICAL` row is currently testable.
3. May a customer set `CRITICAL` themselves? Decision 7 assumes yes, which makes the tightest SLA self-service and invites inflation. The alternative is that customers choose from a narrower set and agents escalate.
4. Should a customer be able to raise a ticket while signed out, identified only by an email address? That would remove the session dependency in TC-AC6 but opens an unauthenticated write endpoint.
5. TC-AC7 retries a pending scan for 30 seconds. Nothing states the expected scan duration, so that figure is a guess; a large ZIP may exceed it routinely.
