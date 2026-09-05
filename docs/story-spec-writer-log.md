# story-spec pipeline — run log

## Run 9 — 2026-08-22 — story-spec-reviewer, third pass on US-3.1

`docs/reviews/US-3.1-list-users-review-03.md`. **Needs Changes** — 0 Blocker, 4 Major,
2 Minor.

F-7 and F-8 Resolved. F-1 to F-3 re-verified, no regression.

### The Run 8 fix introduced two new Major findings

- **F-9** — the replacement Non-Functional bullet says the residual browser-history exposure
  "is accepted and recorded in Open Question 6", while Open Question 6, added in the same
  edit, asks *whether* it is accepted. A decision nobody made, asserted in normative prose,
  with the open question that would have caught it sitting beside it.
- **F-10** — the same edit added "Identifiers and filter codes are not personal data; a name
  or an email fragment is." No criterion reaches data classification, and the claim is legal
  rather than technical: a pseudonymous identifier is personal data under GDPR whenever it
  can be linked back. In a security section it reads as permission to put customer
  identifiers in logs and telemetry. `AGENTS.md` §7.4 puts PII policy behind human review.
- **F-11**, Minor — "Open Question 6" is a positional reference into an unnumbered list, the
  same fragile-pointer class as the dangling `OQ-5` found in `US-2.1` at review 01.

### What three passes now establish

| Pass | Caught | Left behind |
|---|---|---|
| Review 01 | F-1, by script, after three hand passes missed it | Never read the story's Non-Functional section |
| Review 02 | F-7, F-8, by reading that section | — |
| Run 8 fix | Closed F-7, F-8 | Introduced F-9, F-10, F-11 |
| Review 03 | F-9, F-10, F-11 | Unknown, by construction |

Each pass corrects its predecessor and seeds the next. **F-5, reviewer independence, is no
longer a formality — it is the finding that governs the value of everything else in
`docs/reviews/`.** Further self-review does not converge.

### The tooling gap this exposes

`check-specs.pl`, `review-evidence.pl` and `grounding-scan.pl` all inspect the **spec** and
the **criteria**. None reads the story's Non-Functional section or its Enforcement Matrix —
and every finding in reviews 02 and 03 was located in exactly those two places. Run 7 already
noted that a changed criterion leaves three things stale and only one is checked; this pass
confirms it empirically twice over.

Closing that gap is worth more than another review pass by the same author.

---

## Run 8 — 2026-08-22 — story-spec-writer applying review 02 F-7 and F-8

**Request:** fix F-7 and F-8.

Both were in `docs/backlog/US-3.1-list-users.md`, not in the spec, and neither touched a
criterion — so `check-specs.pl` stayed green at 29/29 with no re-sync needed.

### F-7 — the story's Non-Functional section contradicted its own criterion

The bullet asserting that personal data must never appear in a query string, citing UL-AC3
as its justification, is replaced by two bullets that state what UL-AC3 actually says: the
search term is in the URL by design, its exposure is bounded by the two named mitigations,
and neither covers the administrator's own browser history. A second bullet keeps the
original intent where it still holds — no personal data *other than* the search term belongs
in a query string, an access log or telemetry — and says plainly that identifiers and filter
codes are not personal data while a name or an email fragment is.

This closes the last place the pre-correction reading survived in a live document. It now
exists only in review 01 and in this log, both of which are historical records that quote
the defect deliberately.

### F-8 — a `[gate]` marker that could not gate

The single UL-AC3 enforcement row is split into three, because the criterion spans three
owners:

| Clause | Mechanism | Marker |
|---|---|---|
| URL state | Client unit test on query-parameter serialisation | `[gate]` |
| Referrer policy | Contract test on the listing response headers | `[gate]` |
| Log redaction | Gateway configuration review, not verifiable from this repository | `[manual]`, owner: platform |

Backlog-wide the markers move from 194 `[gate]` / 4 `[manual]` to **195 / 6**. The two new
`[manual]` rows are not a regression — they are two assurances that were previously claimed
and not delivered.

### Also added

Three open questions the corrections had left implicit in the story: redaction ownership,
the unenumerated sortable field set, and whether the residual browser-history exposure is
accepted or whether the term should move to a POST body at the cost of shareable URLs.

