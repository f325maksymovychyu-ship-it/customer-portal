---
story_id: US-2.6
title: "Epic 2 — Authentication: Password Reset (confirm)"
source: docs/backlog/US-2.6-password-reset-confirm.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# Password Reset (confirm)

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As a customer holding a reset link, I want to set a new password through it,
> So that I can sign in again and know that any access gained with the old password is gone.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**PN-AC1 — Password changed**
```gherkin
Given a valid, unconsumed reset token
When POST /api/v1/auth/password-reset/confirm is called with a password meeting the policy
Then respond 204
And the password hash is replaced and the token is marked consumed
And every session and refresh-token family for that customer is revoked
And an email confirming the change, with time and source IP, is queued
And an audit_events entry is written with event PASSWORD_CHANGED
```

**PN-AC2 — Expired or already consumed token**
```gherkin
Given a reset token issued more than 30 minutes ago
When POST /api/v1/auth/password-reset/confirm is called with it
Then respond 410 with type ".../errors/reset-token-expired"
And the client shows "This link is no longer valid" and offers to send a new one
And the same response is returned for a token that was already consumed
And no password field is presented to the caller
```

**PN-AC3 — Unknown or tampered token**
```gherkin
Given a token that matches no stored hash
When POST /api/v1/auth/password-reset/confirm is called
Then respond 410 with the same body as PN-AC2
And comparison against stored hashes is constant-time
And the response does not reveal whether the token ever existed
```

**PN-AC4 — Password rejected by policy**
```gherkin
Given a valid reset token
When the new password is shorter than 12 characters
Then respond 400 with type ".../errors/password-rejected" naming the violated rule
When the new password appears in the common-password list
Then respond 400 with detail "That password is too common. Choose another"
When the new password contains the customer's email or name
Then respond 400 with detail "The password must not contain your personal details"
And in every case the token remains unconsumed so the customer can retry
```

**PN-AC5 — Reusing the current password**
```gherkin
Given a valid reset token
When the new password matches the password currently on the account
Then respond 400 with detail "The new password must differ from your previous one"
And the token remains unconsumed
```

**PN-AC6 — Strength meter and password managers**
```gherkin
Given the customer is typing into the new-password field
Then a strength indicator updates locally with no request to the server
When the value is pasted from a password manager
Then the paste is accepted and validated exactly as typed input is
And the field carries autocomplete="new-password"
```

## 3. Functional Specification

### 3.1 Setting the new password

Where the presented reset token is valid and unconsumed, and the supplied password meets
the policy of §3.2, the response is `204`. `[PN-AC1]`

The stored password hash is replaced and the token is marked consumed. `[PN-AC1]`

Every session and refresh-token family belonging to that customer is revoked. `[PN-AC1]`

An email confirming the change, carrying the time and the source IP, is queued.
`[PN-AC1]`

An `audit_events` entry with event `PASSWORD_CHANGED` is written. `[PN-AC1]`

### 3.2 Password policy

A password shorter than 12 characters is rejected with `400`, `type`
`.../errors/password-rejected`, naming the violated rule. `[PN-AC4]`

A password appearing in the common-password list is rejected with `400` and the detail
"That password is too common. Choose another". `[PN-AC4]` Which list, and how membership
is decided, is not stated — see OQ-1.

A password containing the customer's email or name is rejected with `400` and the detail
"The password must not contain your personal details". `[PN-AC4]` What counts as
"containing" is not stated — see OQ-2.

A password matching the one currently on the account is rejected with `400` and the detail
"The new password must differ from your previous one". `[PN-AC5]`

In every rejection above the token remains unconsumed, so the customer can retry.
`[PN-AC4]` `[PN-AC5]`

### 3.3 Invalid tokens

Where the token was issued more than 30 minutes ago, the response is `410` with `type`
`.../errors/reset-token-expired`. The client shows "This link is no longer valid" and
offers to send a new one, and presents no password field. `[PN-AC2]`

The same response is returned for a token that was already consumed. `[PN-AC2]`

Where the token matches no stored hash, the response carries the same body as the expired
case, comparison against stored hashes is constant-time, and the response does not reveal
whether the token ever existed. `[PN-AC3]`

### 3.4 Client-side behaviour

While the customer types into the new-password field, a strength indicator updates locally
with no request to the server. `[PN-AC6]`

A value pasted from a password manager is accepted and validated exactly as typed input
is. `[PN-AC6]`

