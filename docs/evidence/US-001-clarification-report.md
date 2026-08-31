---
artifact_type: clarification_report
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T00:33:59Z
updated_at: 2026-08-31T00:33:59Z
produced_by: us-clarifier
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
supersedes: null
---

# Clarification Report — US-001 Customer Registration

## 1. Scope understanding

US-001 delivers **self-service customer registration**: an unauthenticated
person creates a Customer account by supplying an email address and a password,
receives the `CUSTOMER` role, and can subsequently authenticate (authentication
itself is US-002, out of scope here).

Actor: **Customer** (person who owns an account — business glossary).
Business value: customers self-register without administrator involvement.

In scope (from the Acceptance Criteria):

- Create a Customer account from email + password (AC-001).
- Reject a registration whose email already belongs to an account (AC-002).
- Reject an invalid email format with a validation error (AC-003).
- Persist the password only in non-plaintext form (AC-004).
- Never return the password or its hash in the response (AC-005).

Explicitly out of scope (Story "Out Of Scope"): login, password reset, email
verification, MFA, account activation workflow.

### Requirements already fixed by product / architecture docs (not open)

| Area | Resolved by |
|---|---|
| Email must be unique | `business-rules.md` BR-001 |
| Email comparison is case-insensitive | BR-002 |
| A customer owns only one account | BR-003 |
| Default role after registration is `CUSTOMER` / `ROLE_CUSTOMER` | BR-006, `security-conventions.md` SC-2 |
| Default account state on registration is **enabled** | SC-2 |
| Passwords never stored in plaintext; BCrypt hash only | BR-005, NFR-001, SC-1, `persistence-conventions.md` PC-9 |
| Password policy: length 12–72, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special | SC-1 + training-project security policy block |
| Password validated at request layer **and** re-checked in the service before hashing | SC-1 |
| Plaintext password allowed only on the inbound DTO; never logged, persisted, returned | SC-1, SC-9 |
| Hash column `password_hash VARCHAR(60)` non-null | PC-9 |
| Endpoint is REST under `/api/v1/…`, plural noun resource `/api/v1/customers` | AC-1, AC-3, NFR-003 |
| Create returns `201 Created` + `Location` header + created resource body | AC-4 |
| Duplicate / uniqueness conflict maps to `409`; bean-validation failure to `400`; wrong `Content-Type` to `415` | AC-5 |
| Error responses use the AC-6 JSON shape; `fieldErrors[]` for validation | AC-6 |
| Exception→HTTP mapping only in the single `@RestControllerAdvice` in `exception` package | AC-9, NFR-008 |
| Registration endpoint is **public**; every other endpoint denies by default | SC-4 |
| Auth model is session-based for the MVP (no `Authorization` header) | SC-3, AC-7 |
| Timestamps stored in UTC; entities carry `created_at` / `updated_at` via JPA auditing | BR-007, PC-6 |
| Surrogate `Long id`, `IDENTITY` generation; natural key (email) gets `UNIQUE`, not PK | PC-3 |
| Explicit column mapping: nullability, length for every `String`, named constraints | PC-4, PC-5, NFR-004 |
| Schema is hand-maintained in `schema.sql`; `ddl-auto` = `validate`/`none` | PC-2, SC-8 |
| Index on the email lookup column (the `UNIQUE` constraint supplies it) | PC-7 |
| Layering Controller → Service → Repository | NFR-008 |
| Tests must cover happy path, validation, security | NFR-005 |

## 2. Ambiguities found

| # | Ambiguity | Disposition |
|---|---|---|
| A1 | No email length or format rule is stated, yet PC-4 / NFR-004 require an explicit column length. | **OD-001** |
| A2 | CSRF handling for an unauthenticated, pre-session `POST` is not determined by SC-5's browser-vs-stateless split. | **OD-002** |
| A3 | AC-002 + AC-6 imply an explicit `409` duplicate-email message; SC-3's no-enumeration rule is scoped to authentication only — unclear whether it extends to registration. | **OD-003** |
| A4 | AC-4 requires a "created resource body"; AC-005 only says what must be absent. The positive field list is undefined. | **OD-004** |
| A5 | Story enables administrator-free self-registration but no doc addresses abuse / rate limiting. | **OD-005** (recommend out of scope) |
| A6 | BR-002 fixes case-insensitive comparison; the persistence mechanism (normalize vs. functional index) is unspecified. | **OD-006** (DB_DESIGN) |
| A7 | The Story names only "email address and password" — no display name or profile fields. | **Assumption A-1** below; no additional Customer attributes in US-001 (profile is US-003). |
| A8 | A password-confirmation field is a client concern. | Out of scope; the API accepts a single `password` field. |

