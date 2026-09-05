---
spec: docs/specifications/US-3.1-list-users.md
spec_revision: 2
story_id: US-3.1
source: docs/backlog/US-3.1-list-users.md
source_verified: true
verdict: Fail
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — List and Search Users

## 1. Verdict

**Fail**

| Severity | Count |
|----------|-------|
| Blocker | 1 |
| Major | 4 |
| Minor | 1 |

F-1 is decisive. §3.3 states a normative rule — that the query string never carries
personal data — and cites UL-AC3 for it. UL-AC3 says the opposite: the search term *is*
carried, and the exposure is mitigated by log redaction and a referrer policy. A developer
building §3.3 would omit the search term from the URL and break UL-AC2 and UL-AC3 together.
F-2 and F-3 are the same stale reading surfacing in the matrix and the open questions, so
all three clear together.

F-4 and F-5 are structural and apply to every spec in this set: the story was never
approved by a stakeholder, and this review is not independent of the spec's author.

## 2. Findings

### F-1 · Blocker · D3 Grounding

**Location:** §3.3 View state, second paragraph

**Evidence:**
> The query string carries only identifiers and filter codes, never personal data.
> `[UL-AC3]` This clause conflicts with the free-text search of UL-AC2 — see OQ-1.

**Problem:** UL-AC3 no longer contains that clause. Its current text reads: "And the search
term is carried in the query string, because a shareable filtered view requires it / But the
gateway redacts the q parameter from access logs, and the page is served
Referrer-Policy: no-referrer". The spec asserts the inverse of the criterion it cites, and
then reports a conflict that the criterion resolved.

**Why it matters:** The two statements are not merely different, they are contradictory. A
developer who implements §3.3 will strip `q` from the URL, which makes UL-AC2's shareable
filtered view unbuildable and fails UL-AC3's first clause. The `[UL-AC3]` tag is what stops
a reviewer from checking, which is why this reads as verified.

**Resolves when:** §3.3 states what UL-AC3 states — the term is carried, with log redaction
and `Referrer-Policy: no-referrer` — or UL-AC3 is changed back and the conflict is real
again.

---

### F-2 · Major · D4 Traceability integrity

**Location:** §8 Traceability Matrix, row UL-AC3

**Evidence:**
> | UL-AC3 | View state survives a reload and is shareable | §3.3 | **Partial** — its
> "never personal data" clause contradicts UL-AC2 (OQ-1) |

**Problem:** The row reports a contradiction between UL-AC2 and UL-AC3 that no longer
exists. UL-AC3 resolves it explicitly.

**Why it matters:** This is a false **Partial** — the mirror image of the dangerous case.
It understates coverage, which sends a reader hunting for a decision that has already been
made, and it will keep a buildable criterion out of a sprint.

**Resolves when:** the row reflects whether UL-AC3 is testable from the body as it actually
stands.

---

### F-3 · Major · D6 Gap handling

**Location:** §7 Open Questions, OQ-1

**Evidence:**
> OQ-1 | UL-AC3 requires the query string to carry "never personal data", while UL-AC2
> requires a free-text search term to survive a reload and be shareable. … Which criterion
> wins?

**Problem:** The question is answered in the criteria. UL-AC3 now says the term is carried
and names the two mitigations.

**Why it matters:** An open question that is no longer open costs a stakeholder a decision
cycle and trains readers to skim the section — which is where the genuinely open items live.

**Resolves when:** OQ-1 is retired, or restated as whatever remains undecided about the
mitigation (for example, whether redaction is enforced at the gateway or the application).

---

### F-4 · Major · D1 AC fidelity — provenance

**Location:** Front matter and the provenance callout

**Evidence:**
> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder.

**Problem:** The acceptance criteria this spec restates were authored by an assistant. No
product owner, customer or regulator has agreed to them.

**Why it matters:** Downstream, nobody can distinguish invented acceptance criteria from
agreed ones. The spec is internally faithful to its source and the source has no authority,
which is the same failure as an invented requirement, one level up.

**Resolves when:** a named stakeholder signs off on the criteria in
`docs/backlog/US-3.1-list-users.md`, and the callout records who and when.

---

### F-5 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** The same assistant authored the backlog story, wrote this specification,
corrected the story after the first review pass, and produced this review. The skill's
premise — that the author cannot reliably see their own invented requirements — is not
satisfied.

**Why it matters:** The grounding pass (D3) is the one that most depends on a second
reader. F-1 was found by a token-comparison script rather than by reading, which is
evidence for the concern rather than against it: three hand passes over this file did not
catch it.

