---
spec: docs/specifications/US-5.5-email-notifications.md
spec_revision: 1
story_id: US-5.5
source: docs/backlog/US-5.5-email-notifications.md
source_verified: true
verdict: Needs Changes
reviewed_on: 2026-08-22
review_seq: 01
---

# Review 01 — Email Delivery

## 1. Verdict

**Needs Changes**

| Severity | Count |
|----------|-------|
| Blocker | 0 |
| Major | 4 |
| Minor | 0 |

No Blockers. Six criteria, verbatim, all covered. This story blocks more of the backlog than
any other — eight stories promise an email — and its own dependency is unapproved. The
locale and time-zone gap this spec raised in revision 1 was closed elsewhere: US-3.3 UU-AC7
now creates both fields with defaults, so NE-AC2 has a source.

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

**Problem:** An outbound mail provider and a queue are both new runtime dependencies under
`AGENTS.md` §7.5, and neither is approved. Beyond this story, US-2.1 LI-AC6, US-2.5, US-2.6,
US-3.2, US-3.4, US-4.1, US-4.6 and US-5.8 all promise an email.

**Why it matters:** This is the single highest-fan-out unapproved decision in the backlog.
Refusing it does not merely drop one story; it silently weakens criteria in seven others
that assert a notification the system then cannot send.

**Resolves when:** the dependency is approved, or the affected criteria are rewritten
around in-app delivery only.

---

### F-4 · Major · D5 Testability

**Location:** §4 rows 1 and 2; OQ-2

**Evidence:**
> | 2 | provider webhook | Receives permanent-failure reports; **no path, payload or
> authentication is named** | `[NE-AC4]` |

**Problem:** NE-AC4 describes processing an inbound provider callback and names nothing
about it. Bounce payloads are not standardised between providers, and the criterion's
trigger — "the provider reports a permanent delivery failure" — cannot be constructed in a
test without a payload shape.

**Why it matters:** NE-AC4 disables a delivery channel for a customer. An endpoint that
accepts unauthenticated callbacks of an unspecified shape and disables notifications on
receipt is a denial-of-service primitive against any address an attacker knows.

**Resolves when:** the webhook path, payload and signature verification are specified.

## 3. AC → Spec → Test Traceability

| AC | Spec location | Test condition derivable from the spec | Traceable |
|----|---------------|----------------------------------------|-----------|
| NE-AC1 | §3.1, §4 | Enable email for a type; create the notification → a message queued within 10 s whose subject carries the object reference and whose body links to the page; follow the link signed out → sign-in then the intended page | Yes |
| NE-AC2 | §3.2, §4 | Set locale `uk-UA` and zone `Europe/Kyiv` on the customer; render → Ukrainian text, timestamps in that zone with the zone named, both a text and an HTML part present | Yes |
| NE-AC3 | §3.3, §4 | Stub the port to fail; advance an injected `Clock` → attempts at 1, 5 and 15 minutes, then dead-letter and an alert; assert the in-app row exists throughout | Yes |
| NE-AC4 | §3.4, §4 | Not derivable: the webhook that triggers the criterion has no path or payload — F-4 | No |
| NE-AC5 | §3.5, §4 | Queue a message, confirm a new address, dispatch → sent to the new one; repeat with the new address unconfirmed → sent to the previous confirmed one | Yes |
| NE-AC6 | §3.6, §4 | Queue 501 for one recipient inside an hour → the last discarded, an alert naming the originating event type, the incident recorded | Yes |

## 4. Grounding Register

| Statement (quoted, trimmed) | Location | Tagged | Supported by that AC? |
|-----------------------------|----------|--------|-----------------------|
| "…the previous confirmed address is used." | §3.5 | `[NE-AC5]` | Yes — NE-AC5's `But` clause |
| "…the in-app notification is delivered regardless, because the channels are independent." | §3.3 | `[NE-AC3]` | Yes |
| "…an administrator sees the delivery problem on the customer record." | §3.4 | `[NE-AC4]` | Yes |
| "Where the customer's profile specifies a language and a time zone…" | §3.2 | `[NE-AC2]` | Yes; the fields now exist via US-3.3 UU-AC7 |

## 5. Ambiguities and Missing Edge Cases

- **Mail dependency unapproved, with eight stories behind it** — see F-3. All criteria.
- **Webhook unspecified** — see F-4. `[NE-AC4]`
- **Which message is discarded at the ceiling** — NE-AC6 discards "another" at 501 without
  saying whether the newest or the oldest is dropped, or whether the customer learns
  anything was. Recorded as OQ-4. `[NE-AC6]`
- **Re-enabling after a bounce** — NE-AC4 disables the channel and no criterion covers
  turning it back on after the address is corrected. Recorded as OQ-5. `[NE-AC4]`
- **No end-to-end delivery bound** — NE-AC1 bounds queueing at 10 s; NE-AC3's retries span
  21 minutes. A customer may learn of a password change long after it happened, and nothing
  states a target. Recorded as OQ-6. `[NE-AC1]`
- **SPF, DKIM and DMARC are assumed, not required** — A-2 carries them. Without them the
  mail lands in spam and NE-AC1's premise fails while every test passes. `[NE-AC1]`

## 6. Scope of This Review

| Dimension | Checked | Note |
|-----------|---------|------|
| D1 AC fidelity vs. source | Yes | Six criteria verbatim. Provenance is F-1 |
| D2 Coverage | Yes | |
| D3 Grounding | Yes | No ungrounded statements found |
| D4 Traceability integrity | Yes | |
| D5 Testability and edge cases | Yes | Found F-3, F-4 |
| D6 Gap handling | Yes | The dependency is correctly routed as an escalation |

Mechanical checks: `scripts/trace_check.ps1` **run, but not usable** against the
`NE-AC<n>` scheme. Substitutes: `docs/tools/check-specs.pl`,
`docs/tools/review-evidence.pl`, `docs/tools/grounding-scan.pl`.
