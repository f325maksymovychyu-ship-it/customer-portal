---
story_id: US-5.6
title: "Epic 5 — Notifications: One-Click Unsubscribe"
source: docs/backlog/US-5.6-unsubscribe.md
status: draft
revision: 2
last_updated: 2026-08-22
---

# One-Click Unsubscribe

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.
>
> **⚠ Blocked.** See US-5.1 OQ-1 and US-5.5 OQ-1.

## 1. Story

> As someone receiving email from the portal, I want to unsubscribe directly from the message,
> So that unwanted mail stops without my having to remember a password first.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**NU-AC1 — Unsubscribing from the link**
```gherkin
Given an email for a transactional event carrying an unsubscribe link
When the recipient follows it with GET
Then a page names the event type the token carries and states that its emails will stop
And the GET changes no preference, so a mail scanner following the link unsubscribes nobody
When they confirm with POST
Then the email channel for that event type is disabled
And they see confirmation and a link to full settings
But signing in is not required at any point
And the page names no email address, so a forwarded link discloses nothing about the recipient
```

**NU-AC2 — Unsubscribing from the mail client**
```gherkin
Given an email of the transactional, administrative or informational class
Then it carries List-Unsubscribe and List-Unsubscribe-Post headers
When the recipient uses their mail client's own unsubscribe control
Then a POST is received and processed exactly as NU-AC1's confirmation would be
And no confirmation page is required, because the client already confirmed
```

**NU-AC3 — Security email**
```gherkin
Given an email notifying a password change
Then it contains no unsubscribe link and no List-Unsubscribe header
When a token for a security event type is submitted directly
Then respond 422 with type ".../errors/preference-locked"
And no preference is changed
```

**NU-AC4 — Tampered or expired token**
```gherkin
Given a token whose signature does not verify, or which is older than 90 days
When it is submitted
Then respond 404 with type ".../errors/unsubscribe-token-invalid"
And the page reads "This link is no longer valid" and offers settings after signing in
And no preference is changed
And the page names no address, no account and no event type
Because a token that does not verify proves nothing about who holds it
But a token that does verify proves the holder has the email, so NU-AC1 may name the event type it carries
```

**NU-AC5 — Unsubscribing twice**
```gherkin
Given the recipient has already unsubscribed from this event type
And the token is still within its 90-day lifetime
When the same link is used again
Then respond 200 with the same confirmation
And nothing changes, because the operation is idempotent
But once the token passes 90 days NU-AC4 governs instead, and the response becomes 404
```

## 3. Functional Specification

### 3.1 Unsubscribing from the link

Where an email for a transactional event carries an unsubscribe link, following it
presents a page stating exactly which notifications will stop. `[NU-AC1]`

On confirmation the email channel for that event type is disabled, and the recipient sees
confirmation together with a link to full settings. `[NU-AC1]`

Signing in is not required at any point. `[NU-AC1]`

Where the recipient has already unsubscribed from that event type, using the same link
again responds `200` with the same confirmation and changes nothing. `[NU-AC5]`

### 3.2 Unsubscribing from the mail client

An eligible email carries the `List-Unsubscribe` and `List-Unsubscribe-Post` headers.
`[NU-AC2]`

Where the recipient uses the mail client's own control, the resulting `POST` is processed
exactly as the confirmation of §3.1 would be, and no confirmation page is required.
`[NU-AC2]`

### 3.3 Protected classes

An email notifying a password change contains no unsubscribe link and no
`List-Unsubscribe` header. `[NU-AC3]`

Where a token for a security event type is submitted directly, the response is `422` with
`type` `.../errors/preference-locked`, and no preference is changed. `[NU-AC3]`

### 3.4 Invalid tokens

Where a token's signature does not verify, or the token is older than 90 days, the
response is `404` with `type` `.../errors/unsubscribe-token-invalid`. `[NU-AC4]`