## 3. Contradictions

None found between the Story, the Acceptance Criteria, and the product /
architecture documents. AC-004 ("not plain text") and AC-005 ("hash not
returned") are consistent with and narrower than SC-1 / PC-9, which govern.

## 4. Stated assumptions (carried into the Specification)

- **A-1:** The Customer created by US-001 has exactly these business attributes:
  email, password (as BCrypt hash), role, enabled flag, audit timestamps. No
  name / profile data — that is US-003.
- **A-2:** Only `application/json` is accepted; a request without
  `Content-Type: application/json` returns `415` (AC-2).
- **A-3:** No authentication or session is required to call the registration
  endpoint (SC-4); all other access rules are unchanged by this Story.

## 5. Checklist — what the Specification must cover

Traceability: each item cites the Acceptance Criterion and/or convention it
serves.

- [ ] **Endpoint contract** — `POST /api/v1/customers`, request DTO (`email`,
  `password`), `201` + `Location` + response DTO, public access. (AC-001, AC-3,
  AC-4, SC-4)
- [ ] **Response DTO field list** — resolve per **OD-004**; must exclude
  `password` and any hash. (AC-005, SC-1)
- [ ] **Email validation rule** — format + max length, resolved per **OD-001**;
  `400` with `fieldErrors[]` on failure. (AC-003, AC-6, NFR-002, PC-4)
- [ ] **Password validation rule** — enforce SC-1 policy (12–72, upper/lower/
  digit/special) at the request layer and re-check in the service before
  hashing; `400` with `fieldErrors[]` on failure; never log the value.
  (AC-004, NFR-002, SC-1, SC-9)
- [ ] **Uniqueness handling** — case-insensitive duplicate email is rejected;
  no second account created; HTTP status and message resolved per **OD-003**.
  (AC-002, BR-001, BR-002, BR-003, AC-5)
- [ ] **Password persistence** — store only a BCrypt hash in `password_hash`;
  plaintext never persisted or logged. (AC-004, BR-005, SC-1, PC-9)
- [ ] **Role assignment** — new account receives `CUSTOMER` / `ROLE_CUSTOMER`.
  (AC-001, BR-006, SC-2)
- [ ] **Account state** — new account is enabled by default. (SC-2)
- [ ] **Persistence model direction** — surrogate `Long` id, `UNIQUE` on email,
  explicit lengths/nullability, `created_at`/`updated_at` in UTC, email index;
  normalization mechanism deferred to DB_DESIGN per **OD-006**. (PC-3–PC-7,
  NFR-004, BR-007)
- [ ] **CSRF posture** — state the decision from **OD-002** for
  `POST /api/v1/customers`. (SC-5)
- [ ] **Error handling** — all errors via the single `@RestControllerAdvice`,
  AC-6 body shape, no internal detail leakage (`400`, `409`, `415`, `500`).
  (AC-5, AC-6, AC-9, SC-9)
- [ ] **Content type** — non-JSON request bodies return `415`. (AC-2)
- [ ] **Out-of-scope restatement** — no login, password reset, email
  verification, MFA, activation; abuse protection out of scope per **OD-005**.
- [ ] **Testable acceptance mapping** — every AC (AC-001…AC-005) mapped to
  observable behavior for TEST_WRITING; happy-path, validation, and security
  cases. (NFR-005, NFR-006)

## 6. Open Decisions

All recorded in `docs/decisions/US-001-open-decisions.md`:

| id | Summary | Affects | Status |
|---|---|---|---|
| OD-001 | Email max length + accepted format | SPECIFICATION, API_DESIGN, DB_DESIGN, TEST_WRITING | OPEN |
| OD-002 | CSRF classification of the registration endpoint | SPECIFICATION, API_DESIGN, IMPLEMENTATION, SECURITY_REVIEW | OPEN |
| OD-003 | Duplicate-email response vs. account enumeration | SPECIFICATION, API_DESIGN, SECURITY_REVIEW, TEST_WRITING | OPEN |
| OD-004 | Fields in the successful `201` response body | SPECIFICATION, API_DESIGN | OPEN |
| OD-005 | Anti-abuse controls on registration (scope check) | SPECIFICATION, SECURITY_REVIEW | OPEN |
| OD-006 | Email normalization mechanism at persistence | DB_DESIGN | OPEN |

These do not block CLARIFICATION. They are resolved by a human at
`HUMAN_SPEC_APPROVAL`. `spec-writer` should draft against the `recommended`
options and mark each dependent statement as pending that decision.