Overview rebuilt and republished.

**Status of the findings.** F-7 and F-8 are addressed, not Resolved. A finding is marked
Resolved by a re-review, not by the author who applied the fix — the same discipline that
kept review 01 unedited through Run 6.

---

## Run 7 — 2026-08-22 — story-spec-reviewer re-review of US-3.1

**Request:** run the review again.

Scoped to `US-3.1`, the only spec that changed since Run 5. Re-reviewing 28 unchanged files
would add 28 duplicate reports to an append-only record and no information.
`docs/reviews/US-3.1-list-users-review-02.md`, verdict **Needs Changes** — 0 Blocker,
3 Major, 2 Minor.

### Carried forward

| Finding | Status |
|---|---|
| F-1 Blocker | **Resolved** |
| F-2 Major | **Resolved** |
| F-3 Major | **Resolved** |
| F-4 Major, provenance | Open |
| F-5 Major, independence | Open |
| F-6 Minor, sortable fields | Open |

`grounding-scan.pl` returns 0 candidates for this file, against 3 at review 01.

### F-7 — the Blocker's root is still in the story

New, Major. The Run 6 fix repaired the spec. It did not touch the story, whose
Non-Functional section still reads:

> Personal data must not appear in query strings, access logs or outbound telemetry —
> UL-AC3 constrains what the client may put in the URL for exactly this reason.

That is the pre-correction rule, citing UL-AC3 as its justification, in the same file as the
criterion that now says the opposite. Run 4 changed the criterion and left behind the prose
that had justified the old one. Anyone implementing from the backlog rather than the spec
meets both and will most likely follow the absolute-sounding security bullet — reinstating
the exact defect review 01 raised. A spec regenerated from this story would inherit the
contradiction legitimately.

**Not fixed here.** The reviewer produces findings, not patches.

### F-8 — a `[gate]` marker that cannot gate

New, Minor. UL-AC3 gained two clauses in Run 4 — gateway log redaction and
`Referrer-Policy: no-referrer` — and its Enforcement Matrix row still reads "Client unit
test on query-parameter serialisation", marked `[gate]`. That test can verify neither
clause, so CI goes green with both mitigations absent. A marker promising assurance it
cannot deliver is worse than `[manual]`, because it stops anyone checking by hand.

### What this run says about the process

Review 01 found F-1 by script after three hand passes missed it. Review 02 found F-7 by
opening a section of the source that no automated check inspects and that review 01 never
read. Two consecutive reviews, same author, each missing what the other caught. F-5 stands
and is the finding to act on first: everything else here was found despite the process, not
because of it.

**Correction pattern worth generalising:** when a criterion changes, three things around it
go stale and none is caught by the existing checks — the story's Non-Functional prose, the
story's Enforcement Matrix row, and any spec text that had justified the old wording. Only
the third is covered today, by `check-specs.pl`.

---

## Run 6 — 2026-08-22 — story-spec-writer applying review 01 F-1 on US-3.1

**Request:** fix the Blocker in `US-3.1`.

Applied as a visible, separate change after the finding was recorded, per the reviewer
skill's separation of findings from patches. `docs/specifications/US-3.1-list-users.md` is
now revision 3. **The backlog was not touched** — the criterion was already correct; the
spec had drifted from it.

| Finding | Was | Now |
|---|---|---|
| F-1 Blocker | §3.3 asserted "the query string carries only identifiers and filter codes, never personal data" citing UL-AC3, which says the opposite | §3.3 states the term is carried and records both mitigations; §4 gains the `Referrer-Policy` row and notes the `q` redaction |
| F-2 Major | UL-AC3 matrix row reported a contradiction with UL-AC2 that the criterion had resolved | Covered; UL-AC2's row no longer points at a question that does not concern it |
| F-3 Major | OQ-1 asked which of UL-AC2 and UL-AC3 wins | Restated as the residual question: who owns the gateway redaction, where it is verified, and the browser-history exposure neither mitigation touches |

Also fixed, found while in the file: **OQ-4 had been left tautological** by Run 4's
audit-stream rename — it asked whether to use "the `audit_events` of Epic 2 or the
`audit_events` of US-3.7", having originally named two different tables. It now asks which
`severity` a refused directory read carries, since US-3.7 defines the scale and does not
list this case.

