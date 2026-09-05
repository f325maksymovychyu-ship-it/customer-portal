---
spec: docs/specifications/US-3.2-create-user.md
spec_revision: 1
story_id: US-3.2
source: docs/backlog/US-3.2-create-user.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Create User

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. This is one of eight specs untouched by
the backlog correction, so it is still at revision 1. F-3 is the substantive finding:
UC-AC4 gates on "a role carrying administrative permissions" and nothing defines which
roles those are, which makes the privilege ceiling — the security control this criterion
exists for — untestable at its boundary.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. UC-AC1 decides that an
administrator never sets a password, which is a policy choice with real operational
consequences.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.5; A-4; OQ-1

**Evidence:**
> Where the caller does not hold the `roles:grant:admin` scope and the request names a role
> carrying administrative permissions, the response is `403`… `[UC-AC4]`

**Problem:** UC-AC4 names a scope and a category of role, and defines neither. A-4 assumes
the ceiling is "a role whose permission set is not a subset of the caller's", which is a
permission-level reading; the criterion's own wording is role-level. US-3.5 RA-AC4 states
the same rule in permission terms, so the two stories disagree about the unit.

**Why it matters:** The privilege ceiling is one of the two escalation controls in this
backlog. A tester can build the obvious case — a plain administrator granting a
super-administrator role — but cannot test the boundary, which is where escalation
actually happens: a role carrying one permission the caller lacks.

**Resolves when:** UC-AC4 and US-3.5 RA-AC4 agree on whether the comparison is per role or
per permission, and the administrative category is defined or dropped.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UC-AC1 | §3.1, §3.2, §4 | Caller with `customers:create` posts email, both names and one role → `201` with `Location`, status `PENDING_INVITATION`, one queued invitation valid 72 h, `audit_events` row `CUSTOMER_CREATED`; a body carrying `password` is rejected | Yes |
| UC-AC2 | §3.3, §4 | Existing address → `409` type `email-already-registered`; repeat with the address `DEACTIVATED`, and with different casing → same result, no second row | Yes |
| UC-AC3 | §3.4, §4 | Body missing two required fields → `400` naming both, not just the first; `roleIds: []` → `400` with the exact quoted detail | Yes |
| UC-AC4 | §3.5, §4 | Caller without `roles:grant:admin` names an administrative role → `403` type `privilege-escalation`, zero rows written. The boundary case is not derivable — F-3 | Partly |
| UC-AC5 | §3.6, §4 | Submit twice with one `Idempotency-Key` → one account, the second response repeating the first including `Location` | Yes |
| UC-AC6 | §3.2, §4 | Stub the mail port to fail → account remains `PENDING_INVITATION`, the exact message is shown, and a resend issues a fresh token that invalidates the previous | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "No password is set, and none is accepted in the request body." | §3.1 | `[UC-AC1]` | Yes — UC-AC1's `But` clause |
| "No account is created; the request fails whole rather than partially." | §3.5 | `[UC-AC4]` | Yes — UC-AC4 clause 2 |
| "Matching is case-insensitive." | §3.3 | `[UC-AC2]` | Yes — UC-AC2 final clause |
| "The client disables the submit control while a request is in flight." | §3.6 | `[UC-AC5]` | Yes — UC-AC5 final clause |

## 5. Ambiguities and Missing Edge Cases

- **"Administrative permissions" undefined** — see F-3. `[UC-AC4]`
- **Invitation lifetime versus the consuming endpoint** — UC-AC1's 72 hours meets US-2.6
  PN-AC2's 30-minute expiry test on the endpoint that consumes it. Recorded as OQ-2 here
  and F-5 in the US-2.6 review. `[UC-AC1]`
- **Idempotency-Key retention** — UC-AC5 requires a replay to repeat the first response but
  states no window, and no behaviour for the same key arriving with a different body.
  Recorded as OQ-3. `[UC-AC5]`
- **Unclaimed invitations never expire out of the directory** — nothing states what becomes
  of a `PENDING_INVITATION` account whose token lapses. Recorded as OQ-4, and it interacts
  with US-3.1 OQ-3 on whether such accounts appear in listings. `[UC-AC1]`
- **"A security event" is a category with no definition** — UC-AC4 requires the refusal to
  be recorded as one. Since the backlog correction, US-3.7 defines `severity` including
  `SECURITY`, but UC-AC4 does not reference it. `[UC-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | Five open questions, all genuine |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`UC-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
