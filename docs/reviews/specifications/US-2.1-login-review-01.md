---
spec: docs/specifications/US-2.1-login.md
spec_revision: 2
story_id: US-2.1
source: docs/backlog/US-2.1-login.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Login

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 2 |

No Blockers. The nine criteria are verbatim against the source and every one is covered by
a normative section. F-1 and F-2 are structural and apply to every spec in this set: the
criteria were authored by an assistant and never approved, and this review is not
independent of the spec's author. F-3 is a dangling reference left by the revision-2
re-sync. LI-AC9 is correctly reported as Partial — the account state it gates on is
produced by no story in this backlog, and OQ-1 says so.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Front matter and the provenance callout

**Evidence:**
> The story and its acceptance criteria were drafted by an assistant from the epic list
> supplied by the user. No product owner, customer or regulator has agreed to them.

**Problem:** The criteria carry no stakeholder authority. This matters more here than in
most specs in the set, because several encode security decisions — a 15-minute access
token, a 50 ms timing tolerance, a 5-attempt lockout — that read as agreed policy.

**Why it matters:** `AGENTS.md` §7.1 requires human review for changes to authentication
schemes and token lifetimes. A spec that states them without a sign-off record invites
exactly the unreviewed change that clause exists to prevent.

**Resolves when:** an architect and a product owner sign off on the criteria, and the
callout records who and when.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** The same assistant authored the story, the specification, the corrections
applied after the first pass, and this review.

**Why it matters:** D3 grounding is the dimension that most depends on a second reader. In
this document the pass was clean, but the same pass over US-3.1 missed a Blocker across
three hand readings and was caught only by a token-comparison script.

**Resolves when:** a different reader repeats at least the D3 pass. The same condition is
open on `CP-101` from its review 01.

---

### F-3 · Minor · D4 Traceability integrity

**Location:** §5 Out of Scope, last bullet

**Evidence:**
> - Per-IP throttling — see OQ-5.

**Problem:** §7 declares OQ-1 to OQ-4. There is no OQ-5. Per-IP throttling also stopped
being out of scope when LI-AC6 absorbed it in the story correction.

**Why it matters:** A dangling pointer reads as a deliberate deferral, so a reader looking
for the per-IP decision follows it and finds nothing — while the criterion that now covers
it sits two sections above.

**Resolves when:** the bullet is removed or repointed, and the section reflects that LI-AC6
covers the per-IP limit.

---

### F-4 · Minor · D5 Testability

**Location:** §3.3, second paragraph, and LI-AC4

**Evidence:**
> And the median response time is within 50 ms of the median for LI-AC3 over 100 samples

**Problem:** The criterion is now testable, which is an improvement over revision 1. But
50 ms is documented in the story's Assumptions table as an estimate rather than a
measurement, against a hashing cost the same story sets at ≈100 ms. A test asserting it may
fail on CI jitter or pass while leaking a real signal.

**Why it matters:** An anti-enumeration control that fails intermittently gets quarantined,
and a quarantined security test is equivalent to no test.

**Resolves when:** the tolerance is re-derived from a measurement on the target hardware,
or the criterion states a statistical form the test can hold stably.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| LI-AC1 | §3.1, §4 | Active verified customer, correct credentials → `200`, JWT with 900 s TTL, `Set-Cookie` carrying `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, `last_login_at` advanced, `audit_events` row `LOGIN_SUCCEEDED` | Yes |
| LI-AC2 | §3.2 | Request `/orders/1042` unauthenticated → redirected to login; authenticate → land on `/orders/1042`. Repeat with a stored target on another origin → land on the dashboard | Yes |
| LI-AC3 | §3.3, §4 | Active customer, wrong password → `401`, type `invalid-credentials`, no `Set-Cookie`, `audit_events` row `LOGIN_FAILED` reason `BAD_PASSWORD` | Yes |
| LI-AC4 | §3.3 | 100 requests to an unregistered address and 100 with a wrong password → identical bodies and statuses, medians within 50 ms | Yes — see F-4 |
| LI-AC5 | §3.4, §4 | `DEACTIVATED` customer, correct password → `403` type `account-deactivated`; same customer, wrong password → `401`, proving order | Yes |
| LI-AC6 | §3.5, §4 | Four failures then a fifth → `429` with `Retry-After`; correct password inside the window → `429`; one email only; successful login after expiry resets. Separately, 21 attempts from one IP in a minute → `429` | Yes |
| LI-AC7 | §3.6 | Body without `password` → `400` type `validation-failed` naming it; body with `{"nope":1}` → `400`; rate-limit counter unchanged | Yes |
| LI-AC8 | §3.2, §3.7, §4 | Authenticated caller requests login → redirect to dashboard; `rememberMe=true` → cookie `Max-Age` ≈ 30 days; `rememberMe=false` → no `Max-Age` attribute | Yes |
| LI-AC9 | §3.4, §4 | Not derivable end to end: no story creates the verified/unverified state, so the precondition cannot be established | No — see §5 |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The account owner is emailed once per lock window rather than once per attempt." | §3.5 | `[LI-AC6]` | Yes — LI-AC6 clause 5, added in the story correction |
| "An independent limit of 20 attempts per source IP per minute produces the same `429`." | §3.5 | `[LI-AC6]` | Yes — LI-AC6 final clause |
| "The bound is met by performing a dummy Argon2id verification…" | §3.3 | `[LI-AC4]` | Yes — LI-AC4's `Because` clause |
| "Credential verification runs before both state checks." | §3.4 | `[LI-AC5]` `[LI-AC9]` | Yes — the `But` clause of each |
| "Where the caller already holds a valid session… redirected to the dashboard" | §3.2 | `[LI-AC8]` | Yes — LI-AC8 clauses 1 to 3 |

## 5. Ambiguities and Missing Edge Cases

- **The unverified state has no producer** — LI-AC9 gates login on an email-verified flag.
  `CP-101` registers an account; no story in this backlog verifies an address. The
  criterion is well formed and currently untestable. Recorded as OQ-1. `[LI-AC9]`
- **Lock window and counter window are both 15 minutes** — LI-AC6 uses 15 minutes for the
  failure window and for the lock. Nothing states whether a failure during the lock extends
  it, so a tester cannot decide what happens at minute 16 of a sustained attack. `[LI-AC6]`
- **`rememberMe` absent** — the story's Assumption 7 declares the two values exhaustive,
  which resolves revision 1's contradiction, but the API contract types the field as a
  plain boolean rather than a required one. An omitted field has no defined branch.
  `[LI-AC8]`
- **Per-IP limit and account lock interacting** — LI-AC6 defines both, and both return
  `429`. A caller cannot distinguish which fired, and no criterion says whether the
  `Retry-After` reflects the shorter or the longer. `[LI-AC6]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Verbatim against `docs/backlog/US-2.1-login.md`; nine criteria match exactly. Provenance is F-1 |
| D2 Coverage | Yes | All nine referenced in §3 or §4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | Found F-3 |
| D5 Testability and edge cases | Yes | Found F-4 |
| D6 Gap handling | Yes | Open questions are genuine and properly outside the requirement set |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** — its `AC-<n>` pattern
does not match this spec's `LI-AC<n>` scheme, so it reported zero declared criteria and then
printed `OK` over an empty set. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
