---
spec: docs/specifications/US-2.3-active-sessions.md
spec_revision: 2
story_id: US-2.3
source: docs/backlog/US-2.3-active-sessions.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Active Session Management

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 0 |

No Blockers. Five criteria, verbatim, all covered. F-1 and F-2 are the structural findings
that apply across this set. F-3 is specific: every criterion that exercises the bulk revoke
passes `keepCurrent=true`, so half the parameter's behaviour is undefined while the API
contract advertises both values. SM-AC1 is correctly Partial — the "approximate city" it
requires has no defined source or fallback.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** The criteria were drafted by an assistant and approved by nobody. This story
defines a self-service incident-response control; whether a customer may end every session
without re-authenticating is a security-policy decision.

**Why it matters:** Decision 1 in the story chooses "confirmation dialog, no password" and
no criterion tests it, so the choice is invisible to QA and unreviewed by anyone.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Why it matters:** The grounding pass is the one that needs a second reader; elsewhere in
this set it missed a Blocker until a script caught it.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.2, final paragraph; §4 row 4

**Evidence:**
> The behaviour when `keepCurrent` is `false`, or absent, is not stated by any criterion —
> see OQ-1.

**Problem:** The spec is honest about the gap, and the gap is load-bearing. The API
contract types `keepCurrent` as a boolean and SM-AC2 and SM-AC5 both pass `true`. Nothing
defines what `false` does — whether the caller's own session ends, what the response
carries, or where the client goes next.

**Why it matters:** A developer implementing the endpoint must choose a behaviour for
`false`, and whatever they choose ships as though it were specified. The safest reading —
that `false` ends every session including the caller's — is also the one that signs the
customer out mid-incident, which Decision 2 explicitly calls hostile.

**Resolves when:** a criterion defines `false`, or the parameter is removed and the
endpoint always preserves the current session.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| SM-AC1 | §3.1, §4 | Seed three families for one customer; `GET /api/v1/auth/sessions` → `200`, three entries, each with device, browser, city and last activity, exactly one flagged current. The city assertion cannot be made deterministic — see §5 | Partly |
| SM-AC2 | §3.2, §4 | Three sessions; `revoke-all` with `keepCurrent=true` → `200`, count 2, the two other families revoked, the current one intact, one email queued; assert the other devices fail to refresh within 15 minutes | Yes |
| SM-AC3 | §3.3, §4 | `DELETE` another device's session id → `204`, that family revoked, the caller's own still usable | Yes |
| SM-AC4 | §3.4, §4 | `DELETE` a session id owned by a second customer → `404`, target still active, `audit_events` row for the attempt | Yes |
| SM-AC5 | §3.2, §4 | Single-session customer calls `revoke-all` with `keepCurrent=true` → `200`, count 0, mail port never invoked | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Devices whose sessions were ended lose access at their next refresh, and no later than 15 minutes…" | §3.2 | `[SM-AC2]` | Yes — SM-AC2 clause 3 |
| "Exactly one entry is flagged as the current session." | §3.1 | `[SM-AC1]` | Yes |
| "The target session remains active, and an `audit_events` entry records the unauthorised attempt." | §3.4 | `[SM-AC4]` | Yes |
| "Where the current session is the only active session, the response is `200` with a count of zero and no email is sent." | §3.2 | `[SM-AC5]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **`keepCurrent=false` is undefined** — see F-3. `[SM-AC2]` `[SM-AC5]`
- **Approximate city has no source or fallback** — SM-AC1 requires one on every entry.
  Nothing states the resolution method, the precision, or what is displayed for a
  private-range or unresolvable address, so no deterministic assertion is possible.
  Recorded as OQ-2. `[SM-AC1]`
- **Ending your own current session through `DELETE`** — SM-AC3 covers "another device".
  The endpoint accepts any session id the caller owns, including the current one, which
  would duplicate US-2.2 with a different status code. Recorded as OQ-3. `[SM-AC3]`
- **Email failure after a successful revoke** — SM-AC2 promises an email. Nothing says
  whether a delivery failure makes the operation incomplete. Recorded as OQ-4.
  `[SM-AC2]`
- **Asynchronous revoke and the returned count** — the story permits the bulk revoke to be
  asynchronous, while SM-AC2 requires the response to carry "the number of sessions ended".
  Attempted and completed differ. Recorded as OQ-5. `[SM-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Five criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | Matrix complete and consistent |
| D5 Testability and edge cases | Yes | Found F-3 |
| D6 Gap handling | Yes | Five open questions, all genuine |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`SM-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
