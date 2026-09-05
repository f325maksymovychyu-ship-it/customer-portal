---
spec: docs/specifications/US-4.1-create-ticket.md
spec_revision: 2
story_id: US-4.1
source: docs/backlog/US-4.1-create-ticket.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Create Ticket

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 3 |
| Minor | 1 |

No Blockers. Seven criteria, verbatim, all covered. The revision-2 correction closed three
of revision 1's Partial rows — the upload endpoint is now named, the accepted formats
enumerated, field maxima stated — and added TC-AC7 for the scan outcomes that were
previously undefined. What remains is a dependency problem rather than a specification one.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. TC-AC4 and TC-AC7
encode a malware-handling policy.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** §3.1; OQ-2

**Evidence:**
> The SLA timer starts against the thresholds for the chosen priority in
> `docs/backlog/README.md`, and the resulting first-response and resolution deadlines are
> stored on the ticket. `[TC-AC1]`

**Problem:** The referenced table now exists, which is an improvement, but three of its four
rows are expressed in business days and no calendar, working hours or holiday set is defined
anywhere. Only the `CRITICAL` row, stated in hours, yields a computable deadline.

**Why it matters:** TC-AC1 asserts that deadlines are stored. For `HIGH`, `NORMAL` and `LOW`
a tester cannot compute the expected value, so the assertion covers one priority in four.
US-4.3 TQ-AC1 orders the whole queue by these deadlines.

**Resolves when:** a business calendar is defined, or the thresholds are restated in
elapsed hours.

---

### F-4 · Minor · D5 Testability

**Location:** §3.6; TC-AC7; OQ-5

**Evidence:**
> And the client retries automatically for up to 30 seconds before surfacing the error

**Problem:** The 30-second retry window is stated without any expected scan duration to
justify it. A large ZIP may routinely exceed it, in which case every such upload surfaces an
error the criterion treats as exceptional.

**Why it matters:** Low impact on correctness. It matters because the number will be
implemented literally and nobody has checked it against the scanner's actual behaviour.

**Resolves when:** the window is derived from a measured scan time, or the criterion
expresses the retry as a policy rather than a fixed figure.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| TC-AC1 | §3.1, §4 | Signed-in customer posts valid fields → `201` with `Location` and a `#`-prefixed reference, status `NEW`, one queued mail, ticket at the head of their list. Deadline assertion holds only for `CRITICAL` — F-3 | Partly |
| TC-AC2 | §3.2, §4 | Post three PNGs to `/api/v1/support/attachments` → `201` each with an id and scan status; create the ticket referencing them → attachments visible; an unscanned id is refused | Yes |
| TC-AC3 | §3.3, §4 | Subject of 3 and of 121 characters, description of 19 and of 5 001 → `400` each, detail naming the bound; client keeps submit disabled | Yes |
| TC-AC4 | §3.4, §4 | 11 MB file → `413`; a renamed executable → `415` whose detail lists exactly the six formats; a sixth file → `400` with the exact detail | Yes |
| TC-AC5 | §3.5, §4 | Eleven tickets in an hour with an injected `Clock` → `429` with `Retry-After` | Yes |
| TC-AC6 | §3.7 | Client unit test: stub a network failure → draft retained and restored with a notice; expire the session mid-form → sign-in then return with the draft intact | Yes |
| TC-AC7 | §3.6, §4 | Stub the scanner pending → `409` type `attachment-scan-pending`; stub it malicious → attachment deleted, id permanently unusable, per-file message, ticket still submittable | Yes — see F-4 |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "An upload to `POST /api/v1/support/attachments` responds `201` with an attachment identifier and a scan status per file…" | §3.2 | `[TC-AC2]` | Yes — named in TC-AC2 since the correction |
| "A subject longer than 120 characters, or a description longer than 5 000, is rejected…" | §3.3 | `[TC-AC3]` | Yes — added to TC-AC3 in the correction |
| "…the detail lists exactly PNG, JPEG, PDF, TXT, LOG and ZIP." | §3.4 | `[TC-AC4]` | Yes |
| "Where the scan reports a file as malicious, the attachment is deleted and its identifier becomes permanently unusable." | §3.6 | `[TC-AC7]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Business days undefined** — see F-3. `[TC-AC1]`
- **Customer-set `CRITICAL`** — Decision 7 lets the customer choose any priority, which
  makes the tightest SLA self-service. Recorded as OQ-3, and it is a product decision.
  `[TC-AC1]`
- **Scan window versus scanner reality** — see F-4. `[TC-AC7]`
- **Attachment referenced by a second ticket** — nothing states whether an attachment id can
  be linked twice, which would let a customer attach another customer's file if ids were
  guessable. `[TC-AC2]`
- **`relatedTicketId` has no validation criterion** — the field was added to the contract for
  US-4.7 TO-AC2. No criterion here says what happens when it names a ticket the caller does
  not own. `[TC-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Seven criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | OQ-1 correctly flags the unapproved storage and scanning dependencies |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`TC-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
