---
spec: docs/specifications/US-3.5-assign-roles.md
spec_revision: 2
story_id: US-3.5
source: docs/backlog/US-3.5-assign-roles.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Assign Roles to a User

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Six criteria, verbatim, all covered. RA-AC6 is correctly reported Partial: it
requires the resource server to consult `token_generation` on every request, which
contradicts the stateless-JWT posture `AGENTS.md` §3.4 mandates. That is an architecture
decision, not a spec defect, and the spec surfaces it rather than resolving it.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. RA-AC3 and RA-AC4 are
the backlog's two privilege-escalation controls.

**Resolves when:** the criteria carry a recorded sign-off from a security owner.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.6; OQ-1

**Evidence:**
> A request to a resource covered only by the revoked role responds `403` even within the
> current access token's lifetime. `[RA-AC6]`

**Problem:** The criterion is precise and currently unimplementable within the project's
stated architecture. `AGENTS.md` §3.4 defines a stateless JWT resource server; honouring
`token_generation` per request adds a database or cache read to every authorised call.
`AGENTS.md` §7.1 requires human review for exactly this kind of change.

**Why it matters:** A criterion that cannot be built without an unapproved architectural
change will either be silently dropped — leaving revocation lagging by up to 15 minutes,
which is what US-3.5 exists to prevent — or built anyway, changing the performance profile
of every endpoint without review.

**Resolves when:** an architect rules on per-request generation checks, or RA-AC6 is
relaxed to the refresh boundary.

---

### F-4 · Minor · D4 Traceability integrity

**Location:** §4, closing note; A-3

**Evidence:**
> The story's API Contract requires `If-Match` on the write. No criterion mentions it.

**Problem:** The write replaces a customer's whole role set and no criterion requires
conflict detection, while the equivalent update in US-3.3 does. A-3 assumes `If-Match`
applies.

**Why it matters:** Low impact because the assumption is visible. It matters because two
administrators editing roles concurrently is a realistic scenario and last-write-wins on
privileges is the worse outcome of the two.

**Resolves when:** a criterion requires the precondition, or the story records that
last-write-wins is acceptable here.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| RA-AC1 | §3.1, §4 | Caller with `roles:assign` adds a role → `200` with the new set and an `audit_events` row `ROLE_GRANTED` carrying role, actor and time; remove it → `ROLE_REVOKED` in the same shape | Yes |
| RA-AC2 | §3.2, §4 | Assign two roles sharing a permission → the effective list contains it once and names both source roles | Yes |
| RA-AC3 | §3.3, §4 | Administrator targets their own id → `403` type `self-role-change` with the exact detail; repeat with a request that only removes a role → still `403` | Yes |
| RA-AC4 | §3.4, §4 | Caller without `audit:read` grants a role carrying it → `403` type `privilege-escalation`, no part of the set applied, security event recorded | Yes |
| RA-AC5 | §3.5, §4 | `roleIds: []` on a single-role customer → `422` type `no-roles-assigned` with the exact detail; separately assert the last-administrator invariant | Yes |
| RA-AC6 | §3.6, §4 | Not derivable within the stated architecture: asserting a `403` inside the current token's lifetime requires per-request generation checks that `AGENTS.md` §3.4 excludes — F-3 | No |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The refusal applies even where the change would only remove permissions." | §3.3 | `[RA-AC3]` | Yes — RA-AC3 final clause |
| "No part of the requested role set is applied." | §3.4 | `[RA-AC4]` | Yes |
| "`customers.token_generation` is incremented immediately." | §3.6 | `[RA-AC6]` | Yes — RA-AC6 clause 1 |
| "Where the removal would leave no active administrator, the invariant of US-3.4 UD-AC4 applies." | §3.5 | `[RA-AC5]` | Yes — RA-AC5 final clause |

## 5. Ambiguities and Missing Edge Cases

- **Per-request generation checks versus stateless JWT** — see F-3. `[RA-AC6]`
- **Revoking a permission the caller lacks** — RA-AC4 forbids granting above the ceiling.
  Nothing says whether the same ceiling applies to removal, which reduces privilege.
  Recorded as OQ-2. `[RA-AC4]`
- **Audit granularity on a set replacement** — RA-AC1 writes one entry per role and RA-AC5
  replaces the whole set. For a request swapping three roles for two, the number and order
  of entries is undefined. Recorded as OQ-3. `[RA-AC1]` `[RA-AC5]`
- **Role-level versus permission-level ceiling** — RA-AC4 names a permission; US-3.2 UC-AC4
  names a category of role. The two stories state the same control in different units.
  `[RA-AC4]`
- **No notification on a role change** — nothing states whether the customer learns their
  access changed. Recorded as OQ-6, and US-5.4 has no event type for it. `[RA-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | Found F-4 |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | OQ-1 correctly flags the escalation rather than resolving it |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`RA-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
