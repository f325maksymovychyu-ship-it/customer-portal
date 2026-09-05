---
spec: docs/specifications/US-5.6-unsubscribe.md
spec_revision: 2
story_id: US-5.6
source: docs/backlog/US-5.6-unsubscribe.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — One-Click Unsubscribe

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers, and this spec improved most in the correction. Revision 1 reported a genuine
conflict between two criteria in the same story: NU-AC1 required the page to name what would
stop while NU-AC4 required the endpoint to reveal nothing about the account. Both could not
hold. The criteria now separate the cases — a token that verifies may name its event type, a
token that does not names nothing — and additionally split the safe `GET` from the acting
`POST`, which closes the mail-scanner prefetch hole.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. NU-AC3 decides which
messages a recipient may never switch off.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** Blocked callout

**Problem:** The epic-wide module question (US-5.1 F-3) and the unapproved mail dependency
(US-5.5 F-3) both apply. This story has no independent path to being built.

**Resolves when:** both are resolved.

---

### F-4 · Minor · D5 Testability

**Location:** §4 row 2; OQ-2 as it now stands

**Evidence:**
> | 2 | unsubscribe token | Signed; expires at 90 days; **its scope is not stated by any
> criterion** | `[NU-AC1]` `[NU-AC4]` |

**Problem:** A-1 assumes the token carries one recipient and one event type and grants
nothing else. No criterion says so. The endpoint is unauthenticated by design, so the
token's scope is the only thing bounding what a holder can do.

**Why it matters:** Minor because the assumption is visible and conservative. It matters
because an implementation reading NU-AC1 alone could reasonably issue one token per
recipient covering every event type, which turns a forwarded email into a switch that
silences all notifications.

**Resolves when:** a criterion states the token's scope.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NU-AC1 | §3.1, §4 | Follow a transactional email's link with `GET`, no session → page names the event type, no preference row written; `POST` the confirmation → email channel disabled for that type, confirmation and a settings link shown, no address displayed | Yes |
| NU-AC2 | §3.2, §4 | Assert `List-Unsubscribe` and `List-Unsubscribe-Post` on a transactional, administrative and informational email; `POST` as a mail client would → processed with no confirmation page | Yes |
| NU-AC3 | §3.3, §4 | Assert a password-change email carries neither the link nor the header; submit a security-type token → `422` type `preference-locked`, nothing changed | Yes |
| NU-AC4 | §3.4, §4 | Tamper the signature → `404` type `unsubscribe-token-invalid`, page naming no address, account or event type; age a valid token past 90 days → same | Yes |
| NU-AC5 | §3.1, §4 | Unsubscribe, then reuse the same link inside 90 days → `200` with the same confirmation, nothing changed; past 90 days → `404` per NU-AC4 | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…the `GET` changes no preference, so a mail scanner following the link unsubscribes nobody." | §3.1 | `[NU-AC1]` | Yes — added in the correction |
| "…the page names no email address, so a forwarded link discloses nothing about the recipient." | §3.1 | `[NU-AC1]` | Yes — added in the correction |
| "…a token that does verify proves the holder has the email, so NU-AC1 may name the event type it carries." | §3.4 | `[NU-AC4]` | Yes — the reasoning is now in NU-AC4 itself |
| "But once the token passes 90 days NU-AC4 governs instead, and the response becomes `404`." | §3.1 | `[NU-AC5]` | Yes — precedence stated in NU-AC5 since the correction |

## 5. Ambiguities and Missing Edge Cases

- **Module and mail dependency** — see F-3. All criteria.
- **Token scope unstated** — see F-4. `[NU-AC1]`
- **Endpoint path unnamed** — no criterion gives it. The `GET`/`POST` split is now specified,
  which was the security-relevant half, but a tester still cannot address the endpoint from
  the criteria alone. `[NU-AC1]` `[NU-AC2]`
- **No route back** — a recipient who unsubscribes by mistake can only resubscribe by
  signing in. Defensible, and stated nowhere. Recorded as OQ-4. `[NU-AC1]`
- **Deactivated account** — whether the endpoint still succeeds for an address belonging to a
  deactivated account is unstated; NU-AC4's uniform response suggests it must, to avoid
  disclosing state. Recorded as OQ-6. `[NU-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Five criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | Revision 1's intra-story conflict is resolved in the source |
| D4 Traceability integrity | Yes | Zero Partial rows |
| D5 Testability and edge cases | Yes | Found F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NU-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