The page reads "This link is no longer valid" and offers settings after signing in, no
preference is changed, and the response does not reveal whose token it was or whether that
account exists. `[NU-AC4]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | unsubscribe endpoint | Reached from a link and from a header-driven `POST`; **no path is named by any criterion** — see OQ-1 | `[NU-AC1]` `[NU-AC2]` |
| 2 | unsubscribe token | Signed; expires at 90 days; **its scope is not stated by any criterion** — see OQ-2 | `[NU-AC1]` `[NU-AC4]` |
| 3 | `List-Unsubscribe` header | Present on eligible mail, absent on security mail | `[NU-AC2]` `[NU-AC3]` |
| 4 | `List-Unsubscribe-Post` header | Present alongside it | `[NU-AC2]` |
| 5 | eligibility | Transactional events carry the affordance; security events do not. **The administrative and informational classes are not addressed** | `[NU-AC1]` `[NU-AC3]` |
| 6 | effect | Disables the email channel for one event type, matching US-5.4 | `[NU-AC1]` |
| 7 | `ProblemDetail.type` | Slugs `preference-locked` (shared with US-5.4), `unsubscribe-token-invalid` | `[NU-AC3]` `[NU-AC4]` |
| 8 | Message strings | "This link is no longer valid" | `[NU-AC4]` |

## 5. Out of Scope

- The authenticated preference matrix — US-5.4.
- Global suppression across every event type — no criterion reaches it.
- Resubscribing without signing in — no criterion reaches it; see OQ-4.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | The token carries the recipient and one event type, and grants nothing else. | NU-AC1 disables one event type's channel, which implies the token identifies both. Nothing states the token is limited to that. See OQ-2. |
| A-2 | The token is stateless and signed rather than stored, so no table grows with every email sent. | Named in the story's Data Model Notes. NU-AC4 speaks of a signature, which is consistent. |
| A-3 | Following the link with `GET` changes no state; only the confirmation `POST` does. | Named in the story's Non-Functional section. NU-AC1's two-step flow implies it, but mail scanners follow links automatically and NU-AC2 introduces a `POST` that acts immediately. |
| A-4 | The unsubscribe writes the same preference row US-5.4 would write, so the two paths cannot diverge. | Named in the story's Data Model Notes. |
| A-5 | Every unsubscribe is recorded with time, source address and method. | Named in the story's Non-Functional section; no criterion asserts it, though it is the evidence a provider or regulator asks for. |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | No criterion names the endpoint, or distinguishes the `GET` that renders the confirmation page from the `POST` that acts. NU-AC2's header-driven flow makes that distinction load-bearing: a mail scanner that follows the link must not unsubscribe anyone. | NU-AC1, NU-AC2 |
| OQ-2 | NU-AC4 requires the response not to reveal "whose token it was", yet NU-AC1 requires the page to state "exactly which notifications will stop" before the recipient confirms. Naming the event type discloses that the address is subscribed to it. How much may the page say before confirmation? | NU-AC1, NU-AC4 |
| OQ-3 | The criteria cover transactional (NU-AC1) and security (NU-AC3) classes. What about administrative announcements (US-5.8) and informational digests (US-5.7)? US-5.8's class is described as "only when not critical", which no criterion here implements. | NU-AC1 |
| OQ-4 | How does a recipient resubscribe? Only by signing in, which is defensible, but someone who unsubscribed by mistake has no route back from the page itself. | NU-AC1 |
| OQ-5 | NU-AC5 makes a repeated unsubscribe idempotent with `200`, while NU-AC4 returns `404` for an expired token. A token used twice a year apart hits both rules. Which wins? | NU-AC4, NU-AC5 |
| OQ-6 | If the address belongs to a deactivated account, should the endpoint still succeed? NU-AC4's uniform response hides account state, which suggests yes, but nothing says so. | NU-AC4 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| NU-AC1 | An unauthenticated link stops one event type's email | §3.1, §4 | Covered — the criterion now separates the safe GET from the acting POST and bounds disclosure |
| NU-AC2 | Mail-client headers drive the same operation without a page | §3.2, §4 | Covered |
| NU-AC3 | Security mail carries no unsubscribe affordance at all | §3.3, §4 | Covered — see OQ-3 |
| NU-AC4 | Tampered and expired tokens are refused without disclosure | §3.4, §4 | Covered — both conflicts are now resolved in the criteria themselves |
| NU-AC5 | A repeated unsubscribe is idempotent | §3.1, §4 | Covered — see OQ-5 |

**Coverage:** 5 Covered, 0 Partial, 0 Not covered.

> OQ-2 is a genuine conflict between two criteria in this story rather than a gap. NU-AC1
> requires the page to name what will stop; NU-AC4 requires the endpoint to reveal nothing
> about the account. Both cannot hold for the same page.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-5.6-unsubscribe.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
