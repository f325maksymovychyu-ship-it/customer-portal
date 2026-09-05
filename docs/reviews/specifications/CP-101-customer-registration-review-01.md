---
spec: docs/specifications/CP-101-customer-registration.md
spec_revision: 1
story_id: CP-101
source: not obtained — no ticket exists; the ACs were authored in conversation
source_verified: false
verdict: Needs Changes
reviewed_on: 2026-08-17
review_seq: 01
---

# Review 01 — Customer self-service registration

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 2 |
| Minor | 6 |

No Blockers: every normative statement in the body traces to a criterion in the spec's
own AC section, the traceability matrix is complete and consistent with the body, and
the one AC that cannot be verified as written (AC-6) is honestly marked `Partial`
rather than claimed as covered. The six open questions are properly surfaced and are
not counted against the spec — that is the behavior this format is asking for.

Two Majors drive the verdict. **F-1**: there is no source to check the AC section
against. The criteria were authored in conversation rather than agreed by a
stakeholder, so dimension D1 (AC fidelity) could not be run at all — this review
verifies that the spec is internally faithful to its stated criteria, and says nothing
about whether those criteria represent anything a customer or product owner asked for.
**F-2**: §3.1 mandates account creation when the four fields are merely *present*,
where AC-1 conditions on *valid* — which contradicts §4 and pre-empts OQ-4.

**Reviewer independence is compromised on this review.** The same assistant authored
the specification earlier in the same session. The premise of this skill is a second
reader who did not write the text; that condition is not met here, and self-review is
weakest at exactly the thing it is meant to catch — an invented requirement still looks
reasonable to the person who invented it. Treat the grounding pass (§4) as the least
reliable part of this report and worth redoing by someone else.

## 2. Findings

### F-1 · Major · D1 AC fidelity

**Location:** front matter `source:`, and the provenance callout above §1

**Evidence:**
> Neither this story nor its acceptance criteria came from a product owner, ticket, or
> customer. They were drafted by an assistant at the user's request, inferred from the
> `customer-portal` technical contract in `AGENTS.md`.

**Problem:** No external source exists, so the AC section cannot be compared against
anything. D1 was not run. Every "Covered" in the matrix means *covered relative to
criteria the document itself invented*.

**Why it matters:** This is the invented-requirement failure moved up one level. A
reader who trusts the matrix inherits six requirements that no stakeholder has ever
seen, and once these are estimated and scheduled they become indistinguishable from
agreed scope. The spec's disclosure is what keeps this from being a Blocker — it is
stated in three places and cannot be missed — but disclosure does not make the criteria
agreed.

**Resolves when:** a product owner reviews section 2 and either confirms the criteria or
replaces them, and `source:` points at that record.

---

### F-2 · Major · D3 Grounding / D5 Testability

**Location:** §3.1, paragraph 2

**Evidence:**
> When all four are present and the email is not already registered, the system creates
> a customer account and responds `201 Created`. `[AC-1]`

**Problem:** AC-1 conditions on "a **valid** email, password, first name and last name".
The spec restates this as the four values being **present**. Presence and validity are
different tests, and the substitution is normative: read literally, this sentence
requires the system to create an account for `email = "x"`. It also contradicts §4 row 1,
which records the email format as "valid email address", and it silently answers OQ-4,
which states that the response to a malformed email is undefined.

Secondary issue in the same sentence: the "not already registered" condition comes from
AC-2, but the sentence is tagged `[AC-1]` only.

**Why it matters:** A developer implementing §3.1 as written builds a presence check; a
tester reading §4 writes a format test. One of them is wrong and neither can tell from
the document. Worse, the sentence closes a gap the spec explicitly claims is still open,
so OQ-4 will never be asked.

**Resolves when:** §3.1 states the same condition AC-1 states, and the response to
invalid-but-present input is either left to OQ-4 or answered by a new AC — not both.

---

### F-3 · Minor · D3 Grounding

**Location:** §3.1, paragraph 4

