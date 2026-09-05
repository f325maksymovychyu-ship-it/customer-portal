---
spec: docs/specifications/US-3.4-deactivate-user.md
spec_revision: 2
story_id: US-3.4
source: docs/backlog/US-3.4-deactivate-user.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Deactivate and Reactivate User

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Six criteria, verbatim, all covered. F-3 is the substantive finding: UD-AC6
performs an irreversible privacy operation whose field set and tombstone value are both
undefined, and it is the one criterion in this backlog that destroys data.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. UD-AC6 encodes a GDPR
erasure procedure, which `AGENTS.md` §7.4 places behind mandatory human review.

**Why it matters:** An unapproved erasure procedure is worse than an absent one: it will be
followed, and it is irreversible.

**Resolves when:** a data-protection owner signs off on UD-AC6 specifically.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.5; §4 row 9; OQ-1

**Evidence:**
> | 9 | tombstone marker | **not specified** — no criterion states its value or which
> fields it covers | `[UD-AC6]` |

**Problem:** UD-AC6 requires "personal fields" to be overwritten with "a tombstone marker".
Neither the field set nor the marker exists. The deactivation reason from UD-AC1 is free
text an administrator wrote about the person and may itself be personal data, and nothing
says whether it is in scope.

**Why it matters:** The criterion cannot be tested, and the operation cannot be undone. An
implementation that misses one column leaves personal data behind after the system has
reported the erasure complete, which is the specific failure a regulator asks about.

**Resolves when:** the erasable field set is enumerated and the marker value stated.

---

### F-4 · Minor · D5 Testability

**Location:** §3.1 and §3.4; OQ-2

**Problem:** UD-AC5 gives `200` for a repeated deactivation. UD-AC1 states no status code
for the first, successful one. The spec records the gap rather than assuming, which is
correct, but leaves the primary path's response undefined.

**Why it matters:** Low impact — a tester can assert the state change without the status.
It matters because the idempotent case is specified more precisely than the normal case.

**Resolves when:** UD-AC1 states its response.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UD-AC1 | §3.1, §4 | Caller with `customers:deactivate` posts a reason → status `DEACTIVATED`, every refresh family revoked within 60 s, all columns retained, `audit_events` row carrying reason and actor; sign-in then fails per US-2.1 LI-AC5. Status code not asserted — F-4 | Yes |
| UD-AC2 | §3.2, §4 | Activate a deactivated account → `ACTIVE`, the same role rows present, one queued mail, sign-in succeeds with the unchanged password | Yes |
| UD-AC3 | §3.3, §4 | Administrator posts deactivate against their own id → `422` type `self-deactivation`, status unchanged | Yes |
| UD-AC4 | §3.3, §4 | Leave one active administrator; deactivate them → `422` type `last-administrator` with the exact detail; run two concurrent deactivations of the last two and assert one fails | Yes |
| UD-AC5 | §3.1, §3.4, §4 | Blank reason → `400`; deactivate an already-deactivated account → `200`, nothing changed | Yes |
| UD-AC6 | §3.5, §4 | Not derivable: the field set and the marker are undefined, so no assertion distinguishes a complete erasure from a partial one — F-3 | No |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "All of the account's refresh-token families are revoked within 60 seconds." | §3.1 | `[UD-AC1]` | Yes — UD-AC1 clause 2, verbatim since the correction |
| "`audit_events` entries are retained and continue to reference the anonymised identifier." | §3.5 | `[UD-AC6]` | Yes — UD-AC6's `But` clause |
| "The same invariant is enforced by US-3.5 when the last administrative role would be removed." | §3.3 | `[UD-AC4]` | Yes — UD-AC4 final clause |
| "Subsequent sign-in attempts are refused as described by US-2.1 LI-AC5." | §3.1 | `[UD-AC1]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Tombstone field set and value** — see F-3. `[UD-AC6]`
- **Anonymised-row retention** — UD-AC6 keeps audit entries indefinitely by implication.
  Whether an erasure obligation eventually reaches them is unstated. Recorded as OQ-3.
  `[UD-AC6]`
- **Roles restored after a role changed** — UD-AC2 restores "the previously assigned
  roles". US-3.6 MR-AC5 forbids deleting a role someone holds, but a deactivated holder may
  not count. Recorded as OQ-4. `[UD-AC2]`
- **Which role the last-administrator invariant tracks** — UD-AC4 protects "the
  administrator role". US-3.6 leaves the system-defined set open, so the invariant's subject
  is undefined. Recorded as OQ-6. `[UD-AC4]`
- **60 seconds versus 15 minutes** — UD-AC1 bounds revocation at 60 seconds while US-2.4
  RT-AC7 allows access to survive 15 minutes. A-2 reconciles them as bounding different
  things, and no criterion says so. `[UD-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | UD-AC6 correctly reported as Partial |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`UD-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