Coverage for this spec moved from 4 Covered / 2 Partial to **5 / 1**; the remaining Partial
is UL-AC6's undefined sortable field set, which is review 01 F-6 and was not in scope here.
Backlog-wide: 124 Covered, 52 Partial.

Verification: `check-specs.pl` 29/29 clean; `grounding-scan.pl` down from 3 candidates to 2,
both confirmed false positives (a co-tag omission already filed as US-2.2 F-3, and a
morphological variant in US-2.6).

**Review 01 stays as written.** Reviews are an append-only record of what the author was
told and when; F-1 to F-3 are marked Resolved by a re-review, not by editing the report.

---

## Run 5 — 2026-08-22 — story-spec-reviewer on all 29 specs

**Request:** run the reviewer.

29 reports in `docs/reviews/`, one per spec, sequence `-review-01`.

### Verdicts

| Verdict | Count |
|---|---|
| Fail | 1 |
| Needs Changes | 28 |
| Pass | 0 |

1 Blocker, 91 Major, 18 Minor.

### The Blocker

`US-3.1` §3.3 states "The query string carries only identifiers and filter codes, never
personal data" and cites UL-AC3. Since Run 4, UL-AC3 says the opposite: the search term
*is* carried, with log redaction and `Referrer-Policy: no-referrer`. The spec asserts the
inverse of the criterion it cites, then reports a conflict the criterion resolved. Two more
findings in the same file — a false **Partial** matrix row and a stale open question — are
the same reading surfacing twice more.

**It was found by a script, not by reading.** Run 4 made three hand passes over that file
and missed it every time. `docs/tools/grounding-scan.pl` compares the load-bearing tokens of
each tagged sentence against the criterion it cites, and reduced 29 specs to three
candidates, of which this was one.

### Why every spec is at least Needs Changes

Two Majors recur in all 29 and neither is about the documents:

1. **Provenance.** No story or criterion in this backlog has been approved by anyone. The
   skill treats unapproved provenance as Major at minimum, and it is the correct call —
   downstream nobody can distinguish invented criteria from agreed ones.
2. **Reviewer independence.** The same assistant wrote the backlog, the specs, the Run 4
   corrections and these reviews. The reviewer skill's premise — that an author cannot see
   their own inventions — is not satisfied. `CP-101` review 01 recorded the same condition
   in Run 2 and it is still open there.

Strip those two and the picture is: 1 Fail, 6 clean, 22 carrying one to two substantive
findings each.

### Mechanical checks

**The bundled `trace_check` scripts could not run this backlog.** Their AC pattern is
`AC-\d+[a-z]?`, which does not match the per-story prefixes (`LI-AC1`, `TQ-AC3`). Against
`US-2.1-login.md` the PowerShell twin reported "Declared ACs (0): none found" and then
printed three `OK` lines over an empty set, plus 22 "untagged normative paragraph" warnings
whose tags are visible in its own output. That is precisely the failure the skill warns
about: a mechanical step that silently cannot run is worse than no step.

Substitutes, all ID-scheme aware:

- `docs/tools/check-specs.pl` — AC set, **verbatim** text against the source, coverage,
  dangling tags, matrix completeness. Goes further than the bundled script, which never
  compares against the source at all.
- `docs/tools/review-evidence.pl` — untagged normative paragraphs. Result: 0 across 29.
- `docs/tools/grounding-scan.pl` — token-level support for tagged statements. Found the
  Blocker.

A narrower D4 check — matrix rows marked **Covered** whose §3 body still admits an undefined
assertion — returned nothing, which is the most consequential clean result in the set.

### Recurring findings worth acting on before the next spec pass

- **Business days are undefined**, so three of four SLA rows yield no computable deadline.
  Blocks `US-4.1` TC-AC1 and `US-4.3` TQ-AC1 for every priority except `CRITICAL`.
- **Consumed-but-unowned interfaces:** canned response templates (`US-4.5` TA-AC6), the
  notification delivery mode (`US-5.7` NG-AC4/5), role and permission listings (`US-3.6`
  MR-AC7), the provider bounce webhook (`US-5.5` NE-AC4), and the whole announcement
  interface (`US-5.8`).
