---
spec: docs/specifications/US-5.8-system-announcements.md
spec_revision: 1
story_id: US-5.8
source: docs/backlog/US-5.8-system-announcements.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — System Announcements

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. F-4 is the substantive specification gap:
the entire interface for creating, previewing, scheduling and dispatching lives in the
story's API Contract, which is outside the requirement set, while two criteria refer to a
`DELETE` whose path no criterion gives. F-5 is the one that would keep me awake: an
announcement class that bypasses every customer preference has no gate on who may set it.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody. NA-AC4's 10 000-recipient
approval threshold is an operational control chosen without an operator.

**Resolves when:** the criteria carry a recorded sign-off.

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

**Location:** §4 rows 1 and 2; OQ-1

**Evidence:**
> | 1 | create, preview and dispatch operations | Named by behaviour; **no paths are given by
> any criterion** | `[NA-AC1]` |

**Problem:** NA-AC1 describes composing, previewing and dispatching; NA-AC2 and NA-AC6 refer
to a `DELETE`. No criterion names a path or a request shape for any of them. The interface
exists only in the story's API Contract, which the spec correctly treats as outside the
agreed requirement set.

**Why it matters:** Six criteria describe a workflow no tester can drive. It also means the
approval control in NA-AC4 — the one thing standing between a mistake and every customer —
is specified as a response to a request nobody has defined.

**Resolves when:** criteria name the operations, or the API Contract is promoted into the
requirement set with a sign-off.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NA-AC1 | §3.1, §3.2, §4 | Caller with `announcements:send` composes, selects "all active customers", requests the preview → rendered announcement and an exact count; confirm → `202`, one notification per recipient, audit event carrying author, audience and count. No endpoint to call — F-4 | Partly |
| NA-AC2 | §3.3, §4 | Schedule for a future instant → status `SCHEDULED`, editable and deletable until then, dispatched automatically at the time with an injected `Clock`. Endpoint unknown — F-4 | Partly |
| NA-AC3 | §3.5, §4 | Caller without the scope creates or dispatches → `403` type `insufficient-scope`; the section is not rendered; the attempt is audited | Yes |
| NA-AC4 | §3.2, §4 | Audience of 10 001; dispatch with no `approvedBy` → `422` type `approval-required`; dispatch with `approvedBy` equal to the requester → `422` again | Yes |
| NA-AC5 | §3.4, §4 | No audience selected → `400` with the exact detail; body of 9 characters → `400` with the exact detail; audience resolving to zero accounts → `422` type `empty-audience` | Yes |
| NA-AC6 | §3.6, §4 | Announcement in `DISPATCHING` or `SENT` → no edit, delete or recall offered, only "Send a correction" linking to the original; direct `DELETE` → `422` type `announcement-dispatched` | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "Naming the requesting administrator themselves responds `422` as well, because approval must come from a second person." | §3.2 | `[NA-AC4]` | Yes — NA-AC4's second `When`/`Then` pair |
| "…only \"Send a correction\" is available, which links the new announcement to the original." | §3.6 | `[NA-AC6]` | Yes |
| "…the announcements section is not rendered for that caller…" | §3.5 | `[NA-AC3]` | Yes |
| "…the preview shows the rendered announcement and the exact recipient count." | §3.1 | `[NA-AC1]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Module placement blocks the epic** — see F-3. All criteria.
- **No interface for any operation** — see F-4. `[NA-AC1]` `[NA-AC2]`
- **The critical-maintenance class has no gate** — A-1 assumes announcements respect the
  US-5.4 preference matrix except for a class that always reaches the notification centre.
  Nothing states who may set that class, so any holder of `announcements:send` could make
  every announcement unblockable. Recorded as OQ-2, and it is the most consequential open
  item in this spec. `[NA-AC1]`
- **Which count the threshold tests** — NA-AC1 shows a count at preview and records one at
  dispatch. Accounts are created and deactivated in between. NA-AC4's 10 000 boundary does
  not say which it reads, nor what happens if an approved announcement crosses it before
  sending. Recorded as OQ-4 and OQ-5. `[NA-AC4]`
- **Body format and maximum** — NA-AC5 sets a 10-character minimum and no ceiling, and says
  nothing about markup. Whatever is permitted reaches every customer in the system, so the
  sanitisation rule matters here more than anywhere else in the backlog. Recorded as OQ-6.
  `[NA-AC5]`
- **Emailed or in-app only** — NA-AC1 places announcements in the notification centre and
  says nothing about email. The choice multiplies the blast radius and interacts with the
  unsubscribe rules in US-5.6. Recorded as OQ-3. `[NA-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | Found F-4 |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | |
| D6 Gap handling | Yes | A-1 carries a security-relevant assumption; OQ-2 records the gap |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NA-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
