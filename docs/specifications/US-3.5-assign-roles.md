---
story_id: US-3.5
title: "Epic 3 — Administration: Assign Roles to a User"
source: docs/backlog/US-3.5-assign-roles.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Assign Roles to a User

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to grant and revoke a customer's roles,
> So that their access matches their current responsibilities and nothing more.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

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

**RA-AC5 — Removing the last role**
```gherkin
Given a customer holds exactly one role
When PUT /api/v1/admin/customers/{id}/roles is called with an empty roleIds array
Then respond 422 with type ".../errors/no-roles-assigned"
And the detail reads "A customer must hold at least one role. Replace the role or deactivate the account"
And the same last-administrator invariant as US-3.4 UD-AC4 applies when the removal would leave no active administrator
```

**RA-AC6 — Role change during an active session**
```gherkin
Given a customer is working with a currently valid access token
When a role is revoked from them
Then customers.token_generation is incremented immediately
And a request to a resource covered only by the revoked role responds 403 even within the current access token's lifetime
And the next refresh issues a token carrying the new permission set
```

## 3. Functional Specification

### 3.1 Replacing the role set

A role assignment from a caller holding the `roles:assign` scope responds `200` with the
updated role set. `[RA-AC1]`

Adding a role writes an `audit_events` entry with event `ROLE_GRANTED` recording the
role, the actor and the time. Removing one writes an entry with event `ROLE_REVOKED` in
the same shape. `[RA-AC1]`

### 3.2 Effective permissions

A read of a customer's roles returns their effective permissions as a de-duplicated list,
each naming the role or roles it comes from. `[RA-AC2]`

### 3.3 Separation of duties

Where the request targets the calling administrator's own account, the response is `403`
with `type` `.../errors/self-role-change` and the detail "You cannot change your own
roles. Ask another administrator". `[RA-AC3]`

The refusal applies even where the change would only remove permissions. `[RA-AC3]`

### 3.4 Privilege ceiling

Where the caller's own roles do not include a permission carried by a role they are
granting, the response is `403` with `type` `.../errors/privilege-escalation`.
`[RA-AC4]`

No part of the requested role set is applied. `[RA-AC4]`

The attempt is recorded in the audit log as a security event. `[RA-AC4]`

### 3.5 Minimum role invariant

Where the request would leave a customer with no roles, the response is `422` with `type`
`.../errors/no-roles-assigned` and the detail "A customer must hold at least one role.
Replace the role or deactivate the account". `[RA-AC5]`

Where the removal would leave no active administrator, the invariant of US-3.4 UD-AC4
applies. `[RA-AC5]`

### 3.6 Propagation to live sessions

Where a role is revoked, `customers.token_generation` is incremented immediately.
`[RA-AC6]`

A request to a resource covered only by the revoked role responds `403` even within the
current access token's lifetime. `[RA-AC6]`

The next refresh issues a token carrying the new permission set. `[RA-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/admin/customers/{id}/roles` | Path and method named by the criteria | `[RA-AC2]` |
| 2 | `PUT /api/v1/admin/customers/{id}/roles` | Path and method named by the criteria; replaces the set | `[RA-AC1]` `[RA-AC5]` |
| 3 | `roles:assign` scope | Required for the write | `[RA-AC1]` |
| 4 | `roleIds` (request) | Array; an empty array is rejected | `[RA-AC5]` |
| 5 | effective permissions (response) | De-duplicated, each attributed to its source role or roles | `[RA-AC2]` |
| 6 | permission key | Example value `audit:read`; the catalogue is defined by US-3.6 | `[RA-AC4]` |
| 7 | `customers.token_generation` | Incremented on revocation; type and initial value not specified | `[RA-AC6]` |
| 8 | `audit_events.event` | Values `ROLE_GRANTED`, `ROLE_REVOKED`, one entry per role | `[RA-AC1]` |
| 9 | `ProblemDetail.type` | Slugs `self-role-change`, `privilege-escalation`, `no-roles-assigned` | `[RA-AC3]` `[RA-AC4]` `[RA-AC5]` |
| 10 | Message strings | Two exact strings, quoted in RA-AC3 and RA-AC5 | `[RA-AC3]` `[RA-AC5]` |

The story's API Contract requires `If-Match` on the write. No criterion mentions it — see
A-3 and OQ-4.

## 5. Out of Scope

- Defining roles and their permission sets — US-3.6.
- Creating an account with an initial role set — US-3.2.
- Per-resource or per-record permissions — no criterion reaches them.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The privilege ceiling is compared permission by permission, not role by role. | RA-AC4 names a permission, `audit:read`, as the thing the caller lacks. US-3.2 UC-AC4 states the same rule in terms of roles; the two are only consistent under a permission-level reading. |
| A-2 | Granting a role takes effect at the next refresh, within the access-token TTL, while revocation is immediate. | RA-AC6 states the immediate path for revocation only. Symmetric behaviour would make the `token_generation` increment pointless. |
| A-3 | The write requires `If-Match`, as in US-3.3. | Named only in the story's API Contract. Without it two administrators can silently overwrite each other's role changes. |
| A-4 | "The audit log" in RA-AC4 is the same `audit_events` named in RA-AC1. | RA-AC4 says only "the audit log". |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | RA-AC6 requires the resource server to notice `token_generation` on every request, which means a database or cache read per call. That contradicts the stateless-JWT posture in `AGENTS.md` §3.4 and needs an architect's decision before the criterion can be implemented as written. | RA-AC6 |
| OQ-2 | RA-AC4 refuses a grant the caller cannot make, but nothing states whether an administrator may *revoke* a permission they do not themselves hold. Revocation reduces privilege, so the same ceiling may not apply. | RA-AC4 |
| OQ-3 | RA-AC1 audits one entry per role. RA-AC5 replaces the whole set. For a request that swaps three roles for two, how many entries are written and in what order? | RA-AC1, RA-AC5 |
| OQ-4 | No criterion requires `If-Match` on this endpoint, though the equivalent update in US-3.3 does. Are concurrent role changes expected to conflict-detect, or is last-write-wins acceptable for privileges? | RA-AC1 |
| OQ-5 | RA-AC4 records "a security event" — the same undefined category flagged in US-3.2 OQ-5 and US-2.4 OQ-3. | RA-AC4 |
| OQ-6 | Is the customer notified when their roles change? Nothing states the intent, and US-5.4 would need an event type for it. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| RA-AC1 | Granting and revoking are audited per role | §3.1, §4 | Covered — see OQ-3, OQ-4 |
| RA-AC2 | Effective permissions are de-duplicated and attributed | §3.2, §4 | Covered |
| RA-AC3 | An administrator cannot change their own roles | §3.3, §4 | Covered |
| RA-AC4 | Nobody can grant a permission they do not hold | §3.4, §4 | Covered — see OQ-2, OQ-5 |
| RA-AC5 | A customer must retain at least one role | §3.5, §4 | Covered |
| RA-AC6 | Revocation takes effect inside the current token's lifetime | §3.6, §4 | **Partial** — the mechanism the criterion asserts conflicts with the project's stateless-JWT contract and is unresolved (OQ-1) |

**Coverage:** 5 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.5-assign-roles.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
