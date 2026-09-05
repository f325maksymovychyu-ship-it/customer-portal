---
spec: docs/specifications/US-2.5-password-reset-request.md
spec_revision: 2
story_id: US-2.5
source: docs/backlog/US-2.5-password-reset-request.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Password Reset (request)

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Five criteria, verbatim, all covered. F-3 is the substantive finding: PR-AC1
and PR-AC2 both turn on "the neutral message" being identical, and neither states what it
is. The spec is honest about this — A-3 supplies a candidate and OQ-2 records the gap — but
an assumption is doing the work of the criterion, which is precisely the D6 failure shape.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. The anti-enumeration
posture in PR-AC2 is a security decision.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D6 Gap handling

**Location:** §3.1 and §3.2; A-3; OQ-2

**Evidence:**
> | A-3 | The neutral message is the one quoted in the story's API Contract: "If that
> account exists, we have sent instructions." | PR-AC1 and PR-AC2 both refer to "the neutral
> message" without giving its text; §3 cannot be tested without one. |

**Problem:** The assumption is load-bearing. PR-AC2's entire assertion is that the two
responses are identical, and the thing that must be identical has no agreed text. The spec
routes this correctly to Assumptions rather than inventing a requirement, which is the
right call — but the result is that a criterion cannot be tested without accepting an
unapproved assumption as though it were agreed.

**Why it matters:** If the two paths are implemented from different readings of "the
neutral message", PR-AC2 fails in production while passing any test written from A-3.

**Resolves when:** the message text is stated in the criteria, or PR-AC2 is reworded to
assert byte-identity without naming a specific string.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| PR-AC1 | §3.1, §4 | Active customer's address → `202`, a `password_reset_tokens` row valid 30 minutes, one queued mail, `audit_events` row `PASSWORD_RESET_REQUESTED`. The body assertion depends on A-3 — see F-3 | Partly |
| PR-AC2 | §3.2, §4 | Unregistered address → `202` byte-identical to PR-AC1's response, no token row, mail port never invoked; repeat for a `DEACTIVATED` account | Partly — see F-3 |
| PR-AC3 | §3.4, §4 | Four requests for one address inside an hour → the fourth `429` with `Retry-After` type `too-many-attempts`, no mail; separately, eleven from one IP → `429` | Yes |
| PR-AC4 | §3.3, §4 | Request twice; assert the first token is rejected by the confirm endpoint and the second is accepted | Yes |
| PR-AC5 | §3.5, §4 | Body without `email`, and `email: "olena@"` → `400` type `validation-failed`; assert the per-address counter did not advance | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "PR-AC2 constrains the body and the status. It does not constrain response time — see OQ-1." | §3.2 | `[PR-AC2]` | Yes — an accurate negative statement about the criterion, not a requirement |
| "An independent limit of 10 requests per hour applies per source IP address." | §3.4 | `[PR-AC3]` | Yes — PR-AC3 final clause |
| "…requesting another invalidates the earlier token, so that only the most recently emailed link can be used." | §3.3 | `[PR-AC4]` | Yes |
| "An attempt rejected by validation is not counted against the per-address rate limit." | §3.5 | `[PR-AC5]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **The neutral message has no text** — see F-3. `[PR-AC1]` `[PR-AC2]`
- **Timing is unconstrained here but constrained in US-2.1** — PR-AC2 requires an identical
  body and status; LI-AC4 in the sibling story additionally requires a timing bound. Issuing
  a token and queueing mail is measurably slower than doing nothing, so this endpoint is an
  enumeration oracle by stopwatch. Recorded as OQ-1. `[PR-AC2]`
- **What the rate limiter counts** — PR-AC3 does not say whether the per-address counter
  keys on addresses that exist or on every address submitted. Under the first reading the
  limiter itself confirms account existence. A-4 assumes the second. `[PR-AC3]`
- **Where the token travels in the link** — nothing states path segment, query parameter or
  fragment. A query parameter puts the token in server logs and `Referer`. Recorded as
  OQ-4. `[PR-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Five criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | |
| D6 Gap handling | Yes | Found F-3 |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`PR-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