**Evidence:**
> Registration is a single request; the account exists once the `201` is returned. `[AC-1]`

**Problem:** The second clause restates AC-1. The first — "registration is a single
request" — does not appear in any AC. It forecloses an activation or confirmation step,
which is a design decision, not a restatement. §5 already records email verification as
out of scope, which is the correct place for that boundary; asserting it as normative
text in §3.1 makes it a requirement instead.

**Why it matters:** Small, but it is the exact pattern that produces invented
requirements: a boundary that belongs in Out of Scope migrates into the body and starts
looking agreed.

**Resolves when:** the clause is grounded in an AC, or the boundary lives only in §5.

---

### F-4 · Minor · D5 Missing edge case

**Location:** §3.3 (AC-5 scope)

**Problem:** AC-5 constrains the *stored customer record* only. Nothing in the ACs or
the spec addresses the password appearing in application logs, error response bodies,
request traces, or database backups.

**Why it matters:** The stored record is usually the one place a team does get right.
Plaintext passwords in logs are the common real-world leak, and no AC reaches it, so no
test will look for it.

**Resolves when:** an AC extends the constraint beyond the stored record, or an open
question records that the wider scope was considered and deferred.

---

### F-5 · Minor · D5 Missing edge case

**Location:** §3.2 (AC-2, AC-4)

**Problem:** The spec does not address two registrations for the same email submitted
concurrently. AC-2's assertion — "no second account is created" — is a uniqueness
guarantee, and a race between two in-flight requests is the standard way such a
guarantee fails.

**Why it matters:** A sequential test of AC-2 passes against an implementation that
checks-then-inserts without a unique constraint. The AC reads as verified while the
guarantee it asserts does not hold.

**Resolves when:** the concurrent case is either covered by an AC or recorded as an open
question.

---

### F-6 · Minor · D5 Ambiguity

**Location:** §3.2, paragraph 1 (AC-2)

**Problem:** "Already registered" is undefined when the prior account is not active —
for example an account in a `DELETED` or deactivated state. `AGENTS.md` §3.2 anticipates
exactly such state transitions, so this is a state the system will have.

**Why it matters:** Whether a departed customer can re-register with their old address
is a product decision with a visible customer consequence, and the spec currently gives
a tester no basis to assert either behavior.

**Resolves when:** an AC or open question states whether non-active accounts hold their
email.

---

### F-7 · Minor · D5 Ambiguity

**Location:** §3.2, paragraph 4 (AC-4)

**Problem:** AC-4 mandates case-insensitive comparison. The spec adopts it as
"case-insensitive on the whole address" but says nothing about other normalization that
determines equality — surrounding whitespace, Unicode forms, or `+`-suffixed addresses.

Worth flagging separately: case-insensitivity across the *whole* address is a deliberate
deviation from RFC 5321, which treats the local part as case-sensitive. AC-4's own
example varies case in both the local part and the domain, so the spec is faithful to
the criterion — but the criterion embeds a decision that a reviewer should see rather
than inherit.

**Why it matters:** Equality rules that are half-specified produce duplicate accounts
that both appear to satisfy AC-2.

**Resolves when:** an AC or open question defines the normalization applied before
comparison.

---

### F-8 · Minor · D4 Traceability

**Location:** §4, rows 6–8

**Problem:** Three rows record the constraint as "representation not specified",
"member values not specified", and "storage form not specified". These are honest
entries and correctly formatted, but each corresponds to an open question (OQ-3, OQ-4
territory, OQ-1) that the row does not reference, while §8's matrix rows do carry those
references.

**Why it matters:** Minor and purely navigational — a reader working from §4 alone
cannot see that the gap is already tracked, and may re-raise it.

