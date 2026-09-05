# Epic 3 — Administration: List and Search Users

**Story ID:** US-3.1
**Project:** Customer Portal
**AC prefix:** `UL-AC`
**Module:** `customer/`

## User Story
As an administrator,
I want to browse, search and filter the customer directory,
So that I can find one account in seconds instead of paging through thousands of rows.

## Assumptions & Defaults (confirm or override)
| # | Decision | Default chosen | Rationale |
|---|---|---|---|
| 1 | Pagination | Offset-based `Page<T>` with `page`, `size`, `sort` | `AGENTS.md` §3.1 reserves cursor pagination for continuous feeds; a directory is not one |
| 2 | Page size | 25 by default, 100 maximum | Keeps the response and the render cost bounded |
| 3 | Minimum search length | 2 characters | A one-character term degenerates into a full scan |
| 4 | Debounce | 300 ms after typing stops | Fewer wasted round trips without feeling laggy |
| 5 | Filter state | Carried in query parameters | Makes a filtered view shareable and survivable across a reload |

## In Scope
- `GET /api/v1/admin/customers` — paginated, sortable, filterable directory
- Free-text search across given name, family name and email
- Filters on status and role
- Empty-state and error handling that preserve the applied filters

## Out of Scope
- Creating, editing or deactivating accounts (US-3.2, US-3.3, US-3.4)
- Role assignment (US-3.5)
- Exporting the directory — no story yet; see Open Questions

## API Contract
| Method | Path | Auth | Query | Success |
|---|---|---|---|---|
| GET | `/api/v1/admin/customers` | Bearer + `customers:read` | `q`, `status`, `role`, `page`, `size`, `sort` | `200` `Page<CustomerSummary>` |

`CustomerSummary` is a record carrying `id`, `givenName`, `familyName`, `email`, `roles`, `status`, `lastLoginAt`, `createdAt`.

## Data Model Notes
- Search runs against a normalised, lower-cased expression index on `email` and on the concatenated name columns; without it UL-AC2 cannot meet its latency budget
- `status` is the explicit state column introduced in US-2.1
- The endpoint returns a DTO projection, never a JPA entity (`AGENTS.md` §5), and uses `JOIN FETCH` for roles to avoid the N+1 the roles column would otherwise cause

## Acceptance Criteria

### Happy path
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

### Authorisation
**UL-AC4 — Caller without the required scope**
```gherkin
Given a caller authenticated without the customers:read scope
When GET /api/v1/admin/customers is called directly
Then respond 403 with type ".../errors/insufficient-scope"
And the administration entry point is not rendered for that caller at all
And an audit entry records the attempt
```

### Empty and failure states
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

### Input bounds
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

## Error Envelope (RFC 9457 `ProblemDetail`)
```json
{
  "type": "https://portal.internal/errors/insufficient-scope",
  "title": "Insufficient Scope",
  "status": 403,
  "detail": "This action requires the customers:read scope.",
  "instance": "/api/v1/admin/customers"
}
```
Error `type` slugs introduced by this story: `insufficient-scope`.

## Non-Functional / Security Requirements
- Filtering and paging execute in the database. The full directory must never be loaded into memory and filtered in Java.
- **Performance:** p95 ≤ 800 ms against 100 000 accounts, with the search columns indexed.
- The search term appears in the query string by design, because UL-AC3 requires a filtered view to be shareable and reload-safe. Its exposure is bounded by the two mitigations UL-AC3 names: the gateway redacts `q` from access logs, and the listing page is served `Referrer-Policy: no-referrer`. Neither covers the administrator's own browser history, which is accepted and recorded in Open Question 6.
- No personal data other than the search term belongs in a query string, an access log or outbound telemetry. Identifiers and filter codes are not personal data; a name or an email fragment is.
- `sort` is validated against an allow-list of exposed fields; it must never reach the query builder unchecked.

## Enforcement Matrix
| AC | Mechanism | Marker |
|---|---|---|
| UL-AC1, UL-AC2 | Functional suite (RestAssured + Testcontainers) | `[gate]` |
| UL-AC3 — URL state | Client unit test on query-parameter serialisation, asserting filters, sort, page and `q` survive a reload | `[gate]` |
| UL-AC3 — referrer policy | Contract test on the listing response headers asserting `Referrer-Policy: no-referrer` | `[gate]` |
| UL-AC3 — log redaction | Gateway configuration review. **Not verifiable from this repository**, so no CI job can gate it; owner is the platform team | `[manual]` |
| UL-AC4 | Functional test asserting `403` for a caller without the scope | `[gate]` |
| UL-AC5 | Functional test plus a client test with a stubbed 5xx | `[gate]` |
| UL-AC6 | Slice test on the request parameter record | `[gate]` |
| Latency budget | Performance scenario in `perf/` against a seeded 100 000-row dataset | `[gate]` |
| No N+1 on roles | Integration test asserting the query count | `[gate]` |

## Open Questions
1. Should administrators see accounts in `PENDING_INVITATION` by default, or only behind an explicit filter? They are not yet real users and inflate every count.
2. Does the directory need a CSV export? US-3.8 builds an asynchronous export for audit data whose machinery would transfer directly, but no one has asked for it here.
3. Scope naming (`customers:read`) must line up with the role model in US-3.6. If that story chooses a different vocabulary, this contract changes.
4. UL-AC3's log redaction is gateway configuration, so nothing in this repository can verify it and its enforcement row is `[manual]`. Who owns the rule, and where is it reviewed? A misconfigured gateway fails the criterion silently.
5. `sort` is restricted to "fields the summary exposes" and that set is enumerated nowhere. Two implementations could permit different fields and both satisfy UL-AC6.
6. The two mitigations in UL-AC3 cover server logs and outbound requests. The search term still reaches the administrator's own browser history and any profile-sync service. Is that residual exposure accepted, or should the term move to a POST body at the cost of shareable URLs?
