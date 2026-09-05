# Epic 5 — Notifications: System Announcements

**Story ID:** US-5.8
**Project:** Customer Portal
**AC prefix:** `NA-AC`
**Module:** `notification/` — **proposed; see US-5.1**

## User Story
As an administrator,
I want to send an announcement to everyone or to selected roles,
So that I can warn people about maintenance without resorting to a mailing list outside the system.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Preview | Mandatory before sending, showing the recipient count | An announcement cannot be recalled, so the last chance to catch a mistake is before dispatch |
| 2 | Recall | Not supported once sending has begun | A partial recall is worse than none: some people saw it and some did not, with no record of which |
| 3 | Preferences | Respected, except for the critical-maintenance class | Otherwise every announcement becomes an unblockable channel |
| 4 | Large audiences | Above 10 000 recipients, a second administrator must approve | The blast radius of a mistake at that size justifies the friction |
| 5 | Queue priority | Below transactional mail | A bulk announcement must never delay a password-change notice |

## In Scope
- CRUD and dispatch for announcements over `/api/v1/admin/announcements`
- Audience selection by role and account status, with a live recipient count
- Scheduled sending, editable and cancellable until dispatch
- Second-administrator approval above the size threshold

## Out of Scope
- Per-customer targeting or segmentation beyond role and status
- Rich templating or campaign analytics
- Customer-facing announcement history beyond the notification centre (US-5.1)

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/admin/announcements` | Bearer + `announcements:send` | `{"title", "body", "audience": {"roles": [], "statuses": []}, "class", "scheduledFor"?}` | `201` + `Location` |
| GET | `/api/v1/admin/announcements/{id}/preview` | Bearer + `announcements:send` | — | `200` `{"recipientCount", "renderedSample"}` |
| POST | `/api/v1/admin/announcements/{id}/dispatch` | Bearer + `announcements:send` | `{"approvedBy"?: uuid}` | `202` |
| DELETE | `/api/v1/admin/announcements/{id}` | Bearer + `announcements:send` | — | `204` while still `DRAFT` or `SCHEDULED` |

## Data Model Notes
- `announcements`: `id`, `title`, `body`, `audience` (JSONB), `class`, `status`, `scheduledFor`, `createdBy`, `approvedBy`, `dispatchedAt`, `recipientCount`
- `status` moves `DRAFT` → `SCHEDULED` → `DISPATCHING` → `SENT`; there is no path back from `DISPATCHING`, which is what makes NA-AC6 enforceable
- Fan-out writes one `notifications` row per recipient, in batches, so a partially completed dispatch is resumable
- Dispatch is recorded in `audit_events` with the audience definition and the final recipient count

## Acceptance Criteria

### Happy path
**NA-AC1 — Composing and sending**
```gherkin
Given an administrator holding the announcements:send scope
When they compose an announcement, select the audience "all active customers" and request the preview
Then the preview shows the rendered announcement and the exact recipient count
When they confirm dispatch
Then respond 202 and a notification appears in each recipient's notification centre
And an audit event records the author, the audience definition and the recipient count
```

**NA-AC2 — Scheduled sending**
```gherkin
Given an announcement about scheduled maintenance
When a future send time is chosen
Then the announcement is stored with status SCHEDULED
And it can be edited or deleted at any point before that time
And at the scheduled moment it dispatches automatically
```

### Authorisation
**NA-AC3 — Without the scope**
```gherkin
Given an administrator without the announcements:send scope
When they attempt to create or dispatch an announcement
Then respond 403 with type ".../errors/insufficient-scope"
And the announcements section is not rendered for that caller
And the attempt is recorded in the audit log
```

**NA-AC4 — Large audience needs a second approver**
```gherkin
Given the previewed audience exceeds 10 000 recipients
When dispatch is requested without an approvedBy value
Then respond 422 with type ".../errors/approval-required"
When approvedBy names the requesting administrator themselves
Then respond 422 as well, because approval must come from a second person
```

### Validation
**NA-AC5 — Empty audience or body**
```gherkin
Given no role or status is selected
When dispatch is requested
Then respond 400 with detail "Select at least one audience"
Given a body shorter than 10 characters
Then respond 400 with detail "The announcement must be at least 10 characters"
Given the selected audience currently resolves to no accounts
Then respond 422 with type ".../errors/empty-audience"
And the client keeps the dispatch control disabled
```

### Irreversibility
**NA-AC6 — Cannot recall a dispatched announcement**
```gherkin
Given an announcement whose status is DISPATCHING or SENT
When its detail is opened
Then no edit, delete or recall action is offered
And only "Send a correction" is available, which links the new announcement to the original
When DELETE is called against it directly
Then respond 422 with type ".../errors/announcement-dispatched"
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/approval-required",
  "title": "Approval Required",
  "status": 422,
  "detail": "Announcements to more than 10 000 recipients require approval from a second administrator.",
  "instance": "/api/v1/admin/announcements/0193f2c1-0000-7000-8000-000000000000/dispatch",
  "recipientCount": 18422
}
```
Error `type` slugs introduced by this story: `approval-required`, `empty-audience`, `announcement-dispatched`.

## Non-Functional / Security Requirements
- Announcements respect the preference matrix in US-5.4. The single exception is the critical-maintenance class, which is always delivered in-app — and that class must not be selectable for routine messages.
- Fan-out runs asynchronously in batches on a queue below transactional mail, so US-5.5 delivery is never delayed by a broadcast.
- The recipient count in the preview and the count recorded at dispatch are both stored. When they differ, the audit trail shows the audience changed between preview and send.
- The second-approver check compares account identifiers, not merely scopes; an administrator must not be able to satisfy it alone.
- **Performance:** fan-out to 100 000 recipients completes within 15 minutes and is resumable after a restart without duplicating rows.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| NA-AC1 | Functional suite asserting preview, dispatch, per-recipient rows and the audit entry | `[gate]` |
| NA-AC2 | Integration test with an injected `Clock` across the scheduled moment | `[gate]` |
| NA-AC3 | Functional test asserting `403` without the scope | `[gate]` |
| NA-AC4 | Functional test with a large audience, with and without a distinct approver | `[gate]` |
| NA-AC5 | Slice test on the request record, plus an integration test on the empty audience | `[gate]` |
| NA-AC6 | Functional test calling `DELETE` against a dispatched announcement | `[gate]` |
| Resumable fan-out | Integration test interrupting dispatch and asserting no duplicate rows on resume | `[gate]` |

## Open Questions
1. Who may declare an announcement critical-maintenance, given that class bypasses customer preferences? It should probably require its own scope rather than being a field anyone with `announcements:send` can set.
2. Should announcements also be emailed, or only delivered in-app? Emailing them multiplies the blast radius of a mistake and interacts with the unsubscribe rules in US-5.6.
3. Is 10 000 the right approval threshold? It is a placeholder chosen for order of magnitude, not from any operational data.
