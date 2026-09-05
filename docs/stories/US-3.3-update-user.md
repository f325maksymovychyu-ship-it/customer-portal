# Epic 3 — Administration: Update User

**Story ID:** US-3.3
**Project:** Customer Portal
**AC prefix:** `UU-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to correct a customer's profile details,
So that the directory keeps matching reality as people change names, numbers and positions.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Concurrency control | Optimistic locking through `@Version`, surfaced as an `ETag` | Mandated by `AGENTS.md` §3.2; a last-write-wins directory silently loses corrections |
| 2 | Email changes | Require confirmation at the new address before taking effect | The email is a sign-in credential, so changing it unverified is an account takeover primitive |
| 3 | Audit granularity | Only changed fields, with before and after values | Logging untouched fields buries the actual change in noise |
| 4 | Role changes | Not part of this endpoint | Roles are a privilege decision with different authorisation rules — US-3.5 |
| 5 | Phone format | E.164, at most 15 digits | A national-only pattern silently excludes every customer with a foreign number |
| 6 | Locale and time zone | Stored on the customer, defaulting to `uk-UA` and `Europe/Kyiv` | US-5.5 and US-5.7 both read them; without this story creating the fields, neither can render correctly |

## In Scope
- `PATCH /api/v1/admin/customers/{id}` — update profile attributes
- Optimistic-locking conflict detection
- The two-step email change with confirmation
- Unsaved-changes protection in the client

## Out of Scope
- Role assignment (US-3.5)
- Status changes (US-3.4)
- A customer editing their own profile — not in Release 1.0

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/admin/customers/{id}` | Bearer + `customers:read` | — | `200` `CustomerDetail` + `ETag` |
| PATCH | `/api/v1/admin/customers/{id}` | Bearer + `customers:update`, `If-Match` required | `{"givenName"?, "familyName"?, "phone"?, "position"?, "locale"?, "timeZone"?, "email"?}` | `200` `CustomerDetail` + new `ETag` |

## Data Model Notes
- `customers.version` is the JPA `@Version` column; the `ETag` is derived from it
- `customers.locale` (BCP 47, default `uk-UA`) and `customers.time_zone` (IANA, default `Europe/Kyiv`) — created here because US-5.5 NE-AC2 and US-5.7 NG-AC4 both read them and no other story defines them
- `customers.pending_email` and `email_change_tokens` hold the unconfirmed address until it is verified — the live `email` column is untouched until then
- `audit_events` records one entry per update with a `changes` map of `field → {before, after}`

## Acceptance Criteria

### Happy path
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

### Concurrency and existence
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

### Validation
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

### Client behaviour
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/stale-resource",
  "title": "Stale Resource",
  "status": 409,
  "detail": "This record was modified by another administrator. Reload and try again.",
  "instance": "/api/v1/admin/customers/0193f2c1-0000-7000-8000-000000000000"
}
```
Error `type` slugs introduced by this story: `stale-resource`, `precondition-required`.

## Non-Functional / Security Requirements
- `If-Match` is mandatory, not advisory. Accepting an unconditional PATCH reintroduces exactly the lost-update problem UU-AC3 exists to prevent.
- The live `email` column must never be written by this endpoint. Only the confirmation flow may promote `pending_email`.
- Audit entries must not record unchanged fields, and must never record credential material.
- **Performance:** p95 ≤ 400 ms.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| UU-AC1 | Functional suite asserting the response, the new `ETag` and the audit diff | `[gate]` |
| UU-AC2 | Integration test asserting the live email is unchanged and both mails are queued | `[gate]` |
| UU-AC3 | Integration test with two concurrent updates against one record | `[gate]` |
| UU-AC4 | Functional test covering the missing header and the deleted record | `[gate]` |
| UU-AC5 | Slice test on validation, plus an integration test on the uniqueness conflict | `[gate]` |
| UU-AC6 | Client unit test on the navigation guard | `[gate]` |
| UU-AC7 | Slice test over valid and invalid locale and time-zone values, plus a default-on-create test | `[gate]` |

## Open Questions
1. How long does a pending email change stay valid before it is discarded? US-2.5's 30 minutes is too short for a change an administrator initiates on someone else's behalf.
2. May an administrator cancel a pending email change they started? There is currently no endpoint for it.
3. Should a name change notify the customer? It is their identity, but a flood of notices for administrative corrections would be noise. Interacts with US-5.4.
