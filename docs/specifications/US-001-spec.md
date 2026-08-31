---
artifact_type: specification
story: US-001
version: 1
status: APPROVED
created_at: 2026-08-31T00:33:59Z
updated_at: 2026-08-31T07:48:48Z
produced_by: spec-writer
inputs:
  - path: docs/stories/US-001-register-customer.md
    version: null
  - path: docs/evidence/US-001-clarification-report.md
    version: 1
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
supersedes: null
---

# Specification — US-001 Customer Registration

## 1. Overview

Enable a Customer to self-register an account in the Customer Portal by
supplying an email address and a password over a public REST endpoint. On
success the account is persisted with a BCrypt password hash, assigned the
`CUSTOMER` role, enabled, and made available for later authentication (US-002,
out of scope here). Registration attempts that use a duplicate email or invalid
input are rejected without creating an account.

This Specification is the primary source of truth for API design, database
design, planning, testing, and implementation of US-001.

## 2. Business Goal

Allow customers to create accounts without administrator involvement
(`product-vision.md` — self-registration; Story "Business Value"). Customer
self-registration is an MVP success criterion.

## 3. Business Flow

1. An unauthenticated client submits an email and password to the registration
   endpoint.
2. The system validates the request body (email format and length, password
   policy).
3. The system checks that no account already exists for the email
   (case-insensitive).
4. The system hashes the password with BCrypt, creates the account with role
   `CUSTOMER`, `enabled = true`, and UTC audit timestamps.
5. The system returns `201 Created` with a `Location` header and a response body
   that never contains the password or its hash.
6. On any validation failure the system returns `400`; on a duplicate email it
   returns `409`; neither creates or mutates an account.

## 4. Functional Requirements

| id | Requirement |
|---|---|
| FR-1 | The system SHALL expose `POST /api/v1/customers` as a public (no authentication) endpoint that creates a Customer account. |
| FR-2 | The request body SHALL be `application/json` with exactly two fields: `email` (string) and `password` (string). A request without `Content-Type: application/json` SHALL be rejected with `415`. |
| FR-3 | The system SHALL validate `email` and `password` per section 6 before any persistence access; on failure it SHALL return `400` with the `api-conventions.md` AC-6 body including a `fieldErrors[]` entry per failed field, and SHALL NOT create an account. |
| FR-4 | The system SHALL reject a registration whose `email` matches an existing account, compared case-insensitively, without creating a second account. The response is governed by **OD-003** (draft: `409 Conflict` with the AC-6 body and message "An account with this email already exists."). |
| FR-5 | On successful validation and uniqueness check, the system SHALL persist a new Customer with: the submitted email, a BCrypt hash of the submitted password stored in `password_hash`, role `CUSTOMER`, `enabled = true`, and `created_at` / `updated_at` set in UTC via JPA auditing. |
| FR-6 | The system SHALL re-check the password against the policy in the Service layer before hashing, in addition to request-layer validation (`security-conventions.md` SC-1). |
| FR-7 | The system SHALL NOT persist, log, or return the plaintext password or the password hash at any point (`security-conventions.md` SC-1, SC-9; AC-004, AC-005). |
| FR-8 | On success the system SHALL return `201 Created`, a `Location` header pointing at the created resource (`/api/v1/customers/{id}`), and a JSON body whose fields are governed by **OD-004** (draft: `id`, `email`, `role`, `createdAt`). |
| FR-9 | The newly created account SHALL be immediately usable as an authentication subject once US-002 exists — i.e. the stored hash verifies against the submitted password and the role is present. No activation step is required. |
| FR-10 | Exception-to-HTTP mapping SHALL occur only in the single `@RestControllerAdvice` in the `exception` package (`api-conventions.md` AC-9). Controllers SHALL NOT build error responses. |
| FR-11 | The CSRF posture of `POST /api/v1/customers` SHALL follow the decision recorded in **OD-002** (draft: exempt this single public path from CSRF, CSRF remains enabled elsewhere). |

## 5. Acceptance Criteria

Ids are stable and traceable to the Story.

| id | Story AC | Statement |
|---|---|---|
| AC-001 | AC-001 | Given a valid email and a policy-compliant password, when registration is submitted, then an account is created with role `CUSTOMER`, the password is verifiable later, and the response is `201` with a `Location` header. |
| AC-002 | AC-002 | Given an account already exists for an email, when another registration submits the same email (any letter case), then the request is rejected (per OD-003) and no second account is created. |
| AC-003 | AC-003 | Given an email that fails the format or length rule, when registration is submitted, then the response is `400` with a `fieldErrors[]` entry for `email` and no account is created. |
| AC-004 | AC-004 | Given a successful registration, when the account is persisted, then the stored credential is a BCrypt hash in `password_hash` and the plaintext password is never persisted. |
| AC-005 | AC-005 | Given a successful registration, when the response is returned, then the body contains neither the password nor the password hash. |
| AC-006 | AC-001 / AC-003 | Given a password that violates the policy (length, character classes), when registration is submitted, then the response is `400` with a `fieldErrors[]` entry for `password` and no account is created. |
| AC-007 | derived (AC-2, `api-conventions.md`) | Given a request without `Content-Type: application/json`, when registration is submitted, then the response is `415` and no account is created. |

