---
story_id: US-4.1
title: "Epic 4 — Feedback / Support: Create Ticket"
source: docs/backlog/US-4.1-create-ticket.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Create Ticket

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a signed-in customer, I want to raise a support ticket with a description and
> screenshots, So that I can get help without hunting for a contact address outside the
> portal.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**TC-AC5 — Rate limiting**
```gherkin
Given the customer has created 10 tickets in the last hour
When another creation is attempted
Then respond 429 with type ".../errors/too-many-attempts" and a Retry-After header
And the detail suggests adding to an existing ticket instead
```

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

## 3. Functional Specification

### 3.1 Creating the ticket

A create request from a signed-in customer carrying a valid subject, category, priority
and description responds `201` with a `Location` header and a human-readable reference
such as `#10425`. `[TC-AC1]`

The ticket is created with status `NEW`. `[TC-AC1]`

A confirmation email is queued to the customer. `[TC-AC1]`

The ticket appears at the top of that customer's own ticket list. `[TC-AC1]`

The SLA timer starts against the thresholds for the chosen priority in
`docs/backlog/README.md`, and the resulting first-response and resolution deadlines are
stored on the ticket. `[TC-AC1]`

### 3.2 Attachments

An upload to `POST /api/v1/support/attachments` responds `201` with an attachment
identifier and a scan status per file, and each file reports progress and then a thumbnail.
`[TC-AC2]`

Any uploaded file can be removed before the ticket is submitted. `[TC-AC2]`

Where the create request references uploaded attachment identifiers, those attachments are
visible on the resulting ticket. `[TC-AC2]`

An attachment whose scan has not passed cannot be linked to a ticket. `[TC-AC2]`

### 3.3 Field validation

A subject shorter than 5 characters is rejected with `400`, `type`
`.../errors/validation-failed`, and a detail naming the minimum of 5. `[TC-AC3]`

A description shorter than 20 characters is rejected with `400` and the detail "Describe
the problem in at least 20 characters". `[TC-AC3]`

A subject longer than 120 characters, or a description longer than 5 000, is rejected with
`400` and a detail naming the maximum. `[TC-AC3]`

The client shows a live character counter for both fields and keeps the submit control
disabled until all four bounds are satisfied. `[TC-AC3]`

### 3.4 Attachment validation

A file larger than 10 MB is rejected with `413` and `type`
`.../errors/attachment-too-large`, and previously accepted files in the same form are
unaffected. `[TC-AC4]`

A file whose content signature is an executable is rejected with `415` and `type`
`.../errors/attachment-type-rejected`, and the detail lists exactly PNG, JPEG, PDF, TXT,
LOG and ZIP. The rejection follows the content signature whatever the extension.
`[TC-AC4]`

A sixth file is rejected with `400` and the detail "You can attach at most 5 files".
`[TC-AC4]`

### 3.5 Rate limiting

Where the customer has created 10 tickets in the last hour, another creation responds
`429` with `type` `.../errors/too-many-attempts` and a `Retry-After` header, and the detail
suggests adding to an existing ticket instead. `[TC-AC5]`

### 3.6 Scan outcomes

Where a referenced attachment's scan has not finished, the create request responds `409`
with `type` `.../errors/attachment-scan-pending`, and the client retries automatically for
up to 30 seconds before surfacing the error. `[TC-AC7]`

Where the scan reports a file as malicious, the attachment is deleted and its identifier
becomes permanently unusable. The customer sees "This file was rejected by our security
scan" against that file alone, and the ticket can still be submitted without it.
`[TC-AC7]`

### 3.7 Draft resilience

Where a submission fails with a network error, the client retains the draft locally, and
on returning to the form the draft is restored with a notice. `[TC-AC6]`

