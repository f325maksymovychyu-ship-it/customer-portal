---
artifact_type: api_design
story: US-001
version: 1
status: DRAFT
created_at: 2026-08-31T08:18:54Z
updated_at: 2026-08-31T08:18:54Z
produced_by: openapi-designer
inputs:
  - path: docs/specifications/US-001-spec.md
    version: 1
  - path: docs/reviews/specifications/US-001-spec-review.md
    version: 1
  - path: docs/decisions/US-001-open-decisions.md
    version: 1
  - path: docs/architecture/api-conventions.md
    version: null
  - path: docs/architecture/security-conventions.md
    version: null
  - path: docs/product/business-rules.md
    version: null
  - path: docs/product/non-functional-requirements.md
    version: null
supersedes: null
---

# API Design — US-001 Customer Registration

Companion to `docs/designs/api/US-001-openapi.yaml` (the authoritative contract).
This document is the traceability anchor: rationale, operation notes,
Acceptance-Criterion map, auth model, error model, and open questions.

## 1. Scope

US-001 adds exactly one operation: `POST /api/v1/customers` — public
self-registration of a Customer account. No existing contract changes; this is
purely additive, so there is no compatibility concern (api-conventions.md AC-1).

`GET /api/v1/customers/{id}` is referenced only by the `Location` header of the
`201` response; it is **not** implemented by this Story (US-003 territory). The
header value is still correct and stable.

## 2. Open Decisions applied

| OD | Resolution | Effect on this contract |
|---|---|---|
| OD-001 | A — Jakarta `@Email` + maxLength 254 | `RegistrationRequest.email`: `format: email`, `maxLength: 254`, `minLength: 1` |
| OD-002 | B — CSRF-exempt for `POST /api/v1/customers` only | `security: []`, `x-csrf-exempt: true`; recorded here as the Story's architecture decision (SC-5). CSRF stays enabled for every other endpoint. |
| OD-003 | A — explicit `409` | `409` response with body message `"An account with this email already exists."` |
| OD-004 | A — `id, email, role, createdAt` | `CustomerResponse` field list |
| OD-005 | A — anti-abuse out of scope | No rate-limit headers, no `429` in this contract |
| OD-006 | A — lowercase-in-service + plain `UNIQUE` | No contract impact; `email` in `CustomerResponse` is the normalized (lowercase) value. DB_DESIGN owns the mechanism. |

## 3. Request-shape policy (resolves spec-review F-2 / Spec §6.3)

**Unknown or extra JSON properties in the request body are rejected with `400`**
(request-shape failure) — never silently ignored. Expressed in the contract as
`additionalProperties: false` on `RegistrationRequest`. Rationale: NFR-002
forbids relying on framework defaults for input handling; an explicit reject is
the stricter, more predictable contract and gives TEST_WRITING a deterministic
case. Implementation note (non-binding on the contract): Jackson
`FAIL_ON_UNKNOWN_PROPERTIES` = true, mapped to `400` by the
`@RestControllerAdvice`.

Malformed JSON → `400` with the AC-6 body (Spec §6.3).

## 4. Operation: `registerCustomer`

| Property | Value |
|---|---|
| Method / path | `POST /api/v1/customers` |
| Auth | None — public (SC-4 lists registration as the sole public endpoint) |
| CSRF | Exempt for this path only (OD-002:B, SC-5 architecture decision) |
| Request media type | `application/json` required; else `415` (AC-2) |
| Request body | `RegistrationRequest { email, password }`, `additionalProperties: false` |
| Idempotency | Not idempotent; a second identical call yields `409` once the first succeeds |
| Rate limiting | Out of scope (OD-005:A) |

### 4.1 Request schema `RegistrationRequest`

| Field | Type | Constraints | Source |
|---|---|---|---|
| `email` | string | required, `minLength 1`, `maxLength 254`, `format: email` (Jakarta `@Email` semantics) | Spec §6.1, OD-001:A |
| `password` | string, `writeOnly` | required, `minLength 12`, `maxLength 72`, must contain ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special | Spec §6.2, SC-1 |

- `password` is `writeOnly` — it never appears in any response schema (SC-1, SEC-3).
- The `maxLength: 72` on `password` is BCrypt's input bound. Downstream
  (TEST_WRITING, IMPLEMENTATION) treat it as **72 bytes** (spec-review F-5); the
  OpenAPI schema can only express a character count.
- Password policy is enforced twice: a custom request-layer constraint **and** a
  service re-check before hashing (FR-6, SC-1). The contract only states the
  observable rule.
- Validation messages for `password` must not echo the submitted value (SC-9).

### 4.2 Response schema `CustomerResponse` (`201` only)

| Field | Type | Notes |
|---|---|---|
| `id` | integer(int64) | surrogate id |
| `email` | string | normalized (lowercase) stored value |
| `role` | string enum `[CUSTOMER]` | always `CUSTOMER` (SC-2, BR-006) |
| `createdAt` | string(date-time) | UTC, JPA auditing |

No `password`, no `password_hash`, no `enabled`, no `updatedAt` (OD-004:A,
SEC-3, SEC-4).

### 4.3 Responses

| Status | Body | When |
|---|---|---|
| `201 Created` | `CustomerResponse` + `Location: /api/v1/customers/{id}` | valid input, email not taken (AC-001, AC-005) |
| `400 Bad Request` | `ErrorResponse` (+ `fieldErrors[]` for field failures) | bean-validation failure, malformed JSON, unknown JSON field (AC-003, AC-006) |
| `409 Conflict` | `ErrorResponse`, message `"An account with this email already exists."` | email already registered, case-insensitive (AC-002, OD-003:A) |
| `415 Unsupported Media Type` | `ErrorResponse` | missing/non-JSON `Content-Type` (AC-007) |
| `500 Internal Server Error` | `ErrorResponse` | unmapped exception; no internal leak (SC-9) |

