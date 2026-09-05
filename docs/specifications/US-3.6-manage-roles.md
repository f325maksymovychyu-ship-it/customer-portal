---
story_id: US-3.6
title: "Epic 3 — Administration: Manage Roles and Permissions"
source: docs/backlog/US-3.6-manage-roles.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Manage Roles and Permissions

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to define roles and the permissions they carry,
> So that the access model can follow the organisation without waiting for a release.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**MR-AC7 — Caller without the scope**
```gherkin
Given a caller without the roles:manage scope
When any write operation on /api/v1/admin/roles is attempted
Then respond 403 with type ".../errors/insufficient-scope"
And the attempt is recorded in the audit log
But a caller holding only roles:read can still list roles and permissions
```

## 3. Functional Specification

### 3.1 Creating a role

A create request from a caller holding the `roles:manage` scope, carrying a name and a set
of permission keys, responds `201` with a `Location` header. `[MR-AC1]`

The role becomes available for assignment through US-3.5. `[MR-AC1]`

An `audit_events` entry records the full permission set it was created with.
`[MR-AC1]`

Where a role with the same name already exists, the response is `409` with `type`
`.../errors/role-name-taken`. Comparison ignores case and surrounding whitespace.
`[MR-AC3]`

### 3.2 Changing a role

Where a role's permission set is changed, the response reports how many customers are
affected. `[MR-AC2]`

Every holder of that role gains the added permission within 60 seconds of the change.
`[MR-AC2]`

An `audit_events` entry records the permission set before and after. `[MR-AC2]`

### 3.3 Permission catalogue

Where a request names a permission key that the code catalogue does not declare, the
response is `400` with `type` `.../errors/unknown-permission`, the `ProblemDetail` names
the offending keys, and no role is created or modified. `[MR-AC4]`

### 3.4 Deletion

Where at least one customer holds the role, deletion responds `409` with `type`
`.../errors/role-in-use`, and the response reports how many customers hold it.
`[MR-AC5]`

The role and its assignments are untouched, and the client offers to reassign those
customers before retrying. `[MR-AC5]`

### 3.5 System roles

Where a role is flagged system-defined, its permission matrix is returned read-only and
the client renders no edit or delete action. `[MR-AC6]`

A direct `PUT` or `DELETE` against it responds `403` with `type`
`.../errors/system-role-immutable`. `[MR-AC6]`

### 3.6 Authorisation

Any write operation attempted without the `roles:manage` scope responds `403` with `type`
`.../errors/insufficient-scope`, and the attempt is recorded in the audit log.
`[MR-AC7]`

A caller holding only `roles:read` can still list roles and permissions. `[MR-AC7]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/admin/roles` | Path and method named by the criteria | `[MR-AC1]` |
| 2 | `PUT /api/v1/admin/roles/{id}` | Path and method named by the criteria | `[MR-AC2]` `[MR-AC6]` |
| 3 | `DELETE /api/v1/admin/roles/{id}` | Path and method named by the criteria; success status not specified | `[MR-AC5]` `[MR-AC6]` |
| 4 | listing endpoints | Implied by MR-AC7's "list roles and permissions"; paths not stated by any criterion | `[MR-AC7]` |
| 5 | `roles:manage`, `roles:read` scopes | Gate writes and reads respectively | `[MR-AC1]` `[MR-AC7]` |
| 6 | role name | Unique, compared case- and whitespace-insensitively; no length bound stated | `[MR-AC1]` `[MR-AC3]` |
| 7 | permission keys | Validated against a catalogue declared in code; example values `tickets:reassign`, `audit:read` | `[MR-AC2]` `[MR-AC4]` |
| 8 | system-defined flag | Boolean; **which roles carry it is not specified** — see OQ-1 | `[MR-AC6]` |
| 9 | affected-holder count | Integer, returned by both MR-AC2 and MR-AC5 | `[MR-AC2]` `[MR-AC5]` |
| 10 | propagation window | 60 seconds from a permission change | `[MR-AC2]` |
| 11 | `audit_events` entry | Full set on creation; before and after on change | `[MR-AC1]` `[MR-AC2]` |
| 12 | `ProblemDetail.type` | Slugs `role-name-taken`, `unknown-permission`, `role-in-use`, `system-role-immutable`, `insufficient-scope` | `[MR-AC3]` `[MR-AC4]` `[MR-AC5]` `[MR-AC6]` `[MR-AC7]` |