## 6. Validation Rules

Server-side only; no reliance on framework defaults (`non-functional-requirements.md`
NFR-002).

### 6.1 `email`

| Rule | Value | On failure |
|---|---|---|
| Required | must be present and non-blank | `400`, `fieldErrors[].field = "email"` |
| Format | valid email address — **OD-001** (draft: Jakarta `@Email` semantics: non-empty local part, `@`, domain) | `400`, `fieldErrors[].field = "email"` |
| Max length | **OD-001** (draft: 254 characters) | `400`, `fieldErrors[].field = "email"` |
| Uniqueness | no existing account with the same email, compared case-insensitively (`business-rules.md` BR-001, BR-002) | per **OD-003** (draft: `409`) |

Email persistence/normalization mechanism (store lowercased vs. functional
unique index) is deferred to DB_DESIGN per **OD-006**; the requirement
(case-insensitive uniqueness) is fixed here.

### 6.2 `password`

Policy from `security-conventions.md` SC-1 / the training-project security
policy block. Enforced by a custom constraint in the `validation` package
**and** re-checked in the Service before hashing.

| Rule | Value | On failure |
|---|---|---|
| Required | must be present | `400`, `fieldErrors[].field = "password"` |
| Min length | 12 characters | `400`, `fieldErrors[].field = "password"` |
| Max length | 72 characters | `400`, `fieldErrors[].field = "password"` |
| Uppercase | at least one `A–Z` | `400`, `fieldErrors[].field = "password"` |
| Lowercase | at least one `a–z` | `400`, `fieldErrors[].field = "password"` |
| Digit | at least one `0–9` | `400`, `fieldErrors[].field = "password"` |
| Special character | at least one non-alphanumeric | `400`, `fieldErrors[].field = "password"` |

The validation message for a password failure MUST NOT echo the submitted value
(`security-conventions.md` SC-9).

### 6.3 Request shape

- Unknown / extra JSON fields: rejected as a `400` request-shape failure
  (no silent ignore), or ignored — **left to API_DESIGN**; the Specification
  requires only that `email` and `password` are the sole meaningful inputs.
- Malformed JSON: `400` with the AC-6 body.

## 7. Security Requirements

All cited from `security-conventions.md` (SC-*) or an Open Decision — none
invented.

| id | Requirement | Source |
|---|---|---|
| SEC-1 | `POST /api/v1/customers` is the only public endpoint added by this Story; every other endpoint remains deny-by-default. | SC-4 |
| SEC-2 | Passwords are hashed with `BCryptPasswordEncoder` (default strength); a plaintext/no-op encoder is forbidden. The encoder bean lives in the `security` package. | SC-1 |
| SEC-3 | The plaintext password appears only on the inbound request DTO. It is never placed on a response DTO, never persisted, never logged, never included in an error message. | SC-1, SC-9 |
| SEC-4 | The password hash is stored only in `password_hash` and is never returned by any endpoint. | SC-1, PC-9 |
| SEC-5 | The new account is created with authority `ROLE_CUSTOMER` and `enabled = true`. | SC-2 |
| SEC-6 | Error responses never leak stack traces, SQL, entity/class names, file paths, or database URLs. | SC-9, AC-6 |
| SEC-7 | CSRF handling for the registration path follows **OD-002**. If the path is exempted, that is recorded as the Story's architecture decision as SC-5 requires. | SC-5, OD-002 |
| SEC-8 | The duplicate-email response follows **OD-003**. Any account-enumeration exposure it introduces is an accepted, human-approved decision, not a default. | OD-003 |
| SEC-9 | `spring.h2.console.enabled=false` remains in every profile; this Story does not change it. | SC-6 |
| SEC-10 | Hibernate `ddl-auto` stays `validate` or `none`; the schema change for this Story is hand-written in `schema.sql`. | SC-8, PC-2 |
| SEC-11 | No secrets are introduced or committed by this Story. | SC-7 |

Anti-abuse / rate limiting on the registration endpoint is **out of scope** for
US-001 per **OD-005**.

## 8. Error Handling

All error responses use the `api-conventions.md` AC-6 JSON shape, produced by
the single `@RestControllerAdvice` (AC-9).

| Condition | Status | `error` | Notes |
|---|---|---|---|
| Bean-validation / request-shape failure, malformed JSON | `400` | `Bad Request` | includes `fieldErrors[]` for field failures |
| Duplicate email | `409` (draft, per OD-003) | `Conflict` | message per OD-003; no account created |
| Missing / wrong `Content-Type` | `415` | `Unsupported Media Type` | |
| Unmapped exception | `500` | `Internal Server Error` | no internal detail leaked (SC-9) |

`401` / `403` are not applicable — the endpoint is public.

