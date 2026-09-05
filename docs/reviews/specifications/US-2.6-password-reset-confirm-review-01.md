---
spec: docs/specifications/US-2.6-password-reset-confirm.md
spec_revision: 2
story_id: US-2.6
source: docs/backlog/US-2.6-password-reset-confirm.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Password Reset (confirm)

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. Two of PN-AC4's three password rules
cannot be turned into a test — F-3 and F-4 — and the spec says so, which is why they are
Major rather than Blocker. F-5 is a cross-story conflict the spec correctly surfaces: this
endpoint is reused by US-3.2 with a 72-hour token while PN-AC2 hard-codes 30 minutes as the
expiry test.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. PN-AC4 states a
password policy, which is a security-scheme decision under `AGENTS.md` §7.1.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.2, second paragraph; PN-AC4

**Evidence:**
> A password appearing in the common-password list is rejected with `400` and the detail
> "That password is too common. Choose another". `[PN-AC4]` Which list, and how membership
> is decided, is not stated — see OQ-1.

**Problem:** The criterion asserts membership of a list that does not exist. No source, no
size, no maintenance owner.

**Why it matters:** Two implementations will disagree about any given password and both
will satisfy PN-AC4. A tester can only assert the rule using a password they have first
confirmed is in whichever list the implementation happens to ship, which is a test of the
implementation against itself.

**Resolves when:** the list is named with a version and a size, or PN-AC4 is reworded to
assert the rejection mechanism rather than a specific verdict.

---

### F-4 · Major · D5 Testability

**Location:** §3.2, third paragraph; PN-AC4

**Evidence:**
> A password containing the customer's email or name is rejected with `400`… What counts as
> "containing" is not stated — see OQ-2.

**Problem:** "Contains the customer's email or name" admits at least five readings: the
whole address, the local part, either name, a case-insensitive substring, or a fuzzy match.
Each returns a different verdict on the same input.

**Why it matters:** The rule is a real security control and currently unimplementable
without inventing its semantics, which is the failure this spec exists to prevent.

**Resolves when:** the matching rule is stated precisely enough that two implementations
agree on a borderline input.

---

### F-5 · Major · D5 Testability

**Location:** §3.3; §5 Out of Scope; OQ-4

**Problem:** PN-AC2 tests expiry at 30 minutes. US-3.2 UC-AC1 issues an invitation token
valid 72 hours and, by the spec's own A-2 there, expects this endpoint to consume it. One
endpoint, two lifetimes, no criterion distinguishing them.

**Why it matters:** Implemented from PN-AC2 alone, every invitation expires after 30
minutes and US-3.2's onboarding flow silently breaks. Implemented from UC-AC1 alone, reset
links live 72 hours, which weakens the control PN-AC2 defines.

**Resolves when:** the token carries a purpose the endpoint reads, or the two flows get
separate endpoints.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| PN-AC1 | §3.1, §4 | Valid unconsumed token, compliant password → `204`, hash replaced, token consumed, every refresh family revoked, one queued mail, `audit_events` row `PASSWORD_CHANGED` | Yes |
| PN-AC2 | §3.3, §4 | Token aged past 30 minutes with an injected `Clock` → `410` type `reset-token-expired`; repeat with a consumed token → identical response. The 30-minute figure conflicts with US-3.2 — F-5 | Partly |
| PN-AC3 | §3.3, §4 | Token matching no stored hash → `410` byte-identical to PN-AC2's response | Yes |
| PN-AC4 | §3.2, §4 | 11-character password → `400` naming the minimum. The other two rules are not derivable — F-3, F-4 | Partly |
| PN-AC5 | §3.2, §4 | Submit the account's current password → `400` with the exact quoted detail; assert the token is still unconsumed | Yes |
| PN-AC6 | §3.4, §4 | Client unit test: typing updates the meter with no network call; paste is accepted and validated identically; the field carries `autocomplete="new-password"` | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Every session and refresh-token family belonging to that customer is revoked." | §3.1 | `[PN-AC1]` | Yes — PN-AC1 clause 3 |
| "In every rejection above the token remains unconsumed, so the customer can retry." | §3.2 | `[PN-AC4]` `[PN-AC5]` | Yes — final clause of each |
| "…comparison against stored hashes is constant-time…" | §3.3 | `[PN-AC3]` | Yes — PN-AC3 clause 2 |
| "The field carries `autocomplete=\"new-password\"`." | §3.4 | `[PN-AC6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Common-password list undefined** — see F-3. `[PN-AC4]`
- **"Containing" undefined** — see F-4. `[PN-AC4]`
- **Two token lifetimes on one endpoint** — see F-5. `[PN-AC2]`
- **No maximum password length** — PN-AC4 sets a 12-character minimum and no ceiling, while
  `CP-101` AC-3 already rejects passwords over 128. An Argon2id hash of an unbounded input
  is a denial-of-service surface. Recorded as OQ-3. `[PN-AC4]`
- **Atomicity is assumed, not required** — A-2 assumes the password write, token
  consumption and session revocation share a transaction. No criterion says so, and a
  partial failure leaves a changed password with live sessions, which inverts PN-AC1's
  purpose. `[PN-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4, F-5 |
| D6 Gap handling | Yes | Six open questions, all genuine |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`PN-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
