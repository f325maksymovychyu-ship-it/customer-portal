---
story_id: US-3.3
title: "Epic 3 — Administration: Update User"
source: docs/backlog/US-3.3-update-user.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Update User

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to correct a customer's profile details,
> So that the directory keeps matching reality as people change names, numbers and positions.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**UU-AC1 — Profile fields updated**
```gherkin
Given an administrator holding the customers:update scope
And they hold the current ETag for the record
When PATCH /api/v1/admin/customers/{id} changes the family name and position
Then respond 200 with the updated resource and a new ETag
And an audit_events entry records only those two fields with their before and after values
And fields absent from the request body are left untouched
And the updatable set is given name, family name, phone, position, locale and time zone
```

**UU-AC2 — Email change requires confirmation**
```gherkin
Given an administrator changes a customer's email to "nova@example.com"
When the request is accepted
Then respond 200 and store the value as a pending change, not as the live email
And a confirmation link is sent to the new address
And a notice of the requested change is sent to the current address
And the customer continues to sign in with the old address until the new one is confirmed
And the record shows a "pending email confirmation" indicator
```

**UU-AC3 — Concurrent edit**
```gherkin
Given two administrators opened the same record
And the first has already saved a change
When the second submits with the now-stale If-Match value
Then respond 409 with type ".../errors/stale-resource"
And the second administrator sees "This record was changed by someone else. Reload to see the current version"
And their values do not overwrite the first administrator's silently
```

**UU-AC4 — Missing If-Match, or a deleted record**
```gherkin
Given a PATCH request carrying no If-Match header
When it is submitted
Then respond 428 with type ".../errors/precondition-required"
Given the record no longer exists
When the update is submitted
Then respond 404 and the client shows "This customer no longer exists"
```

**UU-AC5 — Invalid field values**
```gherkin
Given a phone number that is not a valid E.164 number of at most 15 digits
When the update is submitted
Then respond 400 with type ".../errors/validation-failed" naming the field
And any international number in E.164 form is accepted, not only Ukrainian ones
When the new email is already registered to another account
Then respond 409 with type ".../errors/email-already-registered"
And no pending email change is recorded
```

**UU-AC6 — Unsaved changes**
```gherkin
Given the administrator has edited fields without saving
When they navigate away or close the tab
Then a confirmation prompt asks whether to discard the changes
And nothing is persisted unless Save is pressed explicitly
```

**UU-AC7 — Locale and time zone**
```gherkin
Given the customer record carries a locale and an IANA time zone
When either is updated to a value the platform does not recognise
Then respond 400 with type ".../errors/validation-failed" naming the field
When a customer is created without them
Then locale defaults to "uk-UA" and time zone to "Europe/Kyiv"
And every notification rendered for that customer uses these values, per US-5.5 NE-AC2 and US-5.7 NG-AC4
```

## 3. Functional Specification

### 3.1 Updating profile fields

A partial update from a caller holding the `customers:update` scope, carrying the current
`ETag`, responds `200` with the updated resource and a new `ETag`. `[UU-AC1]`

Fields absent from the request body are left untouched. `[UU-AC1]`

An `audit_events` entry records only the changed fields, each with its before and after
value. `[UU-AC1]`

### 3.2 Changing the email address

Where the request changes the email, the response is `200` and the value is stored as a
pending change rather than as the live email. `[UU-AC2]`

A confirmation link is sent to the new address, and a notice of the requested change is
sent to the current address. `[UU-AC2]`

The customer continues to sign in with the old address until the new one is confirmed.
`[UU-AC2]`

The record shows a "pending email confirmation" indicator. `[UU-AC2]`

Where the new email is already registered to another account, the response is `409` with
`type` `.../errors/email-already-registered`, and no pending change is recorded.
`[UU-AC5]`

### 3.3 Concurrency

Where the submitted `If-Match` value is stale because another administrator has already
saved a change, the response is `409` with `type` `.../errors/stale-resource`.
`[UU-AC3]`

The second administrator sees "This record was changed by someone else. Reload to see the
current version", and their values do not overwrite the first administrator's silently.
`[UU-AC3]`

Where the request carries no `If-Match` header at all, the response is `428` with `type`
`.../errors/precondition-required`. `[UU-AC4]`

Where the record no longer exists, the response is `404` and the client shows "This
customer no longer exists". `[UU-AC4]`

### 3.4 Field validation

Where a phone number is not a valid E.164 number of at most 15 digits, the response is
`400` with `type` `.../errors/validation-failed` naming the field. Any international number
in E.164 form is accepted. `[UU-AC5]`

### 3.5 Locale and time zone

The customer record carries a locale and an IANA time zone, both updatable through this
endpoint. `[UU-AC7]`

A value the platform does not recognise is rejected with `400` and `type`
`.../errors/validation-failed` naming the field. `[UU-AC7]`

