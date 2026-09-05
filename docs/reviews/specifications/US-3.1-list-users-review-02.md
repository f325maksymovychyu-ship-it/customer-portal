---
spec: docs/specifications/US-3.1-list-users.md
spec_revision: 3
story_id: US-3.1
source: docs/backlog/US-3.1-list-users.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 02
---

# Review 02 — List and Search Users

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 2 |

The Blocker is cleared. F-1, F-2 and F-3 were one stale reading surfacing in three places —
the functional body, the matrix and the open questions — and revision 3 closed all three.
The grounding scan that found F-1 now returns zero candidates for this file.

But the same stale reading is still alive **in the story**, one section below the criteria.
F-7 is new and is the reason this is not a Pass: the Non-Functional section still asserts
that personal data must not appear in query strings and cites UL-AC3 as the reason, which is
the exact sentence revision 3 removed from the spec. Fixing the spec did not fix its source.

F-4 and F-5 remain open and are structural rather than fixable here.

## 2. Findings

### F-7 · Major · D1 AC fidelity

**Location:** `docs/backlog/US-3.1-list-users.md`, Non-Functional / Security Requirements,
third bullet

**Evidence:**
> - Personal data must not appear in query strings, access logs or outbound telemetry —
>   UL-AC3 constrains what the client may put in the URL for exactly this reason.

**Problem:** UL-AC3 no longer constrains the client that way. Since the Run 4 correction it
reads "the search term is carried in the query string, because a shareable filtered view
requires it", mitigated by log redaction and a referrer policy. The bullet states the
pre-correction rule, names UL-AC3 as its justification, and sits in the same file.

This is the root of the Blocker in review 01. The correction changed the criterion; the
prose that had justified the old criterion was left behind. The spec was then repaired in
revision 3 while the story it restates still contradicts itself.

**Why it matters:** A developer implementing from the story rather than the spec — the
normal case, since the story is the backlog artefact — reads the criterion and the
Non-Functional section together and finds them incompatible. The most likely resolution, a
security bullet stated in absolute terms beating a criterion's `And` clause, reinstates
exactly the defect review 01 raised. It also means the next spec regenerated from this story
can inherit the contradiction legitimately.

**Resolves when:** the bullet states what UL-AC3 states — that the term is carried and the
exposure is bounded by redaction and the referrer policy — or UL-AC3 is changed back and the
spec follows.

---

### F-4 · Major · D1 AC fidelity — provenance

**Status:** Open — unchanged since review 01.

**Location:** Front matter and the provenance callout

**Problem:** The acceptance criteria were authored by an assistant. No stakeholder has
agreed to them.

**Resolves when:** a named stakeholder signs off on the criteria in
`docs/backlog/US-3.1-list-users.md`, and the callout records who and when.

---

### F-5 · Major · D1 AC fidelity — reviewer independence

**Status:** Open — unchanged, and this review does not improve it.

**Location:** Whole document

**Problem:** The same assistant authored the story, the spec, the Run 4 corrections, the
Run 6 fix, review 01 and this review.

**Why it matters:** Review 01 recorded that F-1 was found by a script rather than by
reading, after three hand passes had missed it. This review repeats the pattern in the other
direction: F-7 was found by opening the source's Non-Functional section, which no automated
check in this repository inspects and which review 01 did not read. Two consecutive reviews
have each missed something the other caught, both by the same author.

**Resolves when:** a different reader, or a session with no memory of authoring the
documents, repeats the D1 and D3 passes.

---

### F-6 · Minor · D4 Traceability integrity

**Status:** Open — unchanged since review 01.

**Location:** §4 Data and Interfaces, row 8

**Evidence:**
> | 8 | `sort` (query) | Restricted to fields the summary exposes; default `createdAt`
> descending | `[UL-AC1]` `[UL-AC6]` |

**Problem:** "Fields the summary exposes" is not enumerated. OQ-2 carries the gap, and
UL-AC6 remains the spec's one Partial row.

**Resolves when:** the sortable field set is stated.

---

### F-8 · Minor · D5 Testability

**Location:** `docs/backlog/US-3.1-list-users.md`, Enforcement Matrix, UL-AC3 row

**Evidence:**
> | UL-AC3 | Client unit test on query-parameter serialisation | `[gate]` |

**Problem:** UL-AC3 gained two clauses in the Run 4 correction — gateway log redaction and
`Referrer-Policy: no-referrer` — and its enforcement row did not change. A client unit test
on query-parameter serialisation can verify neither.

**Why it matters:** The `[gate]` marker asserts that CI blocks a merge which violates
UL-AC3. It does not: the pipeline goes green with the redaction unconfigured and the header
absent. A marker that promises assurance it cannot deliver is worse than `[manual]`, because
nobody then checks by hand.

