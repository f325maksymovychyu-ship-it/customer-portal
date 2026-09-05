---
spec: docs/specifications/US-3.3-update-user.md
spec_revision: 2
story_id: US-3.3
source: docs/backlog/US-3.3-update-user.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Update User

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Seven criteria, verbatim, all covered. The revision-2 correction closed the
Ukraine-only phone format and added UU-AC7 for locale and time zone, which unblocks two
Epic 5 criteria that were reading fields no story created. F-3 remains: UU-AC2 starts an
email change that nothing in the backlog finishes.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. UU-AC2's two-step email
change is an account-security decision.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.2; §5 Out of Scope; OQ-2

**Evidence:**
> A confirmation link is sent to the new address… The customer continues to sign in with
> the old address until the new one is confirmed. `[UU-AC2]`

**Problem:** UU-AC2's assertion terminates on a confirmation step that no criterion in any
story describes. There is no endpoint, no token lifetime, no cancellation path.

**Why it matters:** The criterion is testable up to the pending state and then stops. A
tester cannot assert "until the new one is confirmed" because nothing can perform the
confirmation. In production the pending change either never resolves or resolves through
undocumented behaviour.

**Resolves when:** a criterion covers consuming the confirmation link, or UU-AC2 is
narrowed to the pending state and the confirmation becomes its own story.

---

### F-4 · Minor · D5 Testability

**Location:** §3.5, third paragraph; UU-AC7

**Evidence:**
> Where a customer is created without them, the locale defaults to `uk-UA` and the time
> zone to `Europe/Kyiv`. `[UU-AC7]`

**Problem:** UU-AC7 states a default-on-create rule, but creation belongs to US-3.2, whose
criteria never mention locale or time zone. The rule is asserted by the story that owns
updates.

**Why it matters:** Low impact — the default is unambiguous and testable. It matters
because the assertion sits in the wrong story, so a change to US-3.2's create contract
would not obviously break anything here.

**Resolves when:** the default is stated in US-3.2's criteria, or UU-AC7 cross-references
it explicitly as US-4.7 does for the ticket lifecycle.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UU-AC1 | §3.1, §4 | `PATCH` family name and position with a current `If-Match` → `200`, new `ETag`, `audit_events` diff carrying exactly two fields with before and after, other columns unchanged | Yes |
| UU-AC2 | §3.2, §4 | `PATCH` the email → `200`, live `email` unchanged, `pending_email` set, two mails queued, sign-in still works with the old address, indicator present. The confirmation half is not derivable — F-3 | Partly |
| UU-AC3 | §3.3, §4 | Two concurrent updates from one `ETag` → second gets `409` type `stale-resource` with the exact message; assert the first write survives | Yes |
| UU-AC4 | §3.3, §4 | `PATCH` with no `If-Match` → `428` type `precondition-required`; `PATCH` a deleted id → `404` | Yes |
| UU-AC5 | §3.2, §3.4, §4 | `phone: "0501234567"` → `400`; `phone: "+380501234567"` and `"+14155552671"` → accepted; an address already live on another account → `409`, no pending row written | Yes |
| UU-AC6 | §3.6 | Client unit test: edit without saving, attempt navigation → prompt shown, nothing persisted | Yes |
| UU-AC7 | §3.5, §4 | `locale: "xx-ZZ"` → `400` naming the field; `timeZone: "Mars/Olympus"` → `400`; create without either → `uk-UA` and `Europe/Kyiv` | Yes — see F-4 |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Any international number in E.164 form is accepted." | §3.4 | `[UU-AC5]` | Yes — added to UU-AC5 in the correction |
| "An `audit_events` entry records only the changed fields, each with its before and after value." | §3.1 | `[UU-AC1]` | Yes |
| "Every notification rendered for that customer uses these values…" | §3.5 | `[UU-AC7]` | Yes — UU-AC7 final clause, which names US-5.5 and US-5.7 |
| "Where the new email is already registered to another account, the response is `409`…" | §3.2 | `[UU-AC5]` | Yes — UU-AC5 clauses 4 to 6 |

## 5. Ambiguities and Missing Edge Cases

- **Confirmation of a pending email change has no owner** — see F-3. `[UU-AC2]`
- **Pending change lifetime** — nothing states how long `pending_email` stays valid, or
  whether an administrator can withdraw it. Recorded as OQ-1 and OQ-2. `[UU-AC2]`
- **Two pending changes to the same address** — UU-AC5's uniqueness check compares against
  live addresses. A-4 assumes pending values are ignored, which lets two accounts both
  confirm and collide at promotion. Recorded as OQ-3. `[UU-AC5]`
- **Audit retention versus erasure** — UU-AC1 records before and after values for personal
  fields including a phone number. US-3.7 retains audit rows at least 12 months while
  US-3.4 UD-AC6 anonymises on request. The two obligations meet in this table and no
  criterion resolves them. Recorded as OQ-4. `[UU-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Seven criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`UU-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
