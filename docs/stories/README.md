# Stories

> This folder was `docs/backlog/` until this project adopted the
> `docs/workflow/artifact-paths.yaml` registry, which retires that path in
> favor of `docs/stories/`. Filenames and links below are unchanged.

Stories are the source of truth for scope. Specifications are generated from them
(`story-spec-writer` → `docs/specifications/`), then audited (`story-spec-reviewer`
→ `docs/reviews/specifications/`). A rendered overview of every story in this folder
lives at `docs/backlog-overview.html` — note its internal links still point at the
old `docs/backlog/` path and may need regenerating.

Lifecycle status for each story lives in `docs/catalog/stories.yaml`, not in this
file's table (see `docs/workflow/state-schema.md`).

Where a story and `AGENTS.md` disagree, `AGENTS.md` wins. Each story restates only
what it changes from the conventions below.

## Stories

| Story | Feature | AC prefix | Module | Status |
|---|---|---|---|---|
| [US-2.1](US-2.1-login.md) | Login | `LI-AC` | `customer/` | Backlog — no spec yet |
| [US-2.2](US-2.2-logout.md) | Logout | `LO-AC` | `customer/` | Backlog — no spec yet |
| [US-2.3](US-2.3-active-sessions.md) | Active Session Management | `SM-AC` | `customer/` | Backlog — no spec yet |
| [US-2.4](US-2.4-refresh-token.md) | Refresh Token | `RT-AC` | `customer/` | Backlog — no spec yet |
| [US-2.5](US-2.5-password-reset-request.md) | Password Reset (request) | `PR-AC` | `customer/` | Backlog — no spec yet |
| [US-2.6](US-2.6-password-reset-confirm.md) | Password Reset (confirm) | `PN-AC` | `customer/` | Backlog — no spec yet |
| [US-3.1](US-3.1-list-users.md) | List and Search Users | `UL-AC` | `customer/` | Backlog — no spec yet |
| [US-3.2](US-3.2-create-user.md) | Create User | `UC-AC` | `customer/` | Backlog — no spec yet |
| [US-3.3](US-3.3-update-user.md) | Update User | `UU-AC` | `customer/` | Backlog — no spec yet |
| [US-3.4](US-3.4-deactivate-user.md) | Deactivate and Reactivate User | `UD-AC` | `customer/` | Backlog — no spec yet |
| [US-3.5](US-3.5-assign-roles.md) | Assign Roles | `RA-AC` | `customer/` | Backlog — no spec yet |
| [US-3.6](US-3.6-manage-roles.md) | Manage Roles and Permissions | `MR-AC` | `customer/` | Backlog — no spec yet |
| [US-3.7](US-3.7-view-audit-information.md) | View Audit Information | `AU-AC` | `shared/` | Backlog — no spec yet |
| [US-3.8](US-3.8-export-audit-information.md) | Export Audit Information | `AX-AC` | `shared/` | Backlog — no spec yet |
| [US-4.1](US-4.1-create-ticket.md) | Create Ticket | `TC-AC` | `support/` | Backlog — no spec yet |
| [US-4.2](US-4.2-my-tickets.md) | My Tickets | `TL-AC` | `support/` | Backlog — no spec yet |
| [US-4.3](US-4.3-ticket-queue-assignment.md) | Queue and Assignment | `TQ-AC` | `support/` | Backlog — no spec yet |
| [US-4.4](US-4.4-customer-reply.md) | Customer Reply | `TR-AC` | `support/` | Backlog — no spec yet |
| [US-4.5](US-4.5-agent-reply-internal-notes.md) | Agent Reply and Internal Notes | `TA-AC` | `support/` | Backlog — no spec yet |
| [US-4.6](US-4.6-ticket-resolution.md) | Ticket Resolution | `TS-AC` | `support/` | Backlog — no spec yet |
| [US-4.7](US-4.7-reopen-ticket.md) | Reopen Ticket | `TO-AC` | `support/` | Backlog — no spec yet |
| [US-5.1](US-5.1-notification-centre.md) | Notification Centre | `NC-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.2](US-5.2-realtime-delivery.md) | Real-Time Delivery | `ND-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.3](US-5.3-read-state.md) | Read State | `NR-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.4](US-5.4-notification-preferences.md) | Preferences | `NP-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.5](US-5.5-email-notifications.md) | Email Delivery | `NE-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.6](US-5.6-unsubscribe.md) | One-Click Unsubscribe | `NU-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.7](US-5.7-grouping-and-digest.md) | Grouping and Digest | `NG-AC` | `notification/` * | Backlog — no spec yet |
| [US-5.8](US-5.8-system-announcements.md) | System Announcements | `NA-AC` | `notification/` * | Backlog — no spec yet |

\* `notification/` does not exist in the canonical module map in `AGENTS.md` §2.1.
Its creation is an architect decision and blocks the whole of Epic 5.

Epic 1 (Users) is covered by the existing `CP-101` specification, not by this backlog.

## Conventions

- **Identifiers.** `US-<epic>.<story>` names a story; `<PREFIX>-AC<n>` names one acceptance
  criterion. These identifiers carry into test names and commit messages unchanged — they are
  the trace from requirement to test to code.
- **Gherkin.** `Given` / `When` / `Then` / `And` / `But` / `Because`, describing observable
  behaviour rather than implementation. Where a mechanism is named (Argon2id, `@Version`,
  cursor pagination), it is a deliberate constraint carried over from `AGENTS.md`, not a hint.
- **Enforcement Matrix.** Every story maps each AC to the mechanism that proves it and marks it
  `[gate]` (blocks the merge) or `[manual]` (checked by a person). An AC with no mechanism is a
  wish, not a criterion.
