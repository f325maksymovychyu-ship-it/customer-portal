---
spec: docs/specifications/US-2.4-refresh-token.md
spec_revision: 2
story_id: US-2.4
source: docs/backlog/US-2.4-refresh-token.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Refresh Token

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Seven criteria, verbatim, all covered. RT-AC5's self-contradiction over
revocation scope — flagged in the spec's own revision 1 — was closed in the story and the
body now matches. F-3 is specific and consequential: RT-AC5's cleanup interaction means the
reuse detection this whole story rests on can silently stop working, and no criterion
guards it.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. `AGENTS.md` §3.4
mandates rotation with reuse detection, so the mechanism has architectural backing, but the
specific blast radius chosen in RT-AC5 — every family for the customer — does not.

**Why it matters:** RT-AC5 signs a customer out of every device on one replayed token. That
is a deliberate, aggressive trade-off nobody has agreed to.

**Resolves when:** the criteria carry a recorded architect sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §5 Out of Scope, third bullet; §7 OQ-4

**Evidence:**
> - Removal of expired token rows. The story's Data Model Notes describe a daily cleanup
>   job; no criterion reaches it.

**Problem:** RT-AC5 detects reuse by recognising a retired token. Recognition requires the
row to still exist. The cleanup job deletes expired rows, and no criterion states a
retention floor. With a 30-day refresh TTL and an unspecified cleanup horizon, the control
can be disabled by a configuration change that no test would catch.

**Why it matters:** This is the security control the story exists to provide. A silent
failure mode with no test around it is worse than an absent control, because the audit
trail will show reuse detection as implemented.

**Resolves when:** a criterion states that retired token rows are retained at least as long
as the maximum refresh TTL, or the reuse detection is redesigned not to depend on row
presence.

---

### F-4 · Minor · D5 Testability

**Location:** §3.2 and §3.3

**Problem:** RT-AC2 and RT-AC3 describe client behaviour — retry-once and single-flight —
that is not observable at the API. The spec records this in A-3, which is correct, but the
criteria are written in `When`/`Then` form as though they were server behaviour.

**Why it matters:** A QA engineer reading the criteria will look for an API-level test and
find none. The tests exist, but in a different suite with a different owner.

**Resolves when:** the criteria or the enforcement matrix state which side of the boundary
each belongs to. The story's matrix already does; the criteria do not.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| RT-AC1 | §3.1, §4 | Expired access token, valid refresh → `200`, new access token, rotated refresh; replay the old refresh → rejected | Yes |
| RT-AC2 | §3.2 | Client unit test: stub a `401`, assert exactly one refresh and one replay of the original request, and that a persistent `401` stops after one retry | Yes — see F-4 |
| RT-AC3 | §3.3 | Client unit test: five concurrent calls on an expired token → exactly one refresh call, four replays, zero failures | Yes — see F-4 |
| RT-AC4 | §3.4, §4 | Expired refresh → `401` type `refresh-token-invalid`; revoked refresh → same; assert the client preserves the current path | Yes |
| RT-AC5 | §3.5, §4 | Rotate once, then replay the retired token → `401`; assert every family for that customer is revoked, an email is queued, and an `audit_events` row of severity `SECURITY` carries both IPs | Yes — but see F-3 |
| RT-AC6 | §3.4, §4 | Present a token with a broken signature → `401` with no algorithm, key or structure detail in the body; `audit_events` row written | Yes |
| RT-AC7 | §3.6, §4 | Deactivate the account mid-session, then refresh → `403` type `account-deactivated`; assert the access token stops working within 15 minutes | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Every token in that family is revoked immediately, and all of the customer's sessions end as potentially compromised." | §3.5 | `[RT-AC5]` | Yes — RT-AC5 now states both, and states they mean the same thing |
| "The retry happens at most once, so a persistent `401` does not become a loop." | §3.2 | `[RT-AC2]` | Yes — RT-AC2's `But` clause |
| "…an `audit_events` entry of severity `SECURITY` records the IP addresses of both requests." | §3.5 | `[RT-AC5]` | Yes; the severity scale is defined by US-3.7 since the correction |
| "Access therefore survives deactivation by no more than the access token's 15-minute time to live." | §3.6 | `[RT-AC7]` | Yes — RT-AC7 final clause |

## 5. Ambiguities and Missing Edge Cases

- **Retired-row retention versus cleanup** — see F-3. `[RT-AC5]`
- **Refresh with no token at all** — no criterion covers an absent cookie. A-5 assumes
  RT-AC4 governs, which is reasonable and unstated. `[RT-AC4]`
- **Reuse by a legitimate client** — RT-AC5 makes no distinction between a replay by an
  attacker and one by a prefetcher or a retrying extension. The customer is signed out
  everywhere with no self-service path back except signing in. Recorded as OQ-2.
  `[RT-AC5]`
- **Rate limit on an automatic endpoint** — the story limits refreshes to 10 per minute per
  account, and no criterion says what the client should do on a `429` from an endpoint it
  calls without user action. Recorded as OQ-4 in the spec. `[RT-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Seven criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | RT-AC5's revision-1 contradiction is resolved in the source |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`RT-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
