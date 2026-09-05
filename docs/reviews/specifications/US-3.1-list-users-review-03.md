---
spec: docs/specifications/US-3.1-list-users.md
spec_revision: 3
story_id: US-3.1
source: docs/backlog/US-3.1-list-users.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 03
---

# Review 03 — List and Search Users

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 2 |

F-7 and F-8 are resolved. The story's Non-Functional section no longer contradicts UL-AC3,
and the enforcement row that promised a gate it could not deliver is split into three with
the infrastructure clause marked `[manual]` and owned.

**The fix introduced two new Major findings.** F-9: the replacement Non-Functional bullet
asserts that the residual browser-history exposure "is accepted", while the open question it
cites in the same sentence asks whether it is accepted. F-10: the same edit added a
data-classification claim — that identifiers and filter codes are not personal data — which
is a legal determination stated as engineering fact, in a document `AGENTS.md` §7.4 places
behind human review.

This is the third consecutive review in which the previous pass's work created or missed
something the next one caught. F-5 is no longer a formality.

## 2. Findings

### F-9 · Major · D6 Gap handling

**Location:** `docs/backlog/US-3.1-list-users.md`, Non-Functional / Security Requirements,
third bullet

**Evidence:**
> Neither covers the administrator's own browser history, which is accepted and recorded in
> Open Question 6.

and, in the same file:

> 6. … Is that residual exposure accepted, or should the term move to a POST body at the
>    cost of shareable URLs?

**Problem:** The bullet states the exposure **is accepted**. The open question it cites asks
**whether** it is accepted and names a concrete alternative. One of the two is wrong, and
they were written in the same edit.

**Why it matters:** This is the gap-handling failure the spec discipline exists to prevent,
in its most direct form: a decision nobody made, asserted in normative prose, with an open
question beside it that would have caught the invention if the prose had deferred to it. A
reader who trusts the Non-Functional section stops reading at "accepted" and the question
never reaches a stakeholder.

**Resolves when:** the bullet states the exposure without claiming a verdict on it, or
Open Question 6 is answered by someone with the authority to accept the risk and then closed.

---

### F-10 · Major · D3 Grounding

**Location:** `docs/backlog/US-3.1-list-users.md`, Non-Functional / Security Requirements,
fourth bullet

**Evidence:**
> Identifiers and filter codes are not personal data; a name or an email fragment is.

**Problem:** No acceptance criterion says this, and it is not an engineering statement. Under
GDPR a pseudonymous identifier is personal data whenever it can be linked back to a person,
which a customer identifier in this system plainly can. The sentence asserts the opposite as
settled fact.

**Why it matters:** It sits in a security section and reads as a rule, so it licenses exactly
the behaviour it appears to bound — customer identifiers in query strings, access logs and
outbound telemetry, on the stated grounds that they are not personal data. `AGENTS.md` §7.4
requires human review for anything touching PII policy, and this is a PII policy statement
written by an assistant while fixing an unrelated finding.

**Resolves when:** the classification claim is removed, or a data-protection owner confirms
it and the bullet records who and when.

---

### F-4 · Major · D1 AC fidelity — provenance

**Status:** Open — unchanged since review 01.

**Problem:** The criteria were authored by an assistant and approved by nobody.

**Resolves when:** a named stakeholder signs off on the criteria, and the callout records who
and when.

---

### F-5 · Major · D1 AC fidelity — reviewer independence

**Status:** Open, and now the highest-value finding on this spec.

**Problem:** One assistant wrote the story, the spec, the Run 4 correction, the Run 6 fix,
the Run 8 fix, and all three reviews.

**Why it matters:** The evidence across three passes is no longer circumstantial:

| Pass | What it caught | What it left behind |
|---|---|---|
| Review 01 | F-1, by script, after three hand passes missed it | Did not read the story's Non-Functional section, where the same defect lived |
| Review 02 | F-7 and F-8, by reading that section | — |
| Run 8 fix | Closed F-7 and F-8 | Introduced F-9 and F-10 in the replacement text |
| Review 03 | F-9 and F-10 | Unknown, by construction |

Each pass corrects its predecessor and seeds the next. That is what an author reviewing
their own work looks like, and no amount of further self-review converges.

**Resolves when:** a different reader, or a session with no memory of authoring these
documents, repeats the D1, D3 and D6 passes.

---

### F-6 · Minor · D4 Traceability integrity

**Status:** Open — unchanged.

**Location:** Spec §4 row 8; story Open Question 5

**Problem:** The sortable field set is still enumerated nowhere. Run 8 added story Open
Question 5 recording the gap, which surfaces it but does not close it. UL-AC6 remains the
spec's one Partial row.

**Resolves when:** the sortable field set is stated.

---

### F-11 · Minor · D4 Traceability integrity

**Location:** `docs/backlog/US-3.1-list-users.md`, Non-Functional bullet 3

**Evidence:**
> …recorded in Open Question 6.

**Problem:** A cross-reference to a position in a numbered list. Inserting a question above
it silently repoints the reference.