**Resolves when:** the row names a mechanism covering all three clauses, or splits them and
marks the infrastructure half `[manual]` with an owner.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UL-AC1 | §3.1, §4 | Seed 30 customers; `GET /api/v1/admin/customers` with `customers:read` → `200`, 25 items ordered by `createdAt` descending, total = 30, each carrying six named fields | Yes |
| UL-AC2 | §3.2, §4 | Seed "Olena" and "OLEKSII"; `q=olen` → both, "Ivan" absent; `status=DEACTIVATED&role=SUPPORT_AGENT` → only records satisfying both | Yes |
| UL-AC3 | §3.3, §4 | Apply filters, move to page three, reopen the URL in a new tab → same filters, sort and page, `q` present; assert the response carries `Referrer-Policy: no-referrer`. The redaction clause is not testable in this repository — §5 | Yes, in part — see F-8 |
| UL-AC4 | §3.4, §4 | Call without `customers:read` → `403` type `insufficient-scope` and an `audit_events` row for the refusal | Yes |
| UL-AC5 | §3.2, §3.6, §4 | `q=zzzznomatch` → `200`, empty page, total 0; stub a 500 and assert the client keeps filters and offers retry | Yes |
| UL-AC6 | §3.5, §4 | `size=101` → `400`; `q=o` → `400` naming the minimum; `sort=passwordHash` → `400` | Yes — see F-6 |

## 4. Grounding Register

Re-assessed in full, since revision 3 rewrote the section that carried the Blocker.

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The search term is carried in the query string, because a shareable filtered view requires it." | §3.3 | `[UL-AC3]` | Yes — UL-AC3 clause 5, verbatim in substance |
| "…the gateway redacts the `q` parameter from access logs, and the page is served `Referrer-Policy: no-referrer`." | §3.3 | `[UL-AC3]` | Yes — UL-AC3's `But` clause |
| "Because a search term may itself be personal data and the URL is the only place it can live…" | §3.3 | `[UL-AC3]` | Yes — UL-AC3's `Because` clause |
| "`Referrer-Policy` (response header) — value `no-referrer` on the listing page" | §4 row 3a | `[UL-AC3]` | Yes |
| "`q` … carried in the URL and redacted from access logs" | §4 row 3 | `[UL-AC2]` `[UL-AC3]` | Yes |

`grounding-scan.pl` returns zero candidates for this file, against three at review 01.

## 5. Ambiguities and Missing Edge Cases

- **UL-AC3 spans three owners** — the client holds the URL state, the application sets the
  header, the gateway redacts the log. No single suite covers the criterion, and the third
  clause is unverifiable in this repository. OQ-1 now records this, so it is a surfaced gap
  rather than a defect in the spec — but it is what makes F-8's `[gate]` marker untrue.
  `[UL-AC3]`
- **Browser history is untouched by either mitigation** — redaction covers server logs and
  the referrer policy covers outbound requests. A search for an email fragment still sits in
  the administrator's own history and in any browser-sync service. OQ-1 names this and no
  criterion addresses it. `[UL-AC3]`
- **Sortable field set undefined** — F-6, unchanged. `[UL-AC6]`
- **`PENDING_INVITATION` accounts in the default listing** — OQ-3, unchanged. `[UL-AC1]`
- **Severity of a refused directory read** — OQ-4 was tautological at review 01 and was not
  caught then; revision 3 restated it against the severity scale US-3.7 now defines. The
  question itself remains open: US-3.7 lists refused access to *audit* data as `SECURITY` and
  says nothing about a refused directory read. `[UL-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Found F-7 in the source's Non-Functional section, which review 01 did not read |
| D2 Coverage | Yes | Every criterion referenced in §3 or §4 |
| D3 Grounding | Yes | Full re-assessment; zero ungrounded statements, against one Blocker at review 01 |
| D4 Traceability integrity | Yes | F-2 cleared; F-6 open |
| D5 Testability and edge cases | Yes | Found F-8 |
| D6 Gap handling | Yes | F-3 cleared; OQ-1 now records a real residual question |

Mechanical checks: `scripts/trace_check.ps1` re-run and **still not usable** — it reports
"Declared ACs (0): none found" against the `UL-AC<n>` scheme and then prints `OK` over an
empty set. Substitutes, all clean for this file: `docs/tools/check-specs.pl` (6 criteria
verbatim, covered, matrixed), `docs/tools/review-evidence.pl` (0 untagged normative
paragraphs), `docs/tools/grounding-scan.pl` (0 candidates).

## 7. Carried Forward

| Finding | From review | Status |
|---------|-------------|--------|
| F-1 | 01 | **Resolved** — §3.3 now states UL-AC3's actual rule and both mitigations |
| F-2 | 01 | **Resolved** — the UL-AC3 matrix row is no longer a false Partial |
| F-3 | 01 | **Resolved** — OQ-1 restated as the residual redaction-ownership question |
| F-4 | 01 | Open — unchanged |
| F-5 | 01 | Open — unchanged; see the note in F-5 on what each review missed |
| F-6 | 01 | Open — unchanged |
