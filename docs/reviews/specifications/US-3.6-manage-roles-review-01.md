---
spec: docs/specifications/US-3.6-manage-roles.md
spec_revision: 2
story_id: US-3.6
source: docs/backlog/US-3.6-manage-roles.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Manage Roles and Permissions

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Seven criteria, verbatim, all covered. This spec carries the highest Partial
count in the set — four of seven — and each is a genuine gap in the story rather than a
defect in the document. Two of them, F-3 and F-4, block criteria that other stories depend
on: MR-AC6's system-role protection has no subject, and MR-AC7 grants read access to
endpoints nothing describes.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. This story makes the
authorisation model editable at runtime, which `AGENTS.md` §7.1 places behind human review.

**Resolves when:** an architect signs off on runtime-editable permissions.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.5; §4 row 8; OQ-1

**Evidence:**
> | 8 | system-defined flag | Boolean; **which roles carry it is not specified** | `[MR-AC6]` |

**Problem:** MR-AC6 makes system-defined roles immutable and no story says which roles
those are. US-3.4 UD-AC4 implies the administrator role is protected; whether support-agent
and auditor are is undecided.

**Why it matters:** A tester can only construct the case by first setting the flag by hand,
which tests the mechanism against itself. More seriously, an unprotected administrator role
can be edited into uselessness by its own holder, which is the failure MR-AC6 exists to
prevent.

**Resolves when:** the system-defined set is enumerated, with the migration that sets it.

---

### F-4 · Major · D2 Coverage

**Location:** §3.6; §4 row 4; OQ-4

**Evidence:**
> | 4 | listing endpoints | Implied by MR-AC7's "list roles and permissions"; paths not
> stated by any criterion | `[MR-AC7]` |

**Problem:** MR-AC7 grants `roles:read` the ability to "list roles and permissions". No
criterion describes those endpoints, their response shape, or their paging. US-3.5 RA-AC2
depends on the permission catalogue being readable, and US-3.2 UC-AC4 depends on a role
picker built from it.

**Why it matters:** The positive half of MR-AC7 — what a read-only caller may actually do —
cannot be tested. Two stories consume an interface this one is supposed to define.

**Resolves when:** a criterion describes the listing endpoints, or MR-AC7 drops the claim
and another story owns them.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| MR-AC1 | §3.1, §4 | Caller with `roles:manage` posts a name and permission keys → `201` with `Location`; the role appears in US-3.5's assignable set; `audit_events` row carries the full set | Yes |
| MR-AC2 | §3.2, §4 | Role held by 12 customers; add a key → response reports 12; assert a holder gains the permission within 60 s; audit row carries before and after | Yes |
| MR-AC3 | §3.1, §4 | Create "Support Agent", then " support agent " → `409` type `role-name-taken` | Yes |
| MR-AC4 | §3.3, §4 | Post a key the catalogue does not declare → `400` type `unknown-permission` naming it, no role written | Yes |
| MR-AC5 | §3.4, §4 | Assign a role to one customer, then delete it → `409` type `role-in-use` reporting 1; assignments unchanged. The reassignment the criterion offers does not exist — §5 | Partly |
| MR-AC6 | §3.5, §4 | Not derivable: no role is defined as system-defined, so the precondition cannot be established — F-3 | No |
| MR-AC7 | §3.6, §4 | Negative half derivable: a write without `roles:manage` → `403` type `insufficient-scope`, attempt audited. Positive half not derivable — F-4 | Partly |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Comparison ignores case and surrounding whitespace." | §3.1 | `[MR-AC3]` | Yes — MR-AC3 final clause |
| "Every holder of that role gains the added permission within 60 seconds of the change." | §3.2 | `[MR-AC2]` | Yes |
| "A direct `PUT` or `DELETE` against it responds `403`…" | §3.5 | `[MR-AC6]` | Yes |
| "A caller holding only `roles:read` can still list roles and permissions." | §3.6 | `[MR-AC7]` | Yes as a claim; the endpoints do not exist — F-4 |

## 5. Ambiguities and Missing Edge Cases

- **System-defined role set undefined** — see F-3. `[MR-AC6]`
- **Listing endpoints undefined** — see F-4. `[MR-AC7]`
- **Bulk reassignment does not exist** — MR-AC5 has the client "offer to reassign those
  customers" and no story provides the operation. Recorded as OQ-3. `[MR-AC5]`
- **Propagation asymmetry** — MR-AC2 gives 60 seconds for a permission gained. US-3.5
  RA-AC6 makes a permission lost take effect immediately via `token_generation`. Nothing
  says whether editing a role here also increments it, so a permission removed at this level
  may lag by the full access-token TTL. Recorded as OQ-2. `[MR-AC2]`
- **A key removed from the catalogue by a later release** — MR-AC4 rejects unknown keys on
  write. Nothing states what happens to a role already holding a key that a deployment
  removes. Recorded as OQ-5. `[MR-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Seven criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | Four Partial rows, each supported by an open question |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`MR-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
