# Epic 3 — Administration: Deactivate and Reactivate User

**Story ID:** US-3.4
**Project:** Customer Portal
**AC prefix:** `UD-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to deactivate an account rather than delete it,
So that a departing person loses access immediately while their history stays intact for audit.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Deletion semantics | State transition to `DEACTIVATED`, never a row delete | `AGENTS.md` §3.2 prohibits implicit soft-delete filters and prefers explicit states |
| 2 | Session termination | Every refresh-token family revoked within 60 seconds | Deactivation that leaves live sessions is not deactivation |
| 3 | Reason | Mandatory free text on deactivation | The reason is what makes the audit entry useful in an HR or security investigation |
| 4 | Erasure | A separate, scope-gated operation that anonymises rather than deletes | Satisfies a data-subject request without destroying the audit trail |
| 5 | Password after reactivation | Preserved | Forcing a reset on every reactivation punishes the common case, which is an administrative correction |

## In Scope
- `POST /api/v1/admin/customers/{id}/deactivate` and `/activate`
- Revocation of all sessions belonging to the target
- Self-deactivation and last-administrator invariants
- `POST /api/v1/admin/customers/{id}/erase` — anonymisation on a data-subject request

## Out of Scope
- Profile edits (US-3.3) and role changes (US-3.5)
- The customer ending their own sessions (US-2.3)
- Retention scheduling for anonymised records — no policy exists yet

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| POST | `/api/v1/admin/customers/{id}/deactivate` | Bearer + `customers:deactivate` | `{"reason": str}` | `200` `CustomerDetail` |
| POST | `/api/v1/admin/customers/{id}/activate` | Bearer + `customers:deactivate` | *(empty)* | `200` `CustomerDetail` |
| POST | `/api/v1/admin/customers/{id}/erase` | Bearer + `customers:erase` | `{"requestReference": str}` | `202` |

## Data Model Notes
- `customers.status` moves `ACTIVE` ⇄ `DEACTIVATED`; `deactivated_at`, `deactivated_by` and `deactivation_reason` are written alongside
- Erasure overwrites `email`, `givenName`, `familyName` and `phone` with a tombstone marker and sets `erased_at`; the row and its identifier survive so audit references stay resolvable
- `audit_events` gains `CUSTOMER_DEACTIVATED`, `CUSTOMER_ACTIVATED`, `CUSTOMER_ERASED`

## Acceptance Criteria

### Happy path
**UD-AC1 — Deactivation with a reason**
```gherkin
Given an administrator holding the customers:deactivate scope
When POST /api/v1/admin/customers/{id}/deactivate is called with a reason
Then the account status becomes DEACTIVATED
And every refresh-token family for that customer is revoked within 60 seconds
And the customer's data and history are retained in full
And an audit_events entry records the reason and the acting administrator
And subsequent sign-in attempts are refused per US-2.1 LI-AC5
```

**UD-AC2 — Reactivation**
```gherkin
Given an account whose status is DEACTIVATED
When POST /api/v1/admin/customers/{id}/activate is called
Then the status returns to ACTIVE with the previously assigned roles intact
And the customer is emailed that access has been restored
And they can sign in with their existing password
```

### Invariants
**UD-AC3 — Deactivating yourself**
```gherkin
Given an administrator is viewing their own record
Then the deactivate control is disabled with the explanation "You cannot deactivate your own account"
When the request is sent directly to the API
Then respond 422 with type ".../errors/self-deactivation"
And the status is unchanged
```

**UD-AC4 — The last active administrator**
```gherkin
Given exactly one active account holds the administrator role
When any caller attempts to deactivate it
Then respond 422 with type ".../errors/last-administrator"
And the detail reads "At least one active administrator must remain"
And the same invariant is enforced by US-3.5 when the last administrative role would be removed
```

### Validation
**UD-AC5 — Missing reason, or an already-deactivated target**
```gherkin
Given a deactivation request with a blank reason
When it is submitted
Then respond 400 with type ".../errors/validation-failed"
Given the account is already DEACTIVATED
When deactivation is requested again
Then respond 200 without changing anything, because the operation is idempotent
```

### Erasure
**UD-AC6 — Anonymisation on a data-subject request**
```gherkin
Given a verified erasure request and a caller holding the customers:erase scope
When POST /api/v1/admin/customers/{id}/erase is called with the request reference
Then personal fields are overwritten with a tombstone marker
And the account is deactivated if it was not already
But audit_events entries are retained and continue to reference the anonymised identifier
And the erasure itself is recorded in the audit log with its request reference
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/last-administrator",
  "title": "Last Administrator",
  "status": 422,
  "detail": "At least one active administrator must remain.",
  "instance": "/api/v1/admin/customers/0193f2c1-0000-7000-8000-000000000000/deactivate"
}
```
Error `type` slugs introduced by this story: `self-deactivation`, `last-administrator`.

## Non-Functional / Security Requirements
- No physical delete. A build that removes the row fails this story even if every assertion about access passes.
- Access survives deactivation by at most the access-token TTL of 15 minutes (US-2.4 RT-AC7). Anything tighter requires per-request state checks and is an architect's decision.
- The deactivation reason is personal data about an employment or security matter; it is visible only to callers holding `customers:read` and never returned to the customer.
- UD-AC4 must be enforced inside the transaction, not by a pre-check, or two concurrent deactivations can both pass and leave zero administrators.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| UD-AC1 | Functional suite asserting the state change, revocation and audit entry | `[gate]` |
| UD-AC2 | Functional test asserting roles survive the round trip | `[gate]` |
| UD-AC3 | Functional test asserting `422` on the self-referential call | `[gate]` |
| UD-AC4 | Integration test with two concurrent deactivations of the last two administrators | `[gate]` |
| UD-AC5 | Slice test on validation, plus an idempotency test | `[gate]` |
| UD-AC6 | Integration test asserting audit rows survive and personal columns do not | `[gate]` |
| No hard delete | ArchUnit or repository test asserting no delete path exists for `Customer` | `[gate]` |

## Open Questions
1. **Escalation — `AGENTS.md` §7.4.** UD-AC6 touches GDPR erasure policy and must have human sign-off; the tombstone shape and what counts as a verified request are not the assistant's to decide.
2. How long are anonymised rows kept before removal, and does any regulation require their eventual deletion? No retention policy exists.
3. Should deactivation notify the customer? Current default is silence, on the assumption that HR communicates separately — but that assumption is untested.