- **`US-2.6` PN-AC4** states three password rules and two of them — the common-password list
  and what "containing" means — cannot be turned into a test.
- **`US-2.4` RT-AC5** depends on retired token rows surviving a cleanup job whose retention
  nothing states. The reuse detection can be switched off by configuration with no test
  failing.
- **`US-5.8`** has no gate on who may set the announcement class that bypasses every
  customer preference.

### Not done

No spec was edited. The reviewer produces findings, never patches; applying them is
`story-spec-writer`'s job and should land as a visible, separate change.

---

## Run 4 — 2026-08-22 — backlog corrections from the Run 3 findings

**Request:** fix the backlog against the specification findings.

Corrections were limited to defects — contradictions between criteria, values bounded by
example but never set, and terms used without definition. Genuine product and architecture
decisions were **not** answered; they were sharpened instead, and several are still the
reason a criterion is Partial.

### What changed in the backlog

| Finding | Fix |
|---|---|
| Three names for the audit stream | Unified to `audit_events` across 16 stories. US-3.7 now declares it the single stream for the whole system. |
| `SECURITY` severity undefined | US-3.7 defines `severity` ∈ {`INFO`, `NOTICE`, `SECURITY`} and names which criteria set it. |
| SLA thresholds consumed by four stories, set by none | A threshold table added to `docs/backlog/README.md`, marked as needing sign-off. US-4.1 and US-4.3 now reference it. |
| LO-AC2 vs LO-AC3 — repeated logout | LO-AC2 scoped to the refresh endpoint and exempts logout; LO-AC3 states no reuse event is written. |
| RT-AC5 stated two revocation scopes | Now explicitly every family for the customer, with the reason given. |
| UL-AC3 forbade the search term UL-AC2 requires | The term is carried in the URL; the gateway redacts `q` from logs and the page sets `Referrer-Policy: no-referrer`. |
| NU-AC1 vs NU-AC4 — disclosure | A verifying token may name its event type; a non-verifying one names nothing. `GET` renders, `POST` acts. |
| US-2.1 remember-me contradiction | Assumption 7 rewritten: the two values in LI-AC8 are exhaustive. |
| Reopen window bounded by 5 and 20 days | Fixed at 14 days, inclusive, in TO-AC1 and TO-AC2, with the error text stated. |
| Grouping and burst rules given by one example each | General rules stated: 3 events on one target within 60 minutes (US-5.7), more than 3 in 10 seconds (US-5.2). |
| Locale and time zone read but never created | US-3.3 creates both fields with defaults, and UU-AC7 covers them. |
| Missing interfaces | Attachment upload named in TC-AC2, rating endpoint in TS-AC2, `relatedTicketId` in the US-4.1 contract, message endpoint in TA-AC1. |
| Internal notes could reorder the customer's list | TA-AC3 now pins `tickets.updated_at` and removes sequence numbers entirely. |
| Phone format Ukraine-only | E.164, at most 15 digits. |
| Field maxima and accepted formats missing | Stated in TC-AC3 and TC-AC4. |

Four criteria were added: `LI-AC9` (unverified email), `LO-AC6` (CSRF on logout), `UU-AC7`
(locale and time zone), `TC-AC7` (attachment scan outcomes). 172 criteria became 176.

### What was deliberately not decided

The unapproved runtime dependencies, the `notification/` module, audit retention, GDPR
erasure scope, and the SLA values themselves. "Business day" is still undefined, which is
why only the `CRITICAL` SLA row is currently testable.

### Specification re-sync

Changing criteria invalidated 21 specs — `check-specs.pl` caught every one. Section 2 was
rebuilt mechanically by `docs/tools/sync-spec-criteria.pl`; sections 3 to 8 were revised by
hand where the fix changed the analysis. All 21 are now revision 2.

`docs/tools/recount-coverage.pl` recomputes each spec's coverage line from its own matrix,
so the summary cannot drift from the rows.

**Coverage moved from 99 Covered / 73 Partial to 123 / 53** across 176 criteria. Final
check: 29 specs, 0 problems.

---

## Run 3 — 2026-08-22 — story-spec-writer on the 29-story backlog

**Request:** generate specifications for every story in `docs/backlog/` into
`docs/specifications/`.