`401` / `403` are **not applicable** — the endpoint is public (Spec §8).

## 5. Acceptance Criterion → operation / response map

| Spec AC | Operation | Observable outcome in contract |
|---|---|---|
| AC-001 Successful registration | `registerCustomer` | `201` + `Location` header + `CustomerResponse` with `role = CUSTOMER` |
| AC-002 Unique email | `registerCustomer` | `409` with the OD-003:A message; no second resource |
| AC-003 Email validation | `registerCustomer` | `400` + `fieldErrors[].field = "email"` |
| AC-004 Password storage | (not API-observable) | enforced server-side; contract guarantees `password` is `writeOnly` and absent from all responses |
| AC-005 Secure response | `registerCustomer` | `CustomerResponse` schema excludes password and hash |
| AC-006 Password policy | `registerCustomer` | `400` + `fieldErrors[].field = "password"` |
| AC-007 Media type | `registerCustomer` | `415` when `Content-Type` is not `application/json` |

Every API-relevant AC is covered. AC-004 is a persistence guarantee with no
direct HTTP surface; the contract supports it negatively (no credential field
anywhere in the response schemas).

## 6. Auth model

- **Authentication:** none for `POST /api/v1/customers`. Every other endpoint
  remains deny-by-default (SC-4); this Story adds no other route.
- **Authorization:** none (`x-authorization: none`). No role or ownership check.
- **CSRF:** disabled for this single path (OD-002:B). This document is the
  recorded architecture decision required by SC-5. Session/browser endpoints
  keep CSRF enabled.
- **Session:** the endpoint creates no session and requires none.
- **Transport:** standard project transport posture; unchanged by this Story.

## 7. Error model

- Single JSON error shape from api-conventions.md AC-6: `timestamp`, `status`,
  `error`, `message`, `path`, optional `fieldErrors[]`.
- Produced only by the single `@RestControllerAdvice` in the `exception`
  package (AC-9, FR-10). Controllers never build error bodies.
- `message` is client-safe; never contains stack traces, SQL, class/package
  names, file paths, DB URLs, or the submitted password (SC-9, SEC-6).
- `fieldErrors[]` entries are `{ field, message }`; `field` is the JSON field
  name (`email` / `password`).
- Duplicate-email (`409`) is a deliberate, human-approved account-enumeration
  exposure (OD-003:A / SEC-8), not a default.

## 8. Conventions compliance checklist

| Convention | Status |
|---|---|
| AC-1 URI-path versioning (`/api/v1`) | ✅ `servers: /api/v1`, path `/customers` |
| AC-2 `application/json`, `415` otherwise | ✅ |
| AC-3 plural noun, `camelCase` JSON fields, no verbs | ✅ `/customers`, `createdAt` |
| AC-4 `POST /collection` → `201` + `Location` + body | ✅ |
| AC-5 error codes | ✅ `400/409/415/500` |
| AC-6 error body shape | ✅ `ErrorResponse` + `FieldError` |
| AC-7 session auth, no `Authorization` header | ✅ public endpoint, no header |
| AC-8 pagination | n/a — no collection returned by this Story |
| AC-9 single `@RestControllerAdvice` | ✅ stated in §7 (implementation obligation) |
| SC-1 BCrypt, dual-layer policy, plaintext only inbound | ✅ `writeOnly` password, §4.1 |
| SC-2 role `CUSTOMER`, enabled | ✅ `role` enum, §4.2 |
| SC-4 deny-by-default, registration public | ✅ §6 |
| SC-5 CSRF exemption recorded | ✅ §6, OD-002:B |
| SC-9 error/log hygiene | ✅ §7 |

## 9. Open questions for downstream stages

| # | For | Question |
|---|---|---|
| Q-1 | DB_DESIGN | Email normalization mechanism (OD-006:A says lowercase-in-service + plain `UNIQUE`); confirm the column length is 254 (OD-001:A) and reconcile the entity name (`Customer` vs glossary `Account`, spec-review F-6). |
| Q-2 | DB_DESIGN | Final `password_hash` column definition (persistence-conventions.md PC-9 cites `VARCHAR(60)`; BCrypt output is 60 chars) — spec-review F-4. |
| Q-3 | IMPLEMENTATION | Confirm Jackson `FAIL_ON_UNKNOWN_PROPERTIES` is enabled and mapped to `400` (§3). |
| Q-4 | TEST_WRITING | Password max is 72 **bytes** for multi-byte input (spec-review F-5), not 72 characters as the schema states. |

None of these block the contract.

## 10. Result

```yaml
result:
  verdict: PASS
  stage: API_DESIGN
  story: US-001
  artifact_status: DRAFT
  artifacts:
    - docs/designs/api/US-001-openapi.yaml
    - docs/designs/api/US-001-api-design.md
  next_stage: DB_DESIGN
  loop_back_stage: null
  blocking_issues: []
  non_blocking_findings:
    - "F-2 resolved: unknown/extra JSON request fields are rejected with 400 (additionalProperties: false); recorded in api_design §3."
    - "Q-1: DB_DESIGN to confirm email column length 254 and reconcile entity name Customer vs glossary Account (spec-review F-6)."
    - "Q-2: DB_DESIGN owns final password_hash column definition (spec-review F-4, PC-9 VARCHAR(60))."
    - "Q-4: password 72-limit is BCrypt bytes, not characters (spec-review F-5); OpenAPI expresses it as maxLength 72 characters only."
    - "Location header targets GET /api/v1/customers/{id}, which US-001 does not implement (future Story)."
```