**Why it matters:** Minor, and it is the same defect class as the dangling `OQ-5` reference
found in `US-2.1` at review 01 — a pointer that reads as deliberate and resolves to the wrong
thing. The story's open questions carry no stable identifiers, unlike the spec's.

**Resolves when:** the story's open questions carry stable IDs, or the bullet describes the
question instead of numbering it.

## 3. AC → Spec → Test Traceability

Unchanged from review 02; the spec was not modified since. Repeated so this report stands
alone.

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| UL-AC1 | §3.1, §4 | Seed 30 customers; call with `customers:read` → `200`, 25 items ordered by `createdAt` descending, total = 30, six named fields each | Yes |
| UL-AC2 | §3.2, §4 | Seed "Olena" and "OLEKSII"; `q=olen` → both, "Ivan" absent; `status` and `role` together → only records satisfying both | Yes |
| UL-AC3 | §3.3, §4 | Reopen a filtered URL in a new tab → same filters, sort, page, `q` present; assert `Referrer-Policy: no-referrer` on the response. The redaction clause is `[manual]` and out of CI's reach | Yes, in part |
| UL-AC4 | §3.4, §4 | Call without the scope → `403` type `insufficient-scope` plus an `audit_events` row | Yes |
| UL-AC5 | §3.2, §3.6, §4 | No-match filters → `200`, empty page, total 0; stub a 500 → client keeps filters, offers retry | Yes |
| UL-AC6 | §3.5, §4 | `size=101` → `400`; `q=o` → `400`; `sort=passwordHash` → `400` | Yes — see F-6 |

## 4. Grounding Register

The spec is unchanged and clean. This pass assessed the **story** text that Run 8 rewrote,
since that is where the new findings are.

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The search term appears in the query string by design, because UL-AC3 requires a filtered view to be shareable…" | story NFR 3 | cites UL-AC3 | Yes |
| "…the gateway redacts `q` from access logs, and the listing page is served `Referrer-Policy: no-referrer`." | story NFR 3 | cites UL-AC3 | Yes — UL-AC3's `But` clause |
| "…which is accepted and recorded in Open Question 6." | story NFR 3 | cites UL-AC3 | **No** — no criterion accepts the residual exposure, and OQ-6 says the decision is open. F-9 |
| "Identifiers and filter codes are not personal data; a name or an email fragment is." | story NFR 4 | none | **No** — no criterion reaches data classification, and the claim is legal rather than technical. F-10 |
| Spec §3.3, all three paragraphs | spec §3.3 | `[UL-AC3]` | Yes — verified verbatim against the criterion; `grounding-scan.pl` returns 0 candidates |

## 5. Ambiguities and Missing Edge Cases

- **Whether the browser-history exposure is accepted** — asserted in one place and asked in
  another, in the same document. See F-9. `[UL-AC3]`
- **Who classifies data** — see F-10. `[UL-AC3]`
- **Redaction ownership** — story Open Question 4 and spec OQ-1 now both record it. Surfaced,
  not closed; the `[manual]` marker added in Run 8 makes the gap visible in CI terms, which
  is the improvement F-8 asked for. `[UL-AC3]`
- **Sortable field set** — F-6, unchanged. `[UL-AC6]`
- **Severity of a refused directory read** — spec OQ-4, unchanged since revision 3.
  `[UL-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim, unchanged since review 02 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | Spec clean; found F-10 in the story text rewritten by Run 8 |
| D4 Traceability integrity | Yes | Found F-11; F-6 open |
| D5 Testability and edge cases | Yes | F-8 resolved — the enforcement split makes the untestable clause visible rather than falsely gated |
| D6 Gap handling | Yes | Found F-9 |

Mechanical checks: `scripts/trace_check.ps1` still unusable against the `UL-AC<n>` scheme.
Substitutes, all clean for this spec: `check-specs.pl` (6 criteria verbatim, covered,
matrixed), `review-evidence.pl` (0 untagged normative paragraphs), `grounding-scan.pl`
(0 candidates). **None of the three inspects the story's Non-Functional section or its
Enforcement Matrix**, which is where every finding in reviews 02 and 03 was located. That
blind spot is now the process gap most worth closing.

## 7. Carried Forward

| Finding | From review | Status |
|---------|-------------|--------|
| F-1 | 01 | Resolved at 02 — re-verified, no regression |
| F-2 | 01 | Resolved at 02 — re-verified, no regression |
| F-3 | 01 | Resolved at 02 — re-verified, no regression |
| F-4 | 01 | Open — unchanged |
| F-5 | 01 | Open — strengthened by this pass, see the table in F-5 |
| F-6 | 01 | Open — surfaced by story Open Question 5, not closed |
| F-7 | 02 | **Resolved** — the Non-Functional bullet now states what UL-AC3 states, but see F-9 and F-10 on what replaced it |
| F-8 | 02 | **Resolved** — enforcement split three ways, infrastructure clause `[manual]` with an owner |
