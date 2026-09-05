# Epic 3 — Administration: Assign Roles to a User

**Story ID:** US-3.5
**Project:** Customer Portal
**AC prefix:** `RA-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to grant and revoke a customer's roles,
So that their access matches their current responsibilities and nothing more.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Self-service | An administrator can never change their own roles | Separation of duties; otherwise every administrator is one request away from full control |
| 2 | Privilege ceiling | Nobody can grant a permission they do not themselves hold | Without this, one narrow scope escalates to every scope |
| 3 | Minimum roles | An account must always hold at least one | A role-less account can sign in and do nothing, which reads as an outage |
| 4 | Propagation | New permissions apply at the next token refresh, within 15 minutes | Follows the access-token TTL rather than adding a per-request lookup |
| 5 | Revocation urgency | Removing a role marks existing tokens stale immediately | Revocation must be faster than granting, because it is the security-relevant direction |

## In Scope
- `PUT /api/v1/admin/customers/{id}/roles` — replace the assigned role set
- Effective-permission preview before saving
- Self-assignment and privilege-escalation prevention
- Immediate staleness marking on revocation

## Out of Scope
- Defining roles and their permission sets (US-3.6)
- Creating accounts with an initial role set (US-3.2)
- Per-resource or per-record permissions — the model is role-based only

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/admin/customers/{id}/roles` | Bearer + `customers:read` | — | `200` `{"roles": [...], "effectivePermissions": [{"permission", "grantedBy"}]}` |
| PUT | `/api/v1/admin/customers/{id}/roles` | Bearer + `roles:assign`, `If-Match` required | `{"roleIds": [uuid]}` | `200` same shape |

## Data Model Notes
- `customer_roles` is the join table; a `PUT` replaces the set rather than patching it, so the request is idempotent
- `customers.token_generation` is incremented on any role change; tokens carrying an older generation are treated as stale by the resource server
- `audit_events` records `ROLE_GRANTED` and `ROLE_REVOKED` as separate entries, one per role, so a diff is readable without reconstructing set arithmetic

## Acceptance Criteria

### Happy path
**RA-AC1 — Granting and revoking**
```gherkin
Given an administrator holding the roles:assign scope
When PUT /api/v1/admin/customers/{id}/roles adds the support-agent role
Then respond 200 with the updated role set
And an audit_events entry with event ROLE_GRANTED records the role, the actor and the time
When the same role is removed by a later request
Then an entry with event ROLE_REVOKED is written in the same shape
```

**RA-AC2 — Effective permission preview**
```gherkin
Given two roles are selected whose permission sets partly overlap
When GET /api/v1/admin/customers/{id}/roles is called
Then the effective permissions are returned as a de-duplicated list
And each permission names the role or roles it comes from
```

### Authorisation
**RA-AC3 — Changing your own roles**
```gherkin
Given an administrator is authenticated
When PUT /api/v1/admin/customers/{id}/roles targets their own account
Then respond 403 with type ".../errors/self-role-change"
And the detail reads "You cannot change your own roles. Ask another administrator"
And the change is refused even when it would only remove permissions
```

**RA-AC4 — Granting beyond the caller's ceiling**
```gherkin
Given an administrator whose own roles do not include the permission "audit:read"
When they attempt to grant a role that carries "audit:read"
Then respond 403 with type ".../errors/privilege-escalation"
And no part of the requested role set is applied
And the attempt is recorded in the audit log as a security event
```

### Invariants
**RA-AC5 — Removing the last role**
```gherkin
Given a customer holds exactly one role
When PUT /api/v1/admin/customers/{id}/roles is called with an empty roleIds array
Then respond 422 with type ".../errors/no-roles-assigned"
And the detail reads "A customer must hold at least one role. Replace the role or deactivate the account"
And the same last-administrator invariant as US-3.4 UD-AC4 applies when the removal would leave no active administrator
```

### Propagation
**RA-AC6 — Role change during an active session**
```gherkin
Given a customer is working with a currently valid access token
When a role is revoked from them
Then customers.token_generation is incremented immediately
And a request to a resource covered only by the revoked role responds 403 even within the current access token's lifetime
And the next refresh issues a token carrying the new permission set
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/privilege-escalation",
  "title": "Privilege Escalation Refused",
  "status": 403,
  "detail": "You cannot grant a permission you do not hold yourself.",
  "instance": "/api/v1/admin/customers/0193f2c1-0000-7000-8000-000000000000/roles"
}
```
Error `type` slugs introduced by this story: `self-role-change`, `no-roles-assigned`. `privilege-escalation` is shared with US-3.2.

## Non-Functional / Security Requirements
- Authorisation is evaluated on every request through `@PreAuthorize`, never only at sign-in (`AGENTS.md` §3.4).
- RA-AC3 and RA-AC4 are the two escalation paths that matter in a flat role model. Both must be covered by tests that would fail loudly, not by review discipline.
- Granting is allowed to lag by up to 15 minutes; revocation is not. RA-AC6 exists precisely so the two directions are not implemented symmetrically.
- Every role change is audited, including the attempted ones that were refused.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| RA-AC1 | Functional suite asserting both audit entries | `[gate]` |
| RA-AC2 | Integration test with two deliberately overlapping roles | `[gate]` |
| RA-AC3 | Functional test asserting `403` on a self-targeted call | `[gate]` |
| RA-AC4 | Functional test with a caller lacking the permission being granted | `[gate]` |
| RA-AC5 | Functional test on the empty set, plus the last-administrator case | `[gate]` |
| RA-AC6 | Integration test asserting `403` before any refresh occurs | `[gate]` |
| Server-side authorisation | ArchUnit rule asserting every admin controller method carries `@PreAuthorize` | `[gate]` |

## Open Questions
1. Should the customer be notified when their roles change? It is a material change to their account, but it may also be routine administration. Interacts with US-5.4.
2. RA-AC6 requires the resource server to check `token_generation` on each request, which is a per-request database or cache read. **Escalation — `AGENTS.md` §7.1:** this changes the stateless-JWT posture and needs architect sign-off.
3. Does the privilege ceiling compare individual permissions or whole roles? Permission-level comparison is stricter and assumed here, but it is more expensive to evaluate.