**Resolves when:** the rows cross-reference their open questions, or the convention is
documented as matrix-only.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| AC-1 | §3.1, §4 | POST a request with a well-formed unused email, a 20-character password, and both names → expect `201`, and a `Location` header resolving to the new customer | Yes — see F-2 |
| AC-2 | §3.2, §4 | Register `a@b.com`, then POST the same email again → expect `409`, an RFC 9457 body, and exactly one account for that email | Yes — see F-5 |
| AC-3 | §3.3, §4 | POST an 11-character password → expect `400` naming the length rule; repeat with 129 characters → expect `400`. Boundaries 12 and 128 must be accepted, since AC-3 rejects only *shorter than* 12 and *longer than* 128 | Yes — see F-2 for interaction with invalid input |
| AC-4 | §3.2, §4 | Register `user@example.com`, then POST `User@Example.com` → expect the AC-2 outcome (`409`), not a second account | Yes — see F-7 |
| AC-5 | §3.3, §4 | Register with a known password, then read the persisted customer row → the password string must not appear in any column | Yes — see F-4 |
| AC-6 | §3.4, §4 | Not derivable: the spec states the account "holds the role `USER`" but never says where that is observable — no persisted field, token claim, or response body is named, so there is nothing to assert against | No — see OQ-3 in the spec |

## 4. Grounding Register

Normative statements whose grounding was not self-evident, plus every statement judged
ungrounded. Statements that plainly restate their AC are omitted.

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "When all four are **present** … the system creates a customer account" | §3.1 | `[AC-1]` | No — AC-1 conditions on *valid*, not present. See F-2 |
| "and the email is not already registered" | §3.1 | `[AC-1]` | Substantively yes, but from AC-2, not AC-1. Mis-tagged. See F-2 |
| "Registration is a single request" | §3.1 | `[AC-1]` | No — no AC addresses whether a second step exists. See F-3 |
| "leaves the existing account untouched" | §3.2 | `[AC-2]` | Partly — AC-2 states only that no second account is created. Non-mutation of the existing account is an inference, though a narrow and safe one |
| "No **representation** of the customer record persists the password as plaintext" | §3.3 | `[AC-5]` | Yes — slightly broader than AC-5's "stored customer record", and the broadening is in the safe direction. See F-4 for what it still does not reach |
| "The uniqueness comparison is case-insensitive on the whole address" | §3.2 | `[AC-4]` | Yes — AC-4's example varies case in both local part and domain |
| "An account created by this flow holds the role `USER`" | §3.4 | `[AC-6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Invalid-but-present input** — §3.1 requires creation, §4 requires a valid email, OQ-4 says the behavior is undefined. Three answers, one question. `[AC-1]`
- **Concurrent duplicate registration** — no basis to assert AC-2's uniqueness guarantee under simultaneous submission. `[AC-2]`
- **Password outside the stored record** — logs, error bodies, traces, backups all unaddressed. `[AC-5]`
- **Non-active prior accounts** — whether a `DELETED` account still holds its email. `[AC-2]`
- **Email normalization beyond case** — whitespace, Unicode form, `+` suffixes. `[AC-4]`
- **Role observability** — AC-6 asserts an assignment with no observable surface, which is why its matrix row is `Partial`. `[AC-6]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | **No** | No source exists — see F-1. This is the review's largest blind spot. |
| D2 Coverage | Yes | All 6 ACs referenced in §3 or §4. Clean. |
| D3 Grounding | Yes | See §4. Reliability reduced by self-review — see the verdict note. |
| D4 Traceability integrity | Yes | Matrix complete, statuses consistent with the body, no dangling tags. |
| D5 Testability and edge cases | Yes | 5 of 6 ACs yield a concrete test condition; AC-6 does not. |
| D6 Gap handling | Yes | A-1/A-2/A-3 stay out of the normative body; OQs are phrased as questions, not proposals. One gap silently closed — F-2. |

Mechanical checks: `scripts/trace_check.ps1` run against revision 1 — declared ACs
AC-1…AC-6, no uncovered ACs, no dangling tags, matrix complete and consistent, no
untagged normative paragraphs. The Python twin was not run: no Python interpreter is
installed on this machine.

## 7. Carried Forward

First review of this spec — nothing carried forward.