- **Assumptions & Defaults.** Every default was chosen by the author, not agreed by a
  stakeholder. Each is stated so it can be overridden cheaply before implementation starts.
- **Escalation.** Items marked *Escalation* in Open Questions match a clause of `AGENTS.md` §7
  and require human review before the story is estimated, not before it is merged.

## Ticket lifecycle (Epic 4)

Transitions are validated on the server by a sealed domain type owned by US-4.6. Any transition
outside this table responds `422` listing the permitted next states.

| Status | Moved by | Permitted next states |
|---|---|---|
| `NEW` | System, on creation | `IN_PROGRESS`, `WAITING_FOR_CUSTOMER` |
| `IN_PROGRESS` | Agent | `WAITING_FOR_CUSTOMER`, `RESOLVED` |
| `WAITING_FOR_CUSTOMER` | Agent | `IN_PROGRESS`, `WAITING_FOR_SUPPORT`, `RESOLVED` |
| `WAITING_FOR_SUPPORT` | Customer, by replying | `IN_PROGRESS`, `RESOLVED` |
| `RESOLVED` | Agent | `CLOSED`, `REOPENED` |
| `CLOSED` | Customer, or the auto-closure job | `REOPENED`, within 14 days |
| `REOPENED` | Customer | `IN_PROGRESS` |

## SLA thresholds (Epic 4)

Four stories consume these figures — US-4.1 starts the timer, US-4.3 orders the queue by
the resulting deadline, US-4.6 pauses it, US-4.7 restarts it. Until this table existed none
of them could be tested.

**These values are a proposal and need a product owner's sign-off.** They are stated here so
the criteria have something concrete to reference; overriding a row changes no criterion.

| Priority | First response due | Resolution due | At risk from |
|---|---|---|---|
| `CRITICAL` | 1 hour | 8 hours | 75% of the deadline elapsed |
| `HIGH` | 4 hours | 2 business days | 75% |
| `NORMAL` | 1 business day | 5 business days | 75% |
| `LOW` | 3 business days | 15 business days | 75% |

`slaState` is `OK` before the at-risk point, `AT_RISK` between it and the deadline, and
`BREACHED` after it. Timers count elapsed time in the ticket's active statuses only; they
pause in `WAITING_FOR_CUSTOMER` and `RESOLVED` (US-4.6) and keep running while an agent
writes internal notes (US-4.5).

"Business day" is unresolved: no calendar, working hours or holiday set is defined anywhere.
Until one is, only `CRITICAL` is fully testable.

## Notification classes (Epic 5)

The class is declared in code beside the event type and is not editable at runtime. It decides
whether a notification can be switched off and whether it can be delayed or bundled.

| Class | Examples | Opt-out | Grouping |
|---|---|---|---|
| `SECURITY` | Password changed, new-device sign-in, token reuse detected | None | Never |
| `TRANSACTIONAL` | Ticket reply, status change, role assigned | Per channel (opt-out) | Yes |
| `ADMINISTRATIVE` | Maintenance announcements | Only when not critical | No |
| `INFORMATIONAL` | Digests, product guidance | Free (opt-in) | Yes |

## Dependency notes

- US-2.2, US-2.3 and US-2.4 all read and write the same refresh-token family metadata. Build
  US-2.4 first; the other two are far cheaper once rotation exists.
- US-2.6 is reused by US-3.2 for invitations, with a different token lifetime.
- US-3.5 and US-3.6 define the scope vocabulary every other admin story depends on.
- US-3.4 UD-AC4 and US-3.5 RA-AC5 enforce the same last-administrator invariant from two
  different endpoints; they must not drift apart.
- US-4.1 is blocked on object storage and antivirus scanning.
- US-4.2 → US-4.4 → US-4.6 → US-4.7 form a chain; US-4.3 and US-4.5 need US-4.1 only.
- Epic 5 is blocked on the module decision. US-5.5 additionally blocks every story that promises
  an email: US-2.1, US-2.5, US-2.6, US-3.2, US-4.1 and US-4.6.

## Open dependencies requiring approval

These are new runtime dependencies under `AGENTS.md` §7.5. None of them is decided, and each
blocks the stories listed beside it.

| Dependency | Needed by | Consequence if refused |
|---|---|---|
| Shared TTL store (Redis / Valkey) | US-2.1, US-2.5, US-4.1 | Rate limiting must be redesigned around the database |
| Outbound mail provider and queue | US-5.5, and every story that sends email | No email channel at all; in-app only |
| Object storage | US-3.8, US-4.1 | No attachments and no asynchronous audit export |
| Antivirus scanning | US-4.1 | Attachments cannot be accepted safely |
| Event fan-out (broker or `LISTEN`/`NOTIFY`) | US-5.2 | Real-time delivery degrades to polling |
| Offline IP-to-city database | US-2.3 | Session list shows no location |

## Suggested build order

1. **US-3.6** — everything else checks the scopes it defines.
2. **US-2.1 → US-2.4 → US-2.2 → US-2.3** — the session lifecycle, rotation first.
3. **US-2.5 → US-2.6** — self-service recovery, once mail is approved.
4. **US-3.1 → US-3.2 → US-3.3 → US-3.4 → US-3.5** — administration over a working directory.
5. **US-3.7 → US-3.8** — audit, once there are events worth reading.
6. **Epic 4**, once object storage and scanning are approved.
7. **Epic 5**, once the module question is settled — US-5.1 and US-5.4 first, since every other
   notification story depends on the centre and the preference matrix.