## 5. Out of Scope

- Assigning roles to customers — US-3.5.
- Role hierarchy or permission inheritance — no criterion reaches it; roles are flat.
- Delegated administration of a role subset — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The permission catalogue is declared in code and exposed read-only; roles reference keys from it. | MR-AC4 refers to "the code catalogue" as a given without defining where it lives or how it is read. |
| A-2 | Roles are flat and additive, with no inheritance. | Named in the story's Assumptions table. RA-AC2 in US-3.5 de-duplicates permissions across roles, which is consistent with but does not establish flatness. |
| A-3 | A system role can be changed only through a migration and a release. | Named in the story's Non-Functional section. MR-AC6 forbids the API path but says nothing about any other. |
| A-4 | `PUT` replaces the role's permission set rather than patching it. | MR-AC2 describes "adds the permission" using `PUT`. The two readings differ for every other field in the payload. |
| A-5 | The write requires `If-Match`, as the story's API Contract states. | No criterion mentions it; without it MR-AC2's before-and-after audit can record a set the administrator never saw. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | MR-AC6 protects roles "flagged as system-defined" but no criterion says which roles those are, or who sets the flag. At minimum the administrator role is implied by US-3.4 UD-AC4, but support-agent and auditor are undecided. | MR-AC6 |
| OQ-2 | MR-AC2 promises every holder gains a permission "within 60 seconds", while US-3.5 RA-AC6 makes revocation immediate through `token_generation`. Do permission changes made *here* also increment it, and if not, how does a removed permission stop applying? | MR-AC2 |
| OQ-3 | MR-AC5 has the client "offer to reassign those customers", but no criterion or endpoint describes a bulk reassignment. Does one exist? | MR-AC5 |
| OQ-4 | MR-AC7 permits `roles:read` to list roles and permissions, but no criterion gives the listing endpoints, their shape, or their paging behaviour. | MR-AC7 |
| OQ-5 | MR-AC4 rejects keys the catalogue does not declare. What happens to a role that already holds a key which a later release removes from the catalogue? | MR-AC4 |
| OQ-6 | The story's Non-Functional section requires an ArchUnit rule asserting every declared key is referenced by at least one `@PreAuthorize`. No criterion asserts it, though a key nothing reads grants nothing while looking like access. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| MR-AC1 | Creating a role yields 201 and audits the permission set | §3.1, §4 | Covered |
| MR-AC2 | Changing a role reports and reaches its holders within 60 s | §3.2, §4 | **Partial** — the removal path the propagation window implies is undefined (OQ-2) |
| MR-AC3 | A duplicate name yields 409, ignoring case and whitespace | §3.1, §4 | Covered |
| MR-AC4 | An unknown permission key yields 400 and changes nothing | §3.3, §4 | Covered — see OQ-5 |
| MR-AC5 | Deleting an assigned role yields 409 and offers reassignment | §3.4, §4 | **Partial** — the reassignment the criterion offers does not exist (OQ-3) |
| MR-AC6 | System roles are read-only through the API | §3.5, §4 | **Partial** — which roles are system-defined is undefined (OQ-1) |
| MR-AC7 | Writes need `roles:manage`; reads need only `roles:read` | §3.6, §4 | **Partial** — the read endpoints it permits are unspecified (OQ-4) |

**Coverage:** 3 Covered, 4 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.6-manage-roles.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
