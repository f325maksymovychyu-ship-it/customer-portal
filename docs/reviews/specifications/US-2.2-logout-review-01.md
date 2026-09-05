---
spec: docs/specifications/US-2.2-logout.md
spec_revision: 2
story_id: US-2.2
source: docs/backlog/US-2.2-logout.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Logout

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 1 |

No Blockers. Six criteria, all verbatim, all covered, matrix complete. This spec improved
most of any in the set between revisions: revision 1 reported four Partial rows including a
direct contradiction between LO-AC2 and LO-AC3, and the story correction closed all of
them. F-1 and F-2 are the structural findings that apply across the set. F-3 is a
single-tag omission.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** The criteria were drafted by an assistant and approved by nobody. LO-AC6 in
particular introduces a CSRF control, which is a security-scheme decision under
`AGENTS.md` §7.1.

**Why it matters:** A security control that entered the contract without review is
indistinguishable, downstream, from one a security owner asked for.

**Resolves when:** the criteria in `docs/backlog/US-2.2-logout.md` carry a recorded
sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant, across the story, the spec, the
correction and this report.

**Why it matters:** The D3 pass is the one that needs a second reader, and elsewhere in
this set it missed a Blocker until a script found it.

**Resolves when:** an independent reader repeats the grounding pass.

---

### F-3 · Minor · D4 Traceability integrity

**Location:** §3.3 Revoked-token handling, first paragraph

**Evidence:**
> The logout endpoint itself is exempt, so a repeated logout still answers `204` per §3.4.
> `[LO-AC2]`

**Problem:** The `204` comes from LO-AC3, not LO-AC2. LO-AC2 states only that the logout
endpoint is exempt "per LO-AC3"; the status code is asserted one criterion over. The
paragraph carries a single tag.

**Why it matters:** Minor, because the statement is grounded — just not by the criterion
cited. It matters at all because a reader verifying the `204` against LO-AC2 will not find
it and may conclude the spec invented it.

**Resolves when:** the paragraph cites both criteria, or the cross-reference to §3.4 stands
without restating the status.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| LO-AC1 | §3.1, §3.2, §4 | Signed-in customer posts to `/api/v1/auth/logout` with the refresh cookie → `204`, `Set-Cookie` with `Max-Age=0`, token row `revoked_at` set, `audit_events` row `LOGOUT_SUCCEEDED`; client asserts `sessionStorage`, `localStorage` and the portal's IndexedDB stores are empty | Yes |
| LO-AC2 | §3.3, §4 | After logout, post the revoked token to `/api/v1/auth/refresh` → `401` type `refresh-token-invalid`; every row sharing its `family_id` is revoked; `audit_events` row `TOKEN_REUSE_AFTER_LOGOUT` | Yes — see F-3 |
| LO-AC3 | §3.4 | Call logout twice → both `204`; assert no `TOKEN_REUSE_AFTER_LOGOUT` row exists after the second | Yes |
| LO-AC4 | §3.5 | Stub a network failure on the logout call → client state cleared, exact message shown; assert the server token is still valid; assert the client reissues logout on its next successful API call | Yes |
| LO-AC5 | §3.7, §4 | After logout, browser back → login screen, no protected markup from cache; with two tabs open, sign out in one → the other reaches signed-out within 5 s | Yes |
| LO-AC6 | §3.6, §4 | Post to logout from a foreign origin with the cookie but no CSRF token → `403` type `csrf-token-missing`; assert the refresh token is still valid | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…a repeated logout still answers `204` per §3.4." | §3.3 | `[LO-AC2]` | Partly — the exemption is LO-AC2's, the `204` is LO-AC3's. F-3 |
| "On logout the client clears the in-memory access token, `sessionStorage`, `localStorage` and any IndexedDB store the portal owns." | §3.2 | `[LO-AC1]` | Yes — enumerated in LO-AC1 since the correction |
| "The server-side token remains valid until its own expiry, at most 30 days…" | §3.5 | `[LO-AC4]` | Yes — LO-AC4 clause 4, which cites US-2.1 LI-AC8 |
| "`SameSite=Strict` on the refresh cookie is a second layer, not the control this criterion tests." | §3.6 | `[LO-AC6]` | Yes — LO-AC6's `Because` clause |

## 5. Ambiguities and Missing Edge Cases

- **CSRF token representation** — LO-AC6 requires "a valid CSRF token" without naming the
  header, cookie or form field that carries it. A tester can construct the negative case
  but not the positive one. `[LO-AC6]`
- **The retry in LO-AC4 has no expiry** — the client retries logout "on its next successful
  request to the API". If the customer never returns, the token lives to its natural
  expiry. OQ-2 records the consequence but no criterion bounds the retry itself.
  `[LO-AC4]`
- **Two tabs, one offline** — LO-AC5 asserts propagation within 5 seconds. If the second
  tab is suspended or offline, nothing states whether the bound applies on resume.
  `[LO-AC5]`
- **Absent cookie on the first call** — LO-AC3 covers "the same or an absent cookie" for a
  repeated logout. A first logout with no cookie at all is not covered by any criterion.
  `[LO-AC3]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | Found F-3 |
| D4 Traceability integrity | Yes | Matrix complete, one row per criterion |
| D5 Testability and edge cases | Yes | |
| D6 Gap handling | Yes | Four open questions, all genuinely open |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`LO-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