The field carries `autocomplete="new-password"`. `[PN-AC6]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `POST /api/v1/auth/password-reset/confirm` | Path and method named by the criteria | `[PN-AC1]` |
| 2 | reset token (request) | Compared against a stored hash in constant time; format not specified | `[PN-AC1]` `[PN-AC3]` |
| 3 | new password (request) | Minimum 12 characters; **no maximum stated** — see OQ-3 | `[PN-AC4]` |
| 4 | token state | `consumed` distinct from expired and from unknown; unchanged on a policy rejection | `[PN-AC1]` `[PN-AC2]` `[PN-AC4]` |
| 5 | Success status | `204`, no body | `[PN-AC1]` |
| 6 | `ProblemDetail.type` | Slugs `reset-token-expired`, `password-rejected` | `[PN-AC2]` `[PN-AC4]` |
| 7 | rejection details | Three exact strings, quoted in PN-AC4 and PN-AC5 | `[PN-AC4]` `[PN-AC5]` |
| 8 | `audit_events.event` | Value `PASSWORD_CHANGED` | `[PN-AC1]` |
| 9 | Confirmation email | Carries time and source IP; template not specified | `[PN-AC1]` |
| 10 | common-password list | **not specified** — no size, source or refresh cadence | `[PN-AC4]` |
| 11 | strength indicator | Local only; scale and thresholds not specified | `[PN-AC6]` |
| 12 | `autocomplete` attribute | Value `new-password` | `[PN-AC6]` |

## 5. Out of Scope

- Issuing the reset token — US-2.5.
- Invitation acceptance for administrator-created accounts. US-3.2 reuses this endpoint;
  no criterion here covers the 72-hour token that story describes — see OQ-4.
- Password expiry or rotation policy — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Argon2id is used to hash the new password, consistent with US-2.1. | No criterion here names an algorithm. |
| A-2 | The password write, the token consumption and the session revocation occur in one transaction. | Named in the story's Data Model Notes. PN-AC1 lists the three outcomes without stating they are atomic; if they are not, a partial failure leaves a changed password with live sessions. |
| A-3 | "Every session and refresh-token family" in PN-AC1 means all of them, including the one on the device performing the reset. | The caller is not signed in, so no criterion distinguishes a current session here. |
| A-4 | The common-password check runs locally, with no candidate password leaving the process. | Named in the story's Non-Functional section, not in PN-AC4. |
| A-5 | Client-side policy checks are a convenience, and §3.2 is enforced server-side regardless. | PN-AC6 describes local validation; PN-AC4 states server responses. Only under this reading are both simultaneously true. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | PN-AC4 rejects passwords that appear "in the common-password list" but names no list, no size, no source and no maintenance owner. Two implementations could disagree on any given password, so the criterion cannot be tested as written. | PN-AC4 |
| OQ-2 | PN-AC4 rejects a password that "contains the customer's email or name". Does that mean the whole address, the local part, either name, a case-insensitive substring, or a fuzzy match? Each gives a different verdict on the same input. | PN-AC4 |
| OQ-3 | PN-AC4 sets a 12-character minimum and no maximum. The story's Assumptions table says 12–128, and `CP-101` AC-3 already rejects passwords over 128. Should the maximum be a criterion here, and does it match `CP-101`? | PN-AC4 |
| OQ-4 | US-3.2 reuses this endpoint for invitations with a 72-hour token, while PN-AC2 hard-codes 30 minutes as the expiry test. Does this endpoint accept two token lifetimes, and if so how does it tell them apart? | PN-AC2 |
| OQ-5 | PN-AC1 queues a confirmation email carrying the source IP. No criterion says what is shown when the address is behind a proxy, nor whether the customer is told which device performed the reset. | PN-AC1 |
| OQ-6 | The story's Non-Functional section states p95 ≤ 500 ms and requires the confirm page to be `noindex` and to keep the token out of the `Referer` header. No criterion asserts any of these, though the last is a real disclosure risk. | — |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| PN-AC1 | A valid token sets the password and revokes every session | §3.1, §4 | Covered — see OQ-5 |
| PN-AC2 | An expired or consumed token yields 410 | §3.3, §4 | **Partial** — the 30-minute expiry conflicts with the invitation reuse in US-3.2 (OQ-4) |
| PN-AC3 | An unknown token is indistinguishable from an expired one | §3.3, §4 | Covered |
| PN-AC4 | Policy violations yield 400 and leave the token unconsumed | §3.2, §4 | **Partial** — two of the three rules the criterion asserts are undefined (OQ-1, OQ-2) |
| PN-AC5 | Reusing the current password is rejected | §3.2, §4 | Covered |
| PN-AC6 | Local strength meter, paste support, `autocomplete` | §3.4, §4 | Covered |

**Coverage:** 4 Covered, 2 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-2.6-password-reset-confirm.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