Where the customer's session expired while the form was open, submitting sends them to
sign in and returns them to the form afterwards with the draft intact. `[TC-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/support/tickets` | Path and method named by the criteria | `[TC-AC1]` |
| 2 | attachment upload endpoint | Implied by TC-AC2's "uploaded"; **no path is named by any criterion** — see OQ-2 | `[TC-AC2]` |
| 3 | `subject` (request) | Minimum 5 characters; **no maximum stated** | `[TC-AC1]` `[TC-AC3]` |
| 4 | `description` (request) | Minimum 20 characters; **no maximum stated** | `[TC-AC1]` `[TC-AC3]` |
| 5 | `category` (request) | Required and "valid"; **the value set is not specified** — see OQ-3 | `[TC-AC1]` |
| 6 | `priority` (request) | Required; drives the SLA timer; **the value set is not specified** | `[TC-AC1]` |
| 7 | attachment identifiers (request) | Referenced at creation; format not specified | `[TC-AC2]` |
| 8 | attachment size limit | 10 MB per file | `[TC-AC4]` |
| 9 | attachment count limit | 5 files | `[TC-AC4]` |
| 10 | accepted formats | PNG, JPEG, PDF, TXT, LOG, ZIP, enumerated in the `415` detail | `[TC-AC2]` `[TC-AC4]` |
| 11 | attachment scan state | Returned on upload; pending blocks linkage, malicious deletes the file | `[TC-AC2]` `[TC-AC7]` |
| 11a | scan retry window | 30 seconds of client-side retry before the error surfaces | `[TC-AC7]` |
| 12 | ticket `reference` | Human-readable, of the form `#10425`; generation rule not specified | `[TC-AC1]` |
| 13 | ticket `status` | Value `NEW` at creation | `[TC-AC1]` |
| 14 | `Location` (response header) | Points at the new ticket | `[TC-AC1]` |
| 15 | rate limit | 10 tickets per customer per hour | `[TC-AC5]` |
| 16 | `ProblemDetail.type` | Slugs `validation-failed`, `attachment-too-large`, `attachment-type-rejected`, `too-many-attempts` | `[TC-AC3]` `[TC-AC4]` `[TC-AC5]` |
| 17 | Message strings | Two exact strings, quoted in TC-AC3 and TC-AC4 | `[TC-AC3]` `[TC-AC4]` |

## 5. Out of Scope

- Viewing tickets — US-4.2; replying — US-4.4; assignment — US-4.3; resolution — US-4.6.
- Ticket creation by an agent on a customer's behalf — no criterion reaches it.
- SLA threshold values — the timer starts here, but no criterion sets a number.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Attachments are uploaded to a separate endpoint before the ticket is created, and referenced by identifier. | TC-AC2 speaks of files "uploaded" and then of "those attachment identifiers", which only holds if upload precedes creation. |
| A-2 | Attachments are stored in private storage and served through short-lived links, as US-4.2 TL-AC2 requires. | No criterion here constrains storage or retrieval. |
| A-3 | Subject and description are sanitised on render. | Named in the story's Non-Functional section. No criterion constrains rendering. |
| A-4 | The draft in TC-AC6 is held in browser storage on the customer's device. | TC-AC6 says "locally" without naming a store. |
| A-5 | The confirmation email in TC-AC1 is subject to the notification preferences of US-5.4. | No criterion here says whether it can be switched off. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | TC-AC1 starts "the SLA timer for the chosen priority" but no criterion in any story sets a threshold for any priority. Until they exist, neither this timer nor the queue ordering in US-4.3 TQ-AC1 can be tested. | TC-AC1 |
| OQ-2 | TC-AC2 and TC-AC4 describe uploading, scanning, rejecting and referencing attachments, but no criterion names the upload endpoint, its request shape, or how a scan result becomes observable. | TC-AC2, TC-AC4 |
| OQ-3 | TC-AC1 requires a "valid" category and priority without enumerating either. Who owns the category list, and can it change without a release? It drives routing in US-4.3. | TC-AC1 |
| OQ-5 | TC-AC2 forbids linking an attachment "whose scan has not passed". What does the customer see while a scan is pending, and what happens to the draft ticket if a scan fails after the form was filled in? | TC-AC2 |
| OQ-6 | TC-AC3 sets minimums for subject and description but no maximums, while the story's Assumptions table gives 120 and 5 000. Should the upper bounds be criteria? | TC-AC3 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| TC-AC1 | Creation yields 201, a reference, status NEW and a started SLA timer | §3.1, §4 | **Partial** — the SLA timer the criterion starts has no defined threshold (OQ-1) |
| TC-AC2 | Attachments upload, preview, and link only once scanned | §3.2, §4 | Covered — the criterion now names the upload endpoint and its response |
| TC-AC3 | Subject and description bounds are enforced both ends | §3.3, §4 | Covered — the criterion now states maximums as well |
| TC-AC4 | Oversized, wrong-type and excess files are each rejected | §3.4, §4 | Covered — the criterion now enumerates the accepted formats |
| TC-AC5 | Ten tickets an hour is the ceiling | §3.5, §4 | Covered |
| TC-AC6 | A draft survives a network failure and a session expiry | §3.7 | Covered |
| TC-AC7 | Pending and malicious scans are both handled | §3.6, §4 | Covered |

**Coverage:** 6 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-4.1-create-ticket.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
