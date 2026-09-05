---
story_id: US-3.1
title: "Epic 3 — Administration: List and Search Users"
source: docs/backlog/US-3.1-list-users.md
status: draft
revision: 4
last_updated: 2026-08-26
---

# List and Search Users

> **⚠ Provenance.** The story and its acceptance criteria were drafted by an assistant,
> not supplied or approved by a stakeholder. Until section 2 is signed off, every row in
> this document is a proposal.

## 1. Story

> As an administrator, I want to browse, search and filter the customer directory,
> So that I can find one account in seconds instead of paging through thousands of rows.

## 2. Acceptance Criteria

Verbatim from the source. These are the only requirements in this document.

**UL-AC1 — Directory listing**
```gherkin
Given an administrator holding the customers:read scope
When GET /api/v1/admin/customers is called with no filters
Then respond 200 with a page of 25 summaries sorted by createdAt descending
And each entry carries name, email, roles, status, last sign-in and creation date
And the response includes the total number of matching records
```

**UL-AC2 — Search and filtering**
```gherkin
Given the directory contains a customer named "Olena"
When GET /api/v1/admin/customers is called with q="olen"
Then only records matching the term in name or email are returned
And matching is case-insensitive
When status=DEACTIVATED and role=SUPPORT_AGENT are supplied together
Then only records satisfying both conditions are returned
```

**UL-AC3 — Shareable, reload-safe state**
```gherkin
Given an administrator has applied filters and moved to page three
When the resulting URL is opened in a new tab
Then the same filters, sort order and page are restored
And a browser reload does not reset them
And the search term is carried in the query string, because a shareable filtered view requires it
But the gateway redacts the q parameter from access logs, and the page is served Referrer-Policy: no-referrer
Because a search term may itself be personal data, and the URL is the only place it can live
```

**UL-AC4 — Caller without the required scope**
```gherkin
Given a caller authenticated without the customers:read scope
When GET /api/v1/admin/customers is called directly
Then respond 403 with type ".../errors/insufficient-scope"
And the administration entry point is not rendered for that caller at all
And an audit entry records the attempt
```

**UL-AC5 — No matches, and a server failure**
```gherkin
Given the applied filters match no records
When the listing is requested
Then respond 200 with an empty page and a total of zero
And the client shows "No customers match these filters" with a reset action
Given the request fails with a 5xx
Then the client shows an error with a retry action
And the applied filters are not discarded
```

**UL-AC6 — Rejecting abusive paging and search terms**
```gherkin
Given a request with size greater than 100
When the listing is requested
Then respond 400 with type ".../errors/validation-failed"
When q is a single character
Then respond 400 naming the minimum length
When sort names a field that is not exposed by CustomerSummary
Then respond 400 rather than passing the value into the query
```

## 3. Functional Specification

### 3.1 Listing

A directory request from a caller holding the `customers:read` scope responds `200` with a
page of 25 summaries sorted by creation date, newest first. `[UL-AC1]`

Each entry carries the name, email, roles, status, last sign-in and creation date.
`[UL-AC1]`

The response includes the total number of records matching the request. `[UL-AC1]`

### 3.2 Search and filters

Where a search term is supplied, only records matching that term in the name or the email
are returned, and matching is case-insensitive. `[UL-AC2]`

Where a status filter and a role filter are supplied together, only records satisfying
both conditions are returned. `[UL-AC2]`

Where the filters match no records, the response is `200` with an empty page and a total
of zero, and the client shows "No customers match these filters" together with a reset
action. `[UL-AC5]`

### 3.3 View state

Filters, sort order and page are carried in the request URL, so that opening it in a new
tab restores the same view and a browser reload does not reset it. `[UL-AC3]`

The search term is carried in the query string, because a shareable filtered view requires
it. `[UL-AC3]`

Because a search term may itself be personal data and the URL is the only place it can
live, two mitigations apply: the gateway redacts the `q` parameter from access logs, and the
page is served `Referrer-Policy: no-referrer`. `[UL-AC3]`

### 3.4 Authorisation

Where the caller does not hold the `customers:read` scope, the response is `403` with
`type` `.../errors/insufficient-scope`, and an audit entry records the attempt.
`[UL-AC4]`

The administration entry point is not rendered for such a caller. `[UL-AC4]`

### 3.5 Request validation

A page size greater than 100 is rejected with `400` and `type`
`.../errors/validation-failed`. `[UL-AC6]`

A search term of a single character is rejected with `400` naming the minimum length.
`[UL-AC6]`

A `sort` value naming a field the summary does not expose is rejected with `400` rather
than being passed into the query. `[UL-AC6]`

### 3.6 Failure handling

Where the request fails with a `5xx`, the client shows an error with a retry action and
does not discard the applied filters. `[UL-AC5]`

## 4. Data and Interfaces

| # | Field / interface | Format | Source |
|---|---|---|---|
| 1 | `GET /api/v1/admin/customers` | Path and method named by the criteria | `[UL-AC1]` |
| 2 | `customers:read` scope | Required for every call | `[UL-AC1]` `[UL-AC4]` |
| 3 | `q` (query) | Minimum 2 characters; matched case-insensitively against name and email; carried in the URL and redacted from access logs | `[UL-AC2]` `[UL-AC3]` `[UL-AC6]` |
| 3a | `Referrer-Policy` (response header) | Value `no-referrer` on the listing page | `[UL-AC3]` |
| 4 | `status` (query) | Value `DEACTIVATED` named; the rest of the set is not specified | `[UL-AC2]` |
| 5 | `role` (query) | Value `SUPPORT_AGENT` named; the rest of the set is not specified | `[UL-AC2]` |
| 6 | `size` (query) | Maximum 100; default 25 | `[UL-AC1]` `[UL-AC6]` |
| 7 | `page` (query) | not specified beyond being addressable | `[UL-AC3]` |
| 8 | `sort` (query) | Restricted to fields the summary exposes; default `createdAt` descending | `[UL-AC1]` `[UL-AC6]` |
| 9 | summary entry | Name, email, roles, status, last sign-in, creation date | `[UL-AC1]` |
| 10 | total count | Integer; zero on an empty result | `[UL-AC1]` `[UL-AC5]` |
| 11 | `ProblemDetail.type` | Slugs `insufficient-scope`, `validation-failed` | `[UL-AC4]` `[UL-AC6]` |
| 12 | Message strings | "No customers match these filters" | `[UL-AC5]` |