### Source material

| Step | Result |
|------|--------|
| Story source | `docs/backlog/US-*.md`, 29 stories across Epics 2–5, authored earlier in the same session |
| Acceptance criteria | Present on every story, already carrying stable per-story IDs (`LI-AC1`, `LO-AC1`, …) |
| Provenance | **Authored by the assistant.** No product owner, customer or regulator has approved any story or criterion. Every spec carries the same warning as `CP-101`. |

AC identifiers were preserved exactly, per skill step 2. No renumbering to `AC-1`.

### Output

29 files, `docs/specifications/US-<epic>.<story>-<slug>.md`, matching the `CP-101`
naming already in use.

### Coverage

| Status | Count |
|--------|-------|
| Covered | 99 |
| Partial | 73 |
| Not covered | 0 |

172 criteria total. 166 open questions and 139 assumptions recorded.

### Verification (skill step 6)

Written as a script rather than a one-off grep, because 29 files cannot be checked by
eye: `docs/tools/check-specs.pl`. It asserts, per spec, that the AC set matches the
backlog source exactly, that each criterion's Gherkin is reproduced **verbatim**, that
every AC is referenced outside section 2, that no tag dangles, and that every AC has
exactly one traceability-matrix row. Final run: 29 checked, 0 problems.

The check earned its keep immediately. Three real defects were found in specs already
written and were fixed:

- `US-3.8` — the first draft reproduced 3 criteria instead of 6, with **invented text**.
  Rewritten from the source.
- `US-3.4` UD-AC1 — one clause paraphrased rather than quoted.
- `US-3.7` AU-AC4 — the clause `with type ".../errors/insufficient-scope"` was dropped,
  and an open question had then been raised about the supposedly missing error type. The
  criterion was restored and the invented finding removed.

All three came from writing criteria from memory instead of reading the file. Sources are
now read immediately before each spec is written.

### Cross-cutting findings

Recorded here because no single spec owns them:

1. **The audit stream has three names.** Epic 2 writes `auth_audit_log`, Epic 3 writes
   `admin_audit_log`, US-3.7 reads `audit-events`. Whether these are one stream is
   unresolved (`US-3.7` OQ-1) and blocks the audit reader against any known schema.
2. **SLA thresholds are consumed by four stories and set by none** — `US-4.1` TC-AC1
   starts the timer, `US-4.3` TQ-AC1 orders the queue by it, `US-4.6` pauses it,
   `US-4.7` restarts it.
3. **Direct contradictions between criteria**, not merely gaps: `US-2.2` OQ-3
   (LO-AC2 vs LO-AC3 on the repeated-logout path), `US-5.6` OQ-2 (NU-AC1 must name the
   event type, NU-AC4 must reveal nothing), `US-2.4` OQ-2 (RT-AC5 states two different
   revocation scopes in one criterion), `US-3.1` OQ-1 (UL-AC3 forbids personal data in the
   query string that UL-AC2 requires to be shareable).
4. **`US-4.7` OQ-1** — the reopen window is bounded by example (5 days and 20 days) but
   never set, and TO-AC2 requires an error message that states it.
5. **Profile fields that no story creates** — `US-5.5` NE-AC2 and `US-5.7` NG-AC4 both
   read a language and a time zone from the customer profile.
6. **An undefined "security event" category** appears in `US-2.4` RT-AC5, `US-3.2` UC-AC4
   and `US-3.5` RA-AC4.

### Escalations carried forward

Three specs restate `AGENTS.md` §7 escalations as blocking open questions: the mail
provider and queue (`US-5.5` OQ-1, which blocks eight stories), event fan-out (`US-5.2`
OQ-1), and the missing `notification/` module (`US-5.1` OQ-1, which blocks all of Epic 5).
The full dependency list is in `docs/backlog/README.md`.

### Deliberately not done

- Did **not** promote the stories' Non-Functional sections into normative spec text.
  Performance budgets, hashing choices and storage rules appear as assumptions, because
  no acceptance criterion asserts them. This is the single largest category of material
  left out of section 3.
- Did **not** resolve any open question. Where a criterion was ambiguous, the ambiguity
  was recorded rather than designed away.
