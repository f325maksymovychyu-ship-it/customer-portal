---
spec: docs/specifications/US-3.8-export-audit-information.md
spec_revision: 1
story_id: US-3.8
source: docs/backlog/US-3.8-export-audit-information.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Export Audit Information

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Six criteria, verbatim, all covered. This spec has history worth recording:
its first draft reproduced three criteria instead of six, **with invented text**, and was
rewritten from the source before any review. The mechanical check that caught it is the
reason this review can treat the current AC section as trustworthy.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. An audit extract is
among the most sensitive artefacts the system produces.

**Resolves when:** a compliance owner signs off on the criteria.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant. This spec is the clearest evidence
in the set for why that matters: the author transcribed three criteria wrongly and invented
their text, and did not notice while writing the surrounding analysis.

**Why it matters:** The defect was caught by `check-specs.pl` comparing text against the
source, not by reading. Nothing in the hand-written review process would have found it.

**Resolves when:** an independent reader repeats the D1 and D3 passes.

---

### F-3 · Major · D5 Testability

**Location:** §3.1; A-1; OQ-1

**Evidence:**
> | A-1 | The completion link is delivered by email. | AX-AC1 says "we will send a link"
> without naming a channel. |

**Problem:** AX-AC1 promises delivery of the download link and names no channel. The
assumption picks email, which couples this story to the unapproved mail dependency in
US-5.5 and to the notification epic that is blocked on a module decision.

**Why it matters:** The criterion's terminal step cannot be tested. Worse, the choice
decides whether an audit extract's location travels through an external mail provider,
which is a security decision hiding in an assumption.

**Resolves when:** AX-AC1 names the channel, or the flow becomes poll-only and the promise
is dropped.

---

### F-4 · Minor · D5 Testability

**Location:** §3.4; A-2; OQ-3

**Evidence:**
> Where more than 24 hours have passed since completion, using the download link responds
> `410`… and the stored file has been deleted from object storage. `[AX-AC5]`

**Problem:** AX-AC5 asserts the file "has been deleted" at the 24-hour mark. A storage
lifecycle rule deletes eventually, not at a moment. Only an active deletion is assertable
at exactly 24 hours.

**Why it matters:** Low impact on correctness, real impact on what a test can claim. A test
written against a lifecycle rule passes by waiting, which is not the property the criterion
states.

**Resolves when:** the criterion says whether expiry deletes actively or marks the link
invalid and lets storage reclaim later.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| AX-AC1 | §3.1, §3.2, §4 | Caller with `audit:export` posts a range and `CSV` → `202` with `Location`; drive the job to completion with Awaitility → `READY` with a link valid 24 h, file rows equal to the filter snapshot, one `AUDIT_EXPORTED` event. The delivery step is not derivable — F-3 | Partly |
| AX-AC2 | §3.3, §4 | Seed 50 001 matching events; request the export → `400` type `export-too-large` with the exact detail, no job row | Yes |
| AX-AC3 | §3.3, §4 | `to` before `from` → `400` type `validation-failed` naming the field; `format: "XML"` → same; no job row | Yes |
| AX-AC4 | §3.4, §4 | Second auditor requests the first's job resource and download link → `404` both times, no filter or size disclosed | Yes |
| AX-AC5 | §3.4, §4 | Advance an injected `Clock` past 24 h → link returns `410` type `export-expired`; job row still present. File deletion timing — F-4 | Partly |
| AX-AC6 | §3.5, §4 | Stub the export port to fail → job resource returns `FAILED` with a reason, client offers retry, no partial file downloadable | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "The file contains exactly the rows that matched the filters at the moment the job started." | §3.2 | `[AX-AC1]` | Yes — AX-AC1 clause 7 |
| "The job record remains after the file is deleted, so the trail of who exported what survives the artefact." | §3.4 | `[AX-AC5]` | Yes — AX-AC5 final clause |
| "No partially written file is ever exposed for download." | §3.5 | `[AX-AC6]` | Yes |
| "…revealing nothing about the export's existence, filters or size." | §3.4 | `[AX-AC4]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Delivery channel unnamed** — see F-3. `[AX-AC1]`
- **Deletion timing at expiry** — see F-4. `[AX-AC5]`
- **Intermediate job states** — AX-AC1 and AX-AC6 name `READY` and `FAILED`. The resource is
  polled, so something is returned in between, and no criterion says what. Recorded as
  OQ-2. `[AX-AC1]`
- **Snapshot divergence is invisible** — AX-AC1 fixes the rows at job start. Re-running the
  same filters later yields a different count, and nothing tells the auditor why the file
  and the screen disagree. Recorded as OQ-4. `[AX-AC1]`
- **No date-range cap** — AX-AC2 caps rows and AX-AC3 validates ordering, but nothing caps
  the span as US-3.7 AU-AC5 does at 90 days. A three-year range under 50 000 rows is
  currently legal. Recorded as OQ-6. `[AX-AC2]`
- **`404` here, `404` elsewhere, but the story's own text said `403`** — AX-AC4 uses `404`,
  matching US-4.2 TL-AC3 and US-2.3 SM-AC4. The spec's §4 row 11 notes AX-AC4 carries no
  error type while the other refusals do. `[AX-AC4]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim **after** an earlier transcription defect was found and corrected — see F-2 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found in the current text |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`AX-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