## 5. Out of Scope

- Creating, editing or deactivating accounts — US-3.2, US-3.3, US-3.4.
- Role assignment — US-3.5.
- Exporting the directory — no criterion reaches it.

## 6. Assumptions

| ID | Assumption | Why it was needed |
|----|-----------|-------------------|
| A-1 | Paging and filtering execute in the database, not in memory. | Named in the story's Non-Functional section. No criterion is observable enough to distinguish the two. |
| A-2 | The client debounces the search field by 300 ms. | Named in the story's Assumptions table. UL-AC2 constrains only the result set. |
| A-3 | `CustomerSummary` is a projection, so `sort` can be validated against a fixed field list. | UL-AC6 refers to "a field that is not exposed by CustomerSummary" without defining the type. |
| A-4 | The status and role vocabularies come from US-2.1 and US-3.6 respectively. | UL-AC2 names one value of each without enumerating either. |
| A-5 | Pagination is offset-based (`Page<T>` with `page`, `size`, `sort`), not cursor-based. | Named in the story's Assumptions & Defaults table, citing `AGENTS.md` §3.1's reservation of cursor pagination for continuous feeds. No criterion asserts the paging mechanism, only its shape (a page of 25, a `size` cap of 100). |

## 7. Open Questions

| ID | Question | Blocks |
|----|----------|--------|
| OQ-1 | UL-AC3 requires the gateway to redact `q` from access logs. That is infrastructure this repository does not own, so no test here can assert it and a misconfigured gateway fails the criterion silently. Who owns the redaction rule, and where is it verified? The residual exposure — the search term in browser history — is not addressed by either mitigation. | UL-AC3 |
| OQ-2 | UL-AC1 sorts by creation date and UL-AC6 restricts `sort` to exposed fields, but no criterion says which fields are sortable or whether direction can be chosen. | UL-AC1, UL-AC6 |
| OQ-3 | Are accounts in `PENDING_INVITATION` (US-3.2) included in the default listing and in the total count? No criterion says, and they are not yet real users. | UL-AC1 |
| OQ-4 | UL-AC4 requires an audit entry for a refused attempt but names no event or severity. US-3.7 now defines `severity` ∈ {`INFO`, `NOTICE`, `SECURITY`} and lists refused access to audit data as `SECURITY`; a refused directory read is not listed. Which value applies? | UL-AC4 |
| OQ-5 | The story's Non-Functional section states p95 ≤ 800 ms over 100 000 accounts. No criterion asserts a latency bound. | — |
| OQ-6 | Does the directory need a CSV export? The story marks this out of scope but notes no story yet exists for it, and that US-3.8's asynchronous export machinery would transfer directly if one is written. | — |
| OQ-7 | `customers:read` is this story's scope name, chosen before US-3.6 defines the scope vocabulary every admin story depends on. If US-3.6 settles on a different name, the endpoint's authorization contract changes. | UL-AC1, UL-AC4 |

## 8. Traceability Matrix

| AC | Summary | Covered in | Status |
|----|---------|-----------|--------|
| UL-AC1 | Default listing of 25, newest first, with a total | §3.1, §4 | Covered — see OQ-3, OQ-7 |
| UL-AC2 | Case-insensitive search and combined filters | §3.2, §4 | Covered |
| UL-AC3 | View state survives a reload and is shareable, with the search term redacted from logs | §3.3, §4 | Covered — see OQ-1 |
| UL-AC4 | Missing scope yields 403 and is audited | §3.4, §4 | Covered — all stories now write to the single `audit_events` stream; see OQ-7 |
| UL-AC5 | Empty result and server failure both stay usable | §3.2, §3.6, §4 | Covered |
| UL-AC6 | Abusive size, short term and unknown sort are rejected | §3.5, §4 | **Partial** — the sortable field set is undefined (OQ-2) |

**Coverage:** 5 Covered, 1 Partial, 0 Not covered.

## 9. Revision History

| Rev | Date | Change |
|-----|------|--------|
| 1 | 2026-08-22 | Initial specification from `docs/backlog/US-3.1-list-users.md`. |
| 2 | 2026-08-22 | Re-synced after the backlog was corrected. Criteria in section 2 are verbatim again; statuses and open questions revised. |
| 3 | 2026-08-22 | Applied review 01 F-1 (Blocker), F-2 and F-3. §3.3 stated the inverse of UL-AC3 and reported a conflict the criterion had resolved; it now states the term is carried with its two mitigations, and §4 records them. The UL-AC3 matrix row is no longer a false Partial. OQ-1 is restated as the residual redaction-ownership question. OQ-4, left tautological by the audit-stream rename, is restated against the severity scale US-3.7 now defines. |
| 4 | 2026-08-26 | Re-synced against the backlog story, which had been edited since rev 3 (acceptance criteria unchanged). Added A-5, the previously undocumented offset-vs-cursor pagination default from the story's Assumptions & Defaults table. Added OQ-6 (CSV export) and OQ-7 (`customers:read` scope name pending US-3.6), both present in the story's Open Questions but missing from this spec; UL-AC1 and UL-AC4 now reference OQ-7. |