**Resolves when:** a different reader, or a different session with no memory of authoring
the spec, repeats at least the D3 pass. The same condition was recorded for `CP-101` in
review 01 and remains open there.

---

### F-6 · Minor · D4 Traceability integrity

**Location:** §4 Data and Interfaces, row 8

**Evidence:**
> | 8 | `sort` (query) | Restricted to fields the summary exposes; default `createdAt`
> descending | `[UL-AC1]` `[UL-AC6]` |

**Problem:** "Fields the summary exposes" is not enumerated anywhere in the spec or the
story. The row is honest about the restriction but not about the set, and OQ-2 carries the
gap.

**Why it matters:** Two implementations could allow different sort fields and both satisfy
UL-AC6. Low impact because OQ-2 records it.

**Resolves when:** the sortable field set is stated, or the row says "not specified" as the
template prescribes.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UL-AC1 | §3.1, §4 | Seed 30 customers; `GET /api/v1/admin/customers` with `customers:read` → `200`, 25 items, ordered by `createdAt` descending, total = 30, each item carrying six named fields | Yes |
| UL-AC2 | §3.2, §4 | Seed "Olena" and "OLEKSII"; `q=olen` → both returned, "Ivan" not; `status=DEACTIVATED&role=SUPPORT_AGENT` → only records satisfying both | Yes |
| UL-AC3 | §3.3 | Not derivable: the body forbids the search term in the URL and the criterion requires it. A tester cannot know which to assert | No — see F-1 |
| UL-AC4 | §3.4, §4 | Call with a token lacking `customers:read` → `403`, type `insufficient-scope`, and an `audit_events` row for the refusal | Yes |
| UL-AC5 | §3.2, §3.6, §4 | `q=zzzznomatch` → `200`, empty page, total 0; stub a 500 and assert the client keeps the filters and offers retry | Yes |
| UL-AC6 | §3.5, §4 | `size=101` → `400`; `q=o` → `400` naming the minimum; `sort=passwordHash` → `400` | Yes — see F-6 |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The query string carries only identifiers and filter codes, never personal data." | §3.3 | `[UL-AC3]` | **No** — UL-AC3 requires the search term to be carried, with redaction and a referrer policy |
| "Filters, sort order and page are carried in the request URL…" | §3.3 | `[UL-AC3]` | Yes — first three clauses of UL-AC3 |
| "Where the filters match no records, the response is `200` with an empty page and a total of zero…" | §3.2 | `[UL-AC5]` | Yes |
| "The administration entry point is not rendered for such a caller." | §3.4 | `[UL-AC4]` | Yes — UL-AC4 clause 3 |
| "A `sort` value naming a field the summary does not expose is rejected with `400`…" | §3.5 | `[UL-AC6]` | Yes, though the field set is undefined — F-6 |

## 5. Ambiguities and Missing Edge Cases

- **Sortable field set** — UL-AC6 rejects a field "the summary does not expose" without
  listing what it exposes. A tester can only test the negative case with a field that is
  obviously absent. `[UL-AC6]`
- **`PENDING_INVITATION` accounts in the default listing** — UL-AC1 states a total and a
  page but says nothing about whether invited-but-unclaimed accounts are counted. Recorded
  as OQ-3. `[UL-AC1]`
- **Redaction ownership** — UL-AC3 now requires the gateway to redact `q` from access logs.
  Nothing states whether this is the application's responsibility or infrastructure's, so
  no test in this repository can assert it. `[UL-AC3]`
- **Combined filter with an empty search term** — UL-AC2 exercises `q` alone and the two
  filters together, never `q` with a filter. The interaction is unspecified. `[UL-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Verbatim comparison against `docs/backlog/US-3.1-list-users.md`; criteria match exactly. Provenance is F-4 |
| D2 Coverage | Yes | Every AC referenced in §3 or §4 |
| D3 Grounding | Yes | Found F-1 |
| D4 Traceability integrity | Yes | Found F-2, F-6 |
| D5 Testability and edge cases | Yes | |
| D6 Gap handling | Yes | Found F-3 |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable**. Its AC pattern matches
only the bare `AC-<n>` form, so against this spec's `UL-AC<n>` scheme it reported
"Declared ACs (0): none found" and then printed three `OK` lines over an empty set. Its
untagged-paragraph warning listed 22 paragraphs whose tags are visible in its own output.
Substitutes were used: `docs/tools/check-specs.pl` (AC set, verbatim text, coverage,
dangling tags, matrix completeness), `docs/tools/review-evidence.pl` (untagged normative
paragraphs) and `docs/tools/grounding-scan.pl` (token-level support for tagged statements),
the last of which found F-1.
