# Epic 3 — Administration: Manage Roles and Permissions

**Story ID:** US-3.6
**Project:** Customer Portal
**AC prefix:** `MR-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to define roles and the permissions they carry,
So that the access model can follow the organisation without waiting for a release.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Permission catalogue | Declared in code, exposed read-only | An arbitrary permission string grants nothing and silently creates dead access rules |
| 2 | Role hierarchy | None; roles are flat and additive | Inheritance makes the effective set hard to reason about, and nothing in Release 1.0 needs it |
| 3 | System roles | Read-only in the UI and the API | The administrator role must not be editable into uselessness by its own holder |
| 4 | Deletion | Refused while any customer holds the role | A silent cascade would strip access from people with no record of why |
| 5 | Cache invalidation | Permission caches expire within 60 seconds of a change | Revoked permissions outliving that window make MR-AC2 untrue in practice |

## In Scope
- CRUD for roles over `/api/v1/admin/roles`
- The permission matrix, presented as resource × action
- Impact warning before changing a role that is in use
- Protection of system-defined roles

## Out of Scope
- Assigning roles to customers (US-3.5)
- Per-record permissions or ownership rules — the model is role-based only
- Delegated administration of a subset of roles

## API Contract
| Method | Path | Auth | Request Body | Success |
|---|---|---|---|---|
| GET | `/api/v1/admin/permissions` | Bearer + `roles:read` | — | `200` `{"permissions": [{"resource", "action", "key"}]}` |
| GET | `/api/v1/admin/roles` | Bearer + `roles:read` | — | `200` `Page<RoleSummary>` |
| POST | `/api/v1/admin/roles` | Bearer + `roles:manage` | `{"name", "description", "permissionKeys": [str]}` | `201` + `Location` |
| PUT | `/api/v1/admin/roles/{id}` | Bearer + `roles:manage`, `If-Match` | same shape | `200` `RoleDetail` |
| DELETE | `/api/v1/admin/roles/{id}` | Bearer + `roles:manage` | — | `204` |

## Data Model Notes
- `roles`: `id`, `name`, `description`, `system` (boolean), `version`
- `role_permissions` stores permission **keys**, validated on write against the code-declared catalogue
- `roles.name` carries a case-insensitive unique constraint, trimmed, so MR-AC3 is enforced by the database
- Deleting a role is blocked by the foreign key from `customer_roles`; the API turns that into a readable `409` rather than a 500

## Acceptance Criteria

### Happy path
**MR-AC1 — Creating a role**
```gherkin
Given an administrator holding the roles:manage scope
When POST /api/v1/admin/roles is called with a name and a set of permission keys
Then respond 201 with a Location header
And the role becomes available for assignment in US-3.5
And an audit_events entry records the full permission set it was created with
```

**MR-AC2 — Changing a role that is in use**
```gherkin
Given the support-agent role is held by 12 customers
When PUT /api/v1/admin/roles/{id} adds the permission "tickets:reassign"
Then the response reports how many customers are affected
And after the change every holder gains that permission within 60 seconds
And an audit_events entry records the permission set before and after
```

### Conflict and validation
**MR-AC3 — Duplicate role name**
```gherkin
Given a role named "Support Agent" already exists
When POST /api/v1/admin/roles is called with the name "support agent"
Then respond 409 with type ".../errors/role-name-taken"
And comparison ignores case and surrounding whitespace
```

**MR-AC4 — Unknown permission key**
```gherkin
Given a request naming a permission key that the code catalogue does not declare
When the role is created or updated
Then respond 400 with type ".../errors/unknown-permission"
And the ProblemDetail names the offending keys
And no role is created or modified
```

### Deletion and protection
**MR-AC5 — Deleting a role that is assigned**
```gherkin
Given at least one customer holds the role
When DELETE /api/v1/admin/roles/{id} is called
Then respond 409 with type ".../errors/role-in-use"
And the response reports how many customers hold it
And the client offers to reassign those customers before retrying
And the role and its assignments are untouched
```

**MR-AC6 — System roles are immutable**
```gherkin
Given a role flagged as system-defined
When its detail is requested
Then the permission matrix is returned read-only
And the client renders no edit or delete action
When PUT or DELETE is called against it directly
Then respond 403 with type ".../errors/system-role-immutable"
```

### Authorisation
**MR-AC7 — Caller without the scope**
```gherkin
Given a caller without the roles:manage scope
When any write operation on /api/v1/admin/roles is attempted
Then respond 403 with type ".../errors/insufficient-scope"
And the attempt is recorded in the audit log
But a caller holding only roles:read can still list roles and permissions
```

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/role-in-use",
  "title": "Role In Use",
  "status": 409,
  "detail": "12 customers hold this role. Reassign them before deleting it.",
  "instance": "/api/v1/admin/roles/0193f2c1-0000-7000-8000-000000000000",
  "holderCount": 12
}
```
Error `type` slugs introduced by this story: `role-name-taken`, `unknown-permission`, `role-in-use`, `system-role-immutable`.

## Non-Functional / Security Requirements
- The permission catalogue is the single source of truth and lives in code. A role must never hold a key that no `@PreAuthorize` expression reads, because such a key looks like access and grants none.
- Changing a system role is possible only through a Flyway migration and a release, never through the API.
- Permission caches must invalidate within 60 seconds. A longer window means a revoked permission stays live past what US-3.5 RA-AC6 promises.
- Every role mutation is audited with the full before and after permission sets, not just the delta, so a reviewer can reconstruct state without replaying history.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| MR-AC1 | Functional suite asserting `201`, `Location` and the audit entry | `[gate]` |
| MR-AC2 | Integration test asserting a holder gains the permission after the change | `[gate]` |
| MR-AC3 | Integration test covering case and whitespace variants | `[gate]` |
| MR-AC4 | Slice test rejecting an undeclared key | `[gate]` |
| MR-AC5 | Integration test asserting the `409` and the untouched assignments | `[gate]` |
| MR-AC6 | Functional test asserting `403` on direct PUT and DELETE | `[gate]` |
| MR-AC7 | Functional test per scope | `[gate]` |
| Catalogue consistency | ArchUnit test asserting every declared key is referenced by at least one `@PreAuthorize` | `[gate]` |

## Open Questions
1. Which roles ship as system-defined? At minimum the administrator role, but whether support-agent and auditor are also protected is a product decision.
2. Does a role need an "effective holders" endpoint for MR-AC5's reassignment flow, or is the existing directory filter in US-3.1 enough?
3. **Escalation — `AGENTS.md` §7.1.** Making the permission model editable at runtime is an authorisation-scheme change and needs architect sign-off before this story is scheduled.
