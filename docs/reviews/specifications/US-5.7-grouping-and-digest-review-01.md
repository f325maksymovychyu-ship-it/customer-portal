---
spec: docs/specifications/US-5.7-grouping-and-digest.md
spec_revision: 2
story_id: US-5.7
source: docs/backlog/US-5.7-grouping-and-digest.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Grouping and Digest

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. The correction stated NG-AC1's grouping
rule in general form — three or more on one target within 60 minutes — rather than leaving
one worked example to be generalised. The time-zone dependency NG-AC4 relies on is now
satisfied by US-3.3 UU-AC7. F-4 remains: the delivery mode the digest turns on is set
nowhere.

## 2. Findings

### F-1 · Major · D1 AC fidelity — provenance

**Location:** Provenance callout

**Problem:** Criteria authored by an assistant, approved by nobody.

**Resolves when:** the criteria carry a recorded sign-off.

---

### F-2 · Major · D1 AC fidelity — reviewer independence

**Location:** Whole document

**Problem:** Author and reviewer are the same assistant.

**Resolves when:** an independent reader repeats the D3 pass.

---

### F-3 · Major · D5 Testability

**Location:** Blocked callout

**Problem:** The epic-wide module question and the unapproved mail dependency both apply.

**Resolves when:** both are resolved.

---

### F-4 · Major · D2 Coverage

**Location:** §3.3; §4 row 1; OQ-2

**Evidence:**
> | 1 | delivery mode | Values `DAILY_DIGEST` and `IMMEDIATE`; **no endpoint for changing it
> is named** | `[NG-AC4]` `[NG-AC5]` |

**Problem:** NG-AC4 and NG-AC5 both turn on a mode the customer "chose", and no story
describes choosing it. US-5.4's preference matrix covers event types and channels, not
cadence.

**Why it matters:** Two criteria depend on a setting that cannot be established, so neither
can be tested end to end. It also leaves the default undefined: a customer who has never
chosen is in neither branch.

**Resolves when:** a criterion covers setting the delivery mode, in this story or in
US-5.4, and states the default.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NG-AC1 | §3.1, §4 | Seed 5 events on one ticket within 10 minutes → one collapsed entry naming the count, expandable to five timed events, counter incremented by one. Assert 3 within 60 minutes collapses and 2 does not | Yes |
| NG-AC2 | §3.2, §4 | Send an email about a ticket; within 15 minutes emit another event on it → no immediate email; advance the clock past the window → one summary email covering both; assert in-app rows appeared immediately | Yes |
| NG-AC3 | §3.2, §4 | Open a quiet window, then emit a security-class event → its email is sent immediately and separately, and appears in no summary or digest | Yes |
| NG-AC4 | §3.3, §4 | Not derivable end to end: the `DAILY_DIGEST` mode cannot be set — F-4. The 09:00-local timing and the empty-day suppression are otherwise testable with an injected `Clock` | Partly |
| NG-AC5 | §3.3, §4 | Not derivable: requires switching a mode that cannot be set — F-4. The criterion also does not say when the final digest is sent — §5 | No |
| NG-AC6 | §3.4, §4 | Run the digest job twice for one recipient and day → one email; run two instances concurrently → exactly one digest per recipient | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…3 or more unread notifications sharing one target within 60 minutes collapse into one group." | §3.1 | `[NG-AC1]` | Yes — added in the correction |
| "But if nothing happened, no email is sent at all." | §3.3 | `[NG-AC4]` | Yes |
| "…it is never folded into a summary email or a digest." | §3.2 | `[NG-AC3]` | Yes |
| "…running the job on more than one instance concurrently produces exactly one digest per recipient." | §3.4 | `[NG-AC6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Module and mail dependency** — see F-3. All criteria.
- **Delivery mode cannot be set** — see F-4. `[NG-AC4]` `[NG-AC5]`
- **When the final digest is sent** — NG-AC5 flushes "one final digest" on switching to
  `IMMEDIATE` without saying whether immediately or at the next 09:00. The two differ by up
  to a day. Recorded as OQ-4. `[NG-AC5]`
- **One quiet window for every priority** — NG-AC2 uses 15 minutes regardless of the
  ticket's priority, so a `CRITICAL` exchange is paced like a `LOW` one. Recorded as OQ-3.
  `[NG-AC2]`
- **Grouping unit versus read-state unit** — NG-AC1 makes the counter treat a group as one
  entry, while US-5.3 NR-AC1 returns `updated` counting rows. Marking a group read is
  therefore one update or several depending on which story you read. Recorded as OQ-6 here
  and unaddressed there. `[NG-AC1]`

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
`NG-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
