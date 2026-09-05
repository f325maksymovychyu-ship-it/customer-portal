---
spec: docs/specifications/US-5.2-realtime-delivery.md
spec_revision: 2
story_id: US-5.2
source: docs/backlog/US-5.2-realtime-delivery.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Real-Time Delivery

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. The correction closed the burst rule —
ND-AC5 now states the general coalescing threshold rather than one worked example. Two
blocking dependencies remain, and F-5 is the specification gap: every criterion assumes a
transport that no criterion names.

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

**Location:** Blocked callout; OQ-1

**Problem:** Beyond the epic-wide module question, this story additionally needs event
fan-out across application instances — a broker or Postgres `LISTEN`/`NOTIFY`. `AGENTS.md`
§7.5 requires human approval for a new runtime dependency and none has been given.

**Why it matters:** Without it every criterion holds only on a single-instance deployment.
A test suite running one instance will pass while production, running several, delivers
nothing to customers connected to the wrong node.

**Resolves when:** the dependency is approved, or the criteria are scoped to a
single-instance deployment explicitly.

---

### F-4 · Major · D5 Testability

**Location:** §4 row 1; OQ-2

**Evidence:**
> | 1 | stream endpoint | Required by every criterion; **no path or transport is named** |

**Problem:** ND-AC3 sends `lastEventId`, which is the Server-Sent Events reconnection field
and strongly implies SSE. No criterion says so. A WebSocket implementation would satisfy
every criterion's wording while requiring different infrastructure, a different security
review and different proxy behaviour.

**Why it matters:** The transport decides whether the connection survives corporate proxies,
how it authenticates, and what the 100-events-per-minute limit applies to. A tester cannot
open the stream without knowing which it is.

**Resolves when:** a criterion names the endpoint and the transport.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| ND-AC1 | §3.1, §4 | Connected client; generate a notification → toast within 5 s carrying the title, counter incremented, toast self-dismisses at 6 s, entry already in the centre. Cannot open the stream without F-4 | Partly |
| ND-AC2 | §3.2, §4 | Two tabs, one browser → exactly one connection; counter updates in both; toast only in the focused tab; read in one clears the highlight in the other | Yes |
| ND-AC3 | §3.3, §4 | Drop the connection → retries at 1, 2, 4, 8 s, never more than one per 30 s; reconnect sends `lastEventId`; a duplicate delivery renders once | Partly — see F-4 |
| ND-AC4 | §3.3, §4 | Block the stream for 61 s → client polls every 60 s, no error surfaced; restore → client returns to streaming | Yes |
| ND-AC5 | §3.4, §4 | Emit 50 notifications in 10 s → one toast reading "50 new notifications" that opens the centre; all 50 present; assert 4 in 10 s also coalesces and 3 does not | Yes |
| ND-AC6 | §3.5, §4 | Revoke the session per US-2.3 → server closes the connection within 5 s; reconnect with the revoked credentials → `401` | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…more than 3 notifications arriving within any 10-second window are coalesced into one counted toast." | §3.4 | `[ND-AC5]` | Yes — added in the correction |
| "…the summary toast opens the notification centre when clicked." | §3.4 | `[ND-AC5]` | Yes — added in the correction |
| "…exactly one stream connection exists across both." | §3.2 | `[ND-AC2]` | Yes |
| "…a reconnection attempt carrying the revoked credentials responds `401`." | §3.5 | `[ND-AC6]` | Yes |

## 5. Ambiguities and Missing Edge Cases

- **Fan-out dependency unapproved** — see F-3. All criteria.
- **Transport unnamed** — see F-4. `[ND-AC1]` `[ND-AC3]`
- **Polling fallback load is unbounded** — ND-AC4 falls back to polling "the notification
  endpoint" every 60 s per disconnected client. Nothing bounds the aggregate, and a fan-out
  outage puts every client on that path simultaneously. Recorded as OQ-4. `[ND-AC4]`
- **Latency origin** — ND-AC1's 5 seconds is measured from an unstated point. US-5.5 NE-AC1
  uses 10 seconds from notification creation for the email path, so the two bounds may or may
  not share an origin. Recorded as OQ-5. `[ND-AC1]`
- **Read-state convergence: tabs yes, devices no** — ND-AC2 requires it between tabs while
  US-5.3 leaves the two-device case open. Recorded as OQ-6 here and OQ-1 there. `[ND-AC2]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | Both dependencies routed as escalations |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`ND-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