- Did **not** edit the backlog. The 73 Partial rows are findings against it; fixing them
  is a separate pass that should be followed by a spec revision.

---

## Run 2 — 2026-08-17 — story-spec-reviewer on CP-101

**Verdict: Needs Changes** (0 Blocker, 2 Major, 6 Minor). Report:
`docs/reviews/CP-101-customer-registration-review-01.md`

- **D1 AC fidelity was not checked** — no source ticket exists, so `source_verified: false`.
  This is the review's largest blind spot and the basis of F-1.
- **F-2** (Major) — §3.1 requires account creation when the four fields are *present*,
  where AC-1 conditions on *valid*; contradicts §4 row 1 and silently answers OQ-4.
- 5 of 6 ACs yield a concrete test condition; AC-6 does not, consistent with its
  `Partial` matrix row.
- Mechanical checks (`trace_check.ps1`) clean: no uncovered ACs, no dangling tags,
  matrix complete and consistent.
- **Reviewer independence compromised** — the same assistant authored the spec earlier
  in the session, which is the one condition the reviewer skill assumes is met. Recorded
  in the report verdict; the grounding pass should be redone by another reader.

---

# story-spec-writer — run log

## Run 1 — 2026-08-17

**Request:** run the `story-spec-writer` skill; write output into `docs/` under `C:\Users\atop7\Code`.

### Source material

| Step | Result |
|------|--------|
| Searched working directory for a story, ticket, or acceptance criteria | None found. Repository contained only `AGENTS.md` and `.claude/skills/**`. |
| Asked the user for a story + ACs | User instead instructed: *"придумай історію на основі AGENTS.md"* (invent a story from `AGENTS.md`). |
| Story origin | **Authored by the assistant** from the `customer-portal` technical contract in `AGENTS.md`. Not supplied by, or approved by, any stakeholder. |

The skill normally halts when a story arrives without acceptance criteria, because
writing the spec anyway means inventing the requirement set. The user explicitly
directed the invention, so the spec was produced with the synthetic provenance stated
in the front matter, the title callout, and the revision history. **Section 2 needs
stakeholder sign-off before the document carries any authority.**

### Output

- Spec: `docs/specifications/CP-101-customer-registration.md`
- Story chosen: customer self-service registration — the most concretely described flow
  in `AGENTS.md` (§1.1 core domain, §4.1 `RegisterCustomerRequest`, §4.2 `RegistrationResult`).
- Story ID `CP-101` assigned by the assistant; no ticket system was consulted.

### Acceptance criteria drafted

AC-1 (201 + `Location`), AC-2 (409 on duplicate email), AC-3 (password 12–128, 400 with
violated rules), AC-4 (case-insensitive email uniqueness), AC-5 (no plaintext password
storage), AC-6 (`USER` role assigned).

### Verification (skill step 6)

```
grep -o "AC-[0-9]\+[a-z]*" docs/specifications/CP-101-customer-registration.md | sort | uniq -c
     15 AC-1
     10 AC-2
     10 AC-3
      4 AC-4
      6 AC-5
      6 AC-6
```

- All 6 ACs appear in the body, not only in section 2 — no AC left uncovered.
- No tag references a non-existent AC — no dangling references.

### Coverage

| Status | Count | ACs |
|--------|-------|-----|
| Covered | 5 | AC-1, AC-2, AC-3, AC-4, AC-5 |
| Partial | 1 | AC-6 |
| Not covered | 0 | — |

### Open questions raised

OQ-1 password hashing form · OQ-2 password rules beyond length · OQ-3 how role
assignment is observable · OQ-4 response to malformed/blank input · OQ-5 precedence of
409 vs 400 · OQ-6 length limits on email and name fields.

### Assumptions recorded

A-1 `AGENTS.md` standards govern implementation shape · A-2 endpoint is unauthenticated ·
A-3 `Location` points into the `/api/v1/customers` URI space.

### Deliberately not done

- Did **not** promote `AGENTS.md` rules (Java records, UUIDv7 keys, Flyway, MapStruct,
  JaCoCo thresholds) into normative spec statements. They are repository standards, not
  requirements of this story; they appear only as assumption A-1.
- Did **not** resolve any open question. Retry policies, hashing choices, and validation
  responses were left as decisions for a human.