Where a customer is created without them, the locale defaults to `uk-UA` and the time zone
to `Europe/Kyiv`. `[UU-AC7]`

Every notification rendered for that customer uses these values, as US-5.5 NE-AC2 and
US-5.7 NG-AC4 require. `[UU-AC7]`

### 3.6 Unsaved changes

Where the administrator has edited fields without saving and then navigates away or closes
the tab, a confirmation prompt asks whether to discard the changes. Nothing is persisted
unless Save is pressed explicitly. `[UU-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `PATCH /api/v1/admin/customers/{id}` | Path and method named by the criteria | `[UU-AC1]` |
| 2 | `customers:update` scope | Required for the update | `[UU-AC1]` |
| 3 | `If-Match` (request header) | Mandatory; carries the `ETag` of the record being edited | `[UU-AC1]` `[UU-AC3]` `[UU-AC4]` |
| 4 | `ETag` (response header) | Changes on every successful update | `[UU-AC1]` |
| 5 | family name, position (request) | Named as updatable; formats not specified | `[UU-AC1]` |
| 6 | phone (request) | E.164, at most 15 digits; international numbers accepted | `[UU-AC5]` |
| 6a | `locale` (request) | BCP 47; default `uk-UA` | `[UU-AC1]` `[UU-AC7]` |
| 6b | `timeZone` (request) | IANA zone identifier; default `Europe/Kyiv` | `[UU-AC1]` `[UU-AC7]` |
| 7 | email (request) | Stored as a pending change; unique against live addresses | `[UU-AC2]` `[UU-AC5]` |
| 8 | pending email state | A stored value distinct from the live email, plus a UI indicator | `[UU-AC2]` |
| 9 | confirmation link | Sent to the new address; lifetime and target not specified | `[UU-AC2]` |
| 10 | notice to current address | Sent on request; content not specified | `[UU-AC2]` |
| 11 | `audit_events` entry | Changed fields only, each with before and after values | `[UU-AC1]` |
| 12 | `ProblemDetail.type` | Slugs `stale-resource`, `precondition-required`, `validation-failed`, `email-already-registered` | `[UU-AC3]` `[UU-AC4]` `[UU-AC5]` |
| 13 | Message strings | Two exact strings, quoted in UU-AC3 and UU-AC4 | `[UU-AC3]` `[UU-AC4]` |

## 5. Out of Scope

- Role assignment — US-3.5.
- Status changes — US-3.4.
- A customer editing their own profile — no criterion reaches it.
- Confirming the pending email change. UU-AC2 sends the link; no criterion covers what
  happens when it is followed — see OQ-2.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The `ETag` is derived from a JPA `@Version` column, per `AGENTS.md` §3.2. | UU-AC3 requires stale-write detection without naming a mechanism. |
| A-2 | The audit entry omits unchanged fields entirely rather than recording them as unchanged. | UU-AC1 says "only those two fields", which admits both readings. |
| A-3 | The confirmation link in UU-AC2 targets an endpoint that promotes the pending address to live. | No criterion describes the endpoint; without it UU-AC2's "until the new one is confirmed" never resolves. |
| A-4 | The uniqueness check in UU-AC5 compares against live addresses, not against other pending changes. | UU-AC5 says "already registered to another account". Two simultaneous pending changes to the same address are not addressed. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-2 | UU-AC2 creates a pending email change but no criterion covers confirming it, cancelling it, or letting it expire. How long does it stay pending, and can the administrator withdraw it? | UU-AC2 |
| OQ-3 | Can two pending changes target the same new address on different accounts? A-4 assumes the uniqueness check ignores pending values, which would let both confirm and collide at promotion time. | UU-AC5 |
| OQ-4 | UU-AC1 audits before and after values for every changed field, including personal data such as a phone number. Does the audit log's retention (US-3.7, at least 12 months) conflict with erasure under US-3.4 UD-AC6? | UU-AC1 |
| OQ-5 | Does changing a name or a phone number notify the customer? UU-AC2 notifies on an email change only. Nothing states the intent for other fields. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| UU-AC1 | A conditional update returns a new ETag and audits the diff | §3.1, §4 | Covered — see OQ-4 |
| UU-AC2 | An email change is pending until confirmed at the new address | §3.2, §4 | **Partial** — the confirmation the criterion depends on is specified nowhere (OQ-2) |
| UU-AC3 | A stale If-Match yields 409 and preserves the earlier write | §3.3, §4 | Covered |
| UU-AC4 | A missing If-Match yields 428; a deleted record yields 404 | §3.3, §4 | Covered |
| UU-AC5 | Invalid phone yields 400; duplicate email yields 409 | §3.2, §3.4, §4 | Covered — the criterion now names E.164 |
| UU-AC6 | Navigating away with unsaved edits prompts first | §3.6 | Covered |
| UU-AC7 | Locale and time zone are stored, validated and defaulted | §3.5, §4 | Covered |

**Coverage:** 6 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.3-update-user.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