## 9. Non-Functional Requirements

| id | Requirement | Source |
|---|---|---|
| NFR-1 | Passwords stored using BCrypt. | NFR-001, SC-1 |
| NFR-2 | All user input validated server-side. | NFR-002 |
| NFR-3 | REST conventions followed (`/api/v1`, plural nouns, JSON, status codes, AC-6 error body). | NFR-003, `api-conventions.md` |
| NFR-4 | The Customer entity declares explicit column lengths, nullability, and the email uniqueness constraint; surrogate `Long id`; `created_at` / `updated_at` in UTC. | NFR-004, PC-3–PC-6, BR-007 |
| NFR-5 | New functionality includes happy-path, validation, and security tests. | NFR-005 |
| NFR-6 | Implementation is traceable to this Story, this Specification, and the test artifacts. | NFR-006 |
| NFR-7 | The build succeeds before the change is considered complete. | NFR-007 |
| NFR-8 | Controller → Service → Repository layering. | NFR-008 |

## 10. Out of Scope

- Login / authentication (US-002).
- Password reset / recovery.
- Email verification / confirmation.
- Multi-factor authentication.
- Account activation workflow (accounts are enabled on creation).
- Social login.
- Profile management (US-003).
- Rate limiting / anti-abuse on registration (per OD-005).
- Administrative creation or management of customer accounts.

## 11. Open Decisions (with impact)

Recorded in `docs/decisions/US-001-open-decisions.md`. Unresolved; decided by a
human at `HUMAN_SPEC_APPROVAL`. Each dependent requirement above is marked with
its OD id and a "draft" assumption reflecting the recommended option.

| id | Question | Requirements it governs | Impact if the recommendation is not chosen |
|---|---|---|---|
| OD-001 | Email max length + accepted format | FR-3, §6.1, AC-003 | Column length and the `@Email`/regex rule in API & DB design change; validation test vectors change. |
| OD-002 | CSRF classification of the registration endpoint | FR-11, SEC-7 | If CSRF stays enabled, the client must fetch a token first and the endpoint contract / tests gain a CSRF step; security config differs. |
| OD-003 | Duplicate-email response vs. account enumeration | FR-4, AC-002, §6.1, §8, SEC-8 | If a neutral/non-disclosing response is chosen, AC-002's observable outcome, the status code, the error body, and the duplicate-email tests all change. |
| OD-004 | Fields in the `201` response body | FR-8, AC-005 | Response DTO field list in API design and the success-path assertions change. |
| OD-005 | Anti-abuse controls on registration | §7, §10 | If in scope, new functional + security requirements and tests are added. |
| OD-006 | Email normalization mechanism | §6.1, DB design | DB_DESIGN chooses; entity/`schema.sql`/repository query differ, but no change to externally observable behavior. |

None of these prevent stating the mandatory requirements — each is captured as a
documented gap with a draft assumption. Verdict is therefore not `BLOCKED`.

## 12. Traceability

### 12.1 Acceptance Criterion → requirements

| AC | Functional requirement(s) | Validation / security rule(s) |
|---|---|---|
| AC-001 | FR-1, FR-5, FR-8, FR-9 | SEC-5; §6.1, §6.2 (valid input path) |
| AC-002 | FR-4 | §6.1 uniqueness; SEC-8; §8 duplicate-email row |
| AC-003 | FR-3 | §6.1 format + length |
| AC-004 | FR-5, FR-6, FR-7 | SEC-2, SEC-4; PC-9 |
| AC-005 | FR-7, FR-8 | SEC-3, SEC-4 |
| AC-006 | FR-3, FR-6 | §6.2 password policy |
| AC-007 | FR-2 | §8 `415` row |

### 12.2 Story AC → Specification AC

| Story | Specification |
|---|---|
| AC-001 Successful Registration | AC-001 |
| AC-002 Unique Email | AC-002 |
| AC-003 Email Validation | AC-003 |
| AC-004 Password Storage | AC-004 |
| AC-005 Secure Response | AC-005 |
| (derived) password policy enforcement | AC-006 |
| (derived) media type enforcement | AC-007 |

### 12.3 Requirement → source

| Requirement | Source |
|---|---|
| Public registration endpoint, `/api/v1/customers`, `201` + `Location` | `api-conventions.md` AC-1, AC-3, AC-4; SC-4 |
| BCrypt hashing, policy enforcement, no plaintext exposure | `security-conventions.md` SC-1, SC-9; NFR-001 |
| Case-insensitive unique email, one account per customer | `business-rules.md` BR-001, BR-002, BR-003 |
| Default role `CUSTOMER`, enabled account | BR-006, SC-2 |
| UTC timestamps, explicit column mapping, `password_hash VARCHAR(60)` | BR-007, PC-3–PC-6, PC-9 |
| AC-6 error body, single `@RestControllerAdvice` | `api-conventions.md` AC-5, AC-6, AC-9 |
| Layering, build stability, test coverage, traceability | `non-functional-requirements.md` NFR-005–NFR-008 |
