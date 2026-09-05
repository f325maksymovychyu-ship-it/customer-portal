---
spec: docs/specifications/US-5.4-notification-preferences.md
spec_revision: 1
story_id: US-5.4
source: docs/backlog/US-5.4-notification-preferences.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Notification Preferences

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. F-4 is the substantive specification gap:
two criteria describe a `PUT` and no criterion gives its request shape, while NP-AC6 asserts
that "no part of the request is applied" — a statement about a payload structure that is
never described.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. NP-AC3 and NP-AC5 encode
consent rules — which notifications a customer may switch off, and whether a new type
arrives opted in or out.

**Why it matters:** Opt-in versus opt-out for a class of message is a compliance question,
not a design preference.

**Resolves when:** the class model and its defaults carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** Blocked callout

**Problem:** The epic-wide module question in US-5.1 F-3 applies unchanged.

**Resolves when:** an architect rules on the module.

---

### F-4 · Major · D2 Coverage

**Location:** §3.3 and §3.5; §4 row 2; OQ-1

**Evidence:**
> | 2 | `PUT` on the same path | Method named by NP-AC3 and NP-AC6; **the request shape is
> not specified** |

**Problem:** NP-AC3 and NP-AC6 both exercise a `PUT` and neither describes what it carries.
NP-AC6's "no part of the request is applied" implies a request holding several changes;
nothing says whether the payload replaces the whole matrix or patches individual triples.

**Why it matters:** The two readings differ in a way that matters. A whole-matrix replace
means a client with a stale view silently reverts changes made elsewhere; a patch does not.
Neither the criterion nor the contract chooses.

**Resolves when:** a criterion states the request shape and whether it replaces or patches.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NP-AC1 | §3.1, §4 | `GET /api/v1/notifications/preferences` → event types grouped by class, each with an explanation and in-app and email state; security-class types flagged locked. The class set is not enumerated — §5 | Partly |
| NP-AC2 | §3.2, §4 | Disable email for "ticket reply"; have an agent reply → in-app row created, mail port never invoked; assert the change is honoured within 60 s of saving | Yes |
| NP-AC3 | §3.3, §4 | Settings render with the security type's switches disabled and the exact explanation; `PUT` attempting to disable it → `422` type `preference-locked`, no row written. Request shape unknown — F-4 | Partly |
| NP-AC4 | §3.6, §4 | Client unit test: stub a failed save → the toggled switch reverts, the exact message appears, no other switch changes | Yes |
| NP-AC5 | §3.4, §4 | Deploy a new transactional type → enabled by default, flagged new for 14 days; deploy a new informational type → disabled by default; assert no migration ran against existing rows | Yes |
| NP-AC6 | §3.5, §4 | `PUT` naming an undeclared event type or channel → `400` type `unknown-event-type`, nothing applied. "No part" is untestable without F-4 | Partly |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…types belonging to the security class are marked locked." | §3.1 | `[NP-AC1]` | Yes |
| "A saved change is in force within 60 seconds." | §3.2 | `[NP-AC2]` | Yes — NP-AC2 final clause |
| "…a newly deployed informational event type is disabled by default." | §3.4 | `[NP-AC5]` | Yes — NP-AC5's `But` clause |
| "…no preference row is written." | §3.3 | `[NP-AC3]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Module placement blocks the epic** — see F-3. All criteria.
- **`PUT` request shape** — see F-4. `[NP-AC3]` `[NP-AC6]`
- **The administrative class appears in no criterion** — the class table in
  `docs/backlog/README.md` names four classes; NP-AC1, NP-AC3 and NP-AC5 name three. US-5.8
  relies on the fourth, and US-5.6 NU-AC2 now sends `List-Unsubscribe` for it. Recorded as
  OQ-2. `[NP-AC1]`
- **In-flight messages when a preference changes** — NP-AC2 gives 60 seconds for a change to
  take effect. Whether an email already queued is cancelled or sent is unstated; US-5.5
  NE-AC5 answers the analogous question for a changed address but not for a changed
  preference. Recorded as OQ-3. `[NP-AC2]`
- **Disabling both channels for a transactional type** — permitted by omission, which makes
  the customer unreachable for that event with no confirmation. Recorded as OQ-5.
  `[NP-AC2]`
- **Who writes the per-type explanations, and in what language** — NP-AC1 requires one per
  event type. US-5.5 NE-AC2 renders in the customer's locale, which US-3.3 UU-AC7 now
  provides. Nothing says the explanations are localised. Recorded as OQ-6. `[NP-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | |
| D6 Gap handling | Yes | |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NP-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
